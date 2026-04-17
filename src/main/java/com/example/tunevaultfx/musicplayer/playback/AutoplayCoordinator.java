package com.example.tunevaultfx.musicplayer.playback;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashSet;
import java.util.Set;

/**
 * Suggestion tail when playlist context ends: primes “up next”, continues playback, and tops up the
 * buffer. Extracted from {@link com.example.tunevaultfx.musicplayer.controller.MusicPlayerController}
 * to keep that class smaller.
 */
public final class AutoplayCoordinator {

    public interface Host {
        PlaybackState state();

        PlaybackLifecycleService lifecycle();

        RecommendationService recommendations();

        ObservableList<Song> activePlaylistSongs();

        int activePlaylistIndex();

        void setActivePlaylistIndex(int index);

        void clearActivePlaylist();

        ObservableList<Song> userQueue();

        void stopPlayback();
    }

    private static final int TARGET_AUTOPLAY_BUFFER = 12;
    private static final int AUTOPLAY_TOPUP_BATCH = 10;

    private final Host host;
    private final ObservableList<Song> suggestions = FXCollections.observableArrayList();
    private int suggestionIndex = -1;
    private boolean playingSuggestions;

    public AutoplayCoordinator(Host host) {
        this.host = host;
    }

    public ObservableList<Song> suggestionsList() {
        return suggestions;
    }

    public boolean isPlayingSuggestions() {
        return playingSuggestions;
    }

    public void setPlayingSuggestions(boolean playingSuggestions) {
        this.playingSuggestions = playingSuggestions;
    }

    public int suggestionIndex() {
        return suggestionIndex;
    }

    public void setSuggestionIndex(int suggestionIndex) {
        this.suggestionIndex = suggestionIndex;
    }

    public void clearSuggestionContext() {
        playingSuggestions = false;
        suggestions.clear();
        suggestionIndex = -1;
    }

    public void primeUpNextIfNoPlaylistTail() {
        if (playingSuggestions) {
            return;
        }
        boolean hasPlaylistTail =
                !host.activePlaylistSongs().isEmpty()
                        && host.activePlaylistIndex() >= 0
                        && host.activePlaylistIndex() + 1 < host.activePlaylistSongs().size();
        if (hasPlaylistTail) {
            return;
        }

        ObservableList<Song> source =
                host.activePlaylistSongs().isEmpty()
                        ? (host.state().getCurrentSong() != null
                                ? FXCollections.observableArrayList(host.state().getCurrentSong())
                                : FXCollections.observableArrayList())
                        : host.activePlaylistSongs();

        if (source.isEmpty()) {
            return;
        }

        String playlistName = host.state().getCurrentSourcePlaylistName();
        if (playlistName == null) {
            playlistName = "";
        }

        ObservableList<Song> next =
                host.recommendations()
                        .getSuggestedSongsForPlaylist(
                                SessionManager.getCurrentUsername(),
                                playlistName,
                                source,
                                Math.max(8, TARGET_AUTOPLAY_BUFFER));

        if (next.isEmpty()) {
            next =
                    host.recommendations()
                            .getSuggestedSongsForUser(SessionManager.getCurrentUsername(), 10);
        }
        if (next.isEmpty()) {
            next =
                    host.recommendations()
                            .getAutoplayContinuation(
                                    SessionManager.getCurrentUsername(),
                                    host.state().getCurrentSong(),
                                    collectExcludeIdsForTopUp(),
                                    Math.max(TARGET_AUTOPLAY_BUFFER, AUTOPLAY_TOPUP_BATCH));
        }

        Song cur = host.state().getCurrentSong();
        if (cur != null && cur.songId() > 0) {
            next.removeIf(s -> s != null && s.songId() == cur.songId());
        }

        if (next.isEmpty()) {
            return;
        }

        suggestions.setAll(next);
        suggestionIndex = -1;
    }

    public boolean beginFromPrimedBufferIfPresent() {
        if (suggestions.isEmpty() || suggestionIndex >= 0) {
            return false;
        }
        host.clearActivePlaylist();
        playingSuggestions = true;
        suggestionIndex = 0;
        host.lifecycle().playSingleSong(suggestions.get(0));
        topUpBuffer();
        return true;
    }

    public void startSuggestions() {
        ObservableList<Song> source =
                host.activePlaylistSongs().isEmpty()
                        ? (host.state().getCurrentSong() != null
                                ? FXCollections.observableArrayList(host.state().getCurrentSong())
                                : FXCollections.observableArrayList())
                        : host.activePlaylistSongs();

        if (source.isEmpty()) {
            fetchGlobalRecommendations();
            return;
        }

        ObservableList<Song> next =
                host.recommendations()
                        .getSuggestedSongsForPlaylist(
                                SessionManager.getCurrentUsername(),
                                host.state().getCurrentSourcePlaylistName(),
                                source,
                                8);

        if (next.isEmpty()) {
            fetchGlobalRecommendations();
            return;
        }

        suggestions.clear();
        suggestions.addAll(next);
        suggestionIndex = 0;
        playingSuggestions = true;
        host.lifecycle().playSingleSong(suggestions.get(0));
        topUpBuffer();
    }

    private void fetchGlobalRecommendations() {
        String user = SessionManager.getCurrentUsername();
        ObservableList<Song> global = host.recommendations().getSuggestedSongsForUser(user, 10);

        if (global.isEmpty()) {
            global =
                    host.recommendations()
                            .getAutoplayContinuation(
                                    user,
                                    host.state().getCurrentSong(),
                                    collectExcludeIdsForTopUp(),
                                    Math.max(TARGET_AUTOPLAY_BUFFER, AUTOPLAY_TOPUP_BATCH));
        }

        if (global.isEmpty()) {
            Song anchor = host.state().getCurrentSong();
            if (anchor != null) {
                suggestions.clear();
                suggestions.add(anchor);
                suggestionIndex = 0;
                playingSuggestions = true;
                host.lifecycle().playSingleSong(anchor);
            } else {
                host.stopPlayback();
            }
            return;
        }

        suggestions.clear();
        suggestions.addAll(global);
        suggestionIndex = 0;
        playingSuggestions = true;
        host.lifecycle().playSingleSong(suggestions.get(0));
        topUpBuffer();
    }

    public void playNextSuggestion() {
        if (suggestions.isEmpty()) {
            startSuggestions();
            return;
        }

        if (suggestionIndex < suggestions.size() - 1) {
            suggestionIndex++;
            host.lifecycle().playSingleSong(suggestions.get(suggestionIndex));
            topUpBuffer();
        } else {
            startSuggestions();
        }
    }

    public void topUpBuffer() {
        if (!playingSuggestions) {
            return;
        }

        int upcoming = suggestions.size() - (suggestionIndex + 1);
        if (upcoming >= TARGET_AUTOPLAY_BUFFER) {
            return;
        }

        int need = TARGET_AUTOPLAY_BUFFER - upcoming;
        ObservableList<Song> more =
                host.recommendations()
                        .getAutoplayContinuation(
                                SessionManager.getCurrentUsername(),
                                host.state().getCurrentSong(),
                                collectExcludeIdsForTopUp(),
                                Math.max(need, AUTOPLAY_TOPUP_BATCH));

        for (Song s : more) {
            if (s != null && s.songId() > 0) {
                suggestions.add(s);
            }
        }
    }

    private Set<Integer> collectExcludeIdsForTopUp() {
        HashSet<Integer> ids = new HashSet<>();
        Song cur = host.state().getCurrentSong();
        if (cur != null && cur.songId() > 0) {
            ids.add(cur.songId());
        }
        for (Song s : host.userQueue()) {
            if (s != null && s.songId() > 0) {
                ids.add(s.songId());
            }
        }
        for (Song s : suggestions) {
            if (s != null && s.songId() > 0) {
                ids.add(s.songId());
            }
        }
        for (Song s : host.activePlaylistSongs()) {
            if (s != null && s.songId() > 0) {
                ids.add(s.songId());
            }
        }
        return ids;
    }

    public int upcomingCount() {
        if (playingSuggestions) {
            return Math.max(0, suggestions.size() - suggestionIndex - 1);
        }
        if (suggestionIndex < 0 && !suggestions.isEmpty()) {
            return suggestions.size();
        }
        return 0;
    }
}

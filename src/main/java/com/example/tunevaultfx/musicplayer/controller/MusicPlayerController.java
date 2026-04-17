package com.example.tunevaultfx.musicplayer.controller;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ListeningEventDAO;
import com.example.tunevaultfx.musicplayer.ListeningSessionTracker;
import com.example.tunevaultfx.musicplayer.ShuffleManager;
import com.example.tunevaultfx.musicplayer.playback.AutoplayCoordinator;
import com.example.tunevaultfx.musicplayer.playback.PlaybackLifecycleService;
import com.example.tunevaultfx.musicplayer.playback.PlaybackNavigator;
import com.example.tunevaultfx.musicplayer.playback.PlaybackQueue;
import com.example.tunevaultfx.musicplayer.playback.PlaybackState;
import com.example.tunevaultfx.playlist.service.PlaylistService;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserLibraryService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates shared playback logic for the whole application.
 */
public class MusicPlayerController {

    private static final MusicPlayerController instance = new MusicPlayerController();

    public static MusicPlayerController getInstance() {
        return instance;
    }

    private final PlaybackState state = new PlaybackState();
    private final PlaybackQueue queue = new PlaybackQueue();
    private final ShuffleManager shuffleManager = new ShuffleManager();
    private final UserLibraryService libraryService = new UserLibraryService();
    private final ListeningEventDAO listeningEventDAO = new ListeningEventDAO();
    private final ListeningSessionTracker sessionTracker = new ListeningSessionTracker(listeningEventDAO);
    private final PlaylistService playlistService = new PlaylistService();
    private final RecommendationService recommendationService = new RecommendationService();

    private final BooleanProperty expandedPlayerVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty currentSongLiked = new SimpleBooleanProperty(false);

    /**
     * While true, {@link #currentSongLikedProperty()} is being synced to the new current track
     * (not a user like/unlike). UI that reacts to heart changes should ignore side effects.
     */
    private boolean applyingTrackDerivedLikedState;

    private final PlaybackLifecycleService lifecycleService;
    private final PlaybackNavigator playbackNavigator;
    private final Timeline timeline;

    private ObservableList<Song> activePlaylistSongs = FXCollections.observableArrayList();
    private int activePlaylistIndex = -1;

    private final AutoplayCoordinator autoplay;

    private final ObservableList<Song> userQueue = FXCollections.observableArrayList();

    /** User shuffle preference per playlist name (Liked Songs and library playlists). */
    private final ConcurrentHashMap<String, Boolean> playlistShufflePreference = new ConcurrentHashMap<>();

    /** Used when a playlist name has no explicit entry in {@link #playlistShufflePreference}. */
    private volatile boolean defaultShufflePreference;

    /**
     * Last non-empty playlist name from {@link #playQueue}; kept while suggestion autoplay clears
     * {@link PlaybackState#getCurrentSourcePlaylistName()} so resume/previous can restore shuffle prefs.
     */
    private String lastKnownPlaylistQueueName = "";

    private final ReadOnlyIntegerWrapper shufflePreferenceRevision = new ReadOnlyIntegerWrapper(0);

    private MusicPlayerController() {
        PlaybackLifecycleService.PlayerRuntime lifecycleRuntime = new PlaybackLifecycleService.PlayerRuntime() {
            @Override
            public void setPlaying(boolean playing) {
                MusicPlayerController.this.setPlaying(playing);
            }

            @Override
            public void loadCurrentQueueSong() {
                MusicPlayerController.this.loadCurrentQueueSong();
            }

            @Override
            public void startSessionForCurrentSong() {
                MusicPlayerController.this.startSessionForCurrentSong();
            }
        };

        PlaybackNavigator.PlayerRuntime navigatorRuntime = new PlaybackNavigator.PlayerRuntime() {
            @Override
            public void setPlaying(boolean playing) {
                MusicPlayerController.this.setPlaying(playing);
            }

            @Override
            public void loadCurrentQueueSong() {
                MusicPlayerController.this.loadCurrentQueueSong();
            }

            @Override
            public void startSessionForCurrentSong() {
                MusicPlayerController.this.startSessionForCurrentSong();
            }

            @Override
            public void stopPlayer() {
                MusicPlayerController.this.stop();
            }
        };

        lifecycleService = new PlaybackLifecycleService(
                state,
                queue,
                shuffleManager,
                sessionTracker,
                lifecycleRuntime
        );

        playbackNavigator = new PlaybackNavigator(
                state,
                queue,
                shuffleManager,
                sessionTracker,
                navigatorRuntime
        );

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);

        autoplay =
                new AutoplayCoordinator(
                        new AutoplayCoordinator.Host() {
                            @Override
                            public PlaybackState state() {
                                return MusicPlayerController.this.state;
                            }

                            @Override
                            public PlaybackLifecycleService lifecycle() {
                                return MusicPlayerController.this.lifecycleService;
                            }

                            @Override
                            public RecommendationService recommendations() {
                                return MusicPlayerController.this.recommendationService;
                            }

                            @Override
                            public ObservableList<Song> activePlaylistSongs() {
                                return MusicPlayerController.this.activePlaylistSongs;
                            }

                            @Override
                            public int activePlaylistIndex() {
                                return MusicPlayerController.this.activePlaylistIndex;
                            }

                            @Override
                            public void setActivePlaylistIndex(int index) {
                                MusicPlayerController.this.activePlaylistIndex = index;
                            }

                            @Override
                            public void clearActivePlaylist() {
                                MusicPlayerController.this.activePlaylistSongs.clear();
                                MusicPlayerController.this.activePlaylistIndex = -1;
                            }

                            @Override
                            public ObservableList<Song> userQueue() {
                                return MusicPlayerController.this.userQueue;
                            }

                            @Override
                            public void stopPlayback() {
                                MusicPlayerController.this.stop();
                            }
                        });

        // Keep liked-state UI in sync whenever the currently playing song changes.
        state.currentSongProperty().addListener((obs, oldSong, newSong) -> refreshCurrentSongLikedFromTrackChange());
    }

    public void playQueue(ObservableList<Song> songs, int index) {
        playQueue(songs, index, "");
    }

    public void playQueue(ObservableList<Song> songs, int index, String playlistName) {
        activePlaylistSongs = songs == null ? FXCollections.observableArrayList() : FXCollections.observableArrayList(songs);
        activePlaylistIndex = index;
        autoplay.clearSuggestionContext();

        prepareShuffleForIncomingQueue(playlistName);
        lifecycleService.playQueue(songs, index, playlistName);
        autoplay.primeUpNextIfNoPlaylistTail();
    }

    public void playSingleSong(Song song) {
        if (song == null) {
            return;
        }
        Song cur = state.getCurrentSong();
        boolean singleContext =
                activePlaylistSongs.isEmpty()
                        && activePlaylistIndex < 0
                        && !autoplay.isPlayingSuggestions();
        if (singleContext
                && cur != null
                && cur.songId() == song.songId()) {
            togglePlayPause();
            return;
        }

        activePlaylistSongs.clear();
        activePlaylistIndex = -1;
        autoplay.clearSuggestionContext();

        lastKnownPlaylistQueueName = "";
        lifecycleService.playSingleSong(song);
        autoplay.primeUpNextIfNoPlaylistTail();
    }

    public void togglePlayPause() {
        if (state.getCurrentSong() == null) {
            return;
        }
        setPlaying(!state.isPlaying());
    }

    public void next() {
        if (!userQueue.isEmpty()) {
            playNextFromUserQueue();
            return;
        }

        if (autoplay.isPlayingSuggestions()) {
            autoplay.playNextSuggestion();
            return;
        }

        if (!activePlaylistSongs.isEmpty() && activePlaylistIndex >= 0) {
            if (activePlaylistIndex < activePlaylistSongs.size() - 1) {
                activePlaylistIndex++;
                playbackNavigator.next();
                return;
            }

            if (!state.isLoopEnabled()) {
                if (!autoplay.beginFromPrimedBufferIfPresent()) {
                    autoplay.startSuggestions();
                }
                return;
            }
        }

        // When a single song finishes with no queue context, start autoplay
        // instead of silently stopping. This covers songs played from search,
        // recent searches, and any other one-off play.
        if (state.getCurrentSong() != null && !state.isLoopEnabled()) {
            if (!autoplay.beginFromPrimedBufferIfPresent()) {
                autoplay.startSuggestions();
            }
            return;
        }

        playbackNavigator.next();
    }

    public void previous() {
        if (autoplay.isPlayingSuggestions()) {
            if (autoplay.suggestionIndex() > 0) {
                autoplay.setSuggestionIndex(autoplay.suggestionIndex() - 1);
                Song previousSuggestion =
                        autoplay.suggestionsList().get(autoplay.suggestionIndex());
                lifecycleService.playSingleSong(previousSuggestion);
                return;
            }

            autoplay.setPlayingSuggestions(false);
            autoplay.setSuggestionIndex(-1);

            if (!activePlaylistSongs.isEmpty()) {
                activePlaylistIndex = activePlaylistSongs.size() - 1;
                String resumeName = resolvePlaylistNameForQueuePlayback(state.getCurrentSourcePlaylistName());
                prepareShuffleForIncomingQueue(resumeName);
                lifecycleService.playQueue(activePlaylistSongs, activePlaylistIndex, resumeName);
                return;
            }
        }

        if (!activePlaylistSongs.isEmpty() && activePlaylistIndex > 0) {
            activePlaylistIndex--;
        }

        playbackNavigator.previous();
    }

    public void seek(int second) {
        if (state.getCurrentSong() == null) {
            return;
        }

        int clamped = Math.max(0, Math.min(second, state.getCurrentDuration()));
        state.setCurrentSecond(clamped);
    }

    public void stop() {
        lastKnownPlaylistQueueName = "";
        lifecycleService.stop();
        timeline.stop();
        setExpandedPlayerVisible(false);
        currentSongLiked.set(false);
        autoplay.clearSuggestionContext();
    }

    public void onSongRemovedFromPlaylist(String playlistName, Song removedSong) {
        lifecycleService.onSongRemovedFromPlaylist(playlistName, removedSong);
        refreshCurrentSongLiked();

        if (removedSong != null && !activePlaylistSongs.isEmpty()) {
            activePlaylistSongs.removeIf(song -> song.songId() == removedSong.songId());
            if (activePlaylistIndex >= activePlaylistSongs.size()) {
                activePlaylistIndex = activePlaylistSongs.size() - 1;
            }
        }
    }

    public void toggleLikeCurrentSong() {
        Song currentSong = state.getCurrentSong();
        if (currentSong == null) {
            return;
        }

        playlistService.toggleLikeSong(currentSong);
        refreshCurrentSongLiked();
    }

    public void resetForNewSession() {
        stop();
        queue.clear();
        shuffleManager.reset();
        sessionTracker.reset();

        state.setCurrentSong(null);
        state.setCurrentSourcePlaylistName("");
        state.setCurrentSecond(0);
        state.setPlaying(false);

        activePlaylistSongs.clear();
        activePlaylistIndex = -1;
        autoplay.clearSuggestionContext();
        userQueue.clear();

        playlistShufflePreference.clear();
        defaultShufflePreference = false;
        lastKnownPlaylistQueueName = "";
        shufflePreferenceRevision.set(0);

        currentSongLiked.set(false);
        expandedPlayerVisible.set(false);
    }

    public ObservableList<Song> getAutoplaySuggestionsForCurrentPlaylist(int limit) {
        if (activePlaylistSongs.isEmpty()) {
            return FXCollections.observableArrayList();
        }

        return recommendationService.getSuggestedSongsForPlaylist(
                SessionManager.getCurrentUsername(),
                state.getCurrentSourcePlaylistName(),
                activePlaylistSongs,
                limit);
    }

    public boolean isCurrentSongLiked() {
        return currentSongLiked.get();
    }

    public BooleanProperty currentSongLikedProperty() {
        return currentSongLiked;
    }

    /**
     * Call when the now-playing song changes so the heart matches that track.
     * Does not represent a user like action — see {@link #isApplyingTrackDerivedLikedState()}.
     */
    private void refreshCurrentSongLikedFromTrackChange() {
        applyingTrackDerivedLikedState = true;
        try {
            currentSongLiked.set(libraryService.isLiked(state.getCurrentSong()));
        } finally {
            applyingTrackDerivedLikedState = false;
        }
    }

    /**
     * True during {@link #currentSongLikedProperty()} updates triggered by a track change.
     */
    public boolean isApplyingTrackDerivedLikedState() {
        return applyingTrackDerivedLikedState;
    }

    /** Refresh heart from the library (user toggled like, playlist changed, etc.). */
    public void refreshCurrentSongLiked() {
        currentSongLiked.set(libraryService.isLiked(state.getCurrentSong()));
    }

    public void addCurrentSongToPlaylist(String playlistName) {
        Song currentSong = state.getCurrentSong();
        if (currentSong == null || playlistName == null || playlistName.isBlank()) {
            return;
        }

        libraryService.addSongToPlaylist(playlistName, currentSong);
    }

    public void toggleLoop() {
        state.setLoopEnabled(!state.isLoopEnabled());
    }

    public void toggleShuffle() {
        String src = normalizedPlaylistKey(state.getCurrentSourcePlaylistName());
        if (src.isEmpty()) {
            return;
        }
        boolean next = !playlistShufflePreference.getOrDefault(src, defaultShufflePreference);
        playlistShufflePreference.put(src, next);
        bumpShufflePreferenceRevision();
        setShuffleEnabled(next);
    }

    /**
     * Default shuffle for playlist names that have no explicit preference yet (settings + login).
     */
    public void setDefaultShufflePreference(boolean enabled) {
        defaultShufflePreference = enabled;
        String src = normalizedPlaylistKey(state.getCurrentSourcePlaylistName());
        if (!src.isEmpty() && !playlistShufflePreference.containsKey(src)) {
            setShuffleEnabled(enabled);
        }
        bumpShufflePreferenceRevision();
    }

    public boolean getShufflePreferenceForPlaylist(String playlistName) {
        String key = normalizedPlaylistKey(playlistName);
        if (key.isEmpty()) {
            return false;
        }
        return playlistShufflePreference.getOrDefault(key, defaultShufflePreference);
    }

    public void toggleShufflePreferenceForPlaylist(String playlistName) {
        String key = normalizedPlaylistKey(playlistName);
        if (key.isEmpty()) {
            return;
        }
        boolean next = !playlistShufflePreference.getOrDefault(key, defaultShufflePreference);
        playlistShufflePreference.put(key, next);
        bumpShufflePreferenceRevision();
        if (key.equals(normalizedPlaylistKey(state.getCurrentSourcePlaylistName()))) {
            setShuffleEnabled(next);
        }
    }

    public ReadOnlyIntegerProperty shufflePreferenceRevisionProperty() {
        return shufflePreferenceRevision.getReadOnlyProperty();
    }

    /**
     * Mini/expanded shuffle highlight: on only when playback is tied to a named queue source
     * (not suggestion/single-track contexts where the property may be stale).
     */
    public boolean isShuffleActiveForCurrentPlayback() {
        return !normalizedPlaylistKey(state.getCurrentSourcePlaylistName()).isEmpty()
                && state.isShuffleEnabled();
    }

    /** Applies shuffle on/off and refreshes the shuffle order when turning on. */
    public void setShuffleEnabled(boolean enabled) {
        String src = normalizedPlaylistKey(state.getCurrentSourcePlaylistName());
        if (src.isEmpty()) {
            if (!enabled && state.isShuffleEnabled()) {
                state.setShuffleEnabled(false);
                shuffleManager.reset();
            }
            return;
        }
        if (state.isShuffleEnabled() == enabled) {
            if (!enabled) {
                shuffleManager.reset();
            }
            return;
        }
        state.setShuffleEnabled(enabled);
        if (enabled) {
            if (!queue.isEmpty() && queue.getCurrentIndex() >= 0) {
                shuffleManager.createShuffleOrderStartingFrom(queue.size(), queue.getCurrentIndex());
            }
        } else {
            shuffleManager.reset();
        }
    }

    public BooleanProperty expandedPlayerVisibleProperty() {
        return expandedPlayerVisible;
    }

    public boolean isExpandedPlayerVisible() {
        return expandedPlayerVisible.get();
    }

    public void setExpandedPlayerVisible(boolean visible) {
        expandedPlayerVisible.set(visible);
    }

    private void playNextFromUserQueue() {
        Song next = userQueue.remove(0);
        activePlaylistSongs.clear();
        activePlaylistIndex = -1;
        autoplay.clearSuggestionContext();
        lastKnownPlaylistQueueName = "";
        lifecycleService.playSingleSong(next);
        autoplay.primeUpNextIfNoPlaylistTail();
    }

    /**
     * Observable tail used by the queue panel; mutations always go through this list.
     */
    public ObservableList<Song> getAutoplaySuggestionsList() {
        return autoplay.suggestionsList();
    }

    // ── User queue management ─────────────────────────────────────

    public ObservableList<Song> getUserQueue() {
        return userQueue;
    }

    public void addToQueueNext(Song song) {
        if (song != null) {
            userQueue.add(0, song);
        }
    }

    /** Appends all songs to the manual play-next queue (after existing user-queued items). */
    public void addSongsToUserQueueEnd(ObservableList<Song> songs) {
        if (songs == null) {
            return;
        }
        for (Song s : songs) {
            if (s != null) {
                userQueue.add(s);
            }
        }
    }

    /** Call when a playlist is removed from the library while it might be the playback source. */
    public void onPlaylistDeleted(String playlistName) {
        if (playlistName == null || playlistName.isBlank()) {
            return;
        }
        String key = normalizedPlaylistKey(playlistName);
        playlistShufflePreference.remove(key);
        bumpShufflePreferenceRevision();
        if (key.equals(normalizedPlaylistKey(state.getCurrentSourcePlaylistName()))) {
            stop();
        }
    }

    public void removeFromQueue(int index) {
        if (index >= 0 && index < userQueue.size()) {
            userQueue.remove(index);
        }
    }

    public void moveInQueue(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= userQueue.size()) return;
        if (toIndex < 0 || toIndex >= userQueue.size()) return;
        Song song = userQueue.remove(fromIndex);
        userQueue.add(toIndex, song);
    }

    /**
     * Moves a play-next item to sit before {@code insertBeforeIndex} in the user queue (0 = before
     * first, userQueue.size() = after last). Indices are pre-move.
     */
    private void reorderUserQueueInsertBefore(int fromIndex, int insertBeforeIndex) {
        if (fromIndex < 0 || fromIndex >= userQueue.size()) {
            return;
        }
        if (insertBeforeIndex < 0 || insertBeforeIndex > userQueue.size()) {
            return;
        }
        if (insertBeforeIndex == fromIndex) {
            return;
        }
        Song song = userQueue.remove(fromIndex);
        int i = insertBeforeIndex;
        if (fromIndex < insertBeforeIndex) {
            i--;
        }
        userQueue.add(i, song);
    }

    /** How many upcoming tracks come from the active playlist (after the current song). */
    public int getPlaylistUpcomingCount() {
        if (activePlaylistSongs.isEmpty() || activePlaylistIndex < 0) {
            return 0;
        }
        return Math.max(0, activePlaylistSongs.size() - activePlaylistIndex - 1);
    }

    /** How many upcoming tracks are shown from autoplay / primed suggestions. */
    public int getAutoplayUpcomingCount() {
        return autoplay.upcomingCount();
    }

    /**
     * Reorders one upcoming row to another within the same segment (play-next, playlist tail, or
     * autoplay tail). Display indices match {@link #getUpcomingQueueSnapshot()} order.
     */
    /** True if both display indices refer to the same upcoming bucket (play-next, playlist, autoplay). */
    public boolean isSameUpcomingSegment(int displayA, int displayB) {
        return upcomingSegment(displayA) == upcomingSegment(displayB);
    }

    private int upcomingSegment(int displayIndex) {
        int uq = userQueue.size();
        int pl = getPlaylistUpcomingCount();
        if (displayIndex < uq) {
            return 0;
        }
        if (displayIndex < uq + pl) {
            return 1;
        }
        return 2;
    }

    /**
     * Reorders within one upcoming segment. {@code insertBeforeDisplay} is the unified upcoming
     * index before which the dragged row should appear (0 .. total). Use the row index for
     * &quot;insert above&quot;, or row+1 for &quot;insert below&quot;.
     */
    public void reorderUpcomingSnapshot(int fromDisplay, int insertBeforeDisplay) {
        if (fromDisplay < 0 || insertBeforeDisplay < 0) {
            return;
        }
        int uq = userQueue.size();
        int pl = getPlaylistUpcomingCount();
        int ap = getAutoplayUpcomingCount();
        int total = uq + pl + ap;
        if (fromDisplay >= total || insertBeforeDisplay > total) {
            return;
        }

        if (fromDisplay < uq) {
            if (insertBeforeDisplay <= uq) {
                reorderUserQueueInsertBefore(fromDisplay, insertBeforeDisplay);
            }
            return;
        }
        if (fromDisplay < uq + pl) {
            if (insertBeforeDisplay >= uq && insertBeforeDisplay <= uq + pl) {
                int base = activePlaylistIndex + 1;
                int relFrom = fromDisplay - uq;
                int absFrom = base + relFrom;
                int absInsertBefore = base + (insertBeforeDisplay - uq);
                if (absFrom < base
                        || absFrom >= activePlaylistSongs.size()
                        || absInsertBefore < base
                        || absInsertBefore > activePlaylistSongs.size()) {
                    return;
                }
                Song moved = activePlaylistSongs.remove(absFrom);
                int i = absInsertBefore;
                if (absFrom < absInsertBefore) {
                    i--;
                }
                activePlaylistSongs.add(i, moved);
                lifecycleService.resyncPlaylistQueueOrder(activePlaylistSongs, activePlaylistIndex);
            }
            return;
        }
        if (fromDisplay >= uq + pl) {
            var sug = autoplay.suggestionsList();
            int base = autoplay.isPlayingSuggestions() ? autoplay.suggestionIndex() + 1 : 0;
            int segStart = uq + pl;
            if (insertBeforeDisplay >= segStart && insertBeforeDisplay <= segStart + ap) {
                int relFrom = fromDisplay - segStart;
                int absFrom = base + relFrom;
                int absInsertBefore = base + (insertBeforeDisplay - segStart);
                if (absFrom < base
                        || absFrom >= sug.size()
                        || absInsertBefore < base
                        || absInsertBefore > sug.size()) {
                    return;
                }
                Song moved = sug.remove(absFrom);
                int i = absInsertBefore;
                if (absFrom < absInsertBefore) {
                    i--;
                }
                sug.add(i, moved);
            }
        }
    }

    public void clearUserQueue() {
        userQueue.clear();
    }

    public ObservableList<Song> getUpcomingQueueSnapshot() {
        ObservableList<Song> upcoming = FXCollections.observableArrayList();

        upcoming.addAll(userQueue);

        if (!activePlaylistSongs.isEmpty() && activePlaylistIndex >= 0) {
            for (int i = activePlaylistIndex + 1; i < activePlaylistSongs.size(); i++) {
                upcoming.add(activePlaylistSongs.get(i));
            }
        }

        if (autoplay.isPlayingSuggestions()) {
            for (int i = autoplay.suggestionIndex() + 1; i < autoplay.suggestionsList().size(); i++) {
                upcoming.add(autoplay.suggestionsList().get(i));
            }
        } else if (autoplay.suggestionIndex() < 0 && !autoplay.suggestionsList().isEmpty()) {
            upcoming.addAll(autoplay.suggestionsList());
        }

        return upcoming;
    }

    /**
     * Returns how many songs the user manually added to the queue.
     */
    public int getUserQueueSize() {
        return userQueue.size();
    }

    /**
     * Returns the total count of all upcoming songs (user queue + playlist remaining + autoplay).
     */
    public int getFullQueueSize() {
        return getUpcomingQueueSnapshot().size();
    }

    /**
     * Plays a specific song from the unified upcoming queue by its index.
     * Index 0..userQueue.size()-1 = user queue items.
     * After that = playlist remaining items, then autoplay.
     * All songs before the target index are skipped/removed.
     */
    public void playFromUpcomingQueue(int index) {
        if (index < 0) return;

        int uqSize = userQueue.size();

        if (index < uqSize) {
            for (int i = 0; i < index; i++) {
                userQueue.remove(0);
            }
            playNextFromUserQueue();
            return;
        }

        userQueue.clear();
        int remaining = index - uqSize;

        if (!activePlaylistSongs.isEmpty() && activePlaylistIndex >= 0) {
            int playlistRemaining = activePlaylistSongs.size() - (activePlaylistIndex + 1);
            if (remaining < playlistRemaining) {
                activePlaylistIndex = activePlaylistIndex + 1 + remaining;
                autoplay.clearSuggestionContext();
                String resumeName = resolvePlaylistNameForQueuePlayback(state.getCurrentSourcePlaylistName());
                prepareShuffleForIncomingQueue(resumeName);
                lifecycleService.playQueue(activePlaylistSongs, activePlaylistIndex, resumeName);
                return;
            }
            remaining -= playlistRemaining;
        }

        if (autoplay.isPlayingSuggestions() && !autoplay.suggestionsList().isEmpty()) {
            int autoplayRemaining =
                    autoplay.suggestionsList().size() - (autoplay.suggestionIndex() + 1);
            if (remaining < autoplayRemaining) {
                autoplay.setSuggestionIndex(autoplay.suggestionIndex() + 1 + remaining);
                lifecycleService.playSingleSong(
                        autoplay.suggestionsList().get(autoplay.suggestionIndex()));
                autoplay.topUpBuffer();
                return;
            }
            remaining -= autoplayRemaining;
        }

        if (autoplay.suggestionIndex() < 0
                && !autoplay.suggestionsList().isEmpty()
                && remaining >= 0
                && remaining < autoplay.suggestionsList().size()) {
            activePlaylistSongs.clear();
            activePlaylistIndex = -1;
            autoplay.setPlayingSuggestions(true);
            autoplay.setSuggestionIndex(remaining);
            state.setCurrentSourcePlaylistName("");
            lifecycleService.playSingleSong(
                    autoplay.suggestionsList().get(autoplay.suggestionIndex()));
            autoplay.topUpBuffer();
        }
    }

    private static String normalizedPlaylistKey(String playlistName) {
        return playlistName == null ? "" : playlistName.trim();
    }

    private void prepareShuffleForIncomingQueue(String playlistName) {
        String key = normalizedPlaylistKey(playlistName);
        if (key.isEmpty()) {
            lastKnownPlaylistQueueName = "";
            state.setShuffleEnabled(false);
            return;
        }
        lastKnownPlaylistQueueName = key;
        boolean want = playlistShufflePreference.getOrDefault(key, defaultShufflePreference);
        state.setShuffleEnabled(want);
    }

    private String resolvePlaylistNameForQueuePlayback(String candidateFromState) {
        String c = normalizedPlaylistKey(candidateFromState);
        if (!c.isEmpty()) {
            return c;
        }
        return normalizedPlaylistKey(lastKnownPlaylistQueueName);
    }

    private void bumpShufflePreferenceRevision() {
        shufflePreferenceRevision.set(shufflePreferenceRevision.get() + 1);
    }

    private void setPlaying(boolean value) {
        state.setPlaying(value);

        if (value) {
            timeline.play();
        } else {
            timeline.pause();
        }
    }

    private void tick() {
        if (state.getCurrentSong() == null || !state.isPlaying()) {
            return;
        }

        state.setCurrentSecond(state.getCurrentSecond() + 1);
        sessionTracker.tick(SessionManager.getCurrentUsername(), state.getCurrentSong());

        if (autoplay.isPlayingSuggestions()) {
            autoplay.topUpBuffer();
        }

        if (state.getCurrentSecond() >= state.getCurrentDuration()) {
            next();
        }
    }

    private void loadCurrentQueueSong() {
        Song song = queue.getCurrentSong();
        state.setCurrentSong(song);
        String src = queue.getSourcePlaylistName();
        String key = normalizedPlaylistKey(src);
        if (!key.isEmpty()) {
            lastKnownPlaylistQueueName = key;
        }
        state.setCurrentSourcePlaylistName(src);
        // Liked state: handled by currentSongProperty listener → refreshCurrentSongLikedFromTrackChange
    }

    private void startSessionForCurrentSong() {
        sessionTracker.startSession(SessionManager.getCurrentUsername(), state.getCurrentSong());
    }

    public Song getCurrentSong() {
        return state.getCurrentSong();
    }

    public boolean isLoopEnabled() {
        return state.isLoopEnabled();
    }

    public boolean isShuffleEnabled() {
        return state.isShuffleEnabled();
    }

    public BooleanProperty loopEnabledProperty() {
        return state.loopEnabledProperty();
    }

    public BooleanProperty shuffleEnabledProperty() {
        return state.shuffleEnabledProperty();
    }

    public ObjectProperty<Song> currentSongProperty() {
        return state.currentSongProperty();
    }

    public BooleanProperty playingProperty() {
        return state.playingProperty();
    }

    public boolean isPlaying() {
        return state.isPlaying();
    }

    public StringProperty currentTitleProperty() {
        return state.currentTitleProperty();
    }

    public StringProperty currentArtistProperty() {
        return state.currentArtistProperty();
    }

    public IntegerProperty currentSecondProperty() {
        return state.currentSecondProperty();
    }

    public IntegerProperty currentDurationProperty() {
        return state.currentDurationProperty();
    }

    public StringProperty currentSourcePlaylistNameProperty() {
        return state.currentSourcePlaylistNameProperty();
    }

    public String getCurrentSourcePlaylistName() {
        return state.getCurrentSourcePlaylistName();
    }
}
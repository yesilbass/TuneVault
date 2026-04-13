package com.example.tunevaultfx.musicplayer.controller;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ListeningEventDAO;
import com.example.tunevaultfx.musicplayer.ListeningSessionTracker;
import com.example.tunevaultfx.musicplayer.ShuffleManager;
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

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

    private final PlaybackLifecycleService lifecycleService;
    private final PlaybackNavigator playbackNavigator;
    private final Timeline timeline;

    private ObservableList<Song> activePlaylistSongs = FXCollections.observableArrayList();
    private int activePlaylistIndex = -1;

    private ObservableList<Song> autoplaySuggestions = FXCollections.observableArrayList();
    private int autoplaySuggestionIndex = -1;
    private boolean playingAutoplaySuggestions = false;

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
    }

    public void playQueue(ObservableList<Song> songs, int index) {
        playQueue(songs, index, "");
    }

    public void playQueue(ObservableList<Song> songs, int index, String playlistName) {
        activePlaylistSongs = songs == null ? FXCollections.observableArrayList() : FXCollections.observableArrayList(songs);
        activePlaylistIndex = index;
        playingAutoplaySuggestions = false;
        autoplaySuggestions.clear();
        autoplaySuggestionIndex = -1;

        lifecycleService.playQueue(songs, index, playlistName);
    }

    public void playSingleSong(Song song) {
        activePlaylistSongs.clear();
        activePlaylistIndex = -1;
        playingAutoplaySuggestions = false;
        autoplaySuggestions.clear();
        autoplaySuggestionIndex = -1;

        lifecycleService.playSingleSong(song);
    }

    public void togglePlayPause() {
        if (state.getCurrentSong() == null) {
            return;
        }
        setPlaying(!state.isPlaying());
    }

    public void next() {
        if (playingAutoplaySuggestions) {
            playNextAutoplaySuggestion();
            return;
        }

        if (!activePlaylistSongs.isEmpty() && activePlaylistIndex >= 0) {
            if (activePlaylistIndex < activePlaylistSongs.size() - 1) {
                activePlaylistIndex++;
                playbackNavigator.next();
                return;
            }

            if (!state.isLoopEnabled()) {
                startAutoplaySuggestions();
                return;
            }
        }

        playbackNavigator.next();
    }

    public void previous() {
        if (playingAutoplaySuggestions) {
            if (autoplaySuggestionIndex > 0) {
                autoplaySuggestionIndex--;
                Song previousSuggestion = autoplaySuggestions.get(autoplaySuggestionIndex);
                lifecycleService.playSingleSong(previousSuggestion);
                return;
            }

            playingAutoplaySuggestions = false;
            autoplaySuggestionIndex = -1;

            if (!activePlaylistSongs.isEmpty() && !activePlaylistSongs.isEmpty()) {
                Song lastPlaylistSong = activePlaylistSongs.get(activePlaylistSongs.size() - 1);
                activePlaylistIndex = activePlaylistSongs.size() - 1;
                lifecycleService.playQueue(activePlaylistSongs, activePlaylistIndex, state.getCurrentSourcePlaylistName());
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
        lifecycleService.stop();
        timeline.stop();
        setExpandedPlayerVisible(false);
        currentSongLiked.set(false);
        playingAutoplaySuggestions = false;
        autoplaySuggestions.clear();
        autoplaySuggestionIndex = -1;
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
        autoplaySuggestions.clear();
        autoplaySuggestionIndex = -1;
        playingAutoplaySuggestions = false;

        currentSongLiked.set(false);
        expandedPlayerVisible.set(false);
    }

    public ObservableList<Song> getAutoplaySuggestionsForCurrentPlaylist(int limit) {
        if (activePlaylistSongs.isEmpty()) {
            return FXCollections.observableArrayList();
        }

        return recommendationService.getSuggestedSongsForPlaylist(
                SessionManager.getCurrentUsername(),
                activePlaylistSongs,
                limit
        );
    }

    public boolean isCurrentSongLiked() {
        return currentSongLiked.get();
    }

    public BooleanProperty currentSongLikedProperty() {
        return currentSongLiked;
    }

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
        boolean newValue = !state.isShuffleEnabled();
        state.setShuffleEnabled(newValue);

        if (newValue) {
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

    private void startAutoplaySuggestions() {
        ObservableList<Song> suggestions = recommendationService.getSuggestedSongsForPlaylist(
                SessionManager.getCurrentUsername(),
                activePlaylistSongs,
                4
        );

        if (suggestions.isEmpty()) {
            stop();
            return;
        }

        autoplaySuggestions = FXCollections.observableArrayList(suggestions);
        autoplaySuggestionIndex = 0;
        playingAutoplaySuggestions = true;
        lifecycleService.playSingleSong(autoplaySuggestions.get(0));
    }

    private void playNextAutoplaySuggestion() {
        if (autoplaySuggestions.isEmpty()) {
            stop();
            return;
        }

        if (autoplaySuggestionIndex < autoplaySuggestions.size() - 1) {
            autoplaySuggestionIndex++;
            lifecycleService.playSingleSong(autoplaySuggestions.get(autoplaySuggestionIndex));
        } else {
            stop();
        }
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

        if (state.getCurrentSecond() >= state.getCurrentDuration()) {
            next();
        }
    }

    private void loadCurrentQueueSong() {
        Song song = queue.getCurrentSong();
        state.setCurrentSong(song);
        state.setCurrentSourcePlaylistName(queue.getSourcePlaylistName());
        refreshCurrentSongLiked();
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
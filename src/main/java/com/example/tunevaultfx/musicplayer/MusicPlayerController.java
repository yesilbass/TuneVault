package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ListeningEventDAO;
import com.example.tunevaultfx.playlist.PlaylistService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserLibraryService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
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

    private final PlaybackLifecycleService lifecycleService;
    private final PlaybackNavigator playbackNavigator;
    private final Timeline timeline;

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
        lifecycleService.playQueue(songs, index, "");
    }

    public void playQueue(ObservableList<Song> songs, int index, String playlistName) {
        lifecycleService.playQueue(songs, index, playlistName);
    }

    /**
     * Plays only one song and stops when it finishes unless loop is enabled.
     * This is used for Search Songs so search is not treated like a playlist.
     */
    public void playSingleSong(Song song) {
        lifecycleService.playSingleSong(song);
    }

    public void togglePlayPause() {
        if (state.getCurrentSong() == null) {
            return;
        }
        setPlaying(!state.isPlaying());
    }

    public void next() {
        playbackNavigator.next();
    }

    public void previous() {
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
    }

    public void onSongRemovedFromPlaylist(String playlistName, Song removedSong) {
        lifecycleService.onSongRemovedFromPlaylist(playlistName, removedSong);
    }

    public void toggleLikeCurrentSong() {
        if (state.getCurrentSong() == null) {
            return;
        }

        new PlaylistService().toggleLikeSong(state.getCurrentSong());
    }

    public boolean isCurrentSongLiked() {
        return libraryService.isLiked(state.getCurrentSong());
    }

    public void addCurrentSongToPlaylist(String playlistName) {
        libraryService.addSongToPlaylist(playlistName, state.getCurrentSong());
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
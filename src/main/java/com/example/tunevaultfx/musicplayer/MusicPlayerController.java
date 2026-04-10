package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.user.UserLibraryService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
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

    private final Timeline timeline;

    private MusicPlayerController() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void playQueue(ObservableList<Song> songs, int index) {
        playQueue(songs, index, "");
    }

    public void playQueue(ObservableList<Song> songs, int index, String playlistName) {
        if (songs == null || songs.isEmpty() || index < 0 || index >= songs.size()) {
            return;
        }

        queue.setQueue(songs, index, playlistName);
        state.setCurrentSourcePlaylistName(queue.getSourcePlaylistName());

        resetShuffleStateIfNeeded();
        loadCurrentQueueSong();
        state.setCurrentSecond(0);
        setPlaying(true);
    }

    /**
     * Plays only one song and stops when it finishes unless loop is enabled.
     * This is used for Search Songs so search is not treated like a playlist.
     */
    public void playSingleSong(Song song) {
        if (song == null) {
            return;
        }

        queue.setSingleSong(song);
        state.setCurrentSourcePlaylistName("");

        resetShuffleStateIfNeeded();
        state.setCurrentSong(song);
        state.setCurrentSecond(0);
        setPlaying(true);
    }

    public void togglePlayPause() {
        if (state.getCurrentSong() == null) {
            return;
        }
        setPlaying(!state.isPlaying());
    }

    public void next() {
        if (queue.isEmpty()) {
            stop();
            return;
        }

        if (state.isShuffleEnabled()) {
            handleNextInShuffleMode();
            return;
        }

        if (!queue.isContinuousPlayback()) {
            if (state.isLoopEnabled()) {
                state.setCurrentSecond(0);
                setPlaying(true);
            } else {
                stop();
            }
            return;
        }

        if (queue.moveNextSequential(state.isLoopEnabled())) {
            loadCurrentQueueSong();
            state.setCurrentSecond(0);
            setPlaying(true);
        } else {
            stop();
        }
    }

    public void previous() {
        if (queue.isEmpty()) {
            return;
        }

        if (state.getCurrentSecond() > 3) {
            state.setCurrentSecond(0);
            return;
        }

        if (state.isShuffleEnabled()) {
            handlePreviousInShuffleMode();
            return;
        }

        if (!queue.isContinuousPlayback()) {
            state.setCurrentSecond(0);
            return;
        }

        if (queue.movePreviousSequential(state.isLoopEnabled())) {
            loadCurrentQueueSong();
            state.setCurrentSecond(0);
        } else {
            state.setCurrentSecond(0);
        }
    }

    public void seek(int second) {
        if (state.getCurrentSong() == null) {
            return;
        }

        int clamped = Math.max(0, Math.min(second, state.getCurrentDuration()));
        state.setCurrentSecond(clamped);
    }

    public void stop() {
        timeline.stop();
        state.clear();
        queue.clear();
        shuffleManager.reset();
    }

    public void onSongRemovedFromPlaylist(String playlistName, Song removedSong) {
        if (removedSong == null || queue.isEmpty()) {
            return;
        }

        if (playlistName == null || !playlistName.equals(queue.getSourcePlaylistName())) {
            return;
        }

        int removedIndex = queue.indexOf(removedSong);
        if (removedIndex == -1) {
            return;
        }

        boolean removedCurrent = state.getCurrentSong() != null && state.getCurrentSong().equals(removedSong);

        queue.removeAt(removedIndex);

        if (queue.isEmpty()) {
            stop();
            return;
        }

        if (removedCurrent) {
            if (removedIndex < queue.size()) {
                queue.setCurrentIndex(removedIndex);
            } else {
                queue.setCurrentIndex(queue.size() - 1);
            }

            loadCurrentQueueSong();
            state.setCurrentSecond(0);
            setPlaying(true);
        } else if (removedIndex < queue.getCurrentIndex()) {
            queue.setCurrentIndex(queue.getCurrentIndex() - 1);
        }

        if (state.isShuffleEnabled()) {
            shuffleManager.createShuffleOrderStartingFrom(queue.size(), queue.getCurrentIndex());
        }
    }

    public void toggleLikeCurrentSong() {
        libraryService.toggleLike(state.getCurrentSong());
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

        if (state.getCurrentSecond() >= state.getCurrentDuration()) {
            next();
        }
    }

    private void handleNextInShuffleMode() {
        if (!queue.isContinuousPlayback()) {
            if (state.isLoopEnabled()) {
                state.setCurrentSecond(0);
                setPlaying(true);
            } else {
                stop();
            }
            return;
        }

        Integer nextIndex = shuffleManager.nextIndex(
                queue.size(),
                queue.getCurrentIndex(),
                state.isLoopEnabled()
        );

        if (nextIndex == null) {
            stop();
            return;
        }

        queue.setCurrentIndex(nextIndex);
        loadCurrentQueueSong();
        state.setCurrentSecond(0);
        setPlaying(true);
    }

    private void handlePreviousInShuffleMode() {
        if (!queue.isContinuousPlayback()) {
            state.setCurrentSecond(0);
            return;
        }

        Integer previousIndex = shuffleManager.previousIndex(
                queue.size(),
                queue.getCurrentIndex(),
                state.isLoopEnabled()
        );

        if (previousIndex == null) {
            state.setCurrentSecond(0);
            return;
        }

        queue.setCurrentIndex(previousIndex);
        loadCurrentQueueSong();
        state.setCurrentSecond(0);
    }

    private void resetShuffleStateIfNeeded() {
        shuffleManager.reset();

        if (state.isShuffleEnabled() && !queue.isEmpty() && queue.getCurrentIndex() >= 0) {
            shuffleManager.createShuffleOrderStartingFrom(queue.size(), queue.getCurrentIndex());
        }
    }

    private void loadCurrentQueueSong() {
        Song song = queue.getCurrentSong();
        state.setCurrentSong(song);
        state.setCurrentSourcePlaylistName(queue.getSourcePlaylistName());
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
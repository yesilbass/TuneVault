package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import javafx.collections.ObservableList;

/**
 * Handles playback lifecycle actions such as starting playback,
 * stopping playback, and reacting to playlist changes.
 */
public class PlaybackLifecycleService {

    public interface PlayerRuntime {
        void setPlaying(boolean playing);
        void loadCurrentQueueSong();
        void startSessionForCurrentSong();
    }

    private final PlaybackState state;
    private final PlaybackQueue queue;
    private final ShuffleManager shuffleManager;
    private final ListeningSessionTracker sessionTracker;
    private final PlayerRuntime runtime;

    public PlaybackLifecycleService(PlaybackState state,
                                    PlaybackQueue queue,
                                    ShuffleManager shuffleManager,
                                    ListeningSessionTracker sessionTracker,
                                    PlayerRuntime runtime) {
        this.state = state;
        this.queue = queue;
        this.shuffleManager = shuffleManager;
        this.sessionTracker = sessionTracker;
        this.runtime = runtime;
    }

    public void playQueue(ObservableList<Song> songs, int index, String playlistName) {
        if (songs == null || songs.isEmpty() || index < 0 || index >= songs.size()) {
            return;
        }

        finishCurrentSessionIfNeeded();

        queue.setQueue(songs, index, playlistName);
        state.setCurrentSourcePlaylistName(queue.getSourcePlaylistName());

        resetShuffleStateIfNeeded();
        runtime.loadCurrentQueueSong();
        state.setCurrentSecond(0);
        runtime.setPlaying(true);
        runtime.startSessionForCurrentSong();
    }

    public void playSingleSong(Song song) {
        if (song == null) {
            return;
        }

        finishCurrentSessionIfNeeded();

        queue.setSingleSong(song);
        state.setCurrentSourcePlaylistName("");

        resetShuffleStateIfNeeded();
        state.setCurrentSong(song);
        state.setCurrentSecond(0);
        runtime.setPlaying(true);
        runtime.startSessionForCurrentSong();
    }

    public void stop() {
        boolean skipped = state.getCurrentSong() != null
                && state.getCurrentDuration() > 0
                && state.getCurrentSecond() < state.getCurrentDuration();

        sessionTracker.finish(
                com.example.tunevaultfx.session.SessionManager.getCurrentUsername(),
                state.getCurrentSong(),
                skipped
        );

        state.clear();
        queue.clear();
        shuffleManager.reset();
        sessionTracker.reset();
        runtime.setPlaying(false);
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

        if (removedCurrent) {
            boolean skipped = state.getCurrentDuration() > 0
                    && state.getCurrentSecond() < state.getCurrentDuration();

            sessionTracker.finish(
                    com.example.tunevaultfx.session.SessionManager.getCurrentUsername(),
                    state.getCurrentSong(),
                    skipped
            );
        }

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

            runtime.loadCurrentQueueSong();
            state.setCurrentSecond(0);
            runtime.setPlaying(true);
            runtime.startSessionForCurrentSong();
        } else if (removedIndex < queue.getCurrentIndex()) {
            queue.setCurrentIndex(queue.getCurrentIndex() - 1);
        }

        if (state.isShuffleEnabled()) {
            shuffleManager.createShuffleOrderStartingFrom(queue.size(), queue.getCurrentIndex());
        }
    }

    private void finishCurrentSessionIfNeeded() {
        boolean replacingCurrentSong = state.getCurrentSong() != null;
        boolean skipped = replacingCurrentSong
                && state.getCurrentDuration() > 0
                && state.getCurrentSecond() < state.getCurrentDuration();

        if (replacingCurrentSong) {
            sessionTracker.finish(
                    com.example.tunevaultfx.session.SessionManager.getCurrentUsername(),
                    state.getCurrentSong(),
                    skipped
            );
        }
    }

    private void resetShuffleStateIfNeeded() {
        shuffleManager.reset();

        if (state.isShuffleEnabled() && !queue.isEmpty() && queue.getCurrentIndex() >= 0) {
            shuffleManager.createShuffleOrderStartingFrom(queue.size(), queue.getCurrentIndex());
        }
    }
}
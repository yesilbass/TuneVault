package com.example.tunevaultfx.musicplayer;

/**
 * Handles next/previous playback navigation logic,
 * including shuffle and loop behavior.
 */
public class PlaybackNavigator {

    public interface PlayerRuntime {
        void setPlaying(boolean playing);
        void loadCurrentQueueSong();
        void startSessionForCurrentSong();
        void stopPlayer();
    }

    private final PlaybackState state;
    private final PlaybackQueue queue;
    private final ShuffleManager shuffleManager;
    private final ListeningSessionTracker sessionTracker;
    private final PlayerRuntime runtime;

    public PlaybackNavigator(PlaybackState state,
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

    public void next() {
        if (queue.isEmpty()) {
            runtime.stopPlayer();
            return;
        }

        finishCurrentSessionIfNeeded();

        if (state.isShuffleEnabled()) {
            handleNextInShuffleMode();
            return;
        }

        if (!queue.isContinuousPlayback()) {
            if (state.isLoopEnabled()) {
                state.setCurrentSecond(0);
                runtime.setPlaying(true);
                runtime.startSessionForCurrentSong();
            } else {
                runtime.stopPlayer();
            }
            return;
        }

        if (queue.moveNextSequential(state.isLoopEnabled())) {
            runtime.loadCurrentQueueSong();
            state.setCurrentSecond(0);
            runtime.setPlaying(true);
            runtime.startSessionForCurrentSong();
        } else {
            runtime.stopPlayer();
        }
    }

    public void previous() {
        if (queue.isEmpty()) {
            return;
        }

        if (state.getCurrentSecond() > 3) {
            state.setCurrentSecond(0);
            sessionTracker.clearProgressButKeepSession();
            return;
        }

        sessionTracker.finish(
                com.example.tunevaultfx.session.SessionManager.getCurrentUsername(),
                state.getCurrentSong(),
                true
        );

        if (state.isShuffleEnabled()) {
            handlePreviousInShuffleMode();
            return;
        }

        if (!queue.isContinuousPlayback()) {
            state.setCurrentSecond(0);
            return;
        }

        if (queue.movePreviousSequential(state.isLoopEnabled())) {
            runtime.loadCurrentQueueSong();
            state.setCurrentSecond(0);
            runtime.startSessionForCurrentSong();
        } else {
            state.setCurrentSecond(0);
        }
    }

    private void handleNextInShuffleMode() {
        if (!queue.isContinuousPlayback()) {
            if (state.isLoopEnabled()) {
                state.setCurrentSecond(0);
                runtime.setPlaying(true);
                runtime.startSessionForCurrentSong();
            } else {
                runtime.stopPlayer();
            }
            return;
        }

        Integer nextIndex = shuffleManager.nextIndex(
                queue.size(),
                queue.getCurrentIndex(),
                state.isLoopEnabled()
        );

        if (nextIndex == null) {
            runtime.stopPlayer();
            return;
        }

        queue.setCurrentIndex(nextIndex);
        runtime.loadCurrentQueueSong();
        state.setCurrentSecond(0);
        runtime.setPlaying(true);
        runtime.startSessionForCurrentSong();
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
        runtime.loadCurrentQueueSong();
        state.setCurrentSecond(0);
        runtime.startSessionForCurrentSong();
    }

    private void finishCurrentSessionIfNeeded() {
        boolean skipped = state.getCurrentSong() != null
                && state.getCurrentDuration() > 0
                && state.getCurrentSecond() < state.getCurrentDuration();

        sessionTracker.finish(
                com.example.tunevaultfx.session.SessionManager.getCurrentUsername(),
                state.getCurrentSong(),
                skipped
        );
    }
}
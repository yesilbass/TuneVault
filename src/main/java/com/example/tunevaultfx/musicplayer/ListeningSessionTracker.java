package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ListeningEventDAO;

/**
 * Tracks one active listening session in real time.
 * Handles listened seconds, play-threshold logic,
 * database syncing, and session finalization.
 */
public class ListeningSessionTracker {

    private final ListeningEventDAO listeningEventDAO;

    private int currentSongListenedSeconds = 0;
    private Integer currentListeningEventId = null;
    private boolean currentSessionAlreadyCountedAsPlay = false;

    public ListeningSessionTracker(ListeningEventDAO listeningEventDAO) {
        this.listeningEventDAO = listeningEventDAO;
    }

    public void startSession(String username, Song song) {
        if (song == null || username == null || username.isBlank()) {
            reset();
            return;
        }

        currentListeningEventId = listeningEventDAO.startListeningSession(username, song);
        currentSongListenedSeconds = 0;
        currentSessionAlreadyCountedAsPlay = false;
    }

    public void tick(String username, Song song) {
        if (currentListeningEventId == null || song == null || username == null || username.isBlank()) {
            return;
        }

        currentSongListenedSeconds++;

        boolean countAsPlay = reachesPlayThreshold(song, currentSongListenedSeconds);
        if (countAsPlay) {
            currentSessionAlreadyCountedAsPlay = true;
        }

        listeningEventDAO.updateListeningSession(
                currentListeningEventId,
                currentSongListenedSeconds,
                currentSessionAlreadyCountedAsPlay
        );
    }

    public void finish(String username, Song song, boolean skipped) {
        if (currentListeningEventId == null || song == null || username == null || username.isBlank()) {
            reset();
            return;
        }

        String finalAction = skipped ? "SKIP" : "PLAY";
        boolean countAsPlay = currentSessionAlreadyCountedAsPlay
                || reachesPlayThreshold(song, currentSongListenedSeconds);

        listeningEventDAO.finalizeListeningSession(
                currentListeningEventId,
                finalAction,
                currentSongListenedSeconds,
                countAsPlay
        );

        reset();
    }

    public void clearProgressButKeepSession() {
        currentSongListenedSeconds = 0;
    }

    public void reset() {
        currentSongListenedSeconds = 0;
        currentListeningEventId = null;
        currentSessionAlreadyCountedAsPlay = false;
    }

    public int getCurrentSongListenedSeconds() {
        return currentSongListenedSeconds;
    }

    public boolean hasActiveSession() {
        return currentListeningEventId != null;
    }

    private boolean reachesPlayThreshold(Song song, int listenedSeconds) {
        if (song == null || listenedSeconds <= 0) {
            return false;
        }

        int duration = Math.max(song.durationSeconds(), 1);
        int threshold = Math.min(240, Math.max(30, (int) Math.ceil(duration * 0.8)));

        return listenedSeconds >= threshold;
    }
}
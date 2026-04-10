package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Holds the active queue and current queue position.
 */
public class PlaybackQueue {

    private ObservableList<Song> currentQueue = FXCollections.observableArrayList();
    private int currentIndex = -1;

    /**
     * True when the player should continue through the queue.
     * False when only one song should play and then stop.
     */
    private boolean continuousPlayback = true;

    private String sourcePlaylistName = "";

    public void setQueue(ObservableList<Song> queue, int index, String playlistName) {
        currentQueue = FXCollections.observableArrayList(queue);
        currentIndex = index;
        continuousPlayback = true;
        sourcePlaylistName = playlistName == null ? "" : playlistName;
    }

    public void setSingleSong(Song song) {
        currentQueue = FXCollections.observableArrayList(song);
        currentIndex = 0;
        continuousPlayback = false;
        sourcePlaylistName = "";
    }

    public void clear() {
        currentQueue.clear();
        currentIndex = -1;
        continuousPlayback = true;
        sourcePlaylistName = "";
    }

    public boolean isEmpty() {
        return currentQueue == null || currentQueue.isEmpty();
    }

    public ObservableList<Song> getQueue() {
        return currentQueue;
    }

    public int size() {
        return currentQueue.size();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    public Song getCurrentSong() {
        if (isEmpty() || currentIndex < 0 || currentIndex >= currentQueue.size()) {
            return null;
        }
        return currentQueue.get(currentIndex);
    }

    public boolean isContinuousPlayback() {
        return continuousPlayback;
    }

    public String getSourcePlaylistName() {
        return sourcePlaylistName;
    }

    public void setSourcePlaylistName(String sourcePlaylistName) {
        this.sourcePlaylistName = sourcePlaylistName == null ? "" : sourcePlaylistName;
    }

    public boolean moveNextSequential(boolean loopEnabled) {
        if (isEmpty()) {
            return false;
        }

        if (!continuousPlayback) {
            return false;
        }

        if (currentIndex + 1 < currentQueue.size()) {
            currentIndex++;
            return true;
        }

        if (loopEnabled) {
            currentIndex = 0;
            return true;
        }

        return false;
    }

    public boolean movePreviousSequential(boolean loopEnabled) {
        if (isEmpty()) {
            return false;
        }

        if (!continuousPlayback) {
            return false;
        }

        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }

        if (loopEnabled) {
            currentIndex = currentQueue.size() - 1;
            return true;
        }

        return false;
    }

    public int indexOf(Song song) {
        return currentQueue.indexOf(song);
    }

    public void removeAt(int index) {
        if (index >= 0 && index < currentQueue.size()) {
            currentQueue.remove(index);
        }
    }
}
package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import javafx.beans.property.*;

/**
 * Holds observable playback state for the UI.
 */
public class PlaybackState {

    private final ObjectProperty<Song> currentSong = new SimpleObjectProperty<>(null);
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final StringProperty currentTitle = new SimpleStringProperty("No song playing");
    private final StringProperty currentArtist = new SimpleStringProperty("");
    private final IntegerProperty currentSecond = new SimpleIntegerProperty(0);
    private final IntegerProperty currentDuration = new SimpleIntegerProperty(0);
    private final StringProperty currentSourcePlaylistName = new SimpleStringProperty("");
    private final BooleanProperty loopEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty shuffleEnabled = new SimpleBooleanProperty(false);

    public PlaybackState() {
        currentSong.addListener((obs, oldSong, newSong) -> refreshMetadata(newSong));
    }

    private void refreshMetadata(Song song) {
        if (song == null) {
            currentTitle.set("No song playing");
            currentArtist.set("");
            currentDuration.set(0);
            currentSecond.set(0);
            return;
        }

        currentTitle.set(song.title());
        currentArtist.set(song.artist());
        currentDuration.set(song.durationSeconds());
    }

    public void clear() {
        currentSong.set(null);
        currentSecond.set(0);
        currentDuration.set(0);
        currentSourcePlaylistName.set("");
        playing.set(false);
    }

    public Song getCurrentSong() {
        return currentSong.get();
    }

    public void setCurrentSong(Song song) {
        currentSong.set(song);
    }

    public ObjectProperty<Song> currentSongProperty() {
        return currentSong;
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public void setPlaying(boolean value) {
        playing.set(value);
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public StringProperty currentTitleProperty() {
        return currentTitle;
    }

    public StringProperty currentArtistProperty() {
        return currentArtist;
    }

    public int getCurrentSecond() {
        return currentSecond.get();
    }

    public void setCurrentSecond(int second) {
        currentSecond.set(second);
    }

    public IntegerProperty currentSecondProperty() {
        return currentSecond;
    }

    public int getCurrentDuration() {
        return currentDuration.get();
    }

    public IntegerProperty currentDurationProperty() {
        return currentDuration;
    }

    public String getCurrentSourcePlaylistName() {
        return currentSourcePlaylistName.get();
    }

    public void setCurrentSourcePlaylistName(String playlistName) {
        currentSourcePlaylistName.set(playlistName == null ? "" : playlistName);
    }

    public StringProperty currentSourcePlaylistNameProperty() {
        return currentSourcePlaylistName;
    }

    public boolean isLoopEnabled() {
        return loopEnabled.get();
    }

    public void setLoopEnabled(boolean enabled) {
        loopEnabled.set(enabled);
    }

    public BooleanProperty loopEnabledProperty() {
        return loopEnabled;
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled.get();
    }

    public void setShuffleEnabled(boolean enabled) {
        shuffleEnabled.set(enabled);
    }

    public BooleanProperty shuffleEnabledProperty() {
        return shuffleEnabled;
    }
}
package com.example.tunevaultfx.controllers;

import com.example.tunevaultfx.musicplayer.MusicPlayerController;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.io.IOException;
/**
 * Controls the shared mini-player shown across pages.
 * Handles compact playback controls, song display, and quick navigation.
 */
public class MiniPlayerController {

    @FXML private Label miniSongLabel;
    @FXML private Label miniArtistLabel;
    @FXML private Label miniTimeLabel;

    @FXML private Hyperlink miniPlaylistLink;

    @FXML private Button miniPlayPauseButton;
    @FXML private Button miniLikeButton;
    @FXML private Button miniShuffleButton;
    @FXML private Button miniLoopButton;

    @FXML private Slider miniProgressSlider;

    private final MusicPlayerController player = MusicPlayerController.getInstance();

    @FXML
    public void initialize() {
        miniSongLabel.textProperty().bind(player.currentTitleProperty());
        miniArtistLabel.textProperty().bind(player.currentArtistProperty());

        miniPlayPauseButton.textProperty().bind(
                Bindings.when(player.playingProperty()).then("⏸").otherwise("▶")
        );

        miniProgressSlider.setOnMouseReleased(e -> player.seek((int) miniProgressSlider.getValue()));
        miniProgressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                player.seek((int) miniProgressSlider.getValue());
            }
        });

        player.currentSongProperty().addListener((obs, oldVal, newVal) -> {
            updateMiniLikeButton();
            updateMiniTime();
            updateMiniPlaylistLink();
        });

        player.currentSecondProperty().addListener((obs, oldVal, newVal) -> updateMiniTime());
        player.currentDurationProperty().addListener((obs, oldVal, newVal) -> updateMiniTime());
        player.currentSourcePlaylistNameProperty().addListener((obs, oldVal, newVal) -> updateMiniPlaylistLink());

        player.shuffleEnabledProperty().addListener((obs, oldVal, newVal) -> updateMiniModeButtons());
        player.loopEnabledProperty().addListener((obs, oldVal, newVal) -> updateMiniModeButtons());

        updateMiniLikeButton();
        updateMiniTime();
        updateMiniPlaylistLink();
        updateMiniModeButtons();
    }

    @FXML
    private void handleMiniPrevious() {
        player.previous();
        updateMiniLikeButton();
    }

    @FXML
    private void handleMiniPlayPause() {
        player.togglePlayPause();
    }

    @FXML
    private void handleMiniNext() {
        player.next();
        updateMiniLikeButton();
    }

    @FXML
    private void handleMiniLike() {
        player.toggleLikeCurrentSong();
        updateMiniLikeButton();
    }

    @FXML
    private void handleMiniShuffle() {
        player.toggleShuffle();
        updateMiniModeButtons();
    }

    @FXML
    private void handleMiniLoop() {
        player.toggleLoop();
        updateMiniModeButtons();
    }

    @FXML
    private void handleOpenNowPlaying(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "nowplaying-page.fxml");
    }

    @FXML
    private void handleOpenCurrentPlaylist(ActionEvent event) throws IOException {
        String playlistName = player.getCurrentSourcePlaylistName();
        if (playlistName == null || playlistName.isBlank()) {
            return;
        }

        SessionManager.requestPlaylistToOpen(playlistName);
        SceneUtil.switchScene((Node) event.getSource(), "playlists-page.fxml");
    }

    @FXML
    private void handleOpenSongDetails() throws IOException {
        Song song = player.getCurrentSong();
        if (song == null) {
            return;
        }

        SessionManager.setSelectedSong(song);
        SceneUtil.switchScene(miniSongLabel, "song-details-page.fxml");
    }

    private void updateMiniPlaylistLink() {
        String playlistName = player.getCurrentSourcePlaylistName();
        if (playlistName == null || playlistName.isBlank()) {
            miniPlaylistLink.setText("");
            miniPlaylistLink.setVisible(false);
            miniPlaylistLink.setManaged(false);
        } else {
            miniPlaylistLink.setText("Playlist: " + playlistName);
            miniPlaylistLink.setVisible(true);
            miniPlaylistLink.setManaged(true);
        }
    }

    private void updateMiniLikeButton() {
        miniLikeButton.setText(player.isCurrentSongLiked() ? "♥" : "♡");
    }

    private void updateMiniModeButtons() {
        miniShuffleButton.setStyle(player.isShuffleEnabled()
                ? "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 16px; -fx-border-color: transparent; -fx-background-radius: 10;"
                : "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-border-color: transparent; -fx-background-radius: 10;");

        miniLoopButton.setStyle(player.isLoopEnabled()
                ? "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-font-size: 16px; -fx-border-color: transparent; -fx-background-radius: 10;"
                : "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 16px; -fx-border-color: transparent; -fx-background-radius: 10;");
    }

    private void updateMiniTime() {
        int current = player.currentSecondProperty().get();
        int total = player.currentDurationProperty().get();

        miniProgressSlider.setMax(total);
        miniProgressSlider.setValue(current);
        miniTimeLabel.setText(formatTime(current) + " / " + formatTime(total));
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
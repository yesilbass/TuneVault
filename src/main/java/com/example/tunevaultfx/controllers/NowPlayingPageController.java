package com.example.tunevaultfx.controllers;

import com.example.tunevaultfx.musicplayer.MusicPlayerController;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.io.IOException;
import java.util.Optional;
/**
 * Controls the full now playing screen.
 * Displays the current song and provides playback controls.
 */
public class NowPlayingPageController {

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label timeLabel;

    @FXML private Button playPauseButton;
    @FXML private Button likeButton;
    @FXML private Button shuffleButton;
    @FXML private Button loopButton;

    @FXML private Slider progressSlider;

    private final MusicPlayerController player = MusicPlayerController.getInstance();

    @FXML
    public void initialize() {
        titleLabel.textProperty().bind(player.currentTitleProperty());
        artistLabel.textProperty().bind(player.currentArtistProperty());

        playPauseButton.textProperty().bind(
                Bindings.when(player.playingProperty()).then("Pause").otherwise("Play")
        );

        player.currentSongProperty().addListener((obs, oldVal, newVal) -> {
            refreshSongInfo();
            refreshLikeButton();
        });
        player.currentSecondProperty().addListener((obs, oldVal, newVal) -> refreshTime());
        player.currentDurationProperty().addListener((obs, oldVal, newVal) -> refreshTime());

        progressSlider.setOnMouseReleased(e -> player.seek((int) progressSlider.getValue()));
        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                player.seek((int) progressSlider.getValue());
            }
        });

        player.shuffleEnabledProperty().addListener((obs, oldVal, newVal) -> updateModeButtons());
        player.loopEnabledProperty().addListener((obs, oldVal, newVal) -> updateModeButtons());

        refreshSongInfo();
        refreshTime();
        refreshLikeButton();
        updateModeButtons();
    }

    @FXML
    private void handlePrevious() {
        player.previous();
        refreshLikeButton();
    }

    @FXML
    private void handleNext() {
        player.next();
        refreshLikeButton();
    }

    @FXML
    private void handlePlayPause() {
        player.togglePlayPause();
    }

    @FXML
    private void handleLike() {
        player.toggleLikeCurrentSong();
        refreshLikeButton();
    }

    @FXML
    private void handleShuffle() {
        player.toggleShuffle();
        updateModeButtons();
    }

    @FXML
    private void handleLoop() {
        player.toggleLoop();
        updateModeButtons();
    }

    @FXML
    private void handleAddToPlaylist() {
        if (player.currentSongProperty().get() == null) {
            return;
        }

        UserProfile profile = SessionManager.getCurrentUserProfile();
        if (profile == null || profile.getPlaylists().isEmpty()) {
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                "Liked Songs",
                javafx.collections.FXCollections.observableArrayList(profile.getPlaylists().keySet())
        );

        dialog.setTitle("Add Current Song");
        dialog.setHeaderText("Add current song to playlist");
        dialog.setContentText("Playlist:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(player::addCurrentSongToPlaylist);
    }

    @FXML
    private void handleBackToMenu(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    @FXML
    private void handleBackToPlaylists(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "playlists-page.fxml");
    }

    private void refreshSongInfo() {
        Song song = player.currentSongProperty().get();

        if (song == null) {
            albumLabel.setText("Album: -");
        } else {
            albumLabel.setText("Album: " + song.album());
        }
    }

    private void refreshTime() {
        int current = player.currentSecondProperty().get();
        int total = player.currentDurationProperty().get();

        progressSlider.setMax(total);
        progressSlider.setValue(current);
        timeLabel.setText(formatTime(current) + " / " + formatTime(total));
    }

    private void refreshLikeButton() {
        likeButton.setText(player.isCurrentSongLiked() ? "Liked ♥" : "Like ♡");
    }

    private void updateModeButtons() {
        shuffleButton.setText(player.isShuffleEnabled() ? "Shuffle On" : "Shuffle Off");
        loopButton.setText(player.isLoopEnabled() ? "Loop On" : "Loop Off");
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
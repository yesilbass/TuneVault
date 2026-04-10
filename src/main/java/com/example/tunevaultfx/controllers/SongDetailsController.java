package com.example.tunevaultfx.controllers;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controls the song details page.
 * Displays information about the currently selected song,
 * and lets the user like/unlike it.
 */
public class SongDetailsController {

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label durationLabel;
    @FXML private Button likeButton;

    private Song selectedSong;

    @FXML
    public void initialize() {
        selectedSong = SessionManager.getSelectedSong();

        if (selectedSong == null) {
            titleLabel.setText("No song selected");
            artistLabel.setText("-");
            albumLabel.setText("Album: -");
            durationLabel.setText("Duration: -");
            if (likeButton != null) {
                likeButton.setDisable(true);
                likeButton.setText("Like ♡");
            }
            return;
        }

        titleLabel.setText(selectedSong.title());
        artistLabel.setText(selectedSong.artist());
        albumLabel.setText("Album: " + selectedSong.album());
        durationLabel.setText("Duration: " + formatTime(selectedSong.durationSeconds()));
        updateLikeButton();
    }

    @FXML
    private void handleLike() {
        if (selectedSong == null) {
            return;
        }

        UserProfile profile = SessionManager.getCurrentUserProfile();
        if (profile == null) {
            return;
        }

        profile.toggleLike(selectedSong);
        SessionManager.saveCurrentProfile();
        updateLikeButton();
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "playlists-page.fxml");
    }

    @FXML
    private void handleBackToMainMenu(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    private void updateLikeButton() {
        if (likeButton == null) {
            return;
        }

        UserProfile profile = SessionManager.getCurrentUserProfile();
        boolean liked = profile != null && selectedSong != null && profile.isLiked(selectedSong);
        likeButton.setText(liked ? "Liked ♥" : "Like ♡");
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }
}
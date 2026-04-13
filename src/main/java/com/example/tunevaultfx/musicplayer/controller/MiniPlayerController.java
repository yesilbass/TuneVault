package com.example.tunevaultfx.musicplayer.controller;

import com.example.tunevaultfx.musicplayer.PlayerStyleConstants;
import com.example.tunevaultfx.playlist.service.PlaylistPickerService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Controls the shared mini-player bar shown at the bottom of every page.
 *
 * Style constants live in PlayerStyleConstants — change them there to
 * re-theme the entire player in one edit.
 */
public class MiniPlayerController {

    @FXML
    private Label miniSongLabel;
    @FXML
    private Hyperlink miniArtistLink;
    @FXML
    private Label miniTimeLabel;
    @FXML
    private Hyperlink miniPlaylistLink;

    @FXML
    private Button miniPlayPauseButton;
    @FXML
    private Button miniLikeButton;
    @FXML
    private Button miniShuffleButton;
    @FXML
    private Button miniLoopButton;
    @FXML
    private Button miniAddButton;

    @FXML
    private Slider miniProgressSlider;

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final PlaylistPickerService addToPlaylistDialog = new PlaylistPickerService();

    // Size tokens for this controller's buttons (smaller than expanded player)
    private static final String FS = "17px";
    private static final String FS_S = "19px";   // slightly bigger for loop symbol
    private static final String R = PlayerStyleConstants.RADIUS_MINI;

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        miniSongLabel.textProperty().bind(player.currentTitleProperty());
        miniArtistLink.textProperty().bind(player.currentArtistProperty());

        miniPlayPauseButton.textProperty().bind(
                Bindings.when(player.playingProperty()).then("⏸").otherwise("▶"));

        miniProgressSlider.setOnMouseReleased(e ->
                player.seek((int) miniProgressSlider.getValue()));
        miniProgressSlider.valueChangingProperty().addListener((obs, was, isChanging) -> {
            if (!isChanging) player.seek((int) miniProgressSlider.getValue());
        });

        player.currentSongProperty().addListener((obs, o, n) -> {
            refreshLikeButton();
            refreshTime();
            refreshPlaylistLink();
            refreshAddButton();
        });
        player.currentSecondProperty().addListener((obs, o, n) -> refreshTime());
        player.currentDurationProperty().addListener((obs, o, n) -> refreshTime());
        player.currentSourcePlaylistNameProperty().addListener((obs, o, n) -> refreshPlaylistLink());
        player.shuffleEnabledProperty().addListener((obs, o, n) -> refreshModeButtons());
        player.loopEnabledProperty().addListener((obs, o, n) -> refreshModeButtons());
        player.currentSongLikedProperty().addListener((obs, o, n) -> refreshLikeButton());

        refreshLikeButton();
        refreshTime();
        refreshPlaylistLink();
        refreshModeButtons();
        refreshAddButton();
    }

    // ── Handlers ──────────────────────────────────────────────────

    @FXML
    private void handleMiniPrevious() {
        player.previous();
        refreshLikeButton();
        refreshAddButton();
    }

    @FXML
    private void handleMiniPlayPause() {
        player.togglePlayPause();
    }

    @FXML
    private void handleMiniNext() {
        player.next();
        refreshLikeButton();
        refreshAddButton();
    }

    @FXML
    private void handleMiniLike() {
        player.toggleLikeCurrentSong();
        refreshLikeButton();
    }

    @FXML
    private void handleMiniShuffle() {
        player.toggleShuffle();
        refreshModeButtons();
    }

    @FXML
    private void handleMiniLoop() {
        player.toggleLoop();
        refreshModeButtons();
    }

    @FXML
    private void handleMiniAddToPlaylist() {
        addToPlaylistDialog.show(player.getCurrentSong());
        refreshAddButton();
    }

    @FXML
    private void handleOpenArtistProfile(ActionEvent event) throws IOException {
        String artist = player.currentArtistProperty().get();
        if (artist == null || artist.isBlank()) return;
        SessionManager.setSelectedArtist(artist);
        SceneUtil.switchScene((Node) event.getSource(), "artist-profile-page.fxml");
    }

    @FXML
    private void handleOpenCurrentPlaylist(ActionEvent event) throws IOException {
        String name = player.getCurrentSourcePlaylistName();
        if (name == null || name.isBlank()) return;
        SessionManager.requestPlaylistToOpen(name);
        SceneUtil.switchScene((Node) event.getSource(), "playlists-page.fxml");
    }

    @FXML
    private void handleOpenNowPlaying(javafx.scene.input.MouseEvent event) {
        if (player.getCurrentSong() == null) return;
        ensureExpandedPlayerAttached((Node) event.getSource());
        player.setExpandedPlayerVisible(true);
    }

    // ── UI refresh helpers ─────────────────────────────────────────

    private void refreshPlaylistLink() {
        String name = player.getCurrentSourcePlaylistName();
        if (name == null || name.isBlank()) {
            miniPlaylistLink.setText("");
            miniPlaylistLink.setVisible(false);
            miniPlaylistLink.setManaged(false);
        } else {
            miniPlaylistLink.setText("From: " + name);
            miniPlaylistLink.setVisible(true);
            miniPlaylistLink.setManaged(true);
        }
    }

    private void refreshLikeButton() {
        boolean liked = player.isCurrentSongLiked();
        miniLikeButton.setText(liked ? "♥" : "♡");
        miniLikeButton.setStyle(liked
                ? PlayerStyleConstants.likeOn(FS, R)
                : PlayerStyleConstants.likeOff(FS, R));
    }

    private void refreshAddButton() {
        miniAddButton.setDisable(player.getCurrentSong() == null);
        miniAddButton.setStyle(PlayerStyleConstants.addButton("20px", R));
    }

    private void refreshModeButtons() {
        miniShuffleButton.setText("⇄");
        miniLoopButton.setText("↻");
        miniShuffleButton.setStyle(player.isShuffleEnabled()
                ? PlayerStyleConstants.modeActive(FS, R)
                : PlayerStyleConstants.modeInactive(FS, R));
        miniLoopButton.setStyle(player.isLoopEnabled()
                ? PlayerStyleConstants.modeActive(FS_S, R)
                : PlayerStyleConstants.modeInactive(FS_S, R));
    }

    private void refreshTime() {
        int current = player.currentSecondProperty().get();
        int total = player.currentDurationProperty().get();
        miniProgressSlider.setMax(Math.max(total, 1));
        miniProgressSlider.setValue(current);
        miniTimeLabel.setText(formatTime(current) + " / " + formatTime(total));
    }

    // ── Expanded player overlay ────────────────────────────────────

    private void ensureExpandedPlayerAttached(Node source) {
        Scene scene = source.getScene();
        if (scene == null) return;

        Parent root = scene.getRoot();
        if (root.lookup("#expandedPlayerOverlay") != null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/tunevaultfx/expanded-page.fxml"));
            Parent overlay = loader.load();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(root, overlay);
            scene.setRoot(wrapper);

            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE)
                    player.setExpandedPlayerVisible(false);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatTime(int totalSeconds) {
        return (totalSeconds / 60) + ":" + String.format("%02d", totalSeconds % 60);
    }
}
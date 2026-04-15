package com.example.tunevaultfx.musicplayer.controller;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.PlayerStyleConstants;
import com.example.tunevaultfx.playlist.service.PlaylistPickerService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
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
    private Button miniQueueButton;

    @FXML
    private Slider miniProgressSlider;

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final PlaylistPickerService addToPlaylistDialog = new PlaylistPickerService();
    private QueuePanelController queuePanelController;

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
            refreshQueueButton();
        });
        player.currentSecondProperty().addListener((obs, o, n) -> refreshTime());
        player.currentDurationProperty().addListener((obs, o, n) -> refreshTime());
        player.currentSourcePlaylistNameProperty().addListener((obs, o, n) -> refreshPlaylistLink());
        player.shuffleEnabledProperty().addListener((obs, o, n) -> refreshModeButtons());
        player.loopEnabledProperty().addListener((obs, o, n) -> refreshModeButtons());
        player.currentSongLikedProperty().addListener((obs, o, n) -> refreshLikeButton());
        player.getUserQueue().addListener((javafx.collections.ListChangeListener<Song>) c -> refreshQueueButton());

        refreshLikeButton();
        refreshTime();
        refreshPlaylistLink();
        refreshModeButtons();
        refreshAddButton();
        refreshQueueButton();
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
        addToPlaylistDialog.show(player.getCurrentSong(), miniPlayPauseButton.getScene());
        refreshAddButton();
    }

    @FXML
    private void handleOpenQueue() {
        ensureQueuePanelAttached(miniQueueButton);
        if (queuePanelController != null) {
            queuePanelController.visibleProperty().set(true);
        }
    }

    @FXML
    private void handleOpenArtistProfile(ActionEvent event) throws IOException {
        String artist = player.currentArtistProperty().get();
        if (artist == null || artist.isBlank()) return;
        SessionManager.setSelectedArtist(artist);
        SceneUtil.switchScene((Node) event.getSource(), FxmlResources.ARTIST_PROFILE);
    }

    @FXML
    private void handleOpenCurrentPlaylist(ActionEvent event) throws IOException {
        String name = player.getCurrentSourcePlaylistName();
        if (name == null || name.isBlank()) return;
        SessionManager.requestPlaylistToOpen(name);
        SceneUtil.switchScene((Node) event.getSource(), FxmlResources.PLAYLISTS);
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
        miniLikeButton.getStyleClass().setAll("button",
                liked ? PlayerStyleConstants.likeOnClass()
                      : PlayerStyleConstants.likeOffClass());
    }

    private void refreshAddButton() {
        miniAddButton.setDisable(player.getCurrentSong() == null);
        miniAddButton.getStyleClass().setAll("button", PlayerStyleConstants.addButtonClass());
    }

    private void refreshModeButtons() {
        miniShuffleButton.setText("⇄");
        miniLoopButton.setText("↻");
        miniShuffleButton.getStyleClass().setAll("button",
                player.isShuffleEnabled()
                        ? PlayerStyleConstants.modeActiveClass()
                        : PlayerStyleConstants.modeInactiveClass());
        miniLoopButton.getStyleClass().setAll("button",
                player.isLoopEnabled()
                        ? PlayerStyleConstants.modeActiveClass()
                        : PlayerStyleConstants.modeInactiveClass());
    }

    private void refreshTime() {
        int current = player.currentSecondProperty().get();
        int total = player.currentDurationProperty().get();
        miniProgressSlider.setMax(Math.max(total, 1));
        if (!miniProgressSlider.isValueChanging()) {
            miniProgressSlider.setValue(current);
        }
        miniTimeLabel.setText(formatTime(current) + " / " + formatTime(total));
        paintSliderTrack(miniProgressSlider, current, total);
    }

    private static void paintSliderTrack(Slider slider, int current, int total) {
        javafx.scene.Node track = slider.lookup(".track");
        if (track == null) return;
        double pct = (total > 0) ? (current * 100.0 / total) : 0;
        String tail = AppTheme.isLightMode() ? "rgba(15,23,42,0.14)" : "rgba(255,255,255,0.08)";
        track.setStyle(
            "-fx-background-color: linear-gradient(to right, #8b5cf6 " + pct + "%, " + tail + " " + pct + "%);" +
            "-fx-background-radius: 3; -fx-pref-height: 5; -fx-background-insets: 0;");
    }

    // ── Queue panel overlay ──────────────────────────────────────────

    private void ensureQueuePanelAttached(Node source) {
        Scene scene = source.getScene();
        if (scene == null) return;

        Parent root = scene.getRoot();
        if (root.lookup("#queuePanelOverlay") != null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("queue-panel.fxml"));
            Parent overlay = loader.load();
            queuePanelController = loader.getController();

            if (root instanceof StackPane sp) {
                sp.getChildren().add(overlay);
            } else {
                StackPane wrapper = new StackPane();
                wrapper.getChildren().addAll(root, overlay);
                scene.setRoot(wrapper);
                SceneUtil.applySavedTheme(scene);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshQueueButton() {
        int total = player.getFullQueueSize();
        if (total > 0) {
            miniQueueButton.setText("☰ " + total);
            miniQueueButton.getStyleClass().setAll("button",
                    PlayerStyleConstants.queueActiveClass());
        } else {
            miniQueueButton.setText("☰");
            miniQueueButton.getStyleClass().setAll("button",
                    PlayerStyleConstants.queueInactiveClass());
        }
    }

    // ── Expanded player overlay ────────────────────────────────────

    private void ensureExpandedPlayerAttached(Node source) {
        Scene scene = source.getScene();
        if (scene == null) return;

        Parent root = scene.getRoot();
        if (root.lookup("#expandedPlayerOverlay") != null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("expanded-page.fxml"));
            Parent overlay = loader.load();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(root, overlay);
            scene.setRoot(wrapper);
            SceneUtil.applySavedTheme(scene);

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
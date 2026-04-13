package com.example.tunevaultfx.musicplayer.controller;

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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Controls the shared mini-player bar shown across all pages.
 * All button styles use the dark theme palette — no light gray backgrounds.
 */
public class MiniPlayerController {

    @FXML private Label     miniSongLabel;
    @FXML private Hyperlink miniArtistLink;
    @FXML private Label     miniTimeLabel;
    @FXML private Hyperlink miniPlaylistLink;

    @FXML private Button miniPlayPauseButton;
    @FXML private Button miniLikeButton;
    @FXML private Button miniShuffleButton;
    @FXML private Button miniLoopButton;
    @FXML private Button miniAddButton;

    @FXML private Slider miniProgressSlider;

    private final MusicPlayerController   player            = MusicPlayerController.getInstance();
    private final PlaylistPickerService   addToPlaylistDialog = new PlaylistPickerService();

    // ─────────────────────────────────────────────────────────────
    // Styles (dark theme)
    // ─────────────────────────────────────────────────────────────

    private static final String BTN_INACTIVE =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #3d3d5c;" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 18;";

    private static final String BTN_MODE_ACTIVE =
            "-fx-background-color: rgba(139,92,246,0.18);" +
                    "-fx-text-fill: #a78bfa;" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(139,92,246,0.28);" +
                    "-fx-border-radius: 18;" +
                    "-fx-border-width: 1;";

    private static final String BTN_LIKE_ON =
            "-fx-background-color: rgba(244,63,94,0.15);" +
                    "-fx-text-fill: #f43f5e;" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(244,63,94,0.22);" +
                    "-fx-border-radius: 18;" +
                    "-fx-border-width: 1;";

    private static final String BTN_LIKE_OFF =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #3d3d5c;" +
                    "-fx-font-size: 18px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 18;";

    private static final String BTN_ADD =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #3d3d5c;" +
                    "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 18;";

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        miniSongLabel.textProperty().bind(player.currentTitleProperty());
        miniArtistLink.textProperty().bind(player.currentArtistProperty());

        miniPlayPauseButton.textProperty().bind(
                Bindings.when(player.playingProperty()).then("⏸").otherwise("▶")
        );

        miniProgressSlider.setOnMouseReleased(e -> player.seek((int) miniProgressSlider.getValue()));
        miniProgressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) player.seek((int) miniProgressSlider.getValue());
        });

        player.currentSongProperty().addListener((obs, o, n) -> {
            updateMiniLikeButton();
            updateMiniTime();
            updateMiniPlaylistLink();
            updateMiniAddButton();
        });

        player.currentSecondProperty().addListener((obs, o, n)   -> updateMiniTime());
        player.currentDurationProperty().addListener((obs, o, n) -> updateMiniTime());
        player.currentSourcePlaylistNameProperty().addListener((obs, o, n) -> updateMiniPlaylistLink());

        player.shuffleEnabledProperty().addListener((obs, o, n) -> updateMiniModeButtons());
        player.loopEnabledProperty().addListener((obs, o, n)    -> updateMiniModeButtons());
        player.currentSongLikedProperty().addListener((obs, o, n) -> updateMiniLikeButton());

        updateMiniLikeButton();
        updateMiniTime();
        updateMiniPlaylistLink();
        updateMiniModeButtons();
        updateMiniAddButton();
    }

    // ─── Button handlers ─────────────────────────────────────────

    @FXML private void handleMiniPrevious()       { player.previous();          updateMiniLikeButton(); updateMiniAddButton(); }
    @FXML private void handleMiniPlayPause()      { player.togglePlayPause(); }
    @FXML private void handleMiniNext()           { player.next();              updateMiniLikeButton(); updateMiniAddButton(); }
    @FXML private void handleMiniLike()           { player.toggleLikeCurrentSong(); updateMiniLikeButton(); }
    @FXML private void handleMiniShuffle()        { player.toggleShuffle();     updateMiniModeButtons(); }
    @FXML private void handleMiniLoop()           { player.toggleLoop();        updateMiniModeButtons(); }
    @FXML private void handleMiniAddToPlaylist()  { addToPlaylistDialog.show(player.getCurrentSong()); updateMiniAddButton(); }

    @FXML
    private void handleOpenArtistProfile(ActionEvent event) throws IOException {
        String artist = player.currentArtistProperty().get();
        if (artist == null || artist.isBlank()) return;
        SessionManager.setSelectedArtist(artist);
        SceneUtil.switchScene((Node) event.getSource(), "artist-profile-page.fxml");
    }

    @FXML
    private void handleOpenCurrentPlaylist(ActionEvent event) throws IOException {
        String playlistName = player.getCurrentSourcePlaylistName();
        if (playlistName == null || playlistName.isBlank()) return;
        SessionManager.requestPlaylistToOpen(playlistName);
        SceneUtil.switchScene((Node) event.getSource(), "playlists-page.fxml");
    }

    @FXML
    private void handleOpenNowPlaying(javafx.scene.input.MouseEvent event) {
        if (player.getCurrentSong() == null) return;
        ensureExpandedPlayerAttached((Node) event.getSource());
        player.setExpandedPlayerVisible(true);
    }

    // ─── UI update helpers ────────────────────────────────────────

    private void updateMiniPlaylistLink() {
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

    private void updateMiniLikeButton() {
        boolean liked = player.isCurrentSongLiked();
        miniLikeButton.setText(liked ? "♥" : "♡");
        miniLikeButton.setStyle(liked ? BTN_LIKE_ON : BTN_LIKE_OFF);
    }

    private void updateMiniAddButton() {
        boolean hasSong = player.getCurrentSong() != null;
        miniAddButton.setDisable(!hasSong);
        miniAddButton.setStyle(BTN_ADD);
    }

    private void updateMiniModeButtons() {
        // ⇄ is a plain Unicode arrow — renders cleanly in JavaFX (unlike 🔀 emoji)
        miniShuffleButton.setText("⇄");
        miniLoopButton.setText("↻");

        miniShuffleButton.setStyle(player.isShuffleEnabled() ? BTN_MODE_ACTIVE : BTN_INACTIVE);
        miniLoopButton.setStyle(player.isLoopEnabled()       ? BTN_MODE_ACTIVE : BTN_INACTIVE);
    }

    private void updateMiniTime() {
        int current = player.currentSecondProperty().get();
        int total   = player.currentDurationProperty().get();

        miniProgressSlider.setMax(Math.max(total, 1));
        miniProgressSlider.setValue(current);
        miniTimeLabel.setText(formatTime(current) + " / " + formatTime(total));
    }

    // ─── Expanded player overlay ──────────────────────────────────

    private void ensureExpandedPlayerAttached(Node sourceNode) {
        Scene scene = sourceNode.getScene();
        if (scene == null) return;

        Parent currentRoot = scene.getRoot();
        if (currentRoot.lookup("#expandedPlayerOverlay") != null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/tunevaultfx/expanded-page.fxml"));
            Parent overlay = loader.load();

            javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane();
            wrapper.getChildren().addAll(currentRoot, overlay);
            scene.setRoot(wrapper);

            scene.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    player.setExpandedPlayerVisible(false);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatTime(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return m + ":" + String.format("%02d", s);
    }
}
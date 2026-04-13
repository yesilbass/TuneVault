package com.example.tunevaultfx.musicplayer.controller;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.playlist.service.PlaylistPickerService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controls the expanded player overlay.
 * All button styles use the dark theme palette.
 */
public class ExpandedPlayerController {

    @FXML private StackPane overlayRoot;
    @FXML private VBox      playerCard;

    @FXML private Label     titleLabel;
    @FXML private Hyperlink artistLink;
    @FXML private Label     albumLabel;
    @FXML private Label     timeLabel;

    @FXML private Button playPauseButton;
    @FXML private Button likeButton;
    @FXML private Button shuffleButton;
    @FXML private Button loopButton;
    @FXML private Button addToPlaylistButton;

    @FXML private Slider progressSlider;

    private final MusicPlayerController  player             = MusicPlayerController.getInstance();
    private final PlaylistPickerService  addToPlaylistDialog = new PlaylistPickerService();

    private boolean animatingClose = false;

    // ─── Style constants (dark theme) ─────────────────────────────

    private static final String BTN_NEUTRAL =
            "-fx-background-color: rgba(255,255,255,0.07);" +
                    "-fx-text-fill: #4a4a70;" +
                    "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 22;" +
                    "-fx-border-color: rgba(255,255,255,0.08);" +
                    "-fx-border-radius: 22;" +
                    "-fx-border-width: 1;";

    private static final String BTN_MODE_ACTIVE =
            "-fx-background-color: rgba(139,92,246,0.2);" +
                    "-fx-text-fill: #a78bfa;" +
                    "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 22;" +
                    "-fx-border-color: rgba(139,92,246,0.32);" +
                    "-fx-border-radius: 22;" +
                    "-fx-border-width: 1;";

    private static final String BTN_LIKE_ON =
            "-fx-background-color: rgba(244,63,94,0.18);" +
                    "-fx-text-fill: #f43f5e;" +
                    "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 22;" +
                    "-fx-border-color: rgba(244,63,94,0.28);" +
                    "-fx-border-radius: 22;" +
                    "-fx-border-width: 1;";

    private static final String BTN_LIKE_OFF =
            "-fx-background-color: rgba(244,63,94,0.07);" +
                    "-fx-text-fill: #4a4a70;" +
                    "-fx-font-size: 20px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 22;" +
                    "-fx-border-color: rgba(244,63,94,0.1);" +
                    "-fx-border-radius: 22;" +
                    "-fx-border-width: 1;";

    private static final String BTN_ADD =
            "-fx-background-color: rgba(139,92,246,0.07);" +
                    "-fx-text-fill: #4a4a70;" +
                    "-fx-font-size: 22px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 22;" +
                    "-fx-border-color: rgba(139,92,246,0.1);" +
                    "-fx-border-radius: 22;" +
                    "-fx-border-width: 1;";

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        overlayRoot.setVisible(false);
        overlayRoot.setManaged(false);
        overlayRoot.setOpacity(0);
        playerCard.setTranslateY(80);

        titleLabel.textProperty().bind(player.currentTitleProperty());
        artistLink.textProperty().bind(player.currentArtistProperty());

        playPauseButton.textProperty().bind(
                Bindings.when(player.playingProperty()).then("⏸").otherwise("▶")
        );

        player.currentSongProperty().addListener((obs, o, n) -> {
            refreshSongInfo();
            refreshLikeButton();
            refreshAddButton();
        });

        player.currentSecondProperty().addListener((obs, o, n)   -> refreshTime());
        player.currentDurationProperty().addListener((obs, o, n) -> refreshTime());

        progressSlider.setOnMouseReleased(e -> player.seek((int) progressSlider.getValue()));
        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) player.seek((int) progressSlider.getValue());
        });

        player.shuffleEnabledProperty().addListener((obs, o, n) -> updateModeButtons());
        player.loopEnabledProperty().addListener((obs, o, n)    -> updateModeButtons());
        player.expandedPlayerVisibleProperty().addListener((obs, o, n) -> {
            if (n) openOverlay(); else closeOverlay();
        });
        player.currentSongLikedProperty().addListener((obs, o, n) -> refreshLikeButton());

        // Apply initial dark styles
        shuffleButton.setStyle(BTN_NEUTRAL);
        loopButton.setStyle(BTN_NEUTRAL);
        likeButton.setStyle(BTN_LIKE_OFF);
        addToPlaylistButton.setStyle(BTN_ADD);

        refreshSongInfo();
        refreshTime();
        refreshLikeButton();
        refreshAddButton();
        updateModeButtons();
    }

    // ─── Handlers ────────────────────────────────────────────────

    @FXML private void handleBackdropClick() { player.setExpandedPlayerVisible(false); }
    @FXML private void handleClose()         { player.setExpandedPlayerVisible(false); }
    @FXML private void handleConsumeClick()  { /* absorb clicks so backdrop doesn't close */ }

    @FXML private void handlePrevious() { player.previous(); refreshLikeButton(); refreshAddButton(); }
    @FXML private void handleNext()     { player.next();     refreshLikeButton(); refreshAddButton(); }
    @FXML private void handlePlayPause(){ player.togglePlayPause(); }
    @FXML private void handleLike()     { player.toggleLikeCurrentSong(); refreshLikeButton(); }
    @FXML private void handleShuffle()  { player.toggleShuffle(); updateModeButtons(); }
    @FXML private void handleLoop()     { player.toggleLoop();    updateModeButtons(); }
    @FXML private void handleAddToPlaylist() { addToPlaylistDialog.show(player.getCurrentSong()); refreshAddButton(); }

    @FXML
    private void handleOpenArtistProfile(ActionEvent event) throws IOException {
        String artist = player.currentArtistProperty().get();
        if (artist == null || artist.isBlank()) return;
        SessionManager.setSelectedArtist(artist);
        SceneUtil.switchScene((Node) event.getSource(), "artist-profile-page.fxml");
    }

    // ─── UI refresh helpers ───────────────────────────────────────

    private void refreshSongInfo() {
        Song song = player.getCurrentSong();
        if (song == null) { albumLabel.setText("Album: -"); return; }
        String album = song.album();
        albumLabel.setText((album == null || album.isBlank()) ? "Album: -" : "Album: " + album);
    }

    private void refreshTime() {
        int current = player.currentSecondProperty().get();
        int total   = player.currentDurationProperty().get();
        progressSlider.setMax(Math.max(total, 1));
        progressSlider.setValue(current);
        timeLabel.setText(formatTime(current) + " / " + formatTime(total));
    }

    private void refreshLikeButton() {
        boolean liked = player.isCurrentSongLiked();
        likeButton.setText(liked ? "♥" : "♡");
        likeButton.setStyle(liked ? BTN_LIKE_ON : BTN_LIKE_OFF);
    }

    private void refreshAddButton() {
        boolean hasSong = player.getCurrentSong() != null;
        addToPlaylistButton.setDisable(!hasSong);
        addToPlaylistButton.setStyle(BTN_ADD);
    }

    private void updateModeButtons() {
        // ⇄ is plain Unicode — renders correctly in JavaFX (unlike 🔀 emoji)
        shuffleButton.setText("⇄");
        loopButton.setText("↻");
        shuffleButton.setStyle(player.isShuffleEnabled() ? BTN_MODE_ACTIVE : BTN_NEUTRAL);
        loopButton.setStyle(player.isLoopEnabled()       ? BTN_MODE_ACTIVE : BTN_NEUTRAL);
    }

    // ─── Overlay animation ────────────────────────────────────────

    private void openOverlay() {
        animatingClose = false;
        overlayRoot.setManaged(true);
        overlayRoot.setVisible(true);
        overlayRoot.setOpacity(0);
        playerCard.setTranslateY(100);
        playerCard.setScaleX(0.97);
        playerCard.setScaleY(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(200), overlayRoot);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), playerCard);
        slide.setToY(0);

        javafx.animation.ScaleTransition scale =
                new javafx.animation.ScaleTransition(Duration.millis(260), playerCard);
        scale.setToX(1.0);
        scale.setToY(1.0);

        new ParallelTransition(fade, slide, scale).play();
    }

    private void closeOverlay() {
        if (!overlayRoot.isVisible() || animatingClose) return;
        animatingClose = true;

        FadeTransition fade = new FadeTransition(Duration.millis(160), overlayRoot);
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), playerCard);
        slide.setToY(80);

        javafx.animation.ScaleTransition scale =
                new javafx.animation.ScaleTransition(Duration.millis(200), playerCard);
        scale.setToX(0.98);
        scale.setToY(0.98);

        ParallelTransition anim = new ParallelTransition(fade, slide, scale);
        anim.setOnFinished(e -> {
            overlayRoot.setVisible(false);
            overlayRoot.setManaged(false);
            animatingClose = false;
        });
        anim.play();
    }

    private String formatTime(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return m + ":" + String.format("%02d", s);
    }
}
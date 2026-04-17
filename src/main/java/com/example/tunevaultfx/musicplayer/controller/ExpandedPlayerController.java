package com.example.tunevaultfx.musicplayer.controller;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.PlayerStyleConstants;
import com.example.tunevaultfx.playlist.service.PlaylistPickerService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controls the expanded player overlay.
 *
 * Style constants live in PlayerStyleConstants — change them there to
 * re-theme the entire player in one edit.
 *
 * Animation logic is self-contained in openOverlay() / closeOverlay().
 */
public class ExpandedPlayerController {

    @FXML private StackPane overlayRoot;
    @FXML private VBox      playerCard;

    @FXML private Label     titleLabel;
    @FXML private Hyperlink artistLink;
    @FXML private Label     albumLabel;
    @FXML private Label     expandedElapsedLabel;
    @FXML private Label     expandedTotalLabel;

    @FXML private Button playPauseButton;
    @FXML private Button likeButton;
    @FXML private Button shuffleButton;
    @FXML private Button loopButton;
    @FXML private Button addToPlaylistButton;

    @FXML private Slider progressSlider;

    private final MusicPlayerController player              = MusicPlayerController.getInstance();
    private final PlaylistPickerService addToPlaylistDialog  = new PlaylistPickerService();

    private boolean animatingClose = false;

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        overlayRoot.setVisible(false);
        overlayRoot.setManaged(false);
        overlayRoot.setOpacity(0);
        playerCard.setTranslateY(80);
        // StackPane would stretch the card to fill the window; keep height to content so it centers.
        playerCard.setMaxHeight(Region.USE_PREF_SIZE);

        titleLabel.textProperty().bind(player.currentTitleProperty());
        artistLink.textProperty().bind(player.currentArtistProperty());

        playPauseButton.textProperty().bind(
                Bindings.when(player.playingProperty()).then("⏸").otherwise("▶"));

        player.currentSongProperty().addListener((obs, o, n) -> {
            refreshAlbum();
            refreshLikeButton();
            refreshAddButton();
        });
        player.currentSecondProperty().addListener((obs, o, n)   -> refreshTime());
        player.currentDurationProperty().addListener((obs, o, n) -> refreshTime());

        progressSlider.setOnMouseReleased(e -> player.seek((int) progressSlider.getValue()));
        progressSlider.valueChangingProperty().addListener((obs, was, isChanging) -> {
            if (!isChanging) player.seek((int) progressSlider.getValue());
        });

        player.shuffleEnabledProperty().addListener((obs, o, n) -> refreshModeButtons());
        player.loopEnabledProperty().addListener((obs, o, n)    -> refreshModeButtons());
        player.expandedPlayerVisibleProperty().addListener((obs, o, n) -> {
            if (n) openOverlay(); else closeOverlay();
        });
        player.currentSongLikedProperty().addListener((obs, o, n) -> refreshLikeButton());

        overlayRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null || newScene.getProperties().containsKey("expandedOverlayEscInstalled")) {
                return;
            }
            newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE && overlayRoot.isVisible()) {
                    player.setExpandedPlayerVisible(false);
                    event.consume();
                }
            });
            newScene.getProperties().put("expandedOverlayEscInstalled", true);
        });

        refreshAlbum();
        refreshTime();
        refreshLikeButton();
        refreshAddButton();
        refreshModeButtons();
    }

    // ── Handlers ──────────────────────────────────────────────────

    @FXML private void handleBackdropClick()  { player.setExpandedPlayerVisible(false); }
    @FXML private void handleClose()          { player.setExpandedPlayerVisible(false); }
    @FXML private void handleConsumeClick()   { /* absorb so backdrop doesn't close */ }

    @FXML private void handlePrevious()       { player.previous();              refreshLikeButton(); refreshAddButton(); }
    @FXML private void handleNext()           { player.next();                  refreshLikeButton(); refreshAddButton(); }
    @FXML private void handlePlayPause()      { player.togglePlayPause(); }
    @FXML private void handleLike()           { player.toggleLikeCurrentSong(); refreshLikeButton(); }
    @FXML private void handleShuffle()        { player.toggleShuffle();         refreshModeButtons(); }
    @FXML private void handleLoop()           { player.toggleLoop();            refreshModeButtons(); }
    @FXML private void handleAddToPlaylist()  { addToPlaylistDialog.show(player.getCurrentSong(), overlayRoot.getScene()); refreshAddButton(); }

    @FXML
    private void handleOpenArtistProfile(ActionEvent event) throws IOException {
        String artist = player.currentArtistProperty().get();
        if (artist == null || artist.isBlank()) return;
        SessionManager.setSelectedArtist(artist);
        SceneUtil.switchScene((Node) event.getSource(), FxmlResources.ARTIST_PROFILE);
    }

    @FXML
    private void handleOpenSongProfile(ActionEvent event) throws IOException {
        Song current = player.getCurrentSong();
        if (current == null) {
            return;
        }
        player.setExpandedPlayerVisible(false);
        SessionManager.setSelectedSong(current);
        SceneUtil.switchScene((Node) event.getSource(), FxmlResources.SONG_PROFILE);
    }

    // ── UI refresh helpers ─────────────────────────────────────────

    private void refreshAlbum() {
        Song song = player.getCurrentSong();
        if (song == null) { albumLabel.setText("Album: -"); return; }
        String album = song.album();
        albumLabel.setText((album == null || album.isBlank()) ? "Album: -" : "Album: " + album);
    }

    private void refreshTime() {
        int current = player.currentSecondProperty().get();
        int total   = player.currentDurationProperty().get();
        progressSlider.setMax(Math.max(total, 1));
        if (!progressSlider.isValueChanging()) {
            progressSlider.setValue(current);
        }
        expandedElapsedLabel.setText(formatTime(Math.max(0, current)));
        expandedTotalLabel.setText(formatTime(Math.max(0, total)));
        paintSliderTrack(progressSlider, current, total);
    }

    private static void paintSliderTrack(Slider slider, int current, int total) {
        javafx.scene.Node track = slider.lookup(".track");
        if (track == null) return;
        double pct = (total > 0) ? (current * 100.0 / total) : 0;
        track.setStyle(
            "-fx-background-color: linear-gradient(to right, #8b5cf6 " + pct + "%, rgba(255,255,255,0.08) " + pct + "%);" +
            "-fx-background-radius: 3; -fx-pref-height: 5; -fx-background-insets: 0;");
    }

    private void refreshLikeButton() {
        boolean liked = player.isCurrentSongLiked();
        likeButton.setText(liked ? "♥" : "♡");
        likeButton.getStyleClass().setAll("button",
                liked ? PlayerStyleConstants.likeOnClass()
                      : PlayerStyleConstants.likeOffClass());
    }

    private void refreshAddButton() {
        addToPlaylistButton.setDisable(player.getCurrentSong() == null);
        addToPlaylistButton.getStyleClass().setAll("button", PlayerStyleConstants.addButtonClass());
    }

    private void refreshModeButtons() {
        shuffleButton.setText("⇄");
        loopButton.setText("↻");
        shuffleButton.getStyleClass().setAll("button",
                player.isShuffleEnabled()
                        ? PlayerStyleConstants.modeActiveClass()
                        : PlayerStyleConstants.modeInactiveClass());
        loopButton.getStyleClass().setAll("button",
                player.isLoopEnabled()
                        ? PlayerStyleConstants.modeActiveClass()
                        : PlayerStyleConstants.modeInactiveClass());
    }

    // ── Overlay animation ──────────────────────────────────────────

    private void openOverlay() {
        animatingClose = false;
        overlayRoot.setManaged(true);
        overlayRoot.setVisible(true);
        overlayRoot.setOpacity(0);
        playerCard.setTranslateY(100);
        playerCard.setScaleX(0.97);
        playerCard.setScaleY(0.97);

        FadeTransition      fade  = new FadeTransition(Duration.millis(200), overlayRoot);
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), playerCard);
        ScaleTransition     scale = new ScaleTransition(Duration.millis(260), playerCard);

        fade.setToValue(1);
        slide.setToY(0);
        scale.setToX(1.0);
        scale.setToY(1.0);

        new ParallelTransition(fade, slide, scale).play();
    }

    private void closeOverlay() {
        if (!overlayRoot.isVisible() || animatingClose) return;
        animatingClose = true;

        FadeTransition      fade  = new FadeTransition(Duration.millis(160), overlayRoot);
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), playerCard);
        ScaleTransition     scale = new ScaleTransition(Duration.millis(200), playerCard);

        fade.setToValue(0);
        slide.setToY(80);
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
        return (totalSeconds / 60) + ":" + String.format("%02d", totalSeconds % 60);
    }
}
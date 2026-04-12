package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.playlist.PlaylistService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls the expanded player overlay shown above the current page.
 */
public class ExpandedPlayerController {

    @FXML private StackPane overlayRoot;
    @FXML private VBox playerCard;

    @FXML private Label titleLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label timeLabel;

    @FXML private Button playPauseButton;
    @FXML private Button likeButton;
    @FXML private Button shuffleButton;
    @FXML private Button loopButton;
    @FXML private Button addToPlaylistButton;

    @FXML private Slider progressSlider;

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final PlaylistService playlistService = new PlaylistService();

    private boolean animatingClose = false;

    @FXML
    public void initialize() {
        overlayRoot.setVisible(false);
        overlayRoot.setManaged(false);
        overlayRoot.setOpacity(0);
        playerCard.setTranslateY(80);

        titleLabel.textProperty().bind(player.currentTitleProperty());
        artistLabel.textProperty().bind(player.currentArtistProperty());

        playPauseButton.textProperty().bind(
                Bindings.when(player.playingProperty()).then("⏸").otherwise("▶")
        );

        player.currentSongProperty().addListener((obs, oldVal, newVal) -> {
            refreshSongInfo();
            refreshLikeButton();
            refreshAddButton();
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

        player.expandedPlayerVisibleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                openOverlay();
            } else {
                closeOverlay();
            }
        });

        shuffleButton.setStyle(neutralButtonStyle(18, 21));
        loopButton.setStyle(neutralButtonStyle(18, 21));
        likeButton.setStyle(neutralButtonStyle(20, 22));
        addToPlaylistButton.setStyle(neutralButtonStyle(22, 22));

        player.currentSongLikedProperty().addListener((obs, oldVal, newVal) -> refreshLikeButton());

        refreshSongInfo();
        refreshTime();
        refreshLikeButton();
        refreshAddButton();
        updateModeButtons();
    }

    @FXML
    private void handleBackdropClick() {
        player.setExpandedPlayerVisible(false);
    }

    @FXML
    private void handleClose() {
        player.setExpandedPlayerVisible(false);
    }

    @FXML
    private void handleConsumeClick() {
        // stops background click-close
    }

    @FXML
    private void handlePrevious() {
        player.previous();
        refreshLikeButton();
        refreshAddButton();
    }

    @FXML
    private void handleNext() {
        player.next();
        refreshLikeButton();
        refreshAddButton();
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
        Song currentSong = player.getCurrentSong();
        UserProfile profile = SessionManager.getCurrentUserProfile();

        if (currentSong == null || profile == null || profile.getPlaylists().isEmpty()) {
            return;
        }

        showPlaylistPicker(profile, currentSong);
        refreshAddButton();
    }

    private void refreshSongInfo() {
        Song song = player.getCurrentSong();

        if (song == null) {
            albumLabel.setText("Album: -");
            return;
        }

        String album = song.album();
        if (album == null || album.isBlank()) {
            albumLabel.setText("Album: -");
        } else {
            albumLabel.setText("Album: " + album);
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
        boolean liked = player.isCurrentSongLiked();

        likeButton.setText(liked ? "♥" : "♡");
        likeButton.setStyle(liked
                ? "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 22;"
                : "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 22;");
    }

    private void refreshAddButton() {
        boolean hasSong = player.getCurrentSong() != null;

        addToPlaylistButton.setDisable(!hasSong);
        addToPlaylistButton.setStyle(
                "-fx-background-color: #e2e8f0; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-font-size: 22px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 22;"
        );
    }

    private void updateModeButtons() {
        shuffleButton.setText("🔀");
        loopButton.setText("↻");

        shuffleButton.setStyle(player.isShuffleEnabled()
                ? "-fx-background-color: #fef3c7; -fx-text-fill: #1DB954; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;"
                : "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;");

        loopButton.setStyle(player.isLoopEnabled()
                ? "-fx-background-color: #e2e8f0; -fx-text-fill: #1DB954; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;"
                : "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;");
    }

    private void openOverlay() {
        animatingClose = false;
        overlayRoot.setManaged(true);
        overlayRoot.setVisible(true);

        overlayRoot.setOpacity(0);
        playerCard.setTranslateY(120);
        playerCard.setScaleX(0.97);
        playerCard.setScaleY(0.97);

        FadeTransition fade = new FadeTransition(Duration.millis(220), overlayRoot);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(280), playerCard);
        slide.setFromY(120);
        slide.setToY(0);

        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(Duration.millis(280), playerCard);
        scale.setFromX(0.97);
        scale.setFromY(0.97);
        scale.setToX(1.0);
        scale.setToY(1.0);

        new ParallelTransition(fade, slide, scale).play();
    }

    private void closeOverlay() {
        if (!overlayRoot.isVisible() || animatingClose) {
            return;
        }

        animatingClose = true;

        FadeTransition fade = new FadeTransition(Duration.millis(180), overlayRoot);
        fade.setFromValue(overlayRoot.getOpacity());
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), playerCard);
        slide.setFromY(playerCard.getTranslateY());
        slide.setToY(100);

        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(Duration.millis(220), playerCard);
        scale.setFromX(playerCard.getScaleX());
        scale.setFromY(playerCard.getScaleY());
        scale.setToX(0.98);
        scale.setToY(0.98);

        ParallelTransition transition = new ParallelTransition(fade, slide, scale);
        transition.setOnFinished(e -> {
            overlayRoot.setVisible(false);
            overlayRoot.setManaged(false);
            animatingClose = false;
        });
        transition.play();
    }

    private String neutralButtonStyle(int fontSize, int radius) {
        return "-fx-background-color: #e2e8f0;" +
                "-fx-text-fill: #334155;" +
                "-fx-font-size: " + fontSize + "px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: " + radius + ";";
    }

    private void showPlaylistPicker(UserProfile profile, Song song) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add to Playlist");
        dialog.setHeaderText("Choose playlists for \"" + song.title() + "\"");

        ButtonType closeButtonType = new ButtonType("Done", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(closeButtonType);

        List<String> playlistNames = new ArrayList<>(profile.getPlaylists().keySet());
        ListView<String> playlistListView = new ListView<>(FXCollections.observableArrayList(playlistNames));
        playlistListView.setPrefHeight(320);
        playlistListView.setFocusTraversable(false);
        playlistListView.getSelectionModel().clearSelection();
        playlistListView.setCellFactory(listView -> new PlaylistPickerCell(profile, song));

        dialog.getDialogPane().setContent(playlistListView);
        dialog.getDialogPane().setPrefWidth(420);
        dialog.showAndWait();
    }

    private boolean songIsInPlaylist(UserProfile profile, String playlistName, Song song) {
        var songs = profile.getPlaylists().get(playlistName);
        return songs != null && songs.contains(song);
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    private class PlaylistPickerCell extends ListCell<String> {
        private final UserProfile profile;
        private final Song song;

        private final HBox root = new HBox();
        private final Label nameLabel = new Label();
        private final Region spacer = new Region();
        private final Button actionButton = new Button();

        PlaylistPickerCell(UserProfile profile, Song song) {
            this.profile = profile;
            this.song = song;

            HBox.setHgrow(spacer, Priority.ALWAYS);
            root.setSpacing(12);
            root.setPadding(new Insets(8, 10, 8, 10));
            root.setStyle("-fx-background-color: transparent; -fx-background-radius: 14;");

            nameLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: bold;");

            actionButton.setPrefWidth(42);
            actionButton.setPrefHeight(32);
            actionButton.setFocusTraversable(false);

            root.getChildren().addAll(nameLabel, spacer, actionButton);

            root.setOnMouseClicked(event -> {
                String playlistName = getItem();
                if (playlistName == null || isEmpty()) {
                    return;
                }

                togglePlaylistMembership(playlistName);
                event.consume();
            });

            setOnMousePressed(event -> {
                if (!isEmpty()) {
                    getListView().getSelectionModel().clearSelection();
                    event.consume();
                }
            });
        }

        @Override
        protected void updateItem(String playlistName, boolean empty) {
            super.updateItem(playlistName, empty);

            if (empty || playlistName == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            nameLabel.setText(playlistName);
            refreshActionButton(playlistName);

            setText(null);
            setGraphic(root);
            setBackground(Background.EMPTY);
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
        }

        @Override
        public void updateSelected(boolean selected) {
            super.updateSelected(false);
        }

        private void playClickFlash(boolean added) {
            Color flashColor = added
                    ? Color.web("#dbeafe")
                    : Color.web("#fee2e2");

            root.setBackground(new Background(
                    new BackgroundFill(flashColor, new CornerRadii(14), Insets.EMPTY)
            ));

            PauseTransition pause = new PauseTransition(Duration.millis(180));
            pause.setOnFinished(e -> root.setBackground(Background.EMPTY));
            pause.play();
        }

        private void togglePlaylistMembership(String playlistName) {
            boolean wasInPlaylist = songIsInPlaylist(profile, playlistName, song);

            if (wasInPlaylist) {
                playlistService.removeSongFromPlaylist(profile, playlistName, song);
            } else {
                playlistService.addSongToPlaylist(profile, playlistName, song);
            }

            playClickFlash(!wasInPlaylist);
            refreshActionButton(playlistName);
            refreshAddButton();
            getListView().getSelectionModel().clearSelection();
        }

        private void refreshActionButton(String playlistName) {
            boolean alreadyInPlaylist = songIsInPlaylist(profile, playlistName, song);

            actionButton.setText(alreadyInPlaylist ? "✓" : "+");
            actionButton.setStyle(alreadyInPlaylist
                    ? "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16;"
                    : "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16;");

            actionButton.setOnAction(event -> {
                togglePlaylistMembership(playlistName);
                event.consume();
            });
        }
    }
}
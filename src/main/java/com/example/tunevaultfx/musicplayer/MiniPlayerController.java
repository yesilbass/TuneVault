package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
import javafx.animation.PauseTransition;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import com.example.tunevaultfx.playlist.PlaylistService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    @FXML private Button miniAddButton;



    @FXML private Slider miniProgressSlider;

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final PlaylistService playlistService = new PlaylistService();

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
            updateMiniAddButton();
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
        updateMiniAddButton();
    }

    @FXML
    private void handleMiniPrevious() {
        player.previous();
        updateMiniLikeButton();
        updateMiniAddButton();
    }

    @FXML
    private void handleMiniPlayPause() {
        player.togglePlayPause();
    }

    @FXML
    private void handleMiniNext() {
        player.next();
        updateMiniLikeButton();
        updateMiniAddButton();
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
    private void handleMiniAddToPlaylist() {
        Song currentSong = player.getCurrentSong();
        UserProfile profile = SessionManager.getCurrentUserProfile();

        if (currentSong == null || profile == null || profile.getPlaylists().isEmpty()) {
            return;
        }

        showPlaylistPicker(profile, currentSong);
        updateMiniAddButton();
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
    private void handleOpenNowPlaying(MouseEvent event) throws IOException {
        if (player.getCurrentSong() == null) {
            return;
        }

        SceneUtil.switchScene((Node) event.getSource(), "nowplaying-page.fxml");
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
        boolean liked = player.isCurrentSongLiked();

        miniLikeButton.setText(liked ? "♥" : "♡");
        miniLikeButton.setStyle(liked
                ? "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 21;"
                : "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-font-size: 20px; -fx-font-weight: bold; -fx-background-radius: 21;");
    }

    private void updateMiniAddButton() {
        boolean hasSong = player.getCurrentSong() != null;

        miniAddButton.setDisable(!hasSong);
        miniAddButton.setStyle(
                "-fx-background-color: #e2e8f0; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-font-size: 20px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 21;"
        );
    }

    private void updateMiniModeButtons() {
        miniShuffleButton.setText("🔀");
        miniLoopButton.setText("↻");

        miniShuffleButton.setStyle(player.isShuffleEnabled()
                ? "-fx-background-color: #fef3c7; -fx-text-fill: #1DB954; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;"
                : "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;");

        miniLoopButton.setStyle(player.isLoopEnabled()
                ? "-fx-background-color: #e2e8f0; -fx-text-fill: #1DB954; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;"
                : "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 21;");
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
            updateMiniAddButton();
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
    }}
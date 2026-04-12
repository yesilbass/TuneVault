package com.example.tunevaultfx.musicplayer;

import com.example.tunevaultfx.core.Song;
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
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the full now playing screen.
 * Displays the current song and provides playback controls.
 */
public class NowPlayingPageController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Label albumLabel;
    @FXML
    private Label timeLabel;

    @FXML
    private Button playPauseButton;
    @FXML
    private Button likeButton;
    @FXML
    private Button shuffleButton;
    @FXML
    private Button loopButton;
    @FXML
    private Button addToPlaylistButton;

    @FXML
    private Slider progressSlider;

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final PlaylistService playlistService = new PlaylistService();

    @FXML
    public void initialize() {
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

        refreshSongInfo();
        refreshTime();
        refreshLikeButton();
        refreshAddButton();
        updateModeButtons();
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
            String album = song.album();
            if (album == null || album.isBlank()) {
                albumLabel.setText("Album: -");
            } else {
                albumLabel.setText("Album: " + album);
            }
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
            root.setStyle("-fx-background-color: transparent;");

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
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
        }

        private void togglePlaylistMembership(String playlistName) {
            boolean isCurrentlyInPlaylist = songIsInPlaylist(profile, playlistName, song);

            if (isCurrentlyInPlaylist) {
                playlistService.removeSongFromPlaylist(profile, playlistName, song);
            } else {
                playlistService.addSongToPlaylist(profile, playlistName, song);
            }

            refreshActionButton(playlistName);
            refreshAddButton();
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
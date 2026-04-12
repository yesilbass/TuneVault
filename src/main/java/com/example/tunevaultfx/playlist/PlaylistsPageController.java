package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.db.SongDAO;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import com.example.tunevaultfx.musicplayer.MusicPlayerController;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.io.IOException;
import java.util.Optional;

/**
 * Controls the playlists page UI.
 * Handles playlist selection, song search, and button actions,
 * while delegating playlist logic to playlist-related helper classes.
 */
public class PlaylistsPageController {

    @FXML
    private ListView<String> playlistListView;
    @FXML
    private ListView<Song> playlistSongsListView;
    @FXML
    private ListView<Song> searchResultsListView;

    @FXML
    private Label selectedPlaylistLabel;
    @FXML
    private Label songCountLabel;
    @FXML
    private Label totalDurationLabel;

    @FXML
    private TextField searchSongsField;
    @FXML
    private VBox searchSongsPanel;

    private final ObservableList<String> playlistNames = FXCollections.observableArrayList();
    private final ObservableList<Song> allLibrarySongs = FXCollections.observableArrayList();

    private final SongDAO songDAO = new SongDAO();

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final PlaylistService playlistService = new PlaylistService();
    private final SongSearchService songSearchService = new SongSearchService();
    private final PlaylistSelectionService playlistSelectionService = new PlaylistSelectionService();

    private UserProfile profile;

    @FXML
    public void initialize() {
        profile = SessionManager.getCurrentUserProfile();
        if (profile == null) {
            return;
        }

        try {
            allLibrarySongs.setAll(songDAO.getAllSongs());
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.info("Database Error", "Could not load songs from the database.");
        }

        searchResultsListView.setItems(allLibrarySongs);

        searchResultsListView.setFocusTraversable(false);
        searchResultsListView.getSelectionModel().clearSelection();
        loadPlaylistNames();
        setupInitialPlaylistSelection();
        setupListeners();
        setupSongCells();
        setupDoubleClickDetails();
        updateSelectedPlaylist();
        hideSearchPanel();
    }

    private void loadPlaylistNames() {
        playlistNames.setAll(profile.getPlaylists().keySet());
        playlistListView.setItems(playlistNames);
    }

    private void setupInitialPlaylistSelection() {
        String requestedPlaylist = SessionManager.consumeRequestedPlaylistToOpen();

        if (requestedPlaylist != null && playlistNames.contains(requestedPlaylist)) {
            playlistListView.getSelectionModel().select(requestedPlaylist);
        } else if (!playlistNames.isEmpty()) {
            playlistListView.getSelectionModel().selectFirst();
        }
    }

    private void setupListeners() {
        playlistListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    updateSelectedPlaylist();
                    refreshSearchResultsCellFactory();
                });

        searchSongsField.textProperty()
                .addListener((obs, oldVal, newVal) -> {
                    searchResultsListView.setItems(songSearchService.filterSongs(allLibrarySongs, newVal));
                    refreshSearchResultsCellFactory();
                });
    }

    private void setupSongCells() {
        playlistSongsListView.setCellFactory(listView ->
                new PlayableSongCell(song -> {
                    String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
                    if (selectedPlaylist == null) {
                        return;
                    }

                    ObservableList<Song> songs = profile.getPlaylists().get(selectedPlaylist);
                    if (songs == null) {
                        return;
                    }

                    int index = songs.indexOf(song);
                    if (index >= 0) {
                        player.playQueue(songs, index, selectedPlaylist);
                    }
                })
        );

        refreshSearchResultsCellFactory();
    }

    private void refreshSearchResultsCellFactory() {
        searchResultsListView.setCellFactory(listView -> new SearchSongToggleCell());
    }

    private void setupDoubleClickDetails() {
        playlistSongsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Song selectedSong = playlistSongsListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    SessionManager.setSelectedSong(selectedSong);
                    try {
                        SceneUtil.switchScene(playlistSongsListView, "song-details-page.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        searchResultsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Song selectedSong = searchResultsListView.getSelectionModel().getSelectedItem();
                if (selectedSong != null) {
                    SessionManager.setSelectedSong(selectedSong);
                    try {
                        SceneUtil.switchScene(searchResultsListView, "song-details-page.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @FXML
    private void handleShowSearchSongs() {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null) {
            AlertUtil.info("No Playlist Selected", "Please select a playlist first.");
            return;
        }

        searchSongsPanel.setVisible(true);
        searchSongsPanel.setManaged(true);
        searchSongsField.requestFocus();
        refreshSearchResultsCellFactory();
    }

    @FXML
    private void handleHideSearchSongs() {
        hideSearchPanel();
    }

    private void hideSearchPanel() {
        searchSongsPanel.setVisible(false);
        searchSongsPanel.setManaged(false);
    }

    @FXML
    private void handleCreatePlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Playlist");
        dialog.setHeaderText("Create a new playlist");
        dialog.setContentText("Playlist name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String name = result.get().trim();
        if (name.isEmpty()) {
            AlertUtil.info("Invalid Name", "Playlist name cannot be empty.");
            return;
        }

        if (!playlistService.createPlaylist(profile, name)) {
            AlertUtil.info("Duplicate Playlist", "A playlist with that name already exists.");
            return;
        }

        loadPlaylistNames();
        playlistListView.getSelectionModel().select(name);
        updateSelectedPlaylist();
        refreshSearchResultsCellFactory();
    }

    @FXML
    private void handleDeletePlaylist() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.info("No Playlist Selected", "Please select a playlist to delete.");
            return;
        }

        if (!playlistService.deletePlaylist(profile, selected)) {
            AlertUtil.info("Protected Playlist", "Liked Songs cannot be deleted.");
            return;
        }

        loadPlaylistNames();
        if (!playlistNames.isEmpty()) {
            playlistListView.getSelectionModel().selectFirst();
        }

        updateSelectedPlaylist();
        refreshSearchResultsCellFactory();
    }

    @FXML
    private void handleRemoveSong() {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        Song selectedSong = playlistSongsListView.getSelectionModel().getSelectedItem();

        if (selectedPlaylist == null || selectedSong == null) {
            AlertUtil.info("Selection Needed", "Select a playlist and a song.");
            return;
        }

        if (playlistService.removeSongFromPlaylist(profile, selectedPlaylist, selectedSong)) {
            player.onSongRemovedFromPlaylist(selectedPlaylist, selectedSong);
            updateSelectedPlaylist();
            refreshSearchResultsCellFactory();
        }
    }

    @FXML
    private void handleBackToMenu(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    private void updateSelectedPlaylist() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            playlistSongsListView.getItems().clear();
            selectedPlaylistLabel.setText("No playlist selected");
            songCountLabel.setText("Songs: 0");
            totalDurationLabel.setText("Duration: 0:00");
            return;
        }

        PlaylistSummary summary = playlistSelectionService.buildSummary(profile, selected);

        selectedPlaylistLabel.setText(summary.getPlaylistName());
        playlistSongsListView.setItems(summary.getSongs());
        songCountLabel.setText("Songs: " + summary.getSongCount());
        totalDurationLabel.setText("Duration: " + summary.getFormattedDuration());
    }

    private boolean songIsInSelectedPlaylist(Song song) {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null || song == null) {
            return false;
        }

        ObservableList<Song> songs = profile.getPlaylists().get(selectedPlaylist);
        return songs != null && songs.contains(song);
    }

    private void toggleSongInSelectedPlaylist(Song song) {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null || song == null) {
            return;
        }

        if (songIsInSelectedPlaylist(song)) {
            playlistService.removeSongFromPlaylist(profile, selectedPlaylist, song);
            player.onSongRemovedFromPlaylist(selectedPlaylist, song);
        } else {
            playlistService.addSongToPlaylist(profile, selectedPlaylist, song);
        }

        updateSelectedPlaylist();
        searchResultsListView.refresh();
    }

    private class SearchSongToggleCell extends ListCell<Song> {
        private final HBox root = new HBox();
        private final VBox textBox = new VBox();
        private final Label titleLabel = new Label();
        private final Label artistLabel = new Label();
        private final Region spacer = new Region();
        private final Button actionButton = new Button();

        SearchSongToggleCell() {
            root.setSpacing(12);
            root.setPadding(new Insets(8, 10, 8, 10));
            root.setStyle("-fx-background-color: transparent; -fx-background-radius: 14;");
            HBox.setHgrow(spacer, Priority.ALWAYS);

            titleLabel.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 15px; -fx-font-weight: bold;");
            artistLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

            textBox.setSpacing(4);
            textBox.getChildren().addAll(titleLabel, artistLabel);

            actionButton.setPrefWidth(42);
            actionButton.setPrefHeight(32);
            actionButton.setFocusTraversable(false);

            root.getChildren().addAll(textBox, spacer, actionButton);

            root.setOnMouseClicked(event -> {
                Song song = getItem();
                if (song == null || isEmpty()) {
                    return;
                }

                toggleSongInCell(song);
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
        protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);

            if (empty || song == null) {
                setText(null);
                setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            titleLabel.setText(song.title());
            artistLabel.setText(song.artist());

            refreshActionButton(song);

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

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(180));
            pause.setOnFinished(e -> root.setBackground(Background.EMPTY));
            pause.play();
        }

        private void toggleSongInCell(Song song) {
            String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
            if (selectedPlaylist == null || song == null) {
                return;
            }

            boolean wasInPlaylist = songIsInSelectedPlaylist(song);

            if (wasInPlaylist) {
                playlistService.removeSongFromPlaylist(profile, selectedPlaylist, song);
                player.onSongRemovedFromPlaylist(selectedPlaylist, song);
            } else {
                playlistService.addSongToPlaylist(profile, selectedPlaylist, song);
            }

            playClickFlash(!wasInPlaylist);
            updateSelectedPlaylist();
            searchResultsListView.refresh();
            searchResultsListView.getSelectionModel().clearSelection();
        }

        private void refreshActionButton(Song song) {
            boolean alreadyInPlaylist = songIsInSelectedPlaylist(song);

            actionButton.setText(alreadyInPlaylist ? "✓" : "+");
            actionButton.setStyle(alreadyInPlaylist
                    ? "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16;"
                    : "-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16;");

            actionButton.setOnAction(event -> {
                toggleSongInCell(song);
                event.consume();
            });
        }
    }
}
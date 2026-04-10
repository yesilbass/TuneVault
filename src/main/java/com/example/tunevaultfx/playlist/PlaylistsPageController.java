package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.DemoLibrary;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

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

        allLibrarySongs.setAll(DemoLibrary.getSongs());
        searchResultsListView.setItems(allLibrarySongs);
        searchResultsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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
                .addListener((obs, oldVal, newVal) -> updateSelectedPlaylist());

        searchSongsField.textProperty()
                .addListener((obs, oldVal, newVal) ->
                        searchResultsListView.setItems(songSearchService.filterSongs(allLibrarySongs, newVal)));
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

        searchResultsListView.setCellFactory(listView ->
                new PlayableSongCell(song -> player.playSingleSong(song))
        );
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
        }
    }

    @FXML
    private void handleAddSelectedSearchSong() {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        ObservableList<Song> selectedSongs = searchResultsListView.getSelectionModel().getSelectedItems();

        if (selectedPlaylist == null) {
            AlertUtil.info("No Playlist Selected", "Please select a playlist first.");
            return;
        }

        if (selectedSongs == null || selectedSongs.isEmpty()) {
            AlertUtil.info("No Songs Selected", "Please select one or more songs from search results.");
            return;
        }

        int addedCount = 0;
        int duplicateCount = 0;

        for (Song song : selectedSongs) {
            boolean added = playlistService.addSongToPlaylist(profile, selectedPlaylist, song);
            if (added) {
                addedCount++;
            } else {
                duplicateCount++;
            }
        }

        updateSelectedPlaylist();

        if (addedCount > 0 && duplicateCount > 0) {
            AlertUtil.info("Songs Added",
                    "Added " + addedCount + " song(s). " + duplicateCount + " were already in the playlist.");
        } else if (addedCount > 0) {
            AlertUtil.info("Songs Added", "Added " + addedCount + " song(s) to the playlist.");
        } else {
            AlertUtil.info("No New Songs Added", "All selected songs were already in the playlist.");
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
}
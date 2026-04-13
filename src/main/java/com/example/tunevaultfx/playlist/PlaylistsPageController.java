package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.playlist.cell.PlayableSongCell;
import com.example.tunevaultfx.playlist.cell.SearchSongToggleCell;
import com.example.tunevaultfx.playlist.cell.SuggestedSongCell;
import com.example.tunevaultfx.playlist.service.PlaylistPickerService;
import com.example.tunevaultfx.playlist.service.PlaylistSelectionService;
import com.example.tunevaultfx.playlist.service.PlaylistService;
import com.example.tunevaultfx.playlist.service.SongSearchService;
import com.example.tunevaultfx.recommendation.RecommendationService;
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
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Optional;

/**
 * Controls the playlists page UI.
 * Handles playlist selection, song search, playlist actions,
 * and smart song suggestions powered by RecommendationService.
 */
public class PlaylistsPageController {

    // --- FXML fields ---
    @FXML private ListView<String> playlistListView;
    @FXML private ListView<Song>   playlistSongsListView;
    @FXML private ListView<Song>   searchResultsListView;
    @FXML private ListView<Song>   suggestedSongsListView;

    @FXML private Label selectedPlaylistLabel;
    @FXML private Label songCountLabel;
    @FXML private Label totalDurationLabel;
    @FXML private Label suggestionSubtitleLabel;

    @FXML private TextField searchSongsField;
    @FXML private VBox      searchSongsPanel;
    @FXML private VBox      suggestionsSection;

    // --- Services ---
    private final ObservableList<String> playlistNames      = FXCollections.observableArrayList();
    private final ObservableList<Song>   allLibrarySongs    = FXCollections.observableArrayList();

    private final SongDAO                songDAO            = new SongDAO();
    private final MusicPlayerController  player             = MusicPlayerController.getInstance();
    private final PlaylistService        playlistService    = new PlaylistService();
    private final SongSearchService      songSearchService  = new SongSearchService();
    private final PlaylistSelectionService selectionService = new PlaylistSelectionService();
    private final PlaylistPickerService  pickerService      = new PlaylistPickerService();
    private final RecommendationService  recommendationService = new RecommendationService();

    private UserProfile profile;

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    @FXML
    public void initialize() {
        profile = SessionManager.getCurrentUserProfile();
        if (profile == null) {
            return;
        }

        loadLibrarySongs();

        // Start with empty search results; no selection highlight
        searchResultsListView.setItems(FXCollections.observableArrayList());
        searchResultsListView.setFocusTraversable(false);
        searchResultsListView.getSelectionModel().clearSelection();
        playlistSongsListView.setFocusTraversable(false);
        playlistSongsListView.getSelectionModel().clearSelection();

        // Suggestions list starts empty; section is hidden
        suggestedSongsListView.setItems(FXCollections.observableArrayList());
        suggestedSongsListView.setFocusTraversable(false);

        loadPlaylistNames();
        setupInitialPlaylistSelection();
        setupListeners();
        setupSongCells();
        setupSuggestedSongCells();
        setupDoubleClickDetails();
        updateSelectedPlaylist();
        hideSearchPanel();
    }

    private void loadLibrarySongs() {
        try {
            allLibrarySongs.setAll(songDAO.getAllSongs());
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.info("Database Error", "Could not load songs from the database.");
        }
    }

    private void loadPlaylistNames() {
        playlistNames.setAll(profile.getPlaylists().keySet());
        playlistListView.setItems(playlistNames);
    }

    private void setupInitialPlaylistSelection() {
        String requested = SessionManager.consumeRequestedPlaylistToOpen();
        if (requested != null && playlistNames.contains(requested)) {
            playlistListView.getSelectionModel().select(requested);
        } else if (!playlistNames.isEmpty()) {
            playlistListView.getSelectionModel().selectFirst();
        }
    }

    // -------------------------------------------------------------------------
    // Listeners
    // -------------------------------------------------------------------------

    private void setupListeners() {
        playlistListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    updateSelectedPlaylist();
                    refreshSuggestions();
                    refreshSearchResultsCellFactory();
                });

        searchSongsField.textProperty()
                .addListener((obs, oldVal, newVal) -> {
                    String query = newVal == null ? "" : newVal.trim();
                    if (query.isEmpty()) {
                        searchResultsListView.setItems(FXCollections.observableArrayList());
                    } else {
                        searchResultsListView.setItems(
                                songSearchService.filterSongs(allLibrarySongs, query)
                        );
                    }
                    refreshSearchResultsCellFactory();
                });
    }

    // -------------------------------------------------------------------------
    // Cell factories
    // -------------------------------------------------------------------------

    private void setupSongCells() {
        playlistSongsListView.setCellFactory(lv ->
                new PlayableSongCell(
                        this::playSongFromSelectedPlaylist,
                        this::showAddToPlaylistPicker,
                        this::removeSongFromSelectedPlaylist,
                        this::getSelectedPlaylistName
                )
        );
        refreshSearchResultsCellFactory();
    }

    private void setupSuggestedSongCells() {
        suggestedSongsListView.setCellFactory(lv ->
                new SuggestedSongCell(
                        this::addSuggestedSongToPlaylist,
                        this::playSuggestedSong
                )
        );
    }

    private void refreshSearchResultsCellFactory() {
        searchResultsListView.setCellFactory(lv ->
                new SearchSongToggleCell(
                        this::isSongInSelectedPlaylist,
                        this::toggleSongInSelectedPlaylist
                )
        );
    }

    // -------------------------------------------------------------------------
    // Suggestion logic — uses RecommendationService
    // -------------------------------------------------------------------------

    private void refreshSuggestions() {
        String selectedPlaylist = playlistListView.getSelectionModel().getSelectedItem();
        if (selectedPlaylist == null || profile == null) {
            hideSuggestionsSection();
            return;
        }

        ObservableList<Song> playlistSongs = profile.getPlaylists().get(selectedPlaylist);
        String username = SessionManager.getCurrentUsername();

        // Delegate entirely to RecommendationService — it uses actual listening
        // behaviour (seconds played, skips, likes, playlist adds/removes, etc.)
        ObservableList<Song> suggestions =
                recommendationService.getSuggestedSongsForPlaylist(username, playlistSongs, 4);

        if (suggestions == null || suggestions.isEmpty()) {
            hideSuggestionsSection();
            return;
        }

        suggestedSongsListView.setItems(suggestions);
        showSuggestionsSection(selectedPlaylist);
    }

    private void showSuggestionsSection(String playlistName) {
        suggestionsSection.setVisible(true);
        suggestionsSection.setManaged(true);
        if (suggestionSubtitleLabel != null) {
            suggestionSubtitleLabel.setText(
                    "Based on \u201c" + playlistName + "\u201d and your listening history"
            );
        }
    }

    private void hideSuggestionsSection() {
        suggestionsSection.setVisible(false);
        suggestionsSection.setManaged(false);
    }

    private void addSuggestedSongToPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return;

        playlistService.addSongToPlaylist(profile, selected, song);
        updateSelectedPlaylist();
        refreshSuggestions();          // re-score now that playlist changed
        refreshSearchResultsCellFactory();
    }

    private void playSuggestedSong(Song song) {
        if (song != null) {
            player.playSingleSong(song);
        }
    }

    // -------------------------------------------------------------------------
    // Playlist actions
    // -------------------------------------------------------------------------

    private void playSongFromSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        if (songs == null) return;

        int index = songs.indexOf(song);
        if (index >= 0) {
            player.playQueue(songs, index, selected);
        }
    }

    private void showAddToPlaylistPicker(Song song) {
        if (song == null || profile == null) return;
        pickerService.show(song);
        updateSelectedPlaylist();
        refreshSuggestions();
        refreshSearchResultsCellFactory();
    }

    private void removeSongFromSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return;

        if (playlistService.removeSongFromPlaylist(profile, selected, song)) {
            player.onSongRemovedFromPlaylist(selected, song);
            updateSelectedPlaylist();
            refreshSuggestions();
            refreshSearchResultsCellFactory();
        }
    }

    private String getSelectedPlaylistName() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        return selected == null ? "this playlist" : selected;
    }

    private boolean isSongInSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return false;
        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        return songs != null && songs.contains(song);
    }

    private void toggleSongInSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return;

        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        if (songs != null && songs.contains(song)) {
            if (playlistService.removeSongFromPlaylist(profile, selected, song)) {
                player.onSongRemovedFromPlaylist(selected, song);
            }
        } else {
            playlistService.addSongToPlaylist(profile, selected, song);
        }

        updateSelectedPlaylist();
        refreshSuggestions();
        searchResultsListView.refresh();
    }

    // -------------------------------------------------------------------------
    // Double-click to open song details
    // -------------------------------------------------------------------------

    private void setupDoubleClickDetails() {
        playlistSongsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Song s = playlistSongsListView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    SessionManager.setSelectedSong(s);
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
                Song s = searchResultsListView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    SessionManager.setSelectedSong(s);
                    try {
                        SceneUtil.switchScene(searchResultsListView, "song-details-page.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // FXML button handlers
    // -------------------------------------------------------------------------

    @FXML
    private void handleShowSearchSongs() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.info("No Playlist Selected", "Please select a playlist first.");
            return;
        }
        searchSongsPanel.setVisible(true);
        searchSongsPanel.setManaged(true);
        searchSongsField.clear();
        searchResultsListView.setItems(FXCollections.observableArrayList());
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
        if (result.isEmpty()) return;

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
        refreshSuggestions();
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
        refreshSuggestions();
        refreshSearchResultsCellFactory();
    }

    @FXML
    private void handleBackToMenu(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    // -------------------------------------------------------------------------
    // UI update helpers
    // -------------------------------------------------------------------------

    private void updateSelectedPlaylist() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            playlistSongsListView.getItems().clear();
            selectedPlaylistLabel.setText("No playlist selected");
            songCountLabel.setText("Songs: 0");
            totalDurationLabel.setText("Duration: 0:00");
            return;
        }

        PlaylistSummary summary = selectionService.buildSummary(profile, selected);
        selectedPlaylistLabel.setText(summary.getPlaylistName());
        playlistSongsListView.setItems(summary.getSongs());
        songCountLabel.setText("Songs: " + summary.getSongCount());
        totalDurationLabel.setText("Duration: " + summary.getFormattedDuration());
    }
}

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
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.Optional;

/**
 * Playlists page controller.
 *
 * Key fixes:
 *  - playlistListView uses a dark custom cell factory (the default is white)
 *  - Platform.runLater ensures suggestions refresh AFTER the scene is rendered,
 *    so the initial selection always triggers them
 *  - all list cell empty branches set Background.EMPTY
 */
public class PlaylistsPageController {

    // ── FXML ─────────────────────────────────────────────────────
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

    // ── Services ──────────────────────────────────────────────────
    private final ObservableList<String> playlistNames   = FXCollections.observableArrayList();
    private final ObservableList<Song>   allLibrarySongs = FXCollections.observableArrayList();

    private final SongDAO               songDAO           = new SongDAO();
    private final MusicPlayerController player            = MusicPlayerController.getInstance();
    private final PlaylistService       playlistService   = new PlaylistService();
    private final SongSearchService     songSearchService = new SongSearchService();
    private final PlaylistSelectionService selectionService = new PlaylistSelectionService();
    private final PlaylistPickerService pickerService     = new PlaylistPickerService();
    private final RecommendationService recommendationService = new RecommendationService();

    private UserProfile profile;

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        profile = SessionManager.getCurrentUserProfile();
        if (profile == null) return;

        loadLibrarySongs();

        // Clear selection so no item is highlighted before setup
        searchResultsListView.setItems(FXCollections.observableArrayList());
        searchResultsListView.setFocusTraversable(false);
        searchResultsListView.getSelectionModel().clearSelection();
        playlistSongsListView.setFocusTraversable(false);
        playlistSongsListView.getSelectionModel().clearSelection();
        suggestedSongsListView.setItems(FXCollections.observableArrayList());
        suggestedSongsListView.setFocusTraversable(false);

        loadPlaylistNames();
        setupPlaylistListCells();   // ← dark cell factory for left panel
        setupInitialPlaylistSelection();
        setupListeners();
        setupSongCells();
        setupSuggestedSongCells();
        setupDoubleClickDetails();
        hideSearchPanel();
        hideSuggestionsSection();

        // Platform.runLater: runs AFTER the scene is fully rendered and the
        // selection is committed, guaranteeing suggestions show on first load
        Platform.runLater(() -> {
            updateSelectedPlaylist();
            refreshSuggestions();
        });
    }

    private void loadLibrarySongs() {
        if (SessionManager.isSongLibraryReady()) {
            allLibrarySongs.setAll(SessionManager.getSongLibrary());
            return;
        }
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

    // ── Dark cell factory for playlist list (left panel) ──────────

    private void setupPlaylistListCells() {
        playlistListView.setCellFactory(lv -> new ListCell<>() {

            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);

                if (empty || name == null) {
                    setText(null);
                    setGraphic(null);
                    setBackground(Background.EMPTY);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // Icon
                StackPane icon = new StackPane();
                icon.setPrefSize(28, 28);
                icon.setMinSize(28, 28);
                icon.setMaxSize(28, 28);
                icon.setStyle(
                        "-fx-background-color: rgba(139,92,246,0.15);" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: rgba(139,92,246,0.2);" +
                                "-fx-border-radius: 8; -fx-border-width: 1;");
                Label iconLbl = new Label("Liked Songs".equals(name) ? "♥" : "♫");
                iconLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #a78bfa;");
                icon.getChildren().add(iconLbl);
                StackPane.setAlignment(iconLbl, Pos.CENTER);

                Label nameLbl = new Label(name);
                nameLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #e2e8f0;");

                HBox row = new HBox(10, icon, nameLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(9, 12, 9, 12));
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");

                setText(null);
                setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);

                if (getGraphic() instanceof HBox row) {
                    row.setStyle(selected
                            ? "-fx-background-color: rgba(139,92,246,0.2); -fx-background-radius: 12;"
                            : "-fx-background-color: transparent; -fx-background-radius: 12;");

                    // Update label text fill
                    row.getChildren().stream()
                            .filter(n -> n instanceof Label)
                            .map(n -> (Label) n)
                            .findFirst()
                            .ifPresent(lbl -> lbl.setStyle(
                                    "-fx-font-size: 13px; -fx-text-fill: "
                                            + (selected ? "#a78bfa" : "#e2e8f0") + ";"));
                }
            }
        });
    }

    // ── Initial playlist selection ─────────────────────────────────

    private void setupInitialPlaylistSelection() {
        String requested = SessionManager.consumeRequestedPlaylistToOpen();
        if (requested != null && playlistNames.contains(requested)) {
            playlistListView.getSelectionModel().select(requested);
        } else if (!playlistNames.isEmpty()) {
            playlistListView.getSelectionModel().selectFirst();
        }
    }

    // ── Listeners ─────────────────────────────────────────────────

    private void setupListeners() {
        playlistListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, o, n) -> {
                    updateSelectedPlaylist();
                    refreshSuggestions();
                    refreshSearchResultsCellFactory();
                });

        searchSongsField.textProperty().addListener((obs, o, n) -> {
            String query = n == null ? "" : n.trim();
            if (query.isEmpty()) {
                searchResultsListView.setItems(FXCollections.observableArrayList());
            } else {
                searchResultsListView.setItems(
                        songSearchService.filterSongs(allLibrarySongs, query));
            }
            refreshSearchResultsCellFactory();
        });
    }

    // ── Cell factories ────────────────────────────────────────────

    private void setupSongCells() {
        playlistSongsListView.setCellFactory(lv ->
                new PlayableSongCell(
                        this::playSongFromSelectedPlaylist,
                        this::showAddToPlaylistPicker,
                        this::removeSongFromSelectedPlaylist,
                        this::getSelectedPlaylistName));
        refreshSearchResultsCellFactory();
    }

    private void setupSuggestedSongCells() {
        suggestedSongsListView.setCellFactory(lv ->
                new SuggestedSongCell(
                        this::addSuggestedSongToPlaylist,
                        this::playSuggestedSong));
    }

    private void refreshSearchResultsCellFactory() {
        searchResultsListView.setCellFactory(lv ->
                new SearchSongToggleCell(
                        this::isSongInSelectedPlaylist,
                        this::toggleSongInSelectedPlaylist));
    }

    // ── Suggestion logic ──────────────────────────────────────────

    private void refreshSuggestions() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || profile == null) {
            hideSuggestionsSection();
            return;
        }

        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        String username = SessionManager.getCurrentUsername();

        ObservableList<Song> suggestions =
                recommendationService.getSuggestedSongsForPlaylist(username, songs, 4);

        if (suggestions == null || suggestions.isEmpty()) {
            hideSuggestionsSection();
            return;
        }

        suggestedSongsListView.setItems(suggestions);
        showSuggestionsSection(selected);
    }

    private void showSuggestionsSection(String playlistName) {
        suggestionsSection.setVisible(true);
        suggestionsSection.setManaged(true);
        if (suggestionSubtitleLabel != null) {
            suggestionSubtitleLabel.setText(
                    "Based on \u201c" + playlistName + "\u201d and your listening history");
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
        refreshSuggestions();
        refreshSearchResultsCellFactory();
    }

    private void playSuggestedSong(Song song) {
        if (song != null) player.playSingleSong(song);
    }

    // ── Playlist actions ──────────────────────────────────────────

    private void playSongFromSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        if (songs == null) return;
        int index = songs.indexOf(song);
        if (index >= 0) player.playQueue(songs, index, selected);
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
        String s = playlistListView.getSelectionModel().getSelectedItem();
        return s == null ? "this playlist" : s;
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

    // ── Double-click → song details ───────────────────────────────

    private void setupDoubleClickDetails() {
        playlistSongsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song s = playlistSongsListView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    SessionManager.setSelectedSong(s);
                    try { SceneUtil.switchScene(playlistSongsListView, "song-details-page.fxml"); }
                    catch (IOException ex) { ex.printStackTrace(); }
                }
            }
        });

        searchResultsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song s = searchResultsListView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    SessionManager.setSelectedSong(s);
                    try { SceneUtil.switchScene(searchResultsListView, "song-details-page.fxml"); }
                    catch (IOException ex) { ex.printStackTrace(); }
                }
            }
        });
    }

    // ── FXML handlers ─────────────────────────────────────────────

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

    @FXML private void handleHideSearchSongs() { hideSearchPanel(); }

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
        if (!playlistNames.isEmpty()) playlistListView.getSelectionModel().selectFirst();

        updateSelectedPlaylist();
        refreshSuggestions();
        refreshSearchResultsCellFactory();
    }

    @FXML
    private void handleBackToMenu(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    // ── UI update ─────────────────────────────────────────────────

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
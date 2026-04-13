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
import javafx.collections.ListChangeListener;
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
 * Suggestion refresh strategy:
 *  - On playlist selection change (always)
 *  - Via an ObservableList listener on the active playlist's song list —
 *    this fires whenever songs are added or removed from ANYWHERE in the app
 *    (mini player add, like button, this controller's own actions) without
 *    needing any callback or event bus
 *  - Via player.currentSongLikedProperty() — catches like/unlike from the
 *    mini player which adds/removes from Liked Songs
 */
public class PlaylistsPageController {

    // ── FXML ─────────────────────────────────────────────────────
    @FXML
    private ListView<String> playlistListView;
    @FXML
    private ListView<Song> playlistSongsListView;
    @FXML
    private ListView<Song> searchResultsListView;
    @FXML
    private ListView<Song> suggestedSongsListView;

    @FXML
    private Label selectedPlaylistLabel;
    @FXML
    private Label songCountLabel;
    @FXML
    private Label totalDurationLabel;
    @FXML
    private Label suggestionSubtitleLabel;

    @FXML
    private TextField searchSongsField;
    @FXML
    private VBox searchSongsPanel;
    @FXML
    private VBox suggestionsSection;

    // ── Services ──────────────────────────────────────────────────
    private final ObservableList<String> playlistNames = FXCollections.observableArrayList();
    private final ObservableList<Song> allLibrarySongs = FXCollections.observableArrayList();

    private final SongDAO songDAO = new SongDAO();
    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final PlaylistService playlistService = new PlaylistService();
    private final SongSearchService songSearchService = new SongSearchService();
    private final PlaylistSelectionService selectionService = new PlaylistSelectionService();
    private final PlaylistPickerService pickerService = new PlaylistPickerService();
    private final RecommendationService recommendationService = new RecommendationService();

    private UserProfile profile;

    /**
     * Tracks the ListChangeListener attached to the currently viewed playlist
     * so we can remove it before attaching a new one on selection change.
     */
    private ListChangeListener<Song> activePlaylistListener;

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        profile = SessionManager.getCurrentUserProfile();
        if (profile == null) return;

        loadLibrarySongs();

        searchResultsListView.setItems(FXCollections.observableArrayList());
        searchResultsListView.setFocusTraversable(false);
        searchResultsListView.getSelectionModel().clearSelection();
        playlistSongsListView.setFocusTraversable(false);
        playlistSongsListView.getSelectionModel().clearSelection();
        suggestedSongsListView.setItems(FXCollections.observableArrayList());
        suggestedSongsListView.setFocusTraversable(false);

        loadPlaylistNames();
        setupPlaylistListCells();
        setupInitialPlaylistSelection();
        setupListeners();
        setupSongCells();
        setupSuggestedSongCells();
        setupDoubleClickDetails();
        hideSearchPanel();
        hideSuggestionsSection();

        // Run after scene is rendered so selection + suggestions both fire correctly
        Platform.runLater(() -> {
            updateSelectedPlaylist();
            attachPlaylistSongsListener();
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

    // ── Dark playlist list cell factory ───────────────────────────

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
        // Playlist selection change
        playlistListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, o, n) -> {
                    updateSelectedPlaylist();
                    attachPlaylistSongsListener();  // re-attach to new playlist's list
                    refreshSuggestions();
                    refreshSearchResultsCellFactory();
                });

        // Search field
        searchSongsField.textProperty().addListener((obs, o, n) -> {
            String query = n == null ? "" : n.trim();
            searchResultsListView.setItems(query.isEmpty()
                    ? FXCollections.observableArrayList()
                    : songSearchService.filterSongs(allLibrarySongs, query));
            refreshSearchResultsCellFactory();
        });

        // Like/unlike from mini player → the song goes into/out of Liked Songs.
        // If "Liked Songs" is the selected playlist, its ObservableList changes and
        // the playlist listener handles it. For all other playlists this covers it.
        player.currentSongLikedProperty().addListener((obs, o, n) ->
                Platform.runLater(this::refreshSuggestions));
    }

    /**
     * Attaches a ListChangeListener to the currently selected playlist's
     * ObservableList<Song>. When any code anywhere in the app adds or removes
     * a song from that list (PlaylistService, PlaylistPickerService, like button),
     * this fires and refreshes the suggestions automatically — no callbacks needed.
     */
    private void attachPlaylistSongsListener() {
        // Remove old listener to avoid memory leaks and duplicate firings
        if (activePlaylistListener != null) {
            for (ObservableList<Song> list : profile.getPlaylists().values()) {
                list.removeListener(activePlaylistListener);
            }
            activePlaylistListener = null;
        }

        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        if (songs == null) return;

        activePlaylistListener = change -> {
            // Small delay ensures DB write from PlaylistService completes first
            Platform.runLater(() -> {
                updateSelectedPlaylist();
                refreshSuggestions();
                refreshSearchResultsCellFactory();
            });
        };

        songs.addListener(activePlaylistListener);
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
        if (suggestionSubtitleLabel != null)
            suggestionSubtitleLabel.setText(
                    "Based on \u201c" + playlistName + "\u201d and your listening history");
    }

    private void hideSuggestionsSection() {
        suggestionsSection.setVisible(false);
        suggestionsSection.setManaged(false);
    }

    private void addSuggestedSongToPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return;
        // PlaylistService updates profile.getPlaylists().get(selected) which is an
        // ObservableList — the attached listener above fires refreshSuggestions() automatically
        playlistService.addSongToPlaylist(profile, selected, song);
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
    }

    private void removeSongFromSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return;
        if (playlistService.removeSongFromPlaylist(profile, selected, song))
            player.onSongRemovedFromPlaylist(selected, song);
        // ObservableList listener handles the refresh automatically
    }

    private String getSelectedPlaylistName() {
        String s = playlistListView.getSelectionModel().getSelectedItem();
        return s == null ? "this playlist" : s;
    }

    private boolean isSongInSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return false;
        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        return songs != null && songs.stream().anyMatch(s -> s.songId() == song.songId());
    }

    private void toggleSongInSelectedPlaylist(Song song) {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null || song == null) return;
        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        if (songs != null && songs.stream().anyMatch(s -> s.songId() == song.songId())) {
            if (playlistService.removeSongFromPlaylist(profile, selected, song))
                player.onSongRemovedFromPlaylist(selected, song);
        } else {
            playlistService.addSongToPlaylist(profile, selected, song);
        }
        searchResultsListView.refresh();
        // ObservableList listener handles suggestions refresh
    }

    // ── Double-click → song details ───────────────────────────────

    private void setupDoubleClickDetails() {
        playlistSongsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song s = playlistSongsListView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    SessionManager.setSelectedSong(s);
                    try {
                        SceneUtil.switchScene(playlistSongsListView, "song-details-page.fxml");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        searchResultsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song s = searchResultsListView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    SessionManager.setSelectedSong(s);
                    try {
                        SceneUtil.switchScene(searchResultsListView, "song-details-page.fxml");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
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

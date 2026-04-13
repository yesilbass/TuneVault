package com.example.tunevaultfx.search;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Search page controller.
 * All cell factories use CellStyleKit for consistent, readable colours.
 */
public class SearchPageController {

    @FXML private TextField  searchField;
    @FXML private Label      resultsSummaryLabel;
    @FXML private VBox       recentSection;
    @FXML private ListView<SearchRecentItem> recentSearchesListView;
    @FXML private ScrollPane resultsScrollPane;
    @FXML private VBox       songResultsSection;
    @FXML private VBox       artistResultsSection;
    @FXML private ListView<Song>   songResultsListView;
    @FXML private ListView<String> artistResultsListView;

    private final ObservableList<Song>   allSongs        = FXCollections.observableArrayList();
    private final ObservableList<Song>   filteredSongs   = FXCollections.observableArrayList();
    private final ObservableList<String> filteredArtists = FXCollections.observableArrayList();

    private final SongDAO               songDAO               = new SongDAO();
    private final RecommendationService recommendationService = new RecommendationService();
    private final MusicPlayerController player                = MusicPlayerController.getInstance();

    // 280ms debounce — no DB hit on every keystroke
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(280));

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadSongs();

        songResultsListView.setItems(filteredSongs);
        artistResultsListView.setItems(filteredArtists);
        recentSearchesListView.setItems(SessionManager.getRecentSearches());

        songResultsListView.setPlaceholder(placeholder("No matching songs"));
        artistResultsListView.setPlaceholder(placeholder("No matching artists"));
        recentSearchesListView.setPlaceholder(placeholder("No recent searches yet"));

        setupSongCells();
        setupArtistCells();
        setupRecentCells();
        setupListeners();
        setupDoubleClickActions();
        showIdleMode();
    }

    private void loadSongs() {
        if (SessionManager.isSongLibraryReady()) { allSongs.setAll(SessionManager.getSongLibrary()); return; }
        try { allSongs.setAll(songDAO.getAllSongs()); }
        catch (Exception e) { e.printStackTrace(); AlertUtil.info("Database Error", "Could not load songs."); }
    }

    // ── Cell factories ────────────────────────────────────────────

    private void setupSongCells() {
        songResultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) {
                    setText(null); setGraphic(null);
                    setBackground(Background.EMPTY);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                StackPane icon = CellStyleKit.iconBox("♫", CellStyleKit.Palette.PURPLE, false);
                VBox      text = CellStyleKit.textBox(
                        song.title(), CellStyleKit.songMeta(song.artist(), song.genre()));
                Label     dur  = CellStyleKit.duration(song.durationSeconds());

                HBox row = CellStyleKit.row(icon, text, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, dur);
                CellStyleKit.addHover(row);

                // Highlight currently playing song
                if (player.getCurrentSong() != null && player.getCurrentSong().songId() == song.songId()) {
                    CellStyleKit.markPlaying(row, true);
                }

                setText(null); setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }
            @Override public void updateSelected(boolean s) { super.updateSelected(false); }
        });
    }

    private void setupArtistCells() {
        artistResultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String artist, boolean empty) {
                super.updateItem(artist, empty);
                if (empty || artist == null || artist.isBlank()) {
                    setText(null); setGraphic(null);
                    setBackground(Background.EMPTY);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                StackPane avatar = CellStyleKit.iconBox(
                        artist.substring(0, 1).toUpperCase(),
                        CellStyleKit.Palette.ROSE, true);

                VBox text = CellStyleKit.textBox(artist, "Artist");
                HBox row  = CellStyleKit.row(avatar, text);
                CellStyleKit.addHover(row);

                setText(null); setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }
            @Override public void updateSelected(boolean s) { super.updateSelected(false); }
        });
    }

    private void setupRecentCells() {
        recentSearchesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchRecentItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                    setBackground(Background.EMPTY);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                boolean isSong = item.getType() == SearchRecentItem.Type.SONG;

                StackPane icon = CellStyleKit.iconBox(
                        isSong ? "♫" : "◎",
                        isSong ? CellStyleKit.Palette.PURPLE : CellStyleKit.Palette.ROSE,
                        !isSong);

                VBox text = CellStyleKit.textBox(item.getPrimaryText(), item.getSecondaryText());

                Label tag = CellStyleKit.tag(
                        isSong ? "Song" : "Artist",
                        isSong ? CellStyleKit.Palette.PURPLE : CellStyleKit.Palette.ROSE);

                HBox row = CellStyleKit.row(icon, text, tag);
                CellStyleKit.addHover(row);

                setText(null); setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }
            @Override public void updateSelected(boolean s) { super.updateSelected(false); }
        });

        recentSearchesListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                SearchRecentItem item = recentSearchesListView.getSelectionModel().getSelectedItem();
                if (item != null) openRecentItem(item);
            }
        });
    }

    // ── Listeners ─────────────────────────────────────────────────

    private void setupListeners() {
        searchField.textProperty().addListener((obs, o, n) -> {
            searchDebounce.setOnFinished(e -> runSearch(n));
            searchDebounce.playFromStart();
        });
    }

    private void setupDoubleClickActions() {
        songResultsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song s = songResultsListView.getSelectionModel().getSelectedItem();
                if (s != null) { player.playSingleSong(s); SessionManager.addRecentSearch(SearchRecentItem.song(s)); }
            }
        });

        artistResultsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String artist = artistResultsListView.getSelectionModel().getSelectedItem();
                if (artist != null && !artist.isBlank()) {
                    SessionManager.setSelectedArtist(artist);
                    SessionManager.addRecentSearch(SearchRecentItem.artist(artist));
                    try { SceneUtil.switchScene(artistResultsListView, "artist-profile-page.fxml"); }
                    catch (IOException ex) { ex.printStackTrace(); }
                }
            }
        });
    }

    // ── Search logic ──────────────────────────────────────────────

    private void runSearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isBlank()) {
            filteredSongs.clear(); filteredArtists.clear();
            resultsSummaryLabel.setText(""); showIdleMode(); return;
        }

        String username = SessionManager.getCurrentUsername();
        filteredSongs.setAll(recommendationService.getRankedSearchSongs(username, query, allSongs, 50));
        filteredArtists.setAll(recommendationService.getRankedSearchArtists(username, query, allSongs, 20));

        int sc = filteredSongs.size(), ac = filteredArtists.size();
        resultsSummaryLabel.setText(sc == 0 && ac == 0
                ? "No results for \"" + query + "\""
                : sc + " song" + (sc != 1 ? "s" : "") + "  \u00B7  "
                  + ac + " artist" + (ac != 1 ? "s" : ""));

        showResultsMode(sc > 0, ac > 0);
    }

    // ── Mode switching ────────────────────────────────────────────

    private void showIdleMode() {
        recentSection.setVisible(true); recentSection.setManaged(true);
        resultsScrollPane.setVisible(false); resultsScrollPane.setManaged(false);
    }

    private void showResultsMode(boolean hasSongs, boolean hasArtists) {
        recentSection.setVisible(false); recentSection.setManaged(false);
        resultsScrollPane.setVisible(true); resultsScrollPane.setManaged(true);
        songResultsSection.setVisible(hasSongs);   songResultsSection.setManaged(hasSongs);
        artistResultsSection.setVisible(hasArtists); artistResultsSection.setManaged(hasArtists);
    }

    // ── Recent item handler ───────────────────────────────────────

    private void openRecentItem(SearchRecentItem item) {
        if (item.getType() == SearchRecentItem.Type.SONG && item.getSong() != null) {
            player.playSingleSong(item.getSong());
            SessionManager.addRecentSearch(SearchRecentItem.song(item.getSong()));
            return;
        }
        if (item.getType() == SearchRecentItem.Type.ARTIST && item.getArtistName() != null) {
            SessionManager.setSelectedArtist(item.getArtistName());
            SessionManager.addRecentSearch(SearchRecentItem.artist(item.getArtistName()));
            try { SceneUtil.switchScene(recentSearchesListView, "artist-profile-page.fxml"); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    // ── FXML handlers ─────────────────────────────────────────────

    @FXML private void handleClearSearch() {
        searchField.clear(); filteredSongs.clear(); filteredArtists.clear();
        resultsSummaryLabel.setText(""); showIdleMode();
    }
    @FXML private void handleClearRecentSearches() {
        SessionManager.clearRecentSearches(); recentSearchesListView.refresh();
    }
    @FXML
    private void handleBackToMenu(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static Label placeholder(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + CellStyleKit.TEXT_MUTED + "; -fx-font-size: 13px;");
        return l;
    }
}
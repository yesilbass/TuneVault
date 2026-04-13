package com.example.tunevaultfx.search;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    @FXML private VBox       searchInputCard;
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

        Platform.runLater(() -> {
            UiMotionUtil.playStaggeredEntrance(java.util.List.of(searchInputCard, recentSection));
            UiMotionUtil.applyHoverLift(searchInputCard);

            if (searchField.getScene() != null) {
                installKeyboardShortcuts();
            }
        });

        searchField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installKeyboardShortcuts();
            }
        });
    }

    private void loadSongs() {
        if (SessionManager.isSongLibraryReady()) { allSongs.setAll(SessionManager.getSongLibrary()); return; }
        try { allSongs.setAll(songDAO.getAllSongs()); }
        catch (Exception e) { e.printStackTrace(); AlertUtil.info("Database Error", "Could not load songs."); }
    }

    // ── Cell factories ────────────────────────────────────────────

    private void setupSongCells() {
        songResultsListView.setCellFactory(lv -> new ListCell<>() {
            private javafx.stage.Popup activePopup;

            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (activePopup != null) { activePopup.hide(); activePopup = null; }
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

                Button moreBtn = new Button("⋯");
                moreBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #58586e; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 0;");
                moreBtn.setPrefSize(32, 32);
                moreBtn.setMinSize(32, 32);
                moreBtn.setMaxSize(32, 32);
                moreBtn.setFocusTraversable(false);
                moreBtn.setOnMouseEntered(e -> moreBtn.setStyle("-fx-background-color: rgba(255,255,255,0.09); -fx-text-fill: #a0a0c0; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 0;"));
                moreBtn.setOnMouseExited(e -> moreBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #58586e; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 0;"));
                moreBtn.setOnAction(ev -> {
                    showSongPopup(song, moreBtn);
                    ev.consume();
                });

                HBox row = CellStyleKit.row(icon, text, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, dur, moreBtn);
                CellStyleKit.addHover(row);

                if (player.getCurrentSong() != null && player.getCurrentSong().songId() == song.songId()) {
                    CellStyleKit.markPlaying(row, true);
                }

                setText(null); setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }
            @Override public void updateSelected(boolean s) { super.updateSelected(false); }

            private void showSongPopup(Song song, Button anchor) {
                if (activePopup != null) { activePopup.hide(); activePopup = null; }
                javafx.stage.Popup popup = new javafx.stage.Popup();
                popup.setAutoHide(true);
                popup.setAutoFix(true);
                popup.setHideOnEscape(true);

                VBox card = new VBox(6);
                card.setPadding(new javafx.geometry.Insets(8));
                card.setPrefWidth(200);
                card.setStyle("-fx-background-color: #16162a; -fx-background-radius: 18; -fx-border-color: rgba(255,255,255,0.12); -fx-border-radius: 18; -fx-border-width: 1; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.65),28,0,0,10);");

                Button playNextBtn = popupBtn("Play Next");
                playNextBtn.setOnAction(ev -> { popup.hide(); player.addToQueueNext(song); ev.consume(); });

                card.getChildren().addAll(playNextBtn);
                popup.getContent().add(card);

                javafx.geometry.Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
                if (b != null) { popup.show(anchor, b.getMaxX() - 190, b.getMaxY() + 4); activePopup = popup; }
            }

            private Button popupBtn(String text) {
                Button btn = new Button(text);
                btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setPrefHeight(38);
                btn.setFocusTraversable(false);
                String base  = "-fx-background-color: transparent; -fx-background-radius: 11; -fx-font-size: 13px; -fx-padding: 0 14 0 14; -fx-text-fill: #e0e0f0;";
                String hover = "-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 11; -fx-font-size: 13px; -fx-padding: 0 14 0 14; -fx-text-fill: #e0e0f0;";
                btn.setStyle(base);
                btn.setOnMouseEntered(e -> btn.setStyle(hover));
                btn.setOnMouseExited(e -> btn.setStyle(base));
                return btn;
            }
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
        SceneUtil.goBack((Node) event.getSource());
    }

    // ── Helpers ───────────────────────────────────────────────────

    private static Label placeholder(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + CellStyleKit.TEXT_MUTED + "; -fx-font-size: 13px;");
        return l;
    }

    private void installKeyboardShortcuts() {
        if (searchField.getScene().getProperties().containsKey("searchEscHandlerInstalled")) {
            return;
        }

        searchField.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && !searchField.getText().isBlank()) {
                handleClearSearch();
                event.consume();
            }
        });
        searchField.getScene().getProperties().put("searchEscHandlerInstalled", true);
    }
}
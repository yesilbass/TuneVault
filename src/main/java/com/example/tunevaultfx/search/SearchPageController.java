package com.example.tunevaultfx.search;

import com.example.tunevaultfx.chrome.SearchBarState;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.PlaylistDAO;
import com.example.tunevaultfx.db.PublicPlaylistSearchRow;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.db.UserDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.recommendation.RankedSearchRow;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.ToastUtil;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;

/**
 * Search page controller.
 * All cell factories use CellStyleKit for consistent, readable colours.
 */
public class SearchPageController {

    @FXML private Label      resultsSummaryLabel;
    @FXML private VBox       searchSummaryCard;
    @FXML private VBox       recentSection;
    @FXML private ListView<SearchRecentItem> recentSearchesListView;
    @FXML private ScrollPane resultsScrollPane;
    @FXML private VBox       musicResultsSection;
    @FXML private VBox       userResultsSection;
    @FXML private VBox       publicPlaylistResultsSection;
    @FXML private ListView<RankedSearchRow> musicResultsListView;
    @FXML private ListView<String> userResultsListView;
    @FXML private ListView<PublicPlaylistSearchRow> publicPlaylistResultsListView;

    private final ObservableList<Song>   allSongs        = FXCollections.observableArrayList();
    private final ObservableList<Song>   musicResultSongsInOrder = FXCollections.observableArrayList();
    private final ObservableList<RankedSearchRow> musicResultRows = FXCollections.observableArrayList();
    private final ObservableList<String> filteredUsers    = FXCollections.observableArrayList();
    private final ObservableList<PublicPlaylistSearchRow> filteredPublicPlaylists =
            FXCollections.observableArrayList();

    private final SongDAO               songDAO               = new SongDAO();
    private final UserDAO               userDAO               = new UserDAO();
    private final PlaylistDAO           playlistDAO           = new PlaylistDAO();
    private final RecommendationService recommendationService = new RecommendationService();
    private final MusicPlayerController player                = MusicPlayerController.getInstance();

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadSongs();

        musicResultsListView.setItems(musicResultRows);
        userResultsListView.setItems(filteredUsers);
        publicPlaylistResultsListView.setItems(filteredPublicPlaylists);
        recentSearchesListView.setItems(SessionManager.getRecentSearches());

        musicResultsListView.setPlaceholder(placeholder("No matching songs or artists"));
        userResultsListView.setPlaceholder(placeholder("No matching people"));
        publicPlaylistResultsListView.setPlaceholder(placeholder("No public playlists match"));
        recentSearchesListView.setPlaceholder(placeholder("No recent searches yet"));

        setupMusicResultCells();
        setupUserCells();
        setupPublicPlaylistCells();
        setupRecentCells();
        setupUserAndPlaylistClicks();

        // Defer wiring: center initialize can run before the included top bar finishes
        // bindTopBarSearchField. One pulse: subscribe + sync; next pulse: sync again so text/QUERY
        // match after bidirectional bind (avoids empty or stale results after navigating to Search).
        Platform.runLater(
                () -> {
                    SearchBarState.setSearchSubscriber(this::runSearch);
                    SearchBarState.clearSearchDropdownAutoOpenSuppress();
                    syncResultsFromSearchBarText();
                    Platform.runLater(this::syncResultsFromSearchBarText);
                });

        Platform.runLater(() -> {
            UiMotionUtil.playStaggeredEntrance(java.util.List.of(searchSummaryCard, recentSection));
            UiMotionUtil.applyHoverLift(searchSummaryCard);
            installKeyboardShortcuts();
        });

        searchSummaryCard.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installKeyboardShortcuts();
            }
        });
    }

    private void loadSongs() {
        allSongs.setAll(catalogSnapshot());
    }

    /**
     * Prefer the cached session library, but fall back to the database when it is empty (race at
     * startup, failed background load, or stale UI). Search ranking needs a full catalog or songs
     * never appear.
     */
    private List<Song> catalogSnapshot() {
        List<Song> catalog = new ArrayList<>();
        if (SessionManager.isSongLibraryReady()) {
            catalog.addAll(SessionManager.getSongLibrary());
        }
        if (catalog.isEmpty()) {
            try {
                catalog.addAll(songDAO.getAllSongs());
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtil.info("Database Error", "Could not load songs.");
            }
        }
        return catalog;
    }

    // ── Cell factories ────────────────────────────────────────────

    private void setupMusicResultCells() {
        musicResultsListView.setCellFactory(
                lv ->
                        new ListCell<>() {
                            private ContextMenu activeSongMenu;

                            @Override
                            protected void updateItem(RankedSearchRow item, boolean empty) {
                                super.updateItem(item, empty);
                                if (activeSongMenu != null) {
                                    activeSongMenu.hide();
                                    activeSongMenu = null;
                                }
                                if (empty || item == null) {
                                    setText(null);
                                    setGraphic(null);
                                    setBackground(Background.EMPTY);
                                    setStyle("-fx-background-color: transparent;");
                                    return;
                                }

                                if (item instanceof RankedSearchRow.SongHit songHit) {
                                    Song song = songHit.song();
                                    StackPane icon =
                                            CellStyleKit.iconBox(
                                                    "♫", CellStyleKit.Palette.PURPLE, false);
                                    VBox text =
                                            CellStyleKit.songTextBoxWithKind(
                                                    song.title(),
                                                    song.artist(),
                                                    null,
                                                    SearchPageController.this::openArtistProfile);
                                    Label dur = CellStyleKit.duration(song.durationSeconds());

                                    Button moreBtn = new Button("⋯");
                                    moreBtn.setStyle(
                                            "-fx-background-color: transparent; -fx-text-fill: #58586e; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 0;");
                                    moreBtn.setPrefSize(32, 32);
                                    moreBtn.setMinSize(32, 32);
                                    moreBtn.setMaxSize(32, 32);
                                    moreBtn.setFocusTraversable(false);
                                    moreBtn.setOnMouseEntered(
                                            e ->
                                                    moreBtn.setStyle(
                                                            "-fx-background-color: rgba(255,255,255,0.09); -fx-text-fill: #a0a0c0; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 0;"));
                                    moreBtn.setOnMouseExited(
                                            e ->
                                                    moreBtn.setStyle(
                                                            "-fx-background-color: transparent; -fx-text-fill: #58586e; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16; -fx-padding: 0;"));
                                    moreBtn.setOnAction(
                                            ev -> {
                                                javafx.geometry.Bounds b =
                                                        moreBtn.localToScreen(
                                                                moreBtn.getBoundsInLocal());
                                                if (b != null) {
                                                    showSongPopup(
                                                            song,
                                                            moreBtn,
                                                            b.getMinX(),
                                                            b.getMaxY() + 4);
                                                }
                                                ev.consume();
                                            });

                                    HBox row =
                                            CellStyleKit.row(
                                                    icon,
                                                    text,
                                                    new Region() {
                                                        {
                                                            HBox.setHgrow(this, Priority.ALWAYS);
                                                        }
                                                    },
                                                    dur,
                                                    moreBtn);
                                    CellStyleKit.addHover(row);

                                    row.setOnMouseClicked(
                                            ev -> {
                                                if (ev.getButton() != MouseButton.PRIMARY
                                                        || ev.getClickCount() != 1) {
                                                    return;
                                                }
                                                if (isSongRowChromeTarget(ev.getTarget())) {
                                                    return;
                                                }
                                                openSongFromSearchResults(song);
                                                ev.consume();
                                            });

                                    row.addEventFilter(
                                            ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                                            ev -> {
                                                RankedSearchRow rowItem = getItem();
                                                if (!(rowItem instanceof RankedSearchRow.SongHit sh)
                                                        || isEmpty()) {
                                                    return;
                                                }
                                                showSongPopup(
                                                        sh.song(),
                                                        row,
                                                        ev.getScreenX(),
                                                        ev.getScreenY());
                                                ev.consume();
                                            });

                                    setText(null);
                                    setGraphic(row);
                                    setBackground(Background.EMPTY);
                                    setStyle(
                                            "-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
                                    return;
                                }

                                if (item instanceof RankedSearchRow.ArtistHit artistHit) {
                                    String artist = artistHit.artistName();
                                    StackPane avatar =
                                            CellStyleKit.iconBox(
                                                    artist.substring(0, 1).toUpperCase(),
                                                    CellStyleKit.Palette.ROSE,
                                                    true);

                                    VBox text = CellStyleKit.textBox(artist, "Artist");
                                    HBox row = CellStyleKit.row(avatar, text);
                                    CellStyleKit.addHover(row);

                                    row.setOnMouseClicked(
                                            ev -> {
                                                if (ev.getButton() != MouseButton.PRIMARY
                                                        || ev.getClickCount() != 1) {
                                                    return;
                                                }
                                                SessionManager.setSelectedArtist(artist);
                                                SessionManager.addRecentSearch(
                                                        SearchRecentItem.artist(artist));
                                                try {
                                                    SceneUtil.switchScene(
                                                            musicResultsListView,
                                                            FxmlResources.ARTIST_PROFILE);
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                }
                                                ev.consume();
                                            });

                                    setText(null);
                                    setGraphic(row);
                                    setBackground(Background.EMPTY);
                                    setStyle(
                                            "-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
                                }
                            }

                            @Override
                            public void updateSelected(boolean s) {
                                super.updateSelected(false);
                            }

                            private void showSongPopup(
                                    Song song,
                                    javafx.scene.Node anchor,
                                    double screenX,
                                    double screenY) {
                                if (activeSongMenu != null) {
                                    activeSongMenu.hide();
                                    activeSongMenu = null;
                                }
                                ContextMenu menu =
                                        SongContextMenuBuilder.build(
                                                song,
                                                anchor,
                                                SongContextMenuBuilder.Spec.general());
                                menu.getStyleClass().add("tv-search-song-menu");
                                activeSongMenu = menu;
                                menu.show(anchor, screenX, screenY);
                            }
                        });
    }

    private void setupUserCells() {
        userResultsListView.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(String username, boolean empty) {
                                super.updateItem(username, empty);
                                if (empty || username == null || username.isBlank()) {
                                    setText(null);
                                    setGraphic(null);
                                    setBackground(Background.EMPTY);
                                    setStyle("-fx-background-color: transparent;");
                                    return;
                                }
                                StackPane avatar =
                                        CellStyleKit.iconBox(
                                                username.substring(0, 1).toUpperCase(),
                                                CellStyleKit.Palette.GREEN,
                                                true);
                                VBox text = CellStyleKit.textBox(username, "Profile · double-click to open");
                                HBox row = CellStyleKit.row(avatar, text);
                                CellStyleKit.addHover(row);
                                setText(null);
                                setGraphic(row);
                                setBackground(Background.EMPTY);
                                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
                            }

                            @Override
                            public void updateSelected(boolean s) {
                                super.updateSelected(false);
                            }
                        });
    }

    private void setupPublicPlaylistCells() {
        publicPlaylistResultsListView.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(PublicPlaylistSearchRow row, boolean empty) {
                                super.updateItem(row, empty);
                                if (empty || row == null) {
                                    setText(null);
                                    setGraphic(null);
                                    setBackground(Background.EMPTY);
                                    setStyle("-fx-background-color: transparent;");
                                    return;
                                }
                                StackPane icon =
                                        CellStyleKit.iconBox(
                                                "\u266A", CellStyleKit.Palette.PURPLE, false);
                                VBox text =
                                        CellStyleKit.textBox(
                                                row.playlistName(),
                                                "@" + row.ownerUsername() + " · " + row.trackCount() + " tracks");
                                HBox h = CellStyleKit.row(icon, text);
                                CellStyleKit.addHover(h);
                                setText(null);
                                setGraphic(h);
                                setBackground(Background.EMPTY);
                                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
                            }

                            @Override
                            public void updateSelected(boolean s) {
                                super.updateSelected(false);
                            }
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

                VBox text =
                        isSong
                                ? CellStyleKit.songTextBoxWithKind(
                                        item.getPrimaryText(),
                                        item.getSecondaryText(),
                                        null,
                                        null)
                                : CellStyleKit.textBox(
                                        item.getPrimaryText(), item.getSecondaryText());

                HBox row = CellStyleKit.row(icon, text);
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
                if (item != null) {
                    try {
                        RecentSearchActions.open(item, recentSearchesListView);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Single tap on a search-result row: play that song in the ranked “Search Results” queue and
     * open its song profile. Clicks on the artist link or ⋯ button are ignored via
     * {@link #isSongRowChromeTarget(Object)}.
     */
    private void openSongFromSearchResults(Song song) {
        if (song == null) {
            return;
        }
        int idx = musicResultSongsInOrder.indexOf(song);
        player.playQueue(musicResultSongsInOrder, Math.max(idx, 0), "Search Results");
        SessionManager.addRecentSearch(SearchRecentItem.song(song));
        SessionManager.setSelectedSong(song);
        try {
            SceneUtil.switchScene(musicResultsListView, FxmlResources.SONG_PROFILE);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** True if the click originated on a hyperlink (e.g. artist) or button (e.g. ⋯). */
    private static boolean isSongRowChromeTarget(Object target) {
        if (!(target instanceof Node n)) {
            return false;
        }
        for (Node p = n; p != null; p = p.getParent()) {
            if (p instanceof Hyperlink || p instanceof Button) {
                return true;
            }
        }
        return false;
    }

    private void openArtistProfile(String artist) {
        if (artist == null || artist.isBlank()) return;
        SessionManager.setSelectedArtist(artist.trim());
        try {
            SceneUtil.switchScene(musicResultsListView, FxmlResources.ARTIST_PROFILE);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void setupUserAndPlaylistClicks() {
        userResultsListView.setOnMouseClicked(
                e -> {
                    if (e.getClickCount() != 2) {
                        return;
                    }
                    String u = userResultsListView.getSelectionModel().getSelectedItem();
                    if (u == null || u.isBlank()) {
                        return;
                    }
                    if (SessionManager.getCurrentUsername() == null
                            || SessionManager.getCurrentUsername().isBlank()) {
                        ToastUtil.info(userResultsListView.getScene(), "Sign in to open member profiles.");
                        return;
                    }
                    SessionManager.setProfileViewUsername(u);
                    try {
                        SceneUtil.switchScene(userResultsListView, FxmlResources.PROFILE);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

        publicPlaylistResultsListView.setOnMouseClicked(
                e -> {
                    if (e.getClickCount() != 2) {
                        return;
                    }
                    PublicPlaylistSearchRow row =
                            publicPlaylistResultsListView.getSelectionModel().getSelectedItem();
                    if (row == null) {
                        return;
                    }
                    if (SessionManager.getCurrentUsername() == null
                            || SessionManager.getCurrentUsername().isBlank()) {
                        ToastUtil.info(
                                publicPlaylistResultsListView.getScene(),
                                "Sign in to open a member profile.");
                        return;
                    }
                    SessionManager.setProfileViewUsername(row.ownerUsername());
                    try {
                        SceneUtil.switchScene(publicPlaylistResultsListView, FxmlResources.PROFILE);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
    }

    // ── Search logic ──────────────────────────────────────────────

    /** Prefer the live TextField text over QUERY in case bidirectional bind lags one frame after nav. */
    private void syncResultsFromSearchBarText() {
        TextField f = SearchBarState.getBoundField();
        String raw = "";
        if (f != null && f.getText() != null) {
            raw = f.getText();
        } else {
            String q = SearchBarState.queryProperty().get();
            raw = q != null ? q : "";
        }
        runSearch(raw);
    }

    private void runSearch(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isBlank()) {
            SearchBarState.consumeFullSearchPresentationRequest();
            musicResultRows.clear();
            musicResultSongsInOrder.clear();
            filteredUsers.clear();
            filteredPublicPlaylists.clear();
            resultsSummaryLabel.setText("");
            showIdleMode();
            return;
        }

        String username = SessionManager.getCurrentUsername();
        ObservableList<Song> catalog = FXCollections.observableArrayList(catalogSnapshot());
        allSongs.setAll(catalog);
        musicResultRows.setAll(
                recommendationService.getRankedCatalogSearchRows(username, query, catalog, 55));
        musicResultSongsInOrder.setAll(
                musicResultRows.stream()
                        .filter(RankedSearchRow.SongHit.class::isInstance)
                        .map(RankedSearchRow.SongHit.class::cast)
                        .map(RankedSearchRow.SongHit::song)
                        .toList());

        try {
            filteredUsers.setAll(userDAO.searchUsernames(query, 30, username));
        } catch (SQLException ex) {
            ex.printStackTrace();
            filteredUsers.clear();
        }
        try {
            filteredPublicPlaylists.setAll(playlistDAO.searchPublicPlaylists(query, 25));
        } catch (SQLException ex) {
            ex.printStackTrace();
            filteredPublicPlaylists.clear();
        }

        long sc =
                musicResultRows.stream()
                        .filter(RankedSearchRow.SongHit.class::isInstance)
                        .count();
        long ac =
                musicResultRows.stream()
                        .filter(RankedSearchRow.ArtistHit.class::isInstance)
                        .count();
        int uc = filteredUsers.size(),
                pc = filteredPublicPlaylists.size();
        boolean hasMusic = !musicResultRows.isEmpty();
        boolean any = hasMusic || uc > 0 || pc > 0;
        resultsSummaryLabel.setText(
                !any
                        ? "No results for \"" + query + "\""
                        : (hasMusic
                                        ? (sc
                                                + " song"
                                                + (sc != 1 ? "s" : "")
                                                + "  \u00B7  "
                                                + ac
                                                + " artist"
                                                + (ac != 1 ? "s" : "")
                                                + "  \u00B7  ")
                                        : "")
                                + uc
                                + " "
                                + (uc == 1 ? "person" : "people")
                                + "  \u00B7  "
                                + pc
                                + " public playlist"
                                + (pc != 1 ? "s" : ""));

        // When there are zero matches, still leave the results area open so the summary + empty scroll
        // region read as "search ran" (all section cards stay hidden).
        showResultsMode(any, hasMusic, uc > 0, pc > 0);

        if (SearchBarState.consumeFullSearchPresentationRequest()) {
            Platform.runLater(this::applyFullSearchPresentation);
        }
    }

    /**
     * After opening the full search page from the top bar (Enter), reset scroll chrome and
     * return keyboard focus to the search field so the flow feels deliberate.
     */
    private void applyFullSearchPresentation() {
        if (resultsScrollPane != null) {
            resultsScrollPane.setVvalue(0);
        }
        if (musicResultsListView != null && !musicResultRows.isEmpty()) {
            musicResultsListView.scrollTo(0);
        }
        if (userResultsListView != null && !filteredUsers.isEmpty()) {
            userResultsListView.scrollTo(0);
        }
        if (publicPlaylistResultsListView != null && !filteredPublicPlaylists.isEmpty()) {
            publicPlaylistResultsListView.scrollTo(0);
        }
        TextField bar = SearchBarState.getBoundField();
        if (bar != null && bar.getScene() != null) {
            bar.requestFocus();
            bar.end();
        }
    }

    // ── Mode switching ────────────────────────────────────────────

    private void showIdleMode() {
        recentSection.setVisible(true); recentSection.setManaged(true);
        resultsScrollPane.setVisible(false); resultsScrollPane.setManaged(false);
    }

    /**
     * @param anyMatch true if the query returned at least one hit in any category; when false, all
     *                 list sections stay hidden but the scroll area remains visible for layout consistency.
     */
    private void showResultsMode(
            boolean anyMatch,
            boolean hasMusic,
            boolean hasUsers,
            boolean hasPublicPlaylists) {
        recentSection.setVisible(false);
        recentSection.setManaged(false);
        resultsScrollPane.setVisible(true);
        resultsScrollPane.setManaged(true);
        boolean showLists = anyMatch;
        userResultsSection.setVisible(showLists && hasUsers);
        userResultsSection.setManaged(showLists && hasUsers);
        publicPlaylistResultsSection.setVisible(showLists && hasPublicPlaylists);
        publicPlaylistResultsSection.setManaged(showLists && hasPublicPlaylists);
        musicResultsSection.setVisible(showLists && hasMusic);
        musicResultsSection.setManaged(showLists && hasMusic);
    }

    // ── FXML handlers ─────────────────────────────────────────────

    @FXML private void handleClearRecentSearches() {
        SessionManager.clearRecentSearches(); recentSearchesListView.refresh();
    }
    // ── Helpers ───────────────────────────────────────────────────

    private static Label placeholder(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + CellStyleKit.getTextMuted() + "; -fx-font-size: 13px;");
        return l;
    }

    private void installKeyboardShortcuts() {
        TextField field = SearchBarState.getBoundField();
        if (field == null || field.getScene() == null) {
            return;
        }
        Scene scene = field.getScene();
        if (scene.getProperties().containsKey("searchEscHandlerInstalled")) {
            return;
        }

        scene.addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        String q = SearchBarState.queryProperty().get();
                        if (q != null && !q.isBlank()) {
                            SearchBarState.clearQuery();
                            event.consume();
                        }
                    }
                });
        scene.getProperties().put("searchEscHandlerInstalled", true);
    }
}
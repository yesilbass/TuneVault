package com.example.tunevaultfx.chrome;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.recommendation.RankedSearchRow;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.search.FullSearchPageOpener;
import com.example.tunevaultfx.search.RecentSearchActions;
import com.example.tunevaultfx.search.SearchRecentItem;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spotify-style popup under the top search field: recent items when empty, live ranked songs and
 * artists while typing, plus “See all results”.
 */
public final class TopBarSearchDropdown {

    private static final String APP_CSS;

    static {
        var res = TopBarSearchDropdown.class.getResource("/com/example/tunevaultfx/app.css");
        APP_CSS = res != null ? res.toExternalForm() : "";
    }

    private final TextField anchor;
    private final Popup popup;
    private final ListView<Object> listView;
    private final ObservableList<Object> rows = FXCollections.observableArrayList();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(140));
    private final RecommendationService recommendationService = new RecommendationService();
    private final SongDAO songDAO = new SongDAO();
    private final ListChangeListener<SearchRecentItem> recentsListener;
    private final AtomicBoolean fullSearchNavigationPending = new AtomicBoolean(false);
    /** Main window last used to show the popup; fallback when resolving {@link Scene} for navigation. */
    private Window lastHostWindow;
    /** Use a mouse-driven row instead of {@link javafx.scene.control.Button}: ActionEvent is unreliable inside {@link Popup}. */
    private final HBox seeAllResultsRow;

    public TopBarSearchDropdown(TextField anchor) {
        this.anchor = anchor;
        this.popup = new Popup();
        popup.setAutoHide(true);
        // Without this, auto-hide can eat the mouse release before controls inside the popup handle clicks.
        popup.setConsumeAutoHidingEvents(true);
        popup.setOnShowing(
                e -> {
                    Scene ps = popup.getScene();
                    if (ps == null) {
                        return;
                    }
                    if (!APP_CSS.isEmpty() && !ps.getStylesheets().contains(APP_CSS)) {
                        ps.getStylesheets().add(APP_CSS);
                    }
                    Scene owner = anchor.getScene();
                    if (owner != null
                            && owner.getRoot() != null
                            && ps.getRoot() != null) {
                        boolean light = owner.getRoot().getStyleClass().contains("theme-light");
                        ps.getRoot().getStyleClass().removeAll("theme-light");
                        if (light) {
                            ps.getRoot().getStyleClass().add("theme-light");
                        }
                    }
                });

        debounce.setOnFinished(e -> Platform.runLater(this::refreshDropdownIfShouldUpdate));

        recentsListener =
                c ->
                        Platform.runLater(
                                () -> {
                                    if (popup.isShowing()) {
                                        refreshDropdownIfShouldUpdate();
                                    }
                                });

        listView = new ListView<>(rows);
        listView.getStyleClass().add("search-dropdown-list");
        listView.setPrefWidth(400);
        listView.setMaxWidth(560);
        listView.setMaxHeight(320);
        listView.setPlaceholder(new Label("No recent searches yet"));
        listView.prefWidthProperty().bind(anchor.widthProperty());

        Label seeAllLabel = new Label("See all results");
        seeAllLabel.getStyleClass().add("search-dropdown-see-all-label");
        seeAllResultsRow = new HBox(seeAllLabel);
        seeAllResultsRow.setAlignment(Pos.CENTER);
        seeAllResultsRow.getStyleClass().add("search-dropdown-see-all-row");
        seeAllResultsRow.setMaxWidth(Double.MAX_VALUE);
        seeAllResultsRow.setPickOnBounds(true);
        seeAllResultsRow.setVisible(false);
        seeAllResultsRow.managedProperty().bind(seeAllResultsRow.visibleProperty());
        seeAllResultsRow.prefWidthProperty().bind(anchor.widthProperty());
        // MOUSE_PRESSED + consume: Popup auto-hide often runs before CLICKED; match top-bar Enter (hide then navigate).
        seeAllResultsRow.addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                e -> {
                    if (e.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    e.consume();
                    requestOpenFullSearchPage();
                });

        listView.setCellFactory(lv -> new SearchDropdownListCell());

        listView.setOnMouseClicked(
                e -> {
                    if (e.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    Object raw = itemAtEvent(e, listView);
                    if (raw == null) {
                        return;
                    }
                    handleActivatedRow(raw);
                });

        listView.addEventHandler(
                KeyEvent.KEY_PRESSED,
                e -> {
                    if (e.getCode() != KeyCode.ENTER) {
                        return;
                    }
                    Object raw = listView.getSelectionModel().getSelectedItem();
                    if (raw == null) {
                        return;
                    }
                    e.consume();
                    handleActivatedRow(raw);
                });

        VBox panel = new VBox(6, listView, seeAllResultsRow);
        panel.getStyleClass().add("search-dropdown-panel");
        popup.getContent().setAll(panel);

        javafx.beans.value.ChangeListener<String> queryKick =
                (obs, o, n) -> {
                    String raw = n != null ? n : "";
                    if (raw.isBlank()) {
                        debounce.stop();
                        Platform.runLater(this::refreshDropdownIfShouldUpdate);
                        return;
                    }
                    if (!popup.isShowing() && !anchor.isFocused()) {
                        return;
                    }
                    debounce.playFromStart();
                };

        anchor.sceneProperty()
                .addListener(
                        (obs, oldScene, newScene) -> {
                            if (oldScene != null) {
                                debounce.stop();
                                popup.hide();
                                SearchBarState.queryProperty().removeListener(queryKick);
                                SessionManager.getRecentSearches().removeListener(recentsListener);
                            }
                            if (newScene != null) {
                                SearchBarState.queryProperty().addListener(queryKick);
                                SessionManager.getRecentSearches().addListener(recentsListener);
                            }
                        });

        if (anchor.getScene() != null) {
            SearchBarState.queryProperty().addListener(queryKick);
            SessionManager.getRecentSearches().addListener(recentsListener);
        }
    }

    /**
     * Resolves the row item for a click without waiting for {@link ListView} selection to update
     * (first-click activation on graphic-heavy cells is otherwise flaky).
     */
    private static Object itemAtEvent(MouseEvent e, ListView<Object> listView) {
        var pick = e.getPickResult();
        if (pick != null && pick.getIntersectedNode() != null) {
            Node n = pick.getIntersectedNode();
            while (n != null) {
                if (n instanceof ListCell<?> cell && !cell.isEmpty()) {
                    Object item = cell.getItem();
                    if (item != null) {
                        return item;
                    }
                }
                n = n.getParent();
            }
        }
        double sx = e.getSceneX();
        double sy = e.getSceneY();
        for (Node node : listView.lookupAll(".list-cell")) {
            if (!(node instanceof ListCell<?> cell) || cell.isEmpty()) {
                continue;
            }
            Bounds b = node.localToScene(node.getBoundsInLocal());
            if (b != null && b.contains(sx, sy)) {
                Object item = cell.getItem();
                if (item != null) {
                    return item;
                }
            }
        }
        return listView.getSelectionModel().getSelectedItem();
    }

    /**
     * Same flow as top-bar Enter ({@link AppTopBarController}): hide the typeahead first, then
     * navigate synchronously. Deferring with {@link Platform#runLater} raced subscriber wiring on the
     * Search page and left results empty or stuck until the next keystroke.
     */
    private void requestOpenFullSearchPage() {
        if (!fullSearchNavigationPending.compareAndSet(false, true)) {
            return;
        }
        debounce.stop();
        try {
            hide();
            Node nav = anchor;
            if (nav.getScene() == null) {
                nav = SearchBarState.getBoundField();
            }
            if (nav != null && nav.getScene() != null) {
                FullSearchPageOpener.open(nav);
            } else {
                Scene host = lastHostWindow != null ? lastHostWindow.getScene() : null;
                if (host != null) {
                    FullSearchPageOpener.open(host);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            fullSearchNavigationPending.set(false);
        }
    }

    private void handleActivatedRow(Object raw) {
        if (raw == SearchDropdownRows.CLEAR_SENTINEL) {
            SessionManager.clearRecentSearches();
            refreshAndShow();
            return;
        }
        if (raw == SearchDropdownRows.NO_RESULTS_SENTINEL) {
            return;
        }
        if (raw instanceof SearchRecentItem item) {
            hide();
            try {
                RecentSearchActions.open(item, anchor);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        if (raw instanceof Song song) {
            hide();
            MusicPlayerController.getInstance().playSingleSong(song);
            SessionManager.addRecentSearch(SearchRecentItem.song(song));
            SessionManager.setSelectedSong(song);
            try {
                SceneUtil.switchScene(anchor, FxmlResources.SONG_PROFILE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        if (raw instanceof SearchDropdownRows.ArtistHit ah) {
            hide();
            SessionManager.setSelectedArtist(ah.name());
            SessionManager.addRecentSearch(SearchRecentItem.artist(ah.name()));
            try {
                SceneUtil.switchScene(anchor, FxmlResources.ARTIST_PROFILE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Applies the current query to the list only when the user is actually using search (field
     * focused or dropdown already open). Avoids popping history on login or when the bar never had
     * an explicit click.
     */
    private void refreshDropdownIfShouldUpdate() {
        if (SearchBarState.isSearchDropdownAutoOpenSuppressed()) {
            if (popup.isShowing()) {
                popup.hide();
            }
            return;
        }
        if (!popup.isShowing() && !anchor.isFocused()) {
            return;
        }
        refreshAndShow();
    }

    /** Rebuild list from current query and show / reposition the popup (call after clear, etc.). */
    public void refreshAndShow() {
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            return;
        }
        lastHostWindow = anchor.getScene().getWindow();
        // Suppress only blocks automatic refreshes (see {@link #refreshDropdownIfShouldUpdate}), not
        // an explicit open from the user — otherwise the dropdown list can stay stale forever.
        if (SearchBarState.isSearchDropdownAutoOpenSuppressed()) {
            rebuildRows();
            listView.refresh();
            if (popup.isShowing()) {
                popup.hide();
            }
            return;
        }
        rebuildRows();
        listView.refresh();
        positionAndShow();
    }

    public void hide() {
        popup.hide();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    /** Call when the user clicks into the search field. */
    public void onFieldActivated() {
        debounce.stop();
        Platform.runLater(this::refreshAndShow);
    }

    private List<Song> snapshotAllSongs() {
        List<Song> catalog = new ArrayList<>();
        if (SessionManager.isSongLibraryReady()) {
            catalog.addAll(SessionManager.getSongLibrary());
        }
        if (catalog.isEmpty()) {
            try {
                Collection<Song> fromDb = songDAO.getAllSongs();
                catalog.addAll(fromDb);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return catalog;
    }

    private void rebuildRows() {
        rows.clear();
        String raw = SearchBarState.queryProperty().get();
        if (raw == null) {
            raw = "";
        }
        String q = raw.trim();
        if (q.isEmpty()) {
            seeAllResultsRow.setVisible(false);
            rows.addAll(SessionManager.getRecentSearches());
            if (!SessionManager.getRecentSearches().isEmpty()) {
                rows.add(SearchDropdownRows.CLEAR_SENTINEL);
            }
            listView.setPlaceholder(new Label("No recent searches yet"));
            return;
        }

        seeAllResultsRow.setVisible(true);
        String username = SessionManager.getCurrentUsername();
        List<Song> lib = snapshotAllSongs();
        ObservableList<Song> libObs = FXCollections.observableArrayList(lib);
        for (RankedSearchRow hit :
                recommendationService.getRankedCatalogSearchRows(username, q, libObs, 10)) {
            if (hit instanceof RankedSearchRow.SongHit sh) {
                rows.add(sh.song());
            } else if (hit instanceof RankedSearchRow.ArtistHit ah) {
                String name = ah.artistName();
                if (name != null && !name.isBlank()) {
                    rows.add(new SearchDropdownRows.ArtistHit(name.trim()));
                }
            }
        }
        if (rows.isEmpty()) {
            rows.add(SearchDropdownRows.NO_RESULTS_SENTINEL);
        }
    }

    private void positionAndShow() {
        Window window = anchor.getScene().getWindow();
        if (window != null) {
            lastHostWindow = window;
        }
        Bounds b = anchor.localToScreen(anchor.getBoundsInLocal());
        if (b == null || window == null) {
            Platform.runLater(this::positionAndShow);
            return;
        }
        double x = b.getMinX();
        double y = b.getMaxY() + 4;
        if (!popup.isShowing()) {
            popup.show(window, x, y);
        } else {
            popup.setX(x);
            popup.setY(y);
        }
    }
}

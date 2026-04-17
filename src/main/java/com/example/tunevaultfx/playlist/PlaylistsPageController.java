package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.playlist.PlaylistCoverGraphic;
import com.example.tunevaultfx.playlist.cell.PlayableSongCell;
import com.example.tunevaultfx.playlist.cell.SearchSongToggleCell;
import com.example.tunevaultfx.playlist.cell.SuggestedSongCell;
import com.example.tunevaultfx.playlist.service.PlaylistPickerService;
import com.example.tunevaultfx.playlist.service.PlaylistSelectionService;
import com.example.tunevaultfx.playlist.service.PlaylistService;
import com.example.tunevaultfx.playlist.service.SongSearchService;
import com.example.tunevaultfx.recommendation.RecommendationConstants;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    /** How songs are ordered in the list view (does not change stored playlist order). */
    private enum PlaylistSortOrder {
        CUSTOM("Playlist order"),
        TITLE("Title"),
        ARTIST("Artist"),
        ALBUM("Album"),
        GENRE("Genre"),
        DURATION("Duration");

        private final String menuLabel;

        PlaylistSortOrder(String menuLabel) {
            this.menuLabel = menuLabel;
        }

        String menuLabel() {
            return menuLabel;
        }
    }

    /** Row height aligned with PlayableSongCell so the page ScrollPane owns vertical scroll. */
    private static final double PLAYLIST_SONG_ROW_HEIGHT = 58;
    /** Row height aligned with SuggestedSongCell / playlists-page list density. */
    private static final double SUGGESTED_SONG_ROW_HEIGHT = 66;
    private static final double PLAYLIST_LIST_EMPTY_HEIGHT = 168;

    private final ListChangeListener<Song> playlistItemsHeightSync = c -> syncPlaylistListHeight();
    private final ListChangeListener<Song> suggestedItemsHeightSync = c -> syncSuggestedListHeight();

    // ── FXML ─────────────────────────────────────────────────────
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
    private Label playlistVisibilityLabel;

    @FXML
    private TextField searchSongsField;
    @FXML
    private VBox searchSongsPanel;
    @FXML
    private VBox suggestionsSection;
    @FXML
    private VBox playlistSongsCard;
    @FXML
    private ScrollPane contentRow;
    @FXML
    private StackPane selectedPlaylistCover;
    @FXML
    private MenuButton playlistOrderMenu;

    // ── Services ──────────────────────────────────────────────────
    /** Active playlist; selection comes from the app sidebar or deep links. */
    private final StringProperty selectedPlaylistName = new SimpleStringProperty();

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

    private final MapChangeListener<String, ObservableList<Song>> playlistKeysChanged =
            c -> Platform.runLater(this::onPlaylistsMapChanged);

    private PlaylistSortOrder playlistSortOrder = PlaylistSortOrder.CUSTOM;
    /** Sorted view of the active playlist; null when no playlist selected. */
    private SortedList<Song> playlistSongsSorted;

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupPlaylistOrderMenu();
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

        profile.getPlaylists().addListener(new WeakMapChangeListener<>(playlistKeysChanged));
        selectedPlaylistName.addListener(
                (obs, previous, name) -> SessionManager.setLastPlaylistsPageSelection(name));
        setupListeners();
        setupInitialPlaylistSelection();
        setupSongCells();
        setupSuggestedSongCells();
        installPlaylistSongsListSizing();
        installSuggestedSongsListSizing();
        hideSearchPanel();

        // Run after scene is rendered so motion reads final layout
        Platform.runLater(() -> {
            UiMotionUtil.playStaggeredEntrance(List.of(playlistSongsCard));
            UiMotionUtil.applyHoverLift(playlistSongsCard);

        });

        contentRow.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            installKeyboardShortcuts();
        });

        SessionManager.setPlaylistPublicUiRefresh(this::applyPlaylistVisibilityBadgeForCurrentSelection);
    }

    private void applyPlaylistVisibilityBadgeForCurrentSelection() {
        applyPlaylistVisibilityBadge(selectedPlaylistName.get());
    }

    private void applyPlaylistVisibilityBadge(String selected) {
        if (playlistVisibilityLabel == null) {
            return;
        }
        if (selected == null || selected.isBlank()) {
            playlistVisibilityLabel.setVisible(false);
            playlistVisibilityLabel.setManaged(false);
            return;
        }
        if (PlaylistNames.isLikedSongs(selected)) {
            playlistVisibilityLabel.setVisible(false);
            playlistVisibilityLabel.setManaged(false);
            return;
        }
        boolean pub = playlistService.isPlaylistPublic(profile, selected);
        playlistVisibilityLabel.setManaged(true);
        playlistVisibilityLabel.setVisible(true);
        playlistVisibilityLabel.setText(pub ? "Public" : "Private");
        playlistVisibilityLabel
                .getStyleClass()
                .removeAll(
                        "profile-playlist-vis",
                        "profile-playlist-vis-public",
                        "profile-playlist-vis-private");
        playlistVisibilityLabel
                .getStyleClass()
                .addAll(
                        "profile-playlist-vis",
                        pub ? "profile-playlist-vis-public" : "profile-playlist-vis-private");
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
            if (contentRow.getScene() != null)
                com.example.tunevaultfx.util.ToastUtil.error(contentRow.getScene(), "Could not load songs from the database.");
        }
    }

    private void onPlaylistsMapChanged() {
        if (profile == null || profile.getPlaylists() == null) {
            return;
        }
        var keys = profile.getPlaylists().keySet();
        String renameTo = SessionManager.peekPendingPlaylistRenameSelection();
        if (renameTo != null && keys.contains(renameTo)) {
            SessionManager.consumePendingPlaylistRenameSelection();
            selectedPlaylistName.set(renameTo);
            attachPlaylistSongsListener();
            updateSelectedPlaylist();
            refreshSearchResultsCellFactory();
            return;
        }
        if (keys.isEmpty()) {
            selectedPlaylistName.set(null);
            attachPlaylistSongsListener();
            updateSelectedPlaylist();
            refreshSearchResultsCellFactory();
            return;
        }
        String sel = selectedPlaylistName.get();
        if (sel == null || !keys.contains(sel)) {
            List<String> names = new ArrayList<>(keys);
            PlaylistNames.sortForDisplay(names);
            selectedPlaylistName.set(names.get(0));
        }
        attachPlaylistSongsListener();
        updateSelectedPlaylist();
        refreshSearchResultsCellFactory();
    }

    // ── Initial playlist selection ─────────────────────────────────

    private void setupInitialPlaylistSelection() {
        if (profile.getPlaylists() == null || profile.getPlaylists().isEmpty()) {
            return;
        }
        List<String> names = new ArrayList<>(profile.getPlaylists().keySet());
        PlaylistNames.sortForDisplay(names);
        String requested = SessionManager.consumeRequestedPlaylistToOpen();
        if (requested != null && names.contains(requested)) {
            selectedPlaylistName.set(requested);
        } else {
            String last = SessionManager.getLastPlaylistsPageSelection();
            if (last != null && names.contains(last)) {
                selectedPlaylistName.set(last);
            } else {
                selectedPlaylistName.set(names.get(0));
            }
        }
        attachPlaylistSongsListener();
        updateSelectedPlaylist();
        refreshSearchResultsCellFactory();
    }

    // ── Listeners ─────────────────────────────────────────────────

    private void setupListeners() {
        // Search field
        searchSongsField.textProperty().addListener((obs, o, n) -> {
            String query = n == null ? "" : n.trim();
            searchResultsListView.setItems(query.isEmpty()
                    ? FXCollections.observableArrayList()
                    : songSearchService.filterSongs(allLibrarySongs, query));
            refreshSearchResultsCellFactory();
        });

        // Like/unlike from mini player → refresh suggestions (taste / Liked Songs changed).
        // When only the *current track* changes, the heart updates to match the new song;
        // that is not a user like action and must not re-run recommendations (list would jump).
        player.currentSongLikedProperty().addListener((obs, o, n) -> {
            if (player.isApplyingTrackDerivedLikedState()) {
                return;
            }
            Platform.runLater(this::refreshSuggestions);
        });

        player.currentSongProperty()
                .addListener((obs, o, n) -> Platform.runLater(() -> {
                    playlistSongsListView.refresh();
                    suggestedSongsListView.refresh();
                    searchResultsListView.refresh();
                }));
        player.playingProperty()
                .addListener((obs, o, n) -> Platform.runLater(() -> {
                    playlistSongsListView.refresh();
                    suggestedSongsListView.refresh();
                    searchResultsListView.refresh();
                }));
        player.currentSourcePlaylistNameProperty()
                .addListener((obs, o, n) -> Platform.runLater(() -> {
                    playlistSongsListView.refresh();
                    suggestedSongsListView.refresh();
                    searchResultsListView.refresh();
                }));
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

        String selected = selectedPlaylistName.get();
        if (selected == null) return;

        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        if (songs == null) return;

        activePlaylistListener = change -> {
            // Small delay ensures DB write from PlaylistService completes first
            Platform.runLater(() -> {
                updateSelectedPlaylist(); // includes suggestion refresh for current selection
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
                        this::getSelectedPlaylistName,
                        this::openArtistProfile,
                        this::openSongProfile));
        refreshSearchResultsCellFactory();
    }

    private void setupSuggestedSongCells() {
        suggestedSongsListView.setCellFactory(lv ->
                new SuggestedSongCell(
                        this::addSuggestedSongToPlaylist,
                        this::playSuggestedSong,
                        this::openArtistProfile,
                        this::openSongProfile,
                        this::showAddToPlaylistPicker));
    }

    private void refreshSearchResultsCellFactory() {
        searchResultsListView.setCellFactory(lv ->
                new SearchSongToggleCell(
                        this::isSongInSelectedPlaylist,
                        this::toggleSongInSelectedPlaylist,
                        this::playSongFromSearchPanel,
                        this::openArtistProfile,
                        this::openSongProfile,
                        this::showAddToPlaylistPicker));
    }

    // ── Suggestion logic ──────────────────────────────────────────

    private void refreshSuggestions() {
        String selected = selectedPlaylistName.get();
        if (selected == null || profile == null) {
            hideSuggestionsSection();
            return;
        }

        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        String username = SessionManager.getCurrentUsername();

        ObservableList<Song> suggestions =
                recommendationService.getSuggestedSongsForPlaylist(
                        username,
                        selected,
                        songs,
                        RecommendationConstants.PLAYLIST_PAGE_SUGGESTION_COUNT);

        boolean empty = suggestions == null || suggestions.isEmpty();
        suggestionsSection.setVisible(true);
        suggestionsSection.setManaged(true);
        applySuggestionSubtitle(selected, empty);
        suggestedSongsListView.setItems(
                empty ? FXCollections.observableArrayList() : suggestions);
        syncSuggestedListHeight();
        suggestedSongsListView.refresh();
    }

    private void applySuggestionSubtitle(String playlistName, boolean emptySuggestions) {
        if (suggestionSubtitleLabel == null) {
            return;
        }
        if (emptySuggestions) {
            if (allLibrarySongs.isEmpty()) {
                suggestionSubtitleLabel.setText(
                        "Add tracks to your library first — then we can suggest what fits this playlist.");
            } else {
                suggestionSubtitleLabel.setText(
                        "Nothing to add from your library right now (everything may already be in \u201c"
                                + playlistName
                                + "\u201d), or the catalog is still loading.");
            }
        } else {
            suggestionSubtitleLabel.setText(
                    "From your library (not already in \u201c"
                            + playlistName
                            + "\u201d). Mostly matched to this playlist\u2019s artists & genres, "
                            + "plus your listening and genre profile.");
        }
    }

    private void hideSuggestionsSection() {
        suggestionsSection.setVisible(false);
        suggestionsSection.setManaged(false);
    }

    private void installPlaylistSongsListSizing() {
        playlistSongsListView.setFixedCellSize(PLAYLIST_SONG_ROW_HEIGHT);
        playlistSongsListView.itemsProperty().addListener((obs, oldList, newList) -> {
            if (oldList != null) {
                oldList.removeListener(playlistItemsHeightSync);
            }
            if (newList != null) {
                newList.addListener(playlistItemsHeightSync);
            }
            syncPlaylistListHeight();
        });
        ObservableList<Song> initial = playlistSongsListView.getItems();
        if (initial != null) {
            initial.addListener(playlistItemsHeightSync);
        }
        syncPlaylistListHeight();
    }

    private void installSuggestedSongsListSizing() {
        suggestedSongsListView.setFixedCellSize(SUGGESTED_SONG_ROW_HEIGHT);
        suggestedSongsListView.itemsProperty().addListener((obs, oldList, newList) -> {
            if (oldList != null) {
                oldList.removeListener(suggestedItemsHeightSync);
            }
            if (newList != null) {
                newList.addListener(suggestedItemsHeightSync);
            }
            syncSuggestedListHeight();
        });
        ObservableList<Song> initial = suggestedSongsListView.getItems();
        if (initial != null) {
            initial.addListener(suggestedItemsHeightSync);
        }
        syncSuggestedListHeight();
    }

    private void syncPlaylistListHeight() {
        ObservableList<Song> items = playlistSongsListView.getItems();
        int n = items == null ? 0 : items.size();
        double h = n == 0 ? PLAYLIST_LIST_EMPTY_HEIGHT : n * PLAYLIST_SONG_ROW_HEIGHT + 4;
        playlistSongsListView.setPrefHeight(h);
        playlistSongsListView.setMinHeight(h);
        playlistSongsListView.setMaxHeight(h);
    }

    private void syncSuggestedListHeight() {
        ObservableList<Song> items = suggestedSongsListView.getItems();
        int n = items == null ? 0 : items.size();
        double h = n == 0 ? SUGGESTED_SONG_ROW_HEIGHT + 4 : n * SUGGESTED_SONG_ROW_HEIGHT + 4;
        suggestedSongsListView.setPrefHeight(h);
        suggestedSongsListView.setMinHeight(h);
        suggestedSongsListView.setMaxHeight(h);
    }

    private void addSuggestedSongToPlaylist(Song song) {
        String selected = selectedPlaylistName.get();
        if (selected == null || song == null) return;
        // PlaylistService updates profile.getPlaylists().get(selected) which is an
        // ObservableList — the attached listener above fires refreshSuggestions() automatically
        playlistService.addSongToPlaylist(profile, selected, song);
    }

    private void playSuggestedSong(Song song) {
        if (song != null) player.playSingleSong(song);
    }

    private void playSongFromSearchPanel(Song song) {
        if (song == null) return;
        ObservableList<Song> items = searchResultsListView.getItems();
        if (items == null) return;
        int idx = items.indexOf(song);
        if (idx < 0) return;
        String selected = selectedPlaylistName.get();
        player.playQueue(items, idx, selected != null ? selected : "");
    }

    private void openArtistProfile(String artist) {
        if (artist == null || artist.isBlank()) return;
        SessionManager.setSelectedArtist(artist.trim());
        try {
            SceneUtil.switchScene(contentRow, FxmlResources.ARTIST_PROFILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openSongProfile(Song song) {
        if (song == null) return;
        SessionManager.setSelectedSong(song);
        try {
            SceneUtil.switchScene(contentRow, FxmlResources.SONG_PROFILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── Playlist actions ──────────────────────────────────────────

    private void playSongFromSelectedPlaylist(Song song) {
        String selected = selectedPlaylistName.get();
        if (selected == null) return;
        ObservableList<Song> queue = playlistSongsListView.getItems();
        if (queue == null) return;
        int index = queue.indexOf(song);
        if (index < 0) return;

        Song current = player.getCurrentSong();
        String source = player.getCurrentSourcePlaylistName();
        if (source == null) {
            source = "";
        }
        if (current != null && current.songId() == song.songId()) {
            if (source.isBlank() || source.equals(selected)) {
                player.togglePlayPause();
                return;
            }
        }
        player.playQueue(queue, index, selected);
    }

    private void showAddToPlaylistPicker(Song song) {
        if (song == null || profile == null) return;
        pickerService.show(song, contentRow.getScene());
    }

    private void removeSongFromSelectedPlaylist(Song song) {
        String selected = selectedPlaylistName.get();
        if (selected == null || song == null) return;
        if (playlistService.removeSongFromPlaylist(profile, selected, song))
            player.onSongRemovedFromPlaylist(selected, song);
        // ObservableList listener handles the refresh automatically
    }

    private String getSelectedPlaylistName() {
        String s = selectedPlaylistName.get();
        return s == null ? "this playlist" : s;
    }

    private boolean isSongInSelectedPlaylist(Song song) {
        String selected = selectedPlaylistName.get();
        if (selected == null || song == null) return false;
        ObservableList<Song> songs = profile.getPlaylists().get(selected);
        return songs != null && songs.stream().anyMatch(s -> s.songId() == song.songId());
    }

    private void toggleSongInSelectedPlaylist(Song song) {
        String selected = selectedPlaylistName.get();
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

    // ── FXML handlers ─────────────────────────────────────────────

    @FXML
    private void handleShowSearchSongs() {
        String selected = selectedPlaylistName.get();
        if (selected == null) {
            com.example.tunevaultfx.util.ToastUtil.info(contentRow.getScene(), "Please select a playlist first.");
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

    // ── UI update ─────────────────────────────────────────────────

    private void updateSelectedPlaylist() {
        String selected = selectedPlaylistName.get();
        if (selected == null) {
            // Never call getItems().clear() here: items may still reference the profile's
            // ObservableList from the last selection, and clear() would wipe the real playlist
            // in memory (DB unchanged until the user logs in again and reloads).
            playlistSongsSorted = null;
            playlistSongsListView.setItems(FXCollections.observableArrayList());
            if (playlistOrderMenu != null) {
                playlistOrderMenu.setDisable(true);
            }
            selectedPlaylistLabel.setText("No playlist selected");
            songCountLabel.setText("Songs: 0");
            totalDurationLabel.setText("Duration: 0:00");
            hideSuggestionsSection();
            refreshPlaylistHeaderCover(null);
            applyPlaylistVisibilityBadge(null);
            return;
        }
        if (playlistOrderMenu != null) {
            playlistOrderMenu.setDisable(false);
        }
        PlaylistSummary summary = selectionService.buildSummary(profile, selected);
        selectedPlaylistLabel.setText(summary.getPlaylistName());
        ObservableList<Song> source = summary.getSongs();
        playlistSongsSorted = new SortedList<>(source, comparatorFor(playlistSortOrder));
        playlistSongsListView.setItems(playlistSongsSorted);
        playlistSongsListView.getSelectionModel().clearSelection();
        songCountLabel.setText("Songs: " + summary.getSongCount());
        totalDurationLabel.setText("Duration: " + summary.getFormattedDuration());
        refreshPlaylistHeaderCover(selected);
        applyPlaylistVisibilityBadge(selected);
        refreshSuggestions();
    }

    private void setupPlaylistOrderMenu() {
        if (playlistOrderMenu == null) {
            return;
        }
        playlistOrderMenu.getItems().clear();
        MenuItem header = new MenuItem("Sort by");
        header.setDisable(true);
        playlistOrderMenu.getItems().add(header);
        ToggleGroup sortGroup = new ToggleGroup();
        for (PlaylistSortOrder order : PlaylistSortOrder.values()) {
            RadioMenuItem item = new RadioMenuItem(order.menuLabel());
            item.setToggleGroup(sortGroup);
            item.setSelected(order == playlistSortOrder);
            item.setOnAction(
                    e -> {
                        if (item.isSelected()) {
                            applyPlaylistSortOrder(order);
                        }
                    });
            playlistOrderMenu.getItems().add(item);
        }
        playlistOrderMenu.setText(playlistSortOrder.menuLabel());
    }

    private void applyPlaylistSortOrder(PlaylistSortOrder order) {
        playlistSortOrder = order;
        if (playlistOrderMenu != null) {
            playlistOrderMenu.setText(order.menuLabel());
        }
        if (playlistSongsSorted != null) {
            playlistSongsSorted.setComparator(comparatorFor(order));
            syncPlaylistListHeight();
        }
    }

    private Comparator<Song> comparatorFor(PlaylistSortOrder order) {
        return switch (order) {
            case CUSTOM -> null;
            case TITLE ->
                    Comparator.comparing((Song s) -> normKey(s.title()))
                            .thenComparingInt(Song::songId);
            case ARTIST ->
                    Comparator.comparing((Song s) -> normKey(s.artist()))
                            .thenComparingInt(Song::songId);
            case ALBUM ->
                    Comparator.comparing((Song s) -> normKey(s.album()))
                            .thenComparingInt(Song::songId);
            case GENRE ->
                    Comparator.comparing((Song s) -> normKey(s.genre()))
                            .thenComparingInt(Song::songId);
            case DURATION ->
                    Comparator.comparingInt(Song::durationSeconds).thenComparingInt(Song::songId);
        };
    }

    private static String normKey(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private void refreshPlaylistHeaderCover(String playlistName) {
        if (selectedPlaylistCover == null) {
            return;
        }
        selectedPlaylistCover.getChildren().clear();
        if (playlistName == null || playlistName.isBlank()) {
            selectedPlaylistCover.getChildren().add(PlaylistCoverGraphic.createPlaceholder(76));
        } else {
            selectedPlaylistCover.getChildren().add(PlaylistCoverGraphic.create(76, playlistName));
        }
    }

    private void installKeyboardShortcuts() {
        if (contentRow.getScene() == null) {
            return;
        }

        if (contentRow.getScene().getProperties().containsKey("playlistEscHandlerInstalled")) {
            return;
        }

        contentRow.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && searchSongsPanel.isVisible()) {
                hideSearchPanel();
                event.consume();
            }
        });
        contentRow.getScene().getProperties().put("playlistEscHandlerInstalled", true);
    }
}

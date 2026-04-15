package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.PlaylistNames;
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
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.OverlayTheme;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;

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

    /** Row height aligned with PlayableSongCell so the page ScrollPane owns vertical scroll. */
    private static final double PLAYLIST_SONG_ROW_HEIGHT = 58;
    /** Row height aligned with SuggestedSongCell / playlists-page list density. */
    private static final double SUGGESTED_SONG_ROW_HEIGHT = 66;
    private static final double PLAYLIST_LIST_EMPTY_HEIGHT = 168;

    private final ListChangeListener<Song> playlistItemsHeightSync = c -> syncPlaylistListHeight();
    private final ListChangeListener<Song> suggestedItemsHeightSync = c -> syncSuggestedListHeight();

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
    @FXML
    private VBox playlistSidebarCard;
    @FXML
    private VBox playlistSongsCard;
    @FXML
    private HBox contentRow;
    @FXML
    private Button deletePlaylistButton;

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

    private final MapChangeListener<String, ObservableList<Song>> playlistKeysChanged =
            c ->
                    Platform.runLater(
                            () -> {
                                loadPlaylistNames();
                                playlistListView.refresh();
                            });

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
        profile.getPlaylists().addListener(new WeakMapChangeListener<>(playlistKeysChanged));
        setupPlaylistListCells();
        setupInitialPlaylistSelection();
        setupListeners();
        deletePlaylistButton
                .disableProperty()
                .bind(
                        Bindings.createBooleanBinding(
                                () -> {
                                    String s =
                                            playlistListView
                                                    .getSelectionModel()
                                                    .getSelectedItem();
                                    return s == null || playlistService.isProtectedPlaylist(s);
                                },
                                playlistListView.getSelectionModel().selectedItemProperty()));
        setupSongCells();
        setupSuggestedSongCells();
        installPlaylistSongsListSizing();
        installSuggestedSongsListSizing();
        hideSearchPanel();
        hideSuggestionsSection();

        // Run after scene is rendered so selection + suggestions both fire correctly
        Platform.runLater(() -> {
            updateSelectedPlaylist();
            attachPlaylistSongsListener();
            refreshSuggestions();

            UiMotionUtil.playStaggeredEntrance(List.of(playlistSidebarCard, playlistSongsCard));
            UiMotionUtil.applyHoverLift(playlistSidebarCard);
            UiMotionUtil.applyHoverLift(playlistSongsCard);

            if (contentRow.getScene() != null) {
                applyResponsiveDensity(contentRow.getScene().getWidth());
            }
        });

        contentRow.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            applyResponsiveDensity(newScene.getWidth());
            newScene.widthProperty().addListener((o, oldW, newW) -> applyResponsiveDensity(newW.doubleValue()));
            installKeyboardShortcuts();
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
            if (contentRow.getScene() != null)
                com.example.tunevaultfx.util.ToastUtil.error(contentRow.getScene(), "Could not load songs from the database.");
        }
    }

    private void loadPlaylistNames() {
        playlistNames.setAll(profile.getPlaylists().keySet());
        playlistListView.setItems(playlistNames);
    }

    // ── Dark playlist list cell factory ───────────────────────────

    private void setupPlaylistListCells() {
        playlistListView.setCellFactory(lv -> new ListCell<>() {

            private final StackPane icon = new StackPane();
            private final Label iconLbl = new Label();
            private final Label nameLbl = new Label();
            private final Label playingBadge = new Label("▸");
            private final Region spacer = new Region();
            private final HBox row = new HBox(10, icon, nameLbl, spacer, playingBadge);

            {
                icon.setPrefSize(28, 28);
                icon.setMinSize(28, 28);
                icon.setMaxSize(28, 28);
                icon.setStyle(
                        "-fx-background-color: rgba(139,92,246,0.15);" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: rgba(139,92,246,0.2);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;");
                iconLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #a78bfa;");
                icon.getChildren().add(iconLbl);
                StackPane.setAlignment(iconLbl, Pos.CENTER);

                playingBadge.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #a78bfa;");

                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(9, 12, 9, 12));
            }

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

                iconLbl.setText(PlaylistNames.glyphForPlaylist(name));
                nameLbl.setText(name);

                boolean activeSource = isActiveSourcePlaylist(name);
                playingBadge.setVisible(activeSource);
                playingBadge.setManaged(activeSource);

                boolean sel = isSelected();
                applySelectionStyle(sel);

                setText(null);
                setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                applySelectionStyle(selected);
            }

            private void applySelectionStyle(boolean selected) {
                boolean light = AppTheme.isLightMode();
                row.setStyle(selected
                        ? "-fx-background-color: "
                        + (light ? "rgba(124,58,237,0.16)" : "rgba(139,92,246,0.18)")
                        + "; -fx-background-radius: 12;"
                        : "-fx-background-color: transparent; -fx-background-radius: 12;");
                String nameColor = selected
                        ? (light ? "#5b21b6" : "#c4b5fd")
                        : (light ? "#1e293b" : "#e2e8f0");
                nameLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + nameColor + ";");
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

        // Like/unlike from mini player → refresh suggestions (taste / Liked Songs changed).
        // When only the *current track* changes, the heart updates to match the new song;
        // that is not a user like action and must not re-run recommendations (list would jump).
        player.currentSongLikedProperty().addListener((obs, o, n) -> {
            if (player.isApplyingTrackDerivedLikedState()) {
                return;
            }
            Platform.runLater(this::refreshSuggestions);
        });

        player.currentSongProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            playlistListView.refresh();
            playlistSongsListView.refresh();
        }));
        player.playingProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            playlistListView.refresh();
            playlistSongsListView.refresh();
        }));
        player.currentSourcePlaylistNameProperty().addListener((obs, o, n) -> Platform.runLater(() -> {
            playlistListView.refresh();
            playlistSongsListView.refresh();
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
                        this::getSelectedPlaylistName,
                        this::openArtistProfile));
        refreshSearchResultsCellFactory();
    }

    private void setupSuggestedSongCells() {
        suggestedSongsListView.setCellFactory(lv ->
                new SuggestedSongCell(
                        this::addSuggestedSongToPlaylist,
                        this::playSuggestedSong,
                        this::openArtistProfile,
                        this::showAddToPlaylistPicker));
    }

    private void refreshSearchResultsCellFactory() {
        searchResultsListView.setCellFactory(lv ->
                new SearchSongToggleCell(
                        this::isSongInSelectedPlaylist,
                        this::toggleSongInSelectedPlaylist,
                        this::playSongFromSearchPanel,
                        this::openArtistProfile,
                        this::showAddToPlaylistPicker));
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
                recommendationService.getSuggestedSongsForPlaylist(username, songs, 12);

        if (suggestions == null || suggestions.isEmpty()) {
            hideSuggestionsSection();
            return;
        }

        showSuggestionsSection(selected);
        suggestedSongsListView.setItems(suggestions);
        syncSuggestedListHeight();
    }

    private void showSuggestionsSection(String playlistName) {
        suggestionsSection.setVisible(true);
        suggestionsSection.setManaged(true);
        if (suggestionSubtitleLabel != null) {
            suggestionSubtitleLabel.setText(
                    "Below your tracks \u2014 based on \u201c"
                            + playlistName
                            + "\u201d and your listening history");
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
        String selected = playlistListView.getSelectionModel().getSelectedItem();
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
        String selected = playlistListView.getSelectionModel().getSelectedItem();
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
        pickerService.show(song, contentRow.getScene());
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

    // ── FXML handlers ─────────────────────────────────────────────

    @FXML
    private void handleShowSearchSongs() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
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

    @FXML
    private void handleCreatePlaylist() {
        Scene scene = contentRow.getScene();
        if (scene == null) return;

        StackPane backdrop = new StackPane();
        backdrop.setStyle(OverlayTheme.backdrop());

        VBox card = new VBox(8);
        card.setMaxWidth(320);
        card.setMaxHeight(220);
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle(OverlayTheme.card());
        card.setOnMouseClicked(e -> e.consume());

        Label title = new Label("Create Playlist");
        title.setStyle(OverlayTheme.title());

        TextField nameField = new TextField();
        nameField.setPromptText("Playlist name");
        nameField.setStyle(OverlayTheme.createPlaylistField());
        nameField.setPrefHeight(38);
        nameField.setMaxHeight(38);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxHeight(44);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(OverlayTheme.secondaryButton());
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(OverlayTheme.secondaryButtonHover()));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(OverlayTheme.secondaryButton()));

        Button createBtn = new Button("Create");
        createBtn.setStyle(OverlayTheme.primaryButton());
        createBtn.setOnMouseEntered(e -> createBtn.setStyle(OverlayTheme.primaryButtonHover()));
        createBtn.setOnMouseExited(e -> createBtn.setStyle(OverlayTheme.primaryButton()));

        HBox actions = new HBox(10, cancelBtn, createBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(createBtn, Priority.ALWAYS);
        createBtn.setMaxWidth(Double.MAX_VALUE);

        Runnable closeOverlay =
                () -> {
                    javafx.animation.FadeTransition ft =
                            new javafx.animation.FadeTransition(javafx.util.Duration.millis(140), backdrop);
                    ft.setToValue(0);
                    ft.setOnFinished(
                            e -> {
                                javafx.scene.Parent p = backdrop.getParent();
                                if (p instanceof StackPane sp) {
                                    sp.getChildren().remove(backdrop);
                                }
                            });
                    ft.play();
                };

        Runnable doCreate =
                () -> {
                    String name = nameField.getText() == null ? "" : nameField.getText().trim();
                    if (name.isEmpty()) {
                        errorLabel.setText("Playlist name cannot be empty.");
                        errorLabel.setVisible(true);
                        errorLabel.setManaged(true);
                        return;
                    }
                    if (!playlistService.createPlaylist(profile, name)) {
                        errorLabel.setText("A playlist with that name already exists.");
                        errorLabel.setVisible(true);
                        errorLabel.setManaged(true);
                        return;
                    }
                    loadPlaylistNames();
                    playlistListView.getSelectionModel().select(name);
                    closeOverlay.run();
                };

        createBtn.setOnAction(e -> doCreate.run());
        cancelBtn.setOnAction(e -> closeOverlay.run());
        backdrop.setOnMouseClicked(e -> closeOverlay.run());

        backdrop.addEventFilter(
                KeyEvent.KEY_PRESSED,
                e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        closeOverlay.run();
                        e.consume();
                    }
                });

        nameField.setOnKeyPressed(
                e -> {
                    if (e.getCode() == KeyCode.ENTER) doCreate.run();
                    if (e.getCode() == KeyCode.ESCAPE) closeOverlay.run();
                });

        card.getChildren().addAll(title, nameField, errorLabel, actions);
        StackPane.setAlignment(card, Pos.CENTER);
        backdrop.getChildren().add(card);

        if (scene.getRoot() instanceof StackPane sp) {
            sp.getChildren().add(backdrop);
        } else {
            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(scene.getRoot(), backdrop);
            scene.setRoot(wrapper);
            SceneUtil.applySavedTheme(scene);
        }

        backdrop.setOpacity(0);
        card.setTranslateY(20);
        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), backdrop);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), card);
        slide.setToY(0);
        new javafx.animation.ParallelTransition(fade, slide).play();

        Platform.runLater(nameField::requestFocus);
    }

    @FXML
    private void handleDeletePlaylist() {
        String selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.example.tunevaultfx.util.ToastUtil.info(contentRow.getScene(), "Please select a playlist to delete.");
            return;
        }
        if (playlistService.isProtectedPlaylist(selected)) {
            com.example.tunevaultfx.util.ToastUtil.info(
                    contentRow.getScene(),
                    PlaylistNames.LIKED_SONGS
                            + " can\u2019t be deleted \u2014 it\u2019s a default playlist for songs you like.");
            return;
        }
        showDeletePlaylistConfirmOverlay(selected);
    }

    private void showDeletePlaylistConfirmOverlay(String playlistName) {
        Scene scene = contentRow.getScene();
        if (scene == null) {
            return;
        }

        StackPane backdrop = new StackPane();
        backdrop.setStyle(OverlayTheme.backdrop());

        VBox card = new VBox(10);
        card.setMaxWidth(380);
        card.setMaxHeight(260);
        card.setMinHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle(OverlayTheme.card());
        card.setOnMouseClicked(e -> e.consume());

        Label title = new Label("Delete playlist?");
        title.setStyle(OverlayTheme.title());

        Label msg =
                new Label(
                        "Are you sure you want to delete \u201c"
                                + playlistName
                                + "\u201d? This cannot be undone.");
        msg.setWrapText(true);
        msg.setStyle(OverlayTheme.subtitle());

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle(OverlayTheme.secondaryButton());
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(OverlayTheme.secondaryButtonHover()));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(OverlayTheme.secondaryButton()));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle(OverlayTheme.dangerButton());
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(OverlayTheme.dangerButtonHover()));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(OverlayTheme.dangerButton()));

        HBox actions = new HBox(10, cancelBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);

        Runnable closeOverlay =
                () -> {
                    javafx.animation.FadeTransition ft =
                            new javafx.animation.FadeTransition(javafx.util.Duration.millis(140), backdrop);
                    ft.setToValue(0);
                    ft.setOnFinished(
                            e -> {
                                javafx.scene.Parent p = backdrop.getParent();
                                if (p instanceof StackPane sp) {
                                    sp.getChildren().remove(backdrop);
                                }
                            });
                    ft.play();
                };

        cancelBtn.setOnAction(e -> closeOverlay.run());
        deleteBtn.setOnAction(
                e -> {
                    if (!playlistService.deletePlaylist(profile, playlistName)) {
                        com.example.tunevaultfx.util.ToastUtil.error(
                                scene, "This playlist cannot be deleted.");
                    } else {
                        loadPlaylistNames();
                        if (!playlistNames.isEmpty()) {
                            playlistListView.getSelectionModel().selectFirst();
                        }
                    }
                    closeOverlay.run();
                    e.consume();
                });
        backdrop.setOnMouseClicked(e -> closeOverlay.run());

        backdrop.addEventFilter(
                KeyEvent.KEY_PRESSED,
                e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        closeOverlay.run();
                        e.consume();
                    }
                });

        card.getChildren().addAll(title, msg, actions);
        StackPane.setAlignment(card, Pos.CENTER);
        backdrop.getChildren().add(card);

        if (scene.getRoot() instanceof StackPane sp) {
            sp.getChildren().add(backdrop);
        } else {
            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(scene.getRoot(), backdrop);
            scene.setRoot(wrapper);
            SceneUtil.applySavedTheme(scene);
        }

        backdrop.setOpacity(0);
        card.setTranslateY(20);
        javafx.animation.FadeTransition fade =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(180), backdrop);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(200), card);
        slide.setToY(0);
        new javafx.animation.ParallelTransition(fade, slide).play();
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
        playlistSongsListView.getSelectionModel().clearSelection();
        songCountLabel.setText("Songs: " + summary.getSongCount());
        totalDurationLabel.setText("Duration: " + summary.getFormattedDuration());
    }

    private void applyResponsiveDensity(double width) {
        boolean compact = width < 1420;
        contentRow.setSpacing(compact ? 14 : 20);
        playlistSidebarCard.setPrefWidth(compact ? 250 : 280);
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

    private boolean isActiveSourcePlaylist(String playlistName) {
        if (playlistName == null || playlistName.isBlank()) {
            return false;
        }
        if (player.getCurrentSong() == null || !player.isPlaying()) {
            return false;
        }
        return playlistName.equals(player.getCurrentSourcePlaylistName());
    }
}

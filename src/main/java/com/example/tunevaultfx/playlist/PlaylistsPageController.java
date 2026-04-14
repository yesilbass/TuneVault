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
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
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

                iconLbl.setText("Liked Songs".equals(name) ? "♥" : "♫");
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
                row.setStyle(selected
                        ? "-fx-background-color: rgba(139,92,246,0.18); -fx-background-radius: 12;"
                        : "-fx-background-color: transparent; -fx-background-radius: 12;");
                nameLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: "
                        + (selected ? "#c4b5fd" : "#e2e8f0") + ";");
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
                        this::openArtistProfile));
    }

    private void refreshSearchResultsCellFactory() {
        searchResultsListView.setCellFactory(lv ->
                new SearchSongToggleCell(
                        this::isSongInSelectedPlaylist,
                        this::toggleSongInSelectedPlaylist,
                        this::playSongFromSearchPanel,
                        this::openArtistProfile));
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
            SceneUtil.switchScene(contentRow, "artist-profile-page.fxml");
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
        backdrop.setStyle("-fx-background-color: rgba(3,2,14,0.72);");

        VBox card = new VBox(16);
        card.setMaxWidth(380);
        card.setPadding(new javafx.geometry.Insets(28, 28, 22, 28));
        card.setStyle(
            "-fx-background-color: #0f0f1c;" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: rgba(139,92,246,0.16);" +
            "-fx-border-radius: 24; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.70), 48, 0, 0, 16);");
        card.setOnMouseClicked(e -> e.consume());

        Label title = new Label("Create Playlist");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #eeeef6;");

        Label sub = new Label("Enter a name for your new playlist");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #9d9db8;");

        TextField nameField = new TextField();
        nameField.setPromptText("Playlist name");
        nameField.setPrefHeight(44);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444;");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        Button createBtn = new Button("Create");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setStyle(
            "-fx-background-color: #8b5cf6; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 14; -fx-padding: 12 24 12 24;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.45), 14, 0, 0, 4);");
        createBtn.setOnMouseEntered(e -> createBtn.setStyle(
            "-fx-background-color: #7c3aed; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 14; -fx-padding: 12 24 12 24; -fx-cursor: hand;"));
        createBtn.setOnMouseExited(e -> createBtn.setStyle(
            "-fx-background-color: #8b5cf6; -fx-text-fill: white;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 14; -fx-padding: 12 24 12 24; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.45), 14, 0, 0, 4);"));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #9d9db8;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 14; -fx-padding: 10 24 10 24; -fx-cursor: hand;" +
            "-fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 14; -fx-border-width: 1;");
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: #eeeef6;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 14; -fx-padding: 10 24 10 24; -fx-cursor: hand;" +
            "-fx-border-color: rgba(255,255,255,0.14); -fx-border-radius: 14; -fx-border-width: 1;"));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.06); -fx-text-fill: #9d9db8;" +
            "-fx-font-size: 14px; -fx-font-weight: bold;" +
            "-fx-background-radius: 14; -fx-padding: 10 24 10 24; -fx-cursor: hand;" +
            "-fx-border-color: rgba(255,255,255,0.08); -fx-border-radius: 14; -fx-border-width: 1;"));

        Runnable closeOverlay = () -> {
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(140), backdrop);
            ft.setToValue(0);
            ft.setOnFinished(e -> {
                if (scene.getRoot() instanceof StackPane sp) sp.getChildren().remove(backdrop);
            });
            ft.play();
        };

        Runnable doCreate = () -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                errorLabel.setText("Playlist name cannot be empty.");
                errorLabel.setVisible(true); errorLabel.setManaged(true);
                return;
            }
            if (!playlistService.createPlaylist(profile, name)) {
                errorLabel.setText("A playlist with that name already exists.");
                errorLabel.setVisible(true); errorLabel.setManaged(true);
                return;
            }
            loadPlaylistNames();
            playlistListView.getSelectionModel().select(name);
            closeOverlay.run();
        };

        createBtn.setOnAction(e -> doCreate.run());
        cancelBtn.setOnAction(e -> closeOverlay.run());
        backdrop.setOnMouseClicked(e -> closeOverlay.run());

        nameField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) doCreate.run();
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) closeOverlay.run();
        });

        card.getChildren().addAll(title, sub, nameField, errorLabel, createBtn, cancelBtn);
        StackPane.setAlignment(card, javafx.geometry.Pos.CENTER);
        backdrop.getChildren().add(card);

        if (scene.getRoot() instanceof StackPane sp) {
            sp.getChildren().add(backdrop);
        } else {
            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(scene.getRoot(), backdrop);
            scene.setRoot(wrapper);
        }

        backdrop.setOpacity(0);
        card.setTranslateY(40);
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
            javafx.util.Duration.millis(180), backdrop);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(220), card);
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
        if (!playlistService.deletePlaylist(profile, selected)) {
            com.example.tunevaultfx.util.ToastUtil.error(contentRow.getScene(), "Liked Songs cannot be deleted.");
            return;
        }
        loadPlaylistNames();
        if (!playlistNames.isEmpty()) playlistListView.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleBackToMenu(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.goBack((Node) event.getSource());
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

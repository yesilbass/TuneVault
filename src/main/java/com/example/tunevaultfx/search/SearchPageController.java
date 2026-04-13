package com.example.tunevaultfx.search;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;

public class SearchPageController {

    // ── FXML fields ───────────────────────────────────────────────
    @FXML private TextField  searchField;
    @FXML private Label      resultsSummaryLabel;

    @FXML private VBox       recentSection;
    @FXML private ListView<SearchRecentItem> recentSearchesListView;

    @FXML private ScrollPane resultsScrollPane;
    @FXML private VBox       songResultsSection;
    @FXML private VBox       artistResultsSection;
    @FXML private ListView<Song>   songResultsListView;
    @FXML private ListView<String> artistResultsListView;

    // ── Data ──────────────────────────────────────────────────────
    private final ObservableList<Song>   allSongs        = FXCollections.observableArrayList();
    private final ObservableList<Song>   filteredSongs   = FXCollections.observableArrayList();
    private final ObservableList<String> filteredArtists = FXCollections.observableArrayList();

    // ── Services ──────────────────────────────────────────────────
    private final SongDAO               songDAO               = new SongDAO();
    private final RecommendationService recommendationService = new RecommendationService();
    private final MusicPlayerController player                = MusicPlayerController.getInstance();

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadSongs();

        songResultsListView.setItems(filteredSongs);
        artistResultsListView.setItems(filteredArtists);
        recentSearchesListView.setItems(SessionManager.getRecentSearches());

        songResultsListView.setPlaceholder(placeholderLabel("No matching songs"));
        artistResultsListView.setPlaceholder(placeholderLabel("No matching artists"));
        recentSearchesListView.setPlaceholder(placeholderLabel("No recent searches yet"));

        setupSongCells();
        setupArtistCells();
        setupRecentCells();
        setupListeners();
        setupDoubleClickActions();

        showIdleMode();
    }

    private void loadSongs() {
        try {
            allSongs.setAll(songDAO.getAllSongs());
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.info("Database Error", "Could not load songs from the database.");
        }
    }

    // ── Cell factories ────────────────────────────────────────────

    private void setupSongCells() {
        songResultsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) { setText(null); setGraphic(null); return; }

                // Icon
                StackPane icon = new StackPane();
                icon.setPrefSize(42, 42);
                icon.setMinSize(42, 42);
                icon.setMaxSize(42, 42);
                icon.setStyle("-fx-background-color: rgba(139,92,246,0.12);" +
                        "-fx-background-radius: 11;" +
                        "-fx-border-color: rgba(139,92,246,0.18);" +
                        "-fx-border-radius: 11;" +
                        "-fx-border-width: 1;");
                Label iconLabel = new Label("♫");
                iconLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #7c6fa6;");
                icon.getChildren().add(iconLabel);
                StackPane.setAlignment(iconLabel, Pos.CENTER);

                // Text
                Label title = new Label(song.title());
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

                String meta = song.artist() == null ? "" : song.artist();
                if (song.genre() != null && !song.genre().isBlank())
                    meta += " · " + song.genre();
                Label metaLabel = new Label(meta);
                metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #3d3d5c;");

                VBox textBox = new VBox(3, title, metaLabel);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                // Duration
                Label dur = new Label(formatDuration(song.durationSeconds()));
                dur.setStyle("-fx-font-size: 12px; -fx-text-fill: #3d3d5c;");

                HBox row = new HBox(12, icon, textBox, dur);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");
                row.setOnMouseEntered(e ->
                        row.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 12;"));
                row.setOnMouseExited(e ->
                        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;"));

                setText(null);
                setGraphic(row);
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
                if (empty || artist == null || artist.isBlank()) { setText(null); setGraphic(null); return; }

                // Avatar circle
                StackPane avatar = new StackPane();
                avatar.setPrefSize(42, 42);
                avatar.setMinSize(42, 42);
                avatar.setMaxSize(42, 42);
                avatar.setStyle("-fx-background-color: rgba(244,63,94,0.12);" +
                        "-fx-background-radius: 21;" +
                        "-fx-border-color: rgba(244,63,94,0.18);" +
                        "-fx-border-radius: 21;" +
                        "-fx-border-width: 1;");
                Label initial = new Label(artist.substring(0, 1).toUpperCase());
                initial.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #9b4f62;");
                avatar.getChildren().add(initial);
                StackPane.setAlignment(initial, Pos.CENTER);

                Label name = new Label(artist);
                name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
                Label type = new Label("Artist");
                type.setStyle("-fx-font-size: 12px; -fx-text-fill: #3d3d5c;");
                VBox textBox = new VBox(3, name, type);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                HBox row = new HBox(12, avatar, textBox);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");
                row.setOnMouseEntered(e ->
                        row.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 12;"));
                row.setOnMouseExited(e ->
                        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;"));

                setText(null);
                setGraphic(row);
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
                if (empty || item == null) { setText(null); setGraphic(null); return; }

                boolean isSong = item.getType() == SearchRecentItem.Type.SONG;

                StackPane icon = new StackPane();
                icon.setPrefSize(42, 42);
                icon.setMinSize(42, 42);
                icon.setMaxSize(42, 42);
                icon.setStyle(isSong
                        ? "-fx-background-color: rgba(139,92,246,0.12); -fx-background-radius: 11; -fx-border-color: rgba(139,92,246,0.18); -fx-border-radius: 11; -fx-border-width: 1;"
                        : "-fx-background-color: rgba(244,63,94,0.12); -fx-background-radius: 21; -fx-border-color: rgba(244,63,94,0.18); -fx-border-radius: 21; -fx-border-width: 1;");
                Label iconLabel = new Label(isSong ? "♫" : "◎");
                iconLabel.setStyle(isSong
                        ? "-fx-font-size: 17px; -fx-text-fill: #7c6fa6;"
                        : "-fx-font-size: 17px; -fx-text-fill: #9b4f62;");
                icon.getChildren().add(iconLabel);
                StackPane.setAlignment(iconLabel, Pos.CENTER);

                Label primary = new Label(item.getPrimaryText());
                primary.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
                Label secondary = new Label(item.getSecondaryText());
                secondary.setStyle("-fx-font-size: 12px; -fx-text-fill: #3d3d5c;");
                VBox textBox = new VBox(3, primary, secondary);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                Label tag = new Label(isSong ? "Song" : "Artist");
                tag.setStyle(isSong
                        ? "-fx-background-color: rgba(139,92,246,0.12); -fx-text-fill: #7c6fa6; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 3 10 3 10; -fx-border-color: rgba(139,92,246,0.18); -fx-border-radius: 10; -fx-border-width: 1;"
                        : "-fx-background-color: rgba(244,63,94,0.1); -fx-text-fill: #9b4f62; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 3 10 3 10; -fx-border-color: rgba(244,63,94,0.15); -fx-border-radius: 10; -fx-border-width: 1;");

                HBox row = new HBox(12, icon, textBox, tag);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");
                row.setOnMouseEntered(e ->
                        row.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 12;"));
                row.setOnMouseExited(e ->
                        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;"));

                setText(null);
                setGraphic(row);
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
        searchField.textProperty().addListener((obs, o, n) -> runSearch(n));
    }

    private void setupDoubleClickActions() {
        songResultsListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Song s = songResultsListView.getSelectionModel().getSelectedItem();
                if (s != null) {
                    player.playSingleSong(s);
                    SessionManager.addRecentSearch(SearchRecentItem.song(s));
                }
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
            filteredSongs.clear();
            filteredArtists.clear();
            resultsSummaryLabel.setText("");
            showIdleMode();
            return;
        }

        String username = SessionManager.getCurrentUsername();

        filteredSongs.setAll(
                recommendationService.getRankedSearchSongs(username, query, allSongs, 50));
        filteredArtists.setAll(
                recommendationService.getRankedSearchArtists(username, query, allSongs, 20));

        int sc = filteredSongs.size();
        int ac = filteredArtists.size();

        resultsSummaryLabel.setText(
                sc == 0 && ac == 0
                        ? "No results for \"" + query + "\""
                        : sc + " song" + (sc != 1 ? "s" : "") + "  \u00B7  " + ac + " artist" + (ac != 1 ? "s" : ""));

        showResultsMode(sc > 0, ac > 0);
    }

    // ── Mode switching ────────────────────────────────────────────

    private void showIdleMode() {
        recentSection.setVisible(true);
        recentSection.setManaged(true);
        resultsScrollPane.setVisible(false);
        resultsScrollPane.setManaged(false);
    }

    private void showResultsMode(boolean hasSongs, boolean hasArtists) {
        recentSection.setVisible(false);
        recentSection.setManaged(false);
        resultsScrollPane.setVisible(true);
        resultsScrollPane.setManaged(true);
        songResultsSection.setVisible(hasSongs);
        songResultsSection.setManaged(hasSongs);
        artistResultsSection.setVisible(hasArtists);
        artistResultsSection.setManaged(hasArtists);
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
        searchField.clear();
        filteredSongs.clear();
        filteredArtists.clear();
        resultsSummaryLabel.setText("");
        showIdleMode();
    }

    @FXML private void handleClearRecentSearches() {
        SessionManager.clearRecentSearches();
        recentSearchesListView.refresh();
    }

    @FXML
    private void handleBackToMenu(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    // ── Helpers ───────────────────────────────────────────────────

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "";
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    private Label placeholderLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #3d3d5c; -fx-font-size: 13px;");
        return l;
    }
}

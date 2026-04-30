package com.example.tunevaultfx.search;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ArtistDAO;
import com.example.tunevaultfx.db.ArtistFollowDAO;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import com.example.tunevaultfx.util.ToastUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Artist Profile page controller.
 * Songs filtered by songId-safe artist match, loaded on background thread.
 * Cells use CellStyleKit for consistent, readable contrast.
 */
public class ArtistProfileController {

    @FXML private Label          artistAvatarInitialLabel;
    @FXML private Label          artistNameLabel;
    @FXML private Label          artistSummaryLabel;
    @FXML private Label          artistStatTracksLabel;
    @FXML private Label          artistStatRuntimeLabel;
    @FXML private Label          artistStatAlbumsLabel;
    @FXML private Button         playDiscographyButton;
    @FXML private Button         shuffleDiscographyButton;
    @FXML private Button         followArtistButton;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ListView<Song> artistSongsListView;

    private final ObservableList<Song> artistSongs = FXCollections.observableArrayList();
    private final SongDAO              songDAO      = new SongDAO();
    private final ArtistDAO            artistDAO    = new ArtistDAO();
    private final ArtistFollowDAO      artistFollowDAO = new ArtistFollowDAO();
    private final MusicPlayerController player      = MusicPlayerController.getInstance();

    private String artistName;
    private Optional<Integer> resolvedArtistId = Optional.empty();

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        artistName = SessionManager.getSelectedArtist();

        if (artistName == null || artistName.isBlank()) {
            artistNameLabel.setText("Unknown Artist");
            if (artistSummaryLabel != null) {
                artistSummaryLabel.setText("No artist selected.");
                artistSummaryLabel.setVisible(true);
                artistSummaryLabel.setManaged(true);
            }
            artistSongsListView.setItems(artistSongs);
            applyAvatarInitial("?");
            clearStatsPlaceholders();
            if (playDiscographyButton != null) {
                playDiscographyButton.setDisable(true);
                playDiscographyButton.setVisible(false);
                playDiscographyButton.setManaged(false);
            }
            if (shuffleDiscographyButton != null) {
                shuffleDiscographyButton.setDisable(true);
                shuffleDiscographyButton.setVisible(false);
                shuffleDiscographyButton.setManaged(false);
            }
            if (sortComboBox != null) {
                sortComboBox.setDisable(true);
            }
            hideFollowArtistButton();
            return;
        }
        if (playDiscographyButton != null) {
            playDiscographyButton.setVisible(true);
            playDiscographyButton.setManaged(true);
            playDiscographyButton.setDisable(true);
        }
        if (shuffleDiscographyButton != null) {
            shuffleDiscographyButton.setVisible(true);
            shuffleDiscographyButton.setManaged(true);
            shuffleDiscographyButton.setDisable(true);
        }
        if (sortComboBox != null) {
            sortComboBox.setDisable(false);
        }

        artistNameLabel.setText(artistName);
        if (artistSummaryLabel != null) {
            artistSummaryLabel.setText("Loading\u2026");
            artistSummaryLabel.setVisible(true);
            artistSummaryLabel.setManaged(true);
        }
        applyAvatarInitial(artistName);
        artistSongsListView.setItems(artistSongs);

        setupSortCombo();
        setupCells();
        installArtistPlayerRefreshListeners();
        loadArtistSongs();
        resolveArtistIdAsync();
    }

    private void setupSortCombo() {
        if (sortComboBox == null) {
            return;
        }
        if (sortComboBox.getItems().isEmpty()) {
            sortComboBox.setItems(
                    FXCollections.observableArrayList(
                            "Title A–Z",
                            "Title Z–A",
                            "Album A–Z",
                            "Duration (longest)",
                            "Duration (shortest)"));
            sortComboBox.getSelectionModel().selectFirst();
            sortComboBox
                    .valueProperty()
                    .addListener(
                            (obs, o, n) -> {
                                if (n != null) {
                                    applySort();
                                }
                            });
        }
    }

    private void applySort() {
        if (sortComboBox == null || artistSongs.isEmpty()) {
            return;
        }
        String key = sortComboBox.getValue();
        if (key == null) {
            return;
        }
        Comparator<Song> cmp =
                switch (key) {
                    case "Title Z–A" ->
                            Comparator.comparing(
                                            (Song s) -> nullToEmpty(s.title()),
                                            String.CASE_INSENSITIVE_ORDER)
                                    .reversed();
                    case "Album A–Z" ->
                            Comparator.comparing(
                                    (Song s) -> nullToEmpty(s.album()), String.CASE_INSENSITIVE_ORDER);
                    case "Duration (longest)" ->
                            Comparator.comparingInt(Song::durationSeconds).reversed();
                    case "Duration (shortest)" ->
                            Comparator.comparingInt(Song::durationSeconds);
                    default ->
                            Comparator.comparing(
                                    (Song s) -> nullToEmpty(s.title()), String.CASE_INSENSITIVE_ORDER);
                };
        List<Song> sorted = artistSongs.stream().sorted(cmp).collect(Collectors.toList());
        artistSongs.setAll(sorted);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void applyAvatarInitial(String name) {
        if (artistAvatarInitialLabel == null) {
            return;
        }
        if (name == null || name.isBlank()) {
            artistAvatarInitialLabel.setText("♫");
            return;
        }
        String t = name.trim();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                artistAvatarInitialLabel.setText(String.valueOf(Character.toUpperCase(c)));
                return;
            }
        }
        artistAvatarInitialLabel.setText("♫");
    }

    private void clearStatsPlaceholders() {
        if (artistStatTracksLabel != null) {
            artistStatTracksLabel.setText("—");
        }
        if (artistStatRuntimeLabel != null) {
            artistStatRuntimeLabel.setText("—");
        }
        if (artistStatAlbumsLabel != null) {
            artistStatAlbumsLabel.setText("—");
        }
    }

    private void refreshDiscographyDerivedUi() {
        int n = artistSongs.size();
        if (artistStatTracksLabel != null) {
            artistStatTracksLabel.setText(String.valueOf(n));
        }
        int totalSec =
                artistSongs.stream().mapToInt(Song::durationSeconds).filter(d -> d > 0).sum();
        if (artistStatRuntimeLabel != null) {
            artistStatRuntimeLabel.setText(formatTotalRuntime(totalSec));
        }
        long albumCount =
                artistSongs.stream()
                        .map(Song::album)
                        .filter(a -> a != null && !a.isBlank())
                        .distinct()
                        .count();
        if (artistStatAlbumsLabel != null) {
            artistStatAlbumsLabel.setText(albumCount > 0 ? String.valueOf(albumCount) : "—");
        }

        applyHeroSummaryAfterLoad(n);
    }

    /** Shown only while loading, on error, or when there are zero tracks — never “library” framing. */
    private void applyHeroSummaryAfterLoad(int trackCount) {
        if (artistSummaryLabel == null) {
            return;
        }
        if (trackCount > 0) {
            artistSummaryLabel.setText("");
            artistSummaryLabel.setVisible(false);
            artistSummaryLabel.setManaged(false);
        } else {
            artistSummaryLabel.setText("No tracks to show yet.");
            artistSummaryLabel.setVisible(true);
            artistSummaryLabel.setManaged(true);
        }
    }

    private static String formatTotalRuntime(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "—";
        }
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        if (h > 0) {
            return h + "h " + m + "m";
        }
        if (m > 0) {
            return m + "m";
        }
        int s = totalSeconds % 60;
        return s + "s";
    }

    private void installArtistPlayerRefreshListeners() {
        if (artistSongsListView == null) {
            return;
        }
        Runnable refresh = () -> Platform.runLater(() -> artistSongsListView.refresh());
        player.currentSongProperty().addListener((obs, o, n) -> refresh.run());
        player.playingProperty().addListener((obs, o, n) -> refresh.run());
        player.currentSourcePlaylistNameProperty().addListener((obs, o, n) -> refresh.run());
    }

    private void hideFollowArtistButton() {
        if (followArtistButton != null) {
            followArtistButton.setVisible(false);
            followArtistButton.setManaged(false);
        }
    }

    /**
     * When name-based lookup is empty or races behind the song list, use the first loaded track's
     * {@code artist_id} (same source as the discography).
     */
    private void tryResolveArtistIdFromLoadedSongs() {
        if (resolvedArtistId.isEmpty() && !artistSongs.isEmpty()) {
            Song first = artistSongs.get(0);
            if (first != null) {
                try {
                    Integer aid = songDAO.findArtistIdBySongId(first.songId());
                    if (aid != null && aid > 0) {
                        resolvedArtistId = Optional.of(aid);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        updateFollowArtistUi();
    }

    private void resolveArtistIdAsync() {
        final String target = artistName.trim();
        Task<Optional<Integer>> task =
                new Task<>() {
                    @Override
                    protected Optional<Integer> call() throws Exception {
                        return artistDAO.findArtistIdByNameIgnoreCase(target);
                    }
                };
        task.setOnSucceeded(
                e -> {
                    Optional<Integer> fromName =
                            task.getValue() != null ? task.getValue() : Optional.empty();
                    // Do not overwrite a resolved id (e.g. from song fallback) when name lookup is empty.
                    if (fromName.isPresent()) {
                        resolvedArtistId = fromName;
                    }
                    updateFollowArtistUi();
                });
        task.setOnFailed(
                e -> {
                    resolvedArtistId = Optional.empty();
                    hideFollowArtistButton();
                });
        Thread t = new Thread(task, "artist-id-resolve");
        t.setDaemon(true);
        t.start();
    }

    private void updateFollowArtistUi() {
        if (followArtistButton == null) {
            return;
        }
        String user = SessionManager.getCurrentUsername();
        if (user == null
                || user.isBlank()
                || resolvedArtistId.isEmpty()) {
            hideFollowArtistButton();
            return;
        }
        followArtistButton.setVisible(true);
        followArtistButton.setManaged(true);
        try {
            boolean following =
                    artistFollowDAO.isFollowingArtist(user, resolvedArtistId.get());
            followArtistButton.setText(following ? "Following" : "Follow");
        } catch (SQLException ex) {
            ex.printStackTrace();
            followArtistButton.setText("Follow");
        }
    }

    @FXML
    private void handleFollowArtist() {
        String user = SessionManager.getCurrentUsername();
        javafx.scene.Scene scene =
                followArtistButton != null && followArtistButton.getScene() != null
                        ? followArtistButton.getScene()
                        : (artistSongsListView != null ? artistSongsListView.getScene() : null);
        if (user == null || user.isBlank()) {
            if (scene != null) {
                ToastUtil.info(scene, "Sign in to follow artists.");
            }
            return;
        }
        if (resolvedArtistId.isEmpty()) {
            if (scene != null) {
                ToastUtil.info(scene, "Could not load artist info for following. Try refreshing this page.");
            }
            return;
        }
        int aid = resolvedArtistId.get();
        try {
            if (artistFollowDAO.isFollowingArtist(user, aid)) {
                artistFollowDAO.unfollowArtist(user, aid);
            } else {
                artistFollowDAO.followArtist(user, aid);
            }
            updateFollowArtistUi();
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (scene != null) {
                ToastUtil.info(scene, "Could not update artist follow.");
            }
        }
    }

    // ── Background loading ────────────────────────────────────────

    private void loadArtistSongs() {
        final String target = artistName.trim();

        Task<List<Song>> task = new Task<>() {
            @Override
            protected List<Song> call() throws Exception {
                ObservableList<Song> source = SessionManager.isSongLibraryReady()
                        ? SessionManager.getSongLibrary()
                        : songDAO.getAllSongs();
                return source.stream()
                        .filter(s -> s != null && s.artist() != null
                                && s.artist().trim().equalsIgnoreCase(target))
                        .toList();
            }
        };

        task.setOnSucceeded(e -> {
            artistSongs.setAll(task.getValue());
            refreshDiscographyDerivedUi();
            applySort();
            int count = task.getValue().size();
            if (playDiscographyButton != null) {
                playDiscographyButton.setDisable(count == 0);
            }
            if (shuffleDiscographyButton != null) {
                shuffleDiscographyButton.setDisable(count == 0);
            }
            tryResolveArtistIdFromLoadedSongs();
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            if (artistSummaryLabel != null) {
                artistSummaryLabel.setText("Could not load songs.");
                artistSummaryLabel.setVisible(true);
                artistSummaryLabel.setManaged(true);
            }
            clearStatsPlaceholders();
            AlertUtil.info("Error", "Could not load songs for this artist.");
            if (playDiscographyButton != null) {
                playDiscographyButton.setDisable(true);
            }
            if (shuffleDiscographyButton != null) {
                shuffleDiscographyButton.setDisable(true);
            }
        });

        Thread t = new Thread(task, "artist-songs-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── Cell factory ──────────────────────────────────────────────

    private void setupCells() {
        artistSongsListView.setCellFactory(lv -> new ListCell<>() {
            private ContextMenu activeSongMenu;

            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (activeSongMenu != null) {
                    activeSongMenu.hide();
                    activeSongMenu = null;
                }
                if (empty || song == null) {
                    setText(null); setGraphic(null);
                    setBackground(Background.EMPTY);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                boolean isCurrent = isCurrentArtistTrack(song);
                boolean audioPlaying = isCurrent && player.isPlaying();

                int index = artistSongs.indexOf(song) + 1;
                Label  num  = CellStyleKit.trackNumber(index);
                StackPane icon = CellStyleKit.iconBox("♫", CellStyleKit.Palette.PURPLE, false);
                VBox   text = CellStyleKit.textBox(
                        song.title(), CellStyleKit.albumMeta(song.album(), song.genre()));
                Label  dur  = CellStyleKit.duration(song.durationSeconds());

                if (isCurrent && !text.getChildren().isEmpty()) {
                    text.getChildren().get(0).setStyle(
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                                    + CellStyleKit.getAccentTitle()
                                    + ";");
                }

                Button playBtn = new Button(audioPlaying ? "⏸" : "▶");
                playBtn.setFocusTraversable(false);
                playBtn.setPrefSize(34, 34);
                playBtn.setMinSize(34, 34);
                playBtn.setMaxSize(34, 34);
                playBtn.setStyle(artistPlayButtonStyle(isCurrent, audioPlaying));
                playBtn.setOnMouseEntered(
                        e -> playBtn.setStyle(artistPlayButtonHoverStyle(isCurrent, audioPlaying)));
                playBtn.setOnMouseExited(
                        e -> playBtn.setStyle(artistPlayButtonStyle(isCurrent, audioPlaying)));
                playBtn.setOnAction(
                        ev -> {
                            playSongFromArtistList(song);
                            ev.consume();
                        });

                StackPane leadSlot = new StackPane();
                leadSlot.setMinSize(34, 34);
                leadSlot.setPrefSize(34, 34);
                leadSlot.setMaxSize(34, 34);
                leadSlot.getChildren().addAll(num, playBtn);
                StackPane.setAlignment(num, Pos.CENTER);
                StackPane.setAlignment(playBtn, Pos.CENTER);
                playBtn.setVisible(false);
                playBtn.setManaged(false);

                Runnable showTrackNumber =
                        () -> {
                            playBtn.setVisible(false);
                            playBtn.setManaged(false);
                            num.setVisible(true);
                            num.setManaged(true);
                        };
                Runnable showPlayControl =
                        () -> {
                            num.setVisible(false);
                            num.setManaged(false);
                            playBtn.setVisible(true);
                            playBtn.setManaged(true);
                        };
                showTrackNumber.run();

                HBox row =
                        CellStyleKit.row(
                                leadSlot,
                                icon,
                                text,
                                new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }},
                                dur);
                Region edgeBar = CellStyleKit.nowPlayingEdgeBar();
                edgeBar.setVisible(isCurrent);
                edgeBar.setManaged(isCurrent);
                row.getChildren().add(0, edgeBar);

                row.setStyle(isCurrent ? CellStyleKit.getRowPlaying() : CellStyleKit.getRowDefault());
                row.setOnMouseEntered(
                        e -> {
                            if (isCurrent) {
                                row.setStyle(CellStyleKit.getRowPlaying());
                            } else {
                                row.setStyle(CellStyleKit.getRowHover());
                            }
                            showPlayControl.run();
                        });
                row.setOnMouseExited(
                        e -> {
                            if (isCurrent) {
                                row.setStyle(CellStyleKit.getRowPlaying());
                            } else {
                                row.setStyle(CellStyleKit.getRowDefault());
                            }
                            showTrackNumber.run();
                        });

                row.setOnMouseClicked(ev -> {
                    if (ev.getButton() == MouseButton.SECONDARY) {
                        showSongContextMenu(song, row, ev.getScreenX(), ev.getScreenY());
                        ev.consume();
                        return;
                    }
                    if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                        playSongFromArtistList(song);
                        ev.consume();
                    }
                });

                row.addEventFilter(
                        ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                        ev -> {
                            Song s = getItem();
                            if (s == null || isEmpty()) {
                                return;
                            }
                            showSongContextMenu(s, row, ev.getScreenX(), ev.getScreenY());
                            ev.consume();
                        });

                setText(null); setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }

            private void showSongContextMenu(Song song, Node anchor, double screenX, double screenY) {
                if (activeSongMenu != null) {
                    activeSongMenu.hide();
                    activeSongMenu = null;
                }
                ContextMenu menu =
                        SongContextMenuBuilder.build(
                                song,
                                anchor,
                                SongContextMenuBuilder.Spec.general());
                activeSongMenu = menu;
                menu.show(anchor, screenX, screenY);
            }

            @Override public void updateSelected(boolean s) { super.updateSelected(false); }
        });
    }

    private boolean isCurrentArtistTrack(Song song) {
        if (song == null || artistName == null || player.getCurrentSong() == null) {
            return false;
        }
        String src = player.getCurrentSourcePlaylistName();
        if (src == null) {
            src = "";
        }
        return src.equals(artistName.trim())
                && player.getCurrentSong().songId() == song.songId();
    }

    private void playSongFromArtistList(Song song) {
        if (song == null || artistName == null) {
            return;
        }
        int idx = artistSongs.indexOf(song);
        if (idx < 0) {
            return;
        }
        String source = artistName.trim();
        Song cur = player.getCurrentSong();
        String curSrc = player.getCurrentSourcePlaylistName();
        if (curSrc == null) {
            curSrc = "";
        }
        if (cur != null && cur.songId() == song.songId() && curSrc.equals(source)) {
            player.togglePlayPause();
        } else {
            player.playQueue(artistSongs, idx, source);
        }
    }

    private static String artistPlayButtonStyle(boolean currentRow, boolean playingAudio) {
        if (!currentRow) {
            if (AppTheme.isLightMode()) {
                return "-fx-background-color: transparent;"
                        + "-fx-text-fill: #64748b;"
                        + "-fx-font-size: 13px; -fx-font-weight: bold;"
                        + "-fx-background-radius: 17;";
            }
            return "-fx-background-color: transparent;"
                    + "-fx-text-fill: #58586e;"
                    + "-fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        if (playingAudio) {
            if (AppTheme.isLightMode()) {
                return "-fx-background-color: #7c3aed;"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 12px; -fx-font-weight: bold;"
                        + "-fx-background-radius: 17;";
            }
            return "-fx-background-color: rgba(139,92,246,0.25);"
                    + "-fx-text-fill: #c4b5fd;"
                    + "-fx-font-size: 12px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: rgba(124,58,237,0.22);"
                    + "-fx-text-fill: #5b21b6;"
                    + "-fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        return "-fx-background-color: rgba(139,92,246,0.14);"
                + "-fx-text-fill: #ddd6fe;"
                + "-fx-font-size: 13px; -fx-font-weight: bold;"
                + "-fx-background-radius: 17;";
    }

    private static String artistPlayButtonHoverStyle(boolean currentRow, boolean playingAudio) {
        if (!currentRow) {
            if (AppTheme.isLightMode()) {
                return "-fx-background-color: rgba(124,58,237,0.14);"
                        + "-fx-text-fill: #5b21b6;"
                        + "-fx-font-size: 13px; -fx-font-weight: bold;"
                        + "-fx-background-radius: 17;";
            }
            return "-fx-background-color: rgba(255,255,255,0.1);"
                    + "-fx-text-fill: #f2f2fa;"
                    + "-fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        return artistPlayButtonStyle(currentRow, playingAudio);
    }

    @FXML
    private void handlePlayDiscography() {
        if (artistName == null || artistName.isBlank() || artistSongs.isEmpty()) {
            if (artistSongsListView != null && artistSongsListView.getScene() != null) {
                ToastUtil.info(artistSongsListView.getScene(), "No songs to play for this artist.");
            }
            return;
        }
        player.setShuffleEnabled(false);
        player.playQueue(artistSongs, 0, artistName.trim());
        if (artistSongsListView != null && artistSongsListView.getScene() != null) {
            ToastUtil.success(artistSongsListView.getScene(), "Playing discography");
        }
    }

    @FXML
    private void handleShuffleDiscography() {
        if (artistName == null || artistName.isBlank() || artistSongs.isEmpty()) {
            if (artistSongsListView != null && artistSongsListView.getScene() != null) {
                ToastUtil.info(artistSongsListView.getScene(), "No songs to shuffle for this artist.");
            }
            return;
        }
        player.playQueue(artistSongs, 0, artistName.trim());
        player.setShuffleEnabled(true);
        if (artistSongsListView != null && artistSongsListView.getScene() != null) {
            ToastUtil.success(artistSongsListView.getScene(), "Shuffling discography");
        }
    }

}
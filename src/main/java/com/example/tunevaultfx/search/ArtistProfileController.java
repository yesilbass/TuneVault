package com.example.tunevaultfx.search;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;

/**
 * Artist Profile page controller.
 * Songs filtered by songId-safe artist match, loaded on background thread.
 * Cells use CellStyleKit for consistent, readable contrast.
 */
public class ArtistProfileController {

    @FXML private Label          artistNameLabel;
    @FXML private Label          artistSummaryLabel;
    @FXML private ListView<Song> artistSongsListView;

    private final ObservableList<Song> artistSongs = FXCollections.observableArrayList();
    private final SongDAO              songDAO      = new SongDAO();
    private final MusicPlayerController player      = MusicPlayerController.getInstance();

    private String artistName;

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        artistName = SessionManager.getSelectedArtist();

        if (artistName == null || artistName.isBlank()) {
            artistNameLabel.setText("Unknown Artist");
            artistSummaryLabel.setText("No artist selected.");
            artistSongsListView.setItems(artistSongs);
            return;
        }

        artistNameLabel.setText(artistName);
        artistSummaryLabel.setText("Loading\u2026");
        artistSongsListView.setItems(artistSongs);

        setupCells();
        setupDoubleClick();
        loadArtistSongs();
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
            int count = task.getValue().size();
            artistSummaryLabel.setText(count + " song" + (count != 1 ? "s" : ""));
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            artistSummaryLabel.setText("Could not load songs.");
            AlertUtil.info("Error", "Could not load songs for this artist.");
        });

        Thread t = new Thread(task, "artist-songs-loader");
        t.setDaemon(true);
        t.start();
    }

    // ── Cell factory ──────────────────────────────────────────────

    private void setupCells() {
        artistSongsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) {
                    setText(null); setGraphic(null);
                    setBackground(Background.EMPTY);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                boolean isPlaying = player.getCurrentSong() != null
                        && player.getCurrentSong().songId() == song.songId();

                int index = artistSongs.indexOf(song) + 1;
                Label  num  = CellStyleKit.trackNumber(index);
                StackPane icon = CellStyleKit.iconBox("♫", CellStyleKit.Palette.PURPLE, false);
                VBox   text = CellStyleKit.textBox(
                        song.title(), CellStyleKit.albumMeta(song.album(), song.genre()));
                Label  dur  = CellStyleKit.duration(song.durationSeconds());

                // Override title color if playing
                if (isPlaying && !text.getChildren().isEmpty()) {
                    text.getChildren().get(0).setStyle(
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #c4b5fd;");
                }

                HBox row = CellStyleKit.row(num, icon, text,
                        new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, dur);
                CellStyleKit.markPlaying(row, isPlaying);

                setText(null); setGraphic(row);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }

            @Override public void updateSelected(boolean s) { super.updateSelected(false); }
        });
    }

    // ── Interactions ──────────────────────────────────────────────

    private void setupDoubleClick() {
        artistSongsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Song selected = artistSongsListView.getSelectionModel().getSelectedItem();
                if (selected == null) return;
                player.playQueue(artistSongs, artistSongs.indexOf(selected), artistName);
                SessionManager.setSelectedSong(selected);
                try { SceneUtil.switchScene(artistSongsListView, "song-details-page.fxml"); }
                catch (IOException e) { e.printStackTrace(); }
            }
        });
    }

    @FXML
    private void handleBackToSearch(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "search-page.fxml");
    }
}
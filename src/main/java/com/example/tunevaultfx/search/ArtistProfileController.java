package com.example.tunevaultfx.search;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AlertUtil;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.*;

import java.io.IOException;

public class ArtistProfileController {

    @FXML private Label artistNameLabel;
    @FXML private Label artistSummaryLabel;
    @FXML private ListView<Song> artistSongsListView;

    private final ObservableList<Song> artistSongs = FXCollections.observableArrayList();
    private final SongDAO songDAO = new SongDAO();
    private final MusicPlayerController player = MusicPlayerController.getInstance();

    private String artistName;

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
        artistSongsListView.setItems(artistSongs);

        setupSongCells();
        setupDoubleClick();
        loadArtistSongs();
    }

    private void loadArtistSongs() {
        try {
            ObservableList<Song> allSongs = FXCollections.observableArrayList(songDAO.getAllSongs());
            artistSongs.clear();
            for (Song song : allSongs) {
                if (song != null && artistName.equalsIgnoreCase(song.artist())) {
                    artistSongs.add(song);
                }
            }
            artistSummaryLabel.setText(artistSongs.size() + " song" + (artistSongs.size() != 1 ? "s" : ""));
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.info("Database Error", "Could not load songs for this artist.");
        }
    }

    private void setupSongCells() {
        artistSongsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) { setText(null); setGraphic(null); return; }

                // Track number
                int index = artistSongs.indexOf(song) + 1;
                Label numLabel = new Label(String.format("%02d", index));
                numLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #3f3f5a; -fx-font-weight: bold; -fx-min-width: 28;");

                // Music icon
                StackPane iconBox = new StackPane();
                iconBox.setPrefSize(40, 40);
                iconBox.setMinSize(40, 40);
                iconBox.setMaxSize(40, 40);
                iconBox.setStyle("-fx-background-color: rgba(139,92,246,0.1); -fx-background-radius: 10; -fx-border-color: rgba(139,92,246,0.15); -fx-border-radius: 10; -fx-border-width: 1;");
                Label icon = new Label("♫");
                icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b5fa6;");
                iconBox.getChildren().add(icon);
                StackPane.setAlignment(icon, Pos.CENTER);

                // Text
                Label title = new Label(song.title());
                title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");

                String meta = "";
                if (song.album() != null && !song.album().isBlank()) meta += song.album();
                if (song.genre() != null && !song.genre().isBlank()) meta += (meta.isEmpty() ? "" : " · ") + song.genre();
                Label metaLabel = new Label(meta);
                metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #3f3f5a;");

                VBox textBox = new VBox(3, title, metaLabel);
                HBox.setHgrow(textBox, Priority.ALWAYS);

                // Duration
                String dur = song.durationSeconds() > 0
                        ? (song.durationSeconds() / 60) + ":" + String.format("%02d", song.durationSeconds() % 60)
                        : "";
                Label durLabel = new Label(dur);
                durLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #3f3f5a;");

                HBox row = new HBox(14, numLabel, iconBox, textBox, durLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;");
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 12;"));
                row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 12;"));

                setText(null);
                setGraphic(row);
                setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(false);
            }
        });
    }

    private void setupDoubleClick() {
        artistSongsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Song selected = artistSongsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    int index = artistSongs.indexOf(selected);
                    player.playQueue(artistSongs, index, artistName);
                    SessionManager.setSelectedSong(selected);
                    try {
                        SceneUtil.switchScene(artistSongsListView, "song-details-page.fxml");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @FXML
    private void handleBackToSearch(javafx.event.ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "search-page.fxml");
    }
}

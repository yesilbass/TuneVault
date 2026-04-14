package com.example.tunevaultfx.playlist.service;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a dark-themed in-scene overlay for picking playlists to add a song to.
 */
public class PlaylistPickerService {

    private final PlaylistService playlistService = new PlaylistService();

    public void show(Song song) {
        show(song, null);
    }

    public void show(Song song, Scene scene) {
        UserProfile profile = SessionManager.getCurrentUserProfile();
        if (song == null || profile == null || profile.getPlaylists().isEmpty()) return;

        if (scene == null) return;

        // Backdrop
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(3,2,14,0.72);");
        backdrop.setOnMouseClicked(e -> closeOverlay(scene, backdrop));

        // Card
        VBox card = new VBox(16);
        card.setMaxWidth(400);
        card.setMaxHeight(480);
        card.setPadding(new Insets(28, 24, 20, 24));
        card.setStyle(
            "-fx-background-color: #0f0f1c;" +
            "-fx-background-radius: 24;" +
            "-fx-border-color: rgba(139,92,246,0.16);" +
            "-fx-border-radius: 24;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.70), 48, 0, 0, 16);");
        card.setOnMouseClicked(e -> e.consume());

        // Title
        Label title = new Label("Add to Playlist");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #eeeef6;");

        Label subtitle = new Label("Choose playlists for \u201c" + song.title() + "\u201d");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #9d9db8;");
        subtitle.setWrapText(true);

        VBox header = new VBox(4, title, subtitle);

        // Playlist list
        List<String> names = new ArrayList<>(profile.getPlaylists().keySet());
        ListView<String> listView = new ListView<>(FXCollections.observableArrayList(names));
        listView.setPrefHeight(300);
        listView.setFocusTraversable(false);
        listView.getSelectionModel().clearSelection();
        listView.setCellFactory(lv -> new PickerCell(profile, song, listView));
        VBox.setVgrow(listView, Priority.ALWAYS);

        // Done button
        Button doneBtn = new Button("Done");
        doneBtn.setMaxWidth(Double.MAX_VALUE);
        doneBtn.setStyle(
            "-fx-background-color: #8b5cf6;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 12 24 12 24;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.45), 14, 0, 0, 4);");
        doneBtn.setOnMouseEntered(e -> doneBtn.setStyle(
            "-fx-background-color: #7c3aed;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 12 24 12 24;" +
            "-fx-cursor: hand;"));
        doneBtn.setOnMouseExited(e -> doneBtn.setStyle(
            "-fx-background-color: #8b5cf6;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 12 24 12 24;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(139,92,246,0.45), 14, 0, 0, 4);"));
        doneBtn.setOnAction(e -> closeOverlay(scene, backdrop));

        card.getChildren().addAll(header, listView, doneBtn);
        StackPane.setAlignment(card, Pos.CENTER);
        backdrop.getChildren().add(card);

        backdrop.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeOverlay(scene, backdrop);
                e.consume();
            }
        });

        // Add to scene
        if (scene.getRoot() instanceof StackPane sp) {
            sp.getChildren().add(backdrop);
        } else {
            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(scene.getRoot(), backdrop);
            scene.setRoot(wrapper);
        }

        // Animate in
        backdrop.setOpacity(0);
        card.setTranslateY(40);
        FadeTransition fade = new FadeTransition(Duration.millis(180), backdrop);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), card);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();

        backdrop.requestFocus();
    }

    private void closeOverlay(Scene scene, StackPane backdrop) {
        FadeTransition fade = new FadeTransition(Duration.millis(140), backdrop);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            if (scene.getRoot() instanceof StackPane sp) {
                sp.getChildren().remove(backdrop);
            }
        });
        fade.play();
    }

    private boolean songIsInPlaylist(UserProfile profile, String playlistName, Song song) {
        List<Song> songs = profile.getPlaylists().get(playlistName);
        if (songs == null || song == null) return false;
        return songs.stream().anyMatch(s -> s != null && s.songId() == song.songId());
    }

    // ── Dark-themed cell ──────────────────────────────────────────

    private class PickerCell extends ListCell<String> {
        private final UserProfile profile;
        private final Song song;
        private final ListView<String> parentList;

        private final HBox row = new HBox(12);
        private final StackPane icon = new StackPane();
        private final Label iconLbl = new Label();
        private final Label nameLabel = new Label();
        private final Region spacer = new Region();
        private final Button actionBtn = new Button();

        private static final String BTN_ADD =
            "-fx-background-color: rgba(139,92,246,0.16);" +
            "-fx-text-fill: #c4b5fd;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(139,92,246,0.28);" +
            "-fx-border-radius: 16; -fx-border-width: 1;" +
            "-fx-cursor: hand;";

        private static final String BTN_ADDED =
            "-fx-background-color: rgba(34,197,94,0.18);" +
            "-fx-text-fill: #86efac;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(34,197,94,0.28);" +
            "-fx-border-radius: 16; -fx-border-width: 1;" +
            "-fx-cursor: hand;";

        private static final String BTN_REMOVE =
            "-fx-background-color: rgba(239,68,68,0.14);" +
            "-fx-text-fill: #fca5a5;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 16;" +
            "-fx-cursor: hand;";

        PickerCell(UserProfile profile, Song song, ListView<String> parentList) {
            this.profile = profile;
            this.song = song;
            this.parentList = parentList;

            icon.setPrefSize(32, 32);
            icon.setMinSize(32, 32);
            icon.setMaxSize(32, 32);
            icon.setStyle(
                "-fx-background-color: rgba(139,92,246,0.14);" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: rgba(139,92,246,0.22);" +
                "-fx-border-radius: 10; -fx-border-width: 1;");
            iconLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #a78bfa;");
            icon.getChildren().add(iconLbl);
            StackPane.setAlignment(iconLbl, Pos.CENTER);

            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #eeeef6;");

            actionBtn.setPrefSize(38, 32);
            actionBtn.setMinSize(38, 32);
            actionBtn.setMaxSize(38, 32);
            actionBtn.setFocusTraversable(false);

            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 12, 8, 12));
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 14;");
            row.getChildren().addAll(icon, nameLabel, spacer, actionBtn);

            row.setOnMouseEntered(e ->
                row.setStyle("-fx-background-color: rgba(139,92,246,0.06); -fx-background-radius: 14;"));
            row.setOnMouseExited(e ->
                row.setStyle("-fx-background-color: transparent; -fx-background-radius: 14;"));

            actionBtn.setOnMouseEntered(e -> {
                String name = getItem();
                if (name != null && songIsInPlaylist(profile, name, song)) {
                    actionBtn.setText("−");
                    actionBtn.setStyle(BTN_REMOVE);
                }
            });
            actionBtn.setOnMouseExited(e -> {
                String name = getItem();
                if (name != null) refreshBtn(name);
            });

            actionBtn.setOnAction(e -> {
                String name = getItem();
                if (name != null) toggle(name);
                e.consume();
            });
            row.setOnMouseClicked(e -> {
                String name = getItem();
                if (name != null && !isEmpty()) toggle(name);
                e.consume();
            });

            setOnMousePressed(e -> {
                if (!isEmpty() && getListView() != null)
                    getListView().getSelectionModel().clearSelection();
            });
        }

        @Override
        protected void updateItem(String name, boolean empty) {
            super.updateItem(name, empty);
            if (empty || name == null) {
                setText(null); setGraphic(null);
                setBackground(Background.EMPTY);
                setStyle("-fx-background-color: transparent;");
                return;
            }
            iconLbl.setText("Liked Songs".equals(name) ? "♥" : "♫");
            nameLabel.setText(name);
            refreshBtn(name);
            setText(null); setGraphic(row);
            setBackground(Background.EMPTY);
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
        }

        @Override
        public void updateSelected(boolean s) { super.updateSelected(false); }

        private void toggle(String name) {
            boolean wasIn = songIsInPlaylist(profile, name, song);
            if (wasIn) playlistService.removeSongFromPlaylist(profile, name, song);
            else playlistService.addSongToPlaylist(profile, name, song);
            flashRow(!wasIn);
            parentList.refresh();
        }

        private void refreshBtn(String name) {
            boolean inPlaylist = songIsInPlaylist(profile, name, song);
            actionBtn.setText(inPlaylist ? "✓" : "+");
            actionBtn.setStyle(inPlaylist ? BTN_ADDED : BTN_ADD);
        }

        private void flashRow(boolean added) {
            String flash = added
                ? "-fx-background-color: rgba(34,197,94,0.12); -fx-background-radius: 14;"
                : "-fx-background-color: rgba(239,68,68,0.10); -fx-background-radius: 14;";
            row.setStyle(flash);
            PauseTransition p = new PauseTransition(Duration.millis(200));
            p.setOnFinished(e -> row.setStyle("-fx-background-color: transparent; -fx-background-radius: 14;"));
            p.play();
        }
    }
}

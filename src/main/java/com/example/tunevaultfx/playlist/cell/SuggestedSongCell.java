package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Cell for a suggested song row.
 *
 * Layout (left → right):
 *   [+]  [♪ icon]  Title / Artist · Genre  ···  [▶ Play]
 *
 * [+]    adds the song to the currently selected playlist.
 * [▶ Play] plays the song immediately via the mini player.
 */
public class SuggestedSongCell extends ListCell<Song> {

    private final Consumer<Song> onAdd;
    private final Consumer<Song> onPlay;

    private final HBox    root       = new HBox(12);
    private final Button  addButton  = new Button("+");
    private final StackPane iconBox  = new StackPane();
    private final Label   iconLabel  = new Label("♪");
    private final VBox    textBox    = new VBox(3);
    private final Label   titleLabel = new Label();
    private final Label   metaLabel  = new Label();
    private final Region  spacer     = new Region();
    private final Button  playButton = new Button("▶  Play");

    public SuggestedSongCell(Consumer<Song> onAdd, Consumer<Song> onPlay) {
        this.onAdd  = onAdd;
        this.onPlay = onPlay;

        // --- Add button ---
        styleAddDefault();
        addButton.setPrefSize(36, 36);
        addButton.setMinSize(36, 36);
        addButton.setMaxSize(36, 36);
        addButton.setFocusTraversable(false);
        addButton.setOnMouseEntered(e -> addButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white;" +
                        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 18;"));
        addButton.setOnMouseExited(e -> styleAddDefault());

        // --- Icon box ---
        iconBox.setPrefSize(40, 40);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        iconBox.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #dbeafe, #bfdbfe);" +
                        "-fx-background-radius: 12;");
        iconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #3b82f6;");
        iconBox.getChildren().add(iconLabel);
        StackPane.setAlignment(iconLabel, Pos.CENTER);

        // --- Text ---
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        textBox.getChildren().addAll(titleLabel, metaLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // --- Spacer ---
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- Play button ---
        stylePlayDefault();
        playButton.setPrefHeight(34);
        playButton.setFocusTraversable(false);
        playButton.setOnMouseEntered(e -> playButton.setStyle(
                "-fx-background-color: #059669; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 17; -fx-padding: 0 14 0 14;"));
        playButton.setOnMouseExited(e -> stylePlayDefault());

        // --- Root row ---
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(10, 14, 10, 14));
        root.setStyle("-fx-background-color: transparent; -fx-background-radius: 16;");
        root.getChildren().addAll(addButton, iconBox, textBox, spacer, playButton);

        root.setOnMouseEntered(e ->
                root.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 16;"));
        root.setOnMouseExited(e ->
                root.setStyle("-fx-background-color: transparent; -fx-background-radius: 16;"));

        // --- Wire actions ---
        addButton.setOnAction(e -> {
            Song song = getItem();
            if (song != null && onAdd != null) {
                flashAddConfirm();
                onAdd.accept(song);
            }
            e.consume();
        });

        playButton.setOnAction(e -> {
            Song song = getItem();
            if (song != null && onPlay != null) {
                onPlay.accept(song);
            }
            e.consume();
        });

        // Prevent list selection highlight
        setOnMousePressed(e -> {
            if (!isEmpty() && getListView() != null) {
                getListView().getSelectionModel().clearSelection();
                e.consume();
            }
        });
    }

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);

        if (empty || song == null) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        titleLabel.setText(song.title());

        String meta = song.artist() == null ? "" : song.artist();
        if (song.genre() != null && !song.genre().isBlank()) {
            meta += " · " + song.genre();
        }
        metaLabel.setText(meta);

        styleAddDefault();   // reset in case it was flashing

        setText(null);
        setGraphic(root);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(false); // never show selection highlight
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void styleAddDefault() {
        addButton.setText("+");
        addButton.setStyle(
                "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb;" +
                        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 18;");
    }

    private void stylePlayDefault() {
        playButton.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white;" +
                        "-fx-font-size: 12px; -fx-font-weight: bold;" +
                        "-fx-background-radius: 17; -fx-padding: 0 14 0 14;");
    }

    private void flashAddConfirm() {
        addButton.setText("✓");
        addButton.setStyle(
                "-fx-background-color: #10b981; -fx-text-fill: white;" +
                        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 18;");

        PauseTransition pause = new PauseTransition(Duration.millis(700));
        pause.setOnFinished(e -> styleAddDefault());
        pause.play();
    }
}

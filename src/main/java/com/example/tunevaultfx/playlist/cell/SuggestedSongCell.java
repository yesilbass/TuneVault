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
 * Cell for a suggested song row — fully dark themed.
 *
 * Layout:  [+]  [♫ icon]  Title / meta  ···  [▶ Play]
 *
 * Key fixes:
 *  - All colors use the dark palette (no #dbeafe, #f1f5f9, #0f172a)
 *  - Empty branch explicitly sets Background.EMPTY so recycled cells don't
 *    retain a white background
 *  - Hover is a very subtle dark tint, not a white flash
 */
public class SuggestedSongCell extends ListCell<Song> {

    private final Consumer<Song> onAdd;
    private final Consumer<Song> onPlay;

    private final HBox     root        = new HBox(12);
    private final Button   addButton   = new Button("+");
    private final StackPane iconBox    = new StackPane();
    private final Label    iconLabel   = new Label("♫");
    private final VBox     textBox     = new VBox(3);
    private final Label    titleLabel  = new Label();
    private final Label    metaLabel   = new Label();
    private final Region   spacer      = new Region();
    private final Button   playButton  = new Button("▶  Play");

    // ── Styles ────────────────────────────────────────────────────

    private static final String ADD_DEFAULT =
            "-fx-background-color: rgba(139,92,246,0.15);" +
                    "-fx-text-fill: #a78bfa;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(139,92,246,0.22);" +
                    "-fx-border-radius: 18; -fx-border-width: 1;";

    private static final String ADD_HOVER =
            "-fx-background-color: #8b5cf6;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18;";

    private static final String ADD_CONFIRM =
            "-fx-background-color: rgba(34,197,94,0.2);" +
                    "-fx-text-fill: #22c55e;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(34,197,94,0.28);" +
                    "-fx-border-radius: 18; -fx-border-width: 1;";

    private static final String PLAY_DEFAULT =
            "-fx-background-color: rgba(34,197,94,0.15);" +
                    "-fx-text-fill: #22c55e;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;" +
                    "-fx-border-color: rgba(34,197,94,0.22);" +
                    "-fx-border-radius: 17; -fx-border-width: 1;" +
                    "-fx-padding: 0 14 0 14;";

    private static final String PLAY_HOVER =
            "-fx-background-color: #22c55e;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;" +
                    "-fx-padding: 0 14 0 14;";

    private static final String ROW_DEFAULT =
            "-fx-background-color: transparent; -fx-background-radius: 14;";

    private static final String ROW_HOVER =
            "-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 14;";

    // ─────────────────────────────────────────────────────────────

    public SuggestedSongCell(Consumer<Song> onAdd, Consumer<Song> onPlay) {
        this.onAdd  = onAdd;
        this.onPlay = onPlay;

        // Add button
        addButton.setPrefSize(36, 36);
        addButton.setMinSize(36, 36);
        addButton.setMaxSize(36, 36);
        addButton.setFocusTraversable(false);
        addButton.setStyle(ADD_DEFAULT);
        addButton.setOnMouseEntered(e -> addButton.setStyle(ADD_HOVER));
        addButton.setOnMouseExited(e  -> addButton.setStyle(ADD_DEFAULT));

        // Icon box — dark, matches card background
        iconBox.setPrefSize(40, 40);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);
        iconBox.setStyle(
                "-fx-background-color: rgba(139,92,246,0.12);" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(139,92,246,0.18);" +
                        "-fx-border-radius: 12; -fx-border-width: 1;");
        iconLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #6b5fa6;");
        iconBox.getChildren().add(iconLabel);
        StackPane.setAlignment(iconLabel, Pos.CENTER);

        // Text — light colors on dark background
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        metaLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #52525b;");
        textBox.getChildren().addAll(titleLabel, metaLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Play button
        playButton.setPrefHeight(34);
        playButton.setFocusTraversable(false);
        playButton.setStyle(PLAY_DEFAULT);
        playButton.setOnMouseEntered(e -> playButton.setStyle(PLAY_HOVER));
        playButton.setOnMouseExited(e  -> playButton.setStyle(PLAY_DEFAULT));

        // Root row
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(10, 14, 10, 14));
        root.setStyle(ROW_DEFAULT);
        root.getChildren().addAll(addButton, iconBox, textBox, spacer, playButton);

        // Row hover — very subtle, not white
        root.setOnMouseEntered(e -> root.setStyle(ROW_HOVER));
        root.setOnMouseExited(e  -> root.setStyle(ROW_DEFAULT));

        // Actions
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
            if (song != null && onPlay != null) onPlay.accept(song);
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

    // ─────────────────────────────────────────────────────────────

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);

        if (empty || song == null) {
            setText(null);
            setGraphic(null);
            setBackground(Background.EMPTY);           // ← critical: prevents white recycled cells
            setStyle("-fx-background-color: transparent;");
            return;
        }

        titleLabel.setText(song.title());

        StringBuilder meta = new StringBuilder();
        if (song.artist() != null && !song.artist().isBlank()) meta.append(song.artist());
        if (song.genre() != null && !song.genre().isBlank()) {
            if (!meta.isEmpty()) meta.append(" \u00B7 ");
            meta.append(song.genre());
        }
        metaLabel.setText(meta.toString());

        // Reset add button in case it was mid-flash
        addButton.setText("+");
        addButton.setStyle(ADD_DEFAULT);

        setText(null);
        setGraphic(root);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(false); // never show selection highlight
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void flashAddConfirm() {
        addButton.setText("✓");
        addButton.setStyle(ADD_CONFIRM);
        PauseTransition pause = new PauseTransition(Duration.millis(700));
        pause.setOnFinished(e -> {
            addButton.setText("+");
            addButton.setStyle(ADD_DEFAULT);
        });
        pause.play();
    }
}
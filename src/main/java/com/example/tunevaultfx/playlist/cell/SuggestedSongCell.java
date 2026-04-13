package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.util.CellStyleKit;
import javafx.animation.FadeTransition;
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
 * Suggested song row — CellStyleKit colours, hover-reveal + button.
 *
 * The [+] button is invisible at rest and fades in on row hover,
 * so the list looks clean until the user wants to act.
 */
public class SuggestedSongCell extends ListCell<Song> {

    private final Consumer<Song> onAdd;
    private final Consumer<Song> onPlay;

    private final HBox      root       = new HBox(12);
    private final StackPane addWrapper = new StackPane();   // fixed slot — no layout shift
    private final Button    addButton  = new Button("+");
    private final Label     titleLabel = new Label();
    private final Label     metaLabel  = new Label();
    private final VBox      textBox    = new VBox(3, titleLabel, metaLabel);
    private final Region    spacer     = new Region();
    private final Button    playButton = new Button("▶  Play");

    // ── Styles ────────────────────────────────────────────────────

    private static final String ADD_HIDDEN =
            "-fx-background-color: transparent; -fx-text-fill: transparent;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 18;";

    private static final String ADD_VISIBLE =
            "-fx-background-color: rgba(139,92,246,0.18);" +
                    "-fx-text-fill: #c4b5fd;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(139,92,246,0.3);" +
                    "-fx-border-radius: 18; -fx-border-width: 1;";

    private static final String ADD_HOVER =
            "-fx-background-color: #8b5cf6;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18;";

    private static final String ADD_CONFIRM =
            "-fx-background-color: rgba(34,197,94,0.22);" +
                    "-fx-text-fill: #86efac;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(34,197,94,0.3);" +
                    "-fx-border-radius: 18; -fx-border-width: 1;";

    private static final String PLAY_DEFAULT =
            "-fx-background-color: rgba(34,197,94,0.12);" +
                    "-fx-text-fill: #86efac;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;" +
                    "-fx-border-color: rgba(34,197,94,0.22);" +
                    "-fx-border-radius: 17; -fx-border-width: 1;" +
                    "-fx-padding: 0 14 0 14;";

    private static final String PLAY_HOVER =
            "-fx-background-color: #22c55e;" +
                    "-fx-text-fill: #052e16;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;" +
                    "-fx-padding: 0 14 0 14;";

    // ─────────────────────────────────────────────────────────────

    public SuggestedSongCell(Consumer<Song> onAdd, Consumer<Song> onPlay) {
        this.onAdd  = onAdd;
        this.onPlay = onPlay;

        // Fixed-size slot for the add button so layout never shifts
        addWrapper.setPrefSize(36, 36);
        addWrapper.setMinSize(36, 36);
        addWrapper.setMaxSize(36, 36);
        addButton.setPrefSize(36, 36);
        addButton.setMinSize(36, 36);
        addButton.setMaxSize(36, 36);
        addButton.setFocusTraversable(false);
        addButton.setStyle(ADD_HIDDEN);
        addButton.setOpacity(0);
        addWrapper.getChildren().add(addButton);

        // Icon
        StackPane iconBox = CellStyleKit.iconBox("♫", CellStyleKit.Palette.PURPLE, false);

        // Labels
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + CellStyleKit.TEXT_PRIMARY + ";");
        metaLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: " + CellStyleKit.TEXT_SECONDARY + ";");
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox.setHgrow(spacer,  Priority.ALWAYS);

        // Play button
        playButton.setPrefHeight(34);
        playButton.setFocusTraversable(false);
        playButton.setStyle(PLAY_DEFAULT);
        playButton.setOnMouseEntered(e -> playButton.setStyle(PLAY_HOVER));
        playButton.setOnMouseExited(e  -> playButton.setStyle(PLAY_DEFAULT));

        // Row
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(10, 14, 10, 14));
        root.setStyle(CellStyleKit.ROW_DEFAULT);
        root.getChildren().addAll(addWrapper, iconBox, textBox, spacer, playButton);

        // Row hover — reveal + button
        root.setOnMouseEntered(e -> {
            root.setStyle(CellStyleKit.ROW_HOVER);
            if (addButton.getText().equals("+")) {
                addButton.setStyle(ADD_VISIBLE);
                fade(addButton, true);
            }
        });
        root.setOnMouseExited(e -> {
            root.setStyle(CellStyleKit.ROW_DEFAULT);
            if (addButton.getText().equals("+")) {
                fade(addButton, false);
                PauseTransition delay = new PauseTransition(Duration.millis(180));
                delay.setOnFinished(ev -> { if (addButton.getText().equals("+")) addButton.setStyle(ADD_HIDDEN); });
                delay.play();
            }
        });

        // Add button inner hover
        addButton.setOnMouseEntered(e -> { if (addButton.getOpacity() > 0) addButton.setStyle(ADD_HOVER); e.consume(); });
        addButton.setOnMouseExited(e  -> { if (addButton.getOpacity() > 0) addButton.setStyle(ADD_VISIBLE); e.consume(); });

        // Actions
        addButton.setOnAction(e -> {
            Song s = getItem();
            if (s != null && onAdd != null) { flashConfirm(); onAdd.accept(s); }
            e.consume();
        });
        playButton.setOnAction(e -> {
            Song s = getItem();
            if (s != null && onPlay != null) onPlay.accept(s);
            e.consume();
        });

        setOnMousePressed(e -> {
            if (!isEmpty() && getListView() != null) {
                getListView().getSelectionModel().clearSelection(); e.consume();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);
        if (empty || song == null) {
            setText(null); setGraphic(null);
            setBackground(Background.EMPTY);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        titleLabel.setText(song.title());
        metaLabel.setText(CellStyleKit.songMeta(song.artist(), song.genre()));

        // Reset button to hidden state on cell recycle
        addButton.setText("+");
        addButton.setStyle(ADD_HIDDEN);
        addButton.setOpacity(0);

        setText(null); setGraphic(root);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override public void updateSelected(boolean s) { super.updateSelected(false); }

    // ── Helpers ───────────────────────────────────────────────────

    private void fade(Button btn, boolean in) {
        FadeTransition ft = new FadeTransition(Duration.millis(160), btn);
        ft.setToValue(in ? 1.0 : 0.0);
        ft.play();
    }

    private void flashConfirm() {
        addButton.setText("✓");
        addButton.setStyle(ADD_CONFIRM);
        PauseTransition p = new PauseTransition(Duration.millis(700));
        p.setOnFinished(e -> { addButton.setText("+"); addButton.setStyle(ADD_VISIBLE); });
        p.play();
    }
}
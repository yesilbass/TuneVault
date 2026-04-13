package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.util.CellStyleKit;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Search-results cell with an add/remove toggle — CellStyleKit colours.
 */
public class SearchSongToggleCell extends ListCell<Song> {

    private final Predicate<Song> isSongInPlaylist;
    private final Consumer<Song>  onToggleSong;

    private final HBox   root        = new HBox(12);
    private final VBox   textBox     = new VBox(3);
    private final Label  titleLabel  = new Label();
    private final Label  metaLabel   = new Label();
    private final Region spacer      = new Region();
    private final Button actionButton = new Button();

    // ── Button styles ─────────────────────────────────────────────

    private static final String BTN_IN =
            "-fx-background-color: rgba(139,92,246,0.22);" +
                    "-fx-text-fill: #c4b5fd;" +
                    "-fx-font-size: 15px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 16;" +
                    "-fx-border-color: rgba(139,92,246,0.35);" +
                    "-fx-border-radius: 16; -fx-border-width: 1;";

    private static final String BTN_OUT =
            "-fx-background-color: rgba(255,255,255,0.07);" +
                    "-fx-text-fill: #a0a0c0;" +
                    "-fx-font-size: 16px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 16;" +
                    "-fx-border-color: rgba(255,255,255,0.12);" +
                    "-fx-border-radius: 16; -fx-border-width: 1;";

    // ─────────────────────────────────────────────────────────────

    public SearchSongToggleCell(Predicate<Song> isSongInPlaylist, Consumer<Song> onToggleSong) {
        this.isSongInPlaylist = isSongInPlaylist;
        this.onToggleSong     = onToggleSong;

        root.setSpacing(12);
        root.setPadding(new Insets(9, 12, 9, 12));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle(CellStyleKit.ROW_DEFAULT);

        HBox.setHgrow(spacer,  Priority.ALWAYS);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + CellStyleKit.TEXT_PRIMARY + ";");
        metaLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: " + CellStyleKit.TEXT_SECONDARY + ";");

        textBox.getChildren().addAll(titleLabel, metaLabel);

        actionButton.setPrefWidth(44);
        actionButton.setPrefHeight(34);
        actionButton.setMinWidth(44);
        actionButton.setFocusTraversable(false);

        root.getChildren().addAll(textBox, spacer, actionButton);

        root.setOnMouseEntered(e -> root.setStyle(CellStyleKit.ROW_HOVER));
        root.setOnMouseExited(e  -> root.setStyle(CellStyleKit.ROW_DEFAULT));

        root.setOnMouseClicked(ev -> {
            if (getItem() == null || isEmpty()) return;
            toggleSong(getItem()); ev.consume();
        });

        setOnMousePressed(ev -> {
            if (!isEmpty() && getListView() != null) {
                getListView().getSelectionModel().clearSelection(); ev.consume();
            }
        });
    }

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
        refreshButton(song);

        setText(null); setGraphic(root);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override public void updateSelected(boolean s) { super.updateSelected(false); }

    // ── Helpers ───────────────────────────────────────────────────

    private void toggleSong(Song song) {
        boolean wasIn = isSongInPlaylist.test(song);
        onToggleSong.accept(song);
        flashRow(!wasIn);
        if (getListView() != null) {
            getListView().refresh();
            getListView().getSelectionModel().clearSelection();
        }
    }

    private void refreshButton(Song song) {
        boolean inPlaylist = isSongInPlaylist.test(song);
        actionButton.setText(inPlaylist ? "✓" : "+");
        actionButton.setStyle(inPlaylist ? BTN_IN : BTN_OUT);
        actionButton.setOnAction(ev -> { toggleSong(song); ev.consume(); });
    }

    private void flashRow(boolean added) {
        String flash = added
                ? "-fx-background-color: rgba(139,92,246,0.18); -fx-background-radius: 13;"
                : "-fx-background-color: rgba(244,63,94,0.12); -fx-background-radius: 13;";
        root.setStyle(flash);
        PauseTransition p = new PauseTransition(Duration.millis(220));
        p.setOnFinished(e -> root.setStyle(CellStyleKit.ROW_DEFAULT));
        p.play();
    }
}
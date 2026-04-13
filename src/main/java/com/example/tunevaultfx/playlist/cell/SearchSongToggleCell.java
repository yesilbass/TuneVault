package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Search-results cell with an add / remove toggle — dark theme.
 */
public class SearchSongToggleCell extends ListCell<Song> {

    private final Predicate<Song> isSongInPlaylist;
    private final Consumer<Song>  onToggleSong;

    private final HBox   root        = new HBox();
    private final VBox   textBox     = new VBox();
    private final Label  titleLabel  = new Label();
    private final Label  artistLabel = new Label();
    private final Region spacer      = new Region();
    private final Button actionButton = new Button();

    // ── Style constants ────────────────────────────────────────────

    private static final String BTN_IN_PLAYLIST =
            "-fx-background-color: rgba(139,92,246,0.2);" +
                    "-fx-text-fill: #a78bfa;" +
                    "-fx-font-size: 15px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 16;" +
                    "-fx-border-color: rgba(139,92,246,0.3);" +
                    "-fx-border-radius: 16;" +
                    "-fx-border-width: 1;";

    private static final String BTN_NOT_IN_PLAYLIST =
            "-fx-background-color: rgba(255,255,255,0.07);" +
                    "-fx-text-fill: #4a4a70;" +
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 16;" +
                    "-fx-border-color: rgba(255,255,255,0.1);" +
                    "-fx-border-radius: 16;" +
                    "-fx-border-width: 1;";

    private static final String ROW_DEFAULT =
            "-fx-background-color: transparent; -fx-background-radius: 13;";
    private static final String ROW_HOVER =
            "-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 13;";

    // ─────────────────────────────────────────────────────────────

    public SearchSongToggleCell(Predicate<Song> isSongInPlaylist, Consumer<Song> onToggleSong) {
        this.isSongInPlaylist = isSongInPlaylist;
        this.onToggleSong     = onToggleSong;

        root.setSpacing(12);
        root.setPadding(new Insets(8, 10, 8, 10));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle(ROW_DEFAULT);

        HBox.setHgrow(spacer,  Priority.ALWAYS);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Labels — light text on dark bg
        titleLabel.setStyle(
                "-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: bold;");
        artistLabel.setStyle(
                "-fx-text-fill: #3d3d5c; -fx-font-size: 12px;");

        textBox.setSpacing(3);
        textBox.getChildren().addAll(titleLabel, artistLabel);

        actionButton.setPrefWidth(42);
        actionButton.setPrefHeight(32);
        actionButton.setMinWidth(42);
        actionButton.setFocusTraversable(false);

        root.getChildren().addAll(textBox, spacer, actionButton);

        // Row hover
        root.setOnMouseEntered(e -> root.setStyle(ROW_HOVER));
        root.setOnMouseExited(e  -> root.setStyle(ROW_DEFAULT));

        // Click anywhere on the row also toggles
        root.setOnMouseClicked(ev -> {
            Song song = getItem();
            if (song == null || isEmpty()) return;
            toggleSong(song);
            ev.consume();
        });

        setOnMousePressed(ev -> {
            if (!isEmpty() && getListView() != null) {
                getListView().getSelectionModel().clearSelection();
                ev.consume();
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
        artistLabel.setText(song.artist()
                + (song.genre() != null && !song.genre().isBlank() ? " · " + song.genre() : ""));
        refreshActionButton(song);

        setText(null);
        setGraphic(root);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(false);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void toggleSong(Song song) {
        boolean wasIn = isSongInPlaylist.test(song);
        onToggleSong.accept(song);
        playClickFlash(!wasIn);
        if (getListView() != null) {
            getListView().refresh();
            getListView().getSelectionModel().clearSelection();
        }
    }

    private void refreshActionButton(Song song) {
        boolean inPlaylist = isSongInPlaylist.test(song);
        actionButton.setText(inPlaylist ? "✓" : "+");
        actionButton.setStyle(inPlaylist ? BTN_IN_PLAYLIST : BTN_NOT_IN_PLAYLIST);
        actionButton.setOnAction(ev -> {
            toggleSong(song);
            ev.consume();
        });
    }

    private void playClickFlash(boolean added) {
        // Subtle dark-theme flash: violet for add, red-tint for remove
        String flashStyle = added
                ? "-fx-background-color: rgba(139,92,246,0.15); -fx-background-radius: 13;"
                : "-fx-background-color: rgba(239,68,68,0.1); -fx-background-radius: 13;";

        root.setStyle(flashStyle);
        PauseTransition pause = new PauseTransition(Duration.millis(200));
        pause.setOnFinished(e -> root.setStyle(ROW_DEFAULT));
        pause.play();
    }
}

package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.stage.Popup;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Playlist song row — dark theme.
 *
 * Play button is a subtle ghost circle that lights up on hover.
 * The ⋯ menu opens a dark popup card.
 * Empty cell branch explicitly sets transparent background to prevent
 * JavaFX from painting recycled cells white.
 */
public class PlayableSongCell extends ListCell<Song> {

    private final Button   playButton = new Button("▶");
    private final Button   moreButton = new Button("⋯");
    private final Label    titleLabel  = new Label();
    private final Label    artistLabel = new Label();
    private final VBox     textBox     = new VBox(2, titleLabel, artistLabel);
    private final Region   spacer      = new Region();
    private final HBox     row         = new HBox(12);

    private final Consumer<Song>   onPlay;
    private final Consumer<Song>   onAddToPlaylist;
    private final Consumer<Song>   onRemoveFromPlaylist;
    private final Supplier<String> playlistNameSupplier;

    private Popup activePopup;

    // ── Style constants ───────────────────────────────────────────

    // Subtle transparent ghost — lights up white on hover
    private static final String PLAY_DEFAULT =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #3d3d5c;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;";

    private static final String PLAY_HOVER =
            "-fx-background-color: rgba(255,255,255,0.1);" +
                    "-fx-text-fill: #ffffff;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;";

    private static final String MORE_DEFAULT =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #3d3d5c;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18; -fx-padding: 0;";

    private static final String MORE_HOVER =
            "-fx-background-color: rgba(255,255,255,0.08);" +
                    "-fx-text-fill: #a1a1aa;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18; -fx-padding: 0;";

    private static final String ROW_DEFAULT =
            "-fx-background-color: transparent; -fx-background-radius: 14;";

    private static final String ROW_HOVER =
            "-fx-background-color: rgba(255,255,255,0.04); -fx-background-radius: 14;";

    // ─────────────────────────────────────────────────────────────

    public PlayableSongCell(Consumer<Song> onPlay,
                            Consumer<Song> onAddToPlaylist,
                            Consumer<Song> onRemoveFromPlaylist,
                            Supplier<String> playlistNameSupplier) {
        this.onPlay               = onPlay;
        this.onAddToPlaylist      = onAddToPlaylist;
        this.onRemoveFromPlaylist = onRemoveFromPlaylist;
        this.playlistNameSupplier = playlistNameSupplier;

        // Play button — ghost, subtle
        playButton.setStyle(PLAY_DEFAULT);
        playButton.setPrefSize(34, 34);
        playButton.setMinSize(34, 34);
        playButton.setMaxSize(34, 34);
        playButton.setFocusTraversable(false);

        // More button
        moreButton.setStyle(MORE_DEFAULT);
        moreButton.setPrefSize(36, 36);
        moreButton.setMinSize(36, 36);
        moreButton.setMaxSize(36, 36);
        moreButton.setFocusTraversable(false);

        // Labels — white/muted on dark
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        artistLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #52525b;");

        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox.setHgrow(spacer,  Priority.ALWAYS);

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle(ROW_DEFAULT);
        row.getChildren().addAll(playButton, textBox, spacer, moreButton);

        // Row hover — play and more buttons also update
        row.setOnMouseEntered(e -> {
            row.setStyle(ROW_HOVER);
            playButton.setStyle(PLAY_HOVER);
            moreButton.setStyle(MORE_HOVER);
        });
        row.setOnMouseExited(e -> {
            row.setStyle(ROW_DEFAULT);
            playButton.setStyle(PLAY_DEFAULT);
            moreButton.setStyle(MORE_DEFAULT);
        });

        playButton.setOnAction(ev -> {
            Song song = getItem();
            if (song != null && onPlay != null) onPlay.accept(song);
            ev.consume();
        });

        moreButton.setOnAction(ev -> {
            Song song = getItem();
            if (song != null) showActionPopup(song);
            ev.consume();
        });

        setOnMousePressed(ev -> {
            if (!isEmpty() && getListView() != null)
                getListView().getSelectionModel().clearSelection();
        });
    }

    // ─────────────────────────────────────────────────────────────

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);
        hidePopup();

        if (empty || song == null) {
            setText(null);
            setGraphic(null);
            setBackground(Background.EMPTY);           // ← prevents white recycled cells
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
        artistLabel.setText(meta.toString());

        setText(null);
        setGraphic(row);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(false);
    }

    // ── Dark popup menu ───────────────────────────────────────────

    private void showActionPopup(Song song) {
        hidePopup();

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);

        VBox card = new VBox(6);
        card.setPadding(new Insets(8));
        card.setPrefWidth(228);
        card.setStyle(
                "-fx-background-color: #1a1a28;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.1);" +
                        "-fx-border-radius: 18; -fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.6),24,0,0,8);");

        Button addBtn = menuButton("Add to Another Playlist", false);
        addBtn.setOnAction(ev -> {
            hidePopup();
            if (onAddToPlaylist != null) onAddToPlaylist.accept(song);
            ev.consume();
        });

        String name = playlistNameSupplier != null ? playlistNameSupplier.get() : "this playlist";
        Button removeBtn = menuButton("Remove from " + name, true);
        removeBtn.setOnAction(ev -> {
            hidePopup();
            if (onRemoveFromPlaylist != null) onRemoveFromPlaylist.accept(song);
            ev.consume();
        });

        card.getChildren().addAll(addBtn, removeBtn);
        popup.getContent().add(card);

        Bounds bounds = moreButton.localToScreen(moreButton.getBoundsInLocal());
        if (bounds != null) {
            popup.show(moreButton, bounds.getMaxX() - 218, bounds.getMaxY() + 6);
            activePopup = popup;
        }
    }

    private static Button menuButton(String text, boolean destructive) {
        Button btn = new Button(text);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);
        btn.setFocusTraversable(false);

        String textFill = destructive ? "#f87171" : "#e2e8f0";
        String hoverBg  = destructive ? "rgba(239,68,68,0.1)" : "rgba(255,255,255,0.06)";

        String base =
                "-fx-background-color: transparent;" +
                        "-fx-background-radius: 11;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 0 12 0 12;" +
                        "-fx-text-fill: " + textFill + ";";

        String hover =
                "-fx-background-color: " + hoverBg + ";" +
                        "-fx-background-radius: 11;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-padding: 0 12 0 12;" +
                        "-fx-text-fill: " + textFill + ";";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private void hidePopup() {
        if (activePopup != null) {
            activePopup.hide();
            activePopup = null;
        }
    }
}
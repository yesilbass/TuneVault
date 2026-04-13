package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.util.CellStyleKit;
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
 * Playlist song row — dark theme via CellStyleKit.
 *
 * New: shows a "now playing" purple indicator bar on the left edge
 * when this song is the one currently playing in the player.
 */
public class PlayableSongCell extends ListCell<Song> {

    private final Button   playButton = new Button("▶");
    private final Button   moreButton = new Button("⋯");
    private final Label    titleLabel  = new Label();
    private final Label    metaLabel   = new Label();
    private final VBox     textBox     = new VBox(3, titleLabel, metaLabel);
    private final Region   spacer      = new Region();
    private final HBox     row         = new HBox(12);

    // Now-playing indicator — thin left-edge bar
    private final Region   nowPlayingBar = new Region();

    private final Consumer<Song>   onPlay;
    private final Consumer<Song>   onAddToPlaylist;
    private final Consumer<Song>   onRemoveFromPlaylist;
    private final Supplier<String> playlistNameSupplier;

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private Popup activePopup;

    // ── Styles ────────────────────────────────────────────────────

    private static final String PLAY_DEFAULT =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #58586e;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;";

    private static final String PLAY_HOVER =
            "-fx-background-color: rgba(255,255,255,0.1);" +
                    "-fx-text-fill: #f2f2fa;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;";

    private static final String PLAY_PLAYING =
            "-fx-background-color: rgba(139,92,246,0.25);" +
                    "-fx-text-fill: #c4b5fd;" +
                    "-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;";

    private static final String MORE_DEFAULT =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #58586e;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18; -fx-padding: 0;";

    private static final String MORE_HOVER =
            "-fx-background-color: rgba(255,255,255,0.09);" +
                    "-fx-text-fill: #a0a0c0;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18; -fx-padding: 0;";

    // ─────────────────────────────────────────────────────────────

    public PlayableSongCell(Consumer<Song> onPlay,
                            Consumer<Song> onAddToPlaylist,
                            Consumer<Song> onRemoveFromPlaylist,
                            Supplier<String> playlistNameSupplier) {
        this.onPlay               = onPlay;
        this.onAddToPlaylist      = onAddToPlaylist;
        this.onRemoveFromPlaylist = onRemoveFromPlaylist;
        this.playlistNameSupplier = playlistNameSupplier;

        // Now-playing bar — hidden by default
        nowPlayingBar.setPrefWidth(3);
        nowPlayingBar.setMinWidth(3);
        nowPlayingBar.setMaxWidth(3);
        nowPlayingBar.setStyle(
                "-fx-background-color: #8b5cf6;" +
                        "-fx-background-radius: 2;");
        nowPlayingBar.setVisible(false);

        // Play button
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

        // Labels
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                        + CellStyleKit.TEXT_PRIMARY + ";");
        metaLabel.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: " + CellStyleKit.TEXT_SECONDARY + ";");

        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox.setHgrow(spacer,  Priority.ALWAYS);

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 12, 9, 12));
        row.setStyle(CellStyleKit.ROW_DEFAULT);
        row.getChildren().addAll(nowPlayingBar, playButton, textBox, spacer, moreButton);

        // Row hover
        row.setOnMouseEntered(e -> {
            Song s = getItem();
            boolean isPlaying = s != null && player.getCurrentSong() != null
                    && player.getCurrentSong().songId() == s.songId();
            if (!isPlaying) {
                row.setStyle(CellStyleKit.ROW_HOVER);
                playButton.setStyle(PLAY_HOVER);
            }
            moreButton.setStyle(MORE_HOVER);
        });
        row.setOnMouseExited(e -> {
            Song s = getItem();
            boolean isPlaying = s != null && player.getCurrentSong() != null
                    && player.getCurrentSong().songId() == s.songId();
            row.setStyle(isPlaying ? CellStyleKit.ROW_PLAYING : CellStyleKit.ROW_DEFAULT);
            playButton.setStyle(isPlaying ? PLAY_PLAYING : PLAY_DEFAULT);
            moreButton.setStyle(MORE_DEFAULT);
        });

        // Actions
        playButton.setOnAction(ev -> {
            Song s = getItem();
            if (s != null && onPlay != null) onPlay.accept(s);
            ev.consume();
        });
        moreButton.setOnAction(ev -> {
            Song s = getItem();
            if (s != null) showActionPopup(s);
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
            setText(null); setGraphic(null);
            setBackground(Background.EMPTY);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        boolean isPlaying = player.getCurrentSong() != null
                && player.getCurrentSong().songId() == song.songId();

        titleLabel.setText(song.title());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (isPlaying ? "#c4b5fd" : CellStyleKit.TEXT_PRIMARY) + ";");
        metaLabel.setText(CellStyleKit.songMeta(song.artist(), song.genre()));

        nowPlayingBar.setVisible(isPlaying);
        nowPlayingBar.setManaged(isPlaying);
        playButton.setStyle(isPlaying ? PLAY_PLAYING : PLAY_DEFAULT);
        row.setStyle(isPlaying ? CellStyleKit.ROW_PLAYING : CellStyleKit.ROW_DEFAULT);
        CellStyleKit.markPlaying(row, isPlaying);

        setText(null); setGraphic(row);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override
    public void updateSelected(boolean selected) { super.updateSelected(false); }

    // ── Dark popup menu ───────────────────────────────────────────

    private void showActionPopup(Song song) {
        hidePopup();
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);

        VBox card = new VBox(6);
        card.setPadding(new Insets(8));
        card.setPrefWidth(232);
        card.setStyle(
                "-fx-background-color: #16162a;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(255,255,255,0.12);" +
                        "-fx-border-radius: 18; -fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.65),28,0,0,10);");

        Button addBtn = menuButton("Add to Another Playlist", false);
        addBtn.setOnAction(ev -> {
            hidePopup(); if (onAddToPlaylist != null) onAddToPlaylist.accept(song); ev.consume();
        });

        String name = playlistNameSupplier != null ? playlistNameSupplier.get() : "this playlist";
        Button removeBtn = menuButton("Remove from " + name, true);
        removeBtn.setOnAction(ev -> {
            hidePopup(); if (onRemoveFromPlaylist != null) onRemoveFromPlaylist.accept(song); ev.consume();
        });

        card.getChildren().addAll(addBtn, removeBtn);
        popup.getContent().add(card);

        Bounds b = moreButton.localToScreen(moreButton.getBoundsInLocal());
        if (b != null) { popup.show(moreButton, b.getMaxX() - 222, b.getMaxY() + 6); activePopup = popup; }
    }

    private static Button menuButton(String text, boolean destructive) {
        Button btn = new Button(text);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(42);
        btn.setFocusTraversable(false);

        String textColor = destructive ? "#fda4af" : "#e0e0f0";
        String hoverBg   = destructive ? "rgba(244,63,94,0.12)" : "rgba(255,255,255,0.07)";

        String base  = "-fx-background-color: transparent; -fx-background-radius: 11;"
                + "-fx-font-size: 13px; -fx-padding: 0 14 0 14;"
                + "-fx-text-fill: " + textColor + ";";
        String hover = "-fx-background-color: " + hoverBg + "; -fx-background-radius: 11;"
                + "-fx-font-size: 13px; -fx-padding: 0 14 0 14;"
                + "-fx-text-fill: " + textColor + ";";

        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private void hidePopup() {
        if (activePopup != null) { activePopup.hide(); activePopup = null; }
    }
}
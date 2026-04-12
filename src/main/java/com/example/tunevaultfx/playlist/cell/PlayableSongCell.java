package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.Popup;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Custom playlist song cell with a play button and a styled three-dot action menu.
 */
public class PlayableSongCell extends ListCell<Song> {

    private final Button playButton = new Button("▶");
    private final Button moreButton = new Button("⋯");

    private final Label titleLabel = new Label();
    private final Label artistLabel = new Label();

    private final VBox textBox = new VBox(2, titleLabel, artistLabel);
    private final Region spacer = new Region();
    private final HBox row = new HBox(12);

    private final Consumer<Song> onPlay;
    private final Consumer<Song> onAddToPlaylist;
    private final Consumer<Song> onRemoveFromPlaylist;
    private final Supplier<String> playlistNameSupplier;

    private Popup activePopup;

    public PlayableSongCell(Consumer<Song> onPlay,
                            Consumer<Song> onAddToPlaylist,
                            Consumer<Song> onRemoveFromPlaylist,
                            Supplier<String> playlistNameSupplier) {
        this.onPlay = onPlay;
        this.onAddToPlaylist = onAddToPlaylist;
        this.onRemoveFromPlaylist = onRemoveFromPlaylist;
        this.playlistNameSupplier = playlistNameSupplier;

        playButton.setStyle(
                "-fx-background-color: #10b981; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 18;"
        );
        playButton.setPrefSize(34, 34);
        playButton.setMinSize(34, 34);
        playButton.setMaxSize(34, 34);
        playButton.setFocusTraversable(false);

        moreButton.setStyle(
                "-fx-background-color: #e2e8f0; " +
                        "-fx-text-fill: #334155; " +
                        "-fx-font-size: 18px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 18; " +
                        "-fx-padding: 0;"
        );
        moreButton.setPrefSize(36, 36);
        moreButton.setMinSize(36, 36);
        moreButton.setMaxSize(36, 36);
        moreButton.setFocusTraversable(false);

        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        artistLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color: transparent; -fx-background-radius: 16;");
        row.getChildren().addAll(playButton, textBox, spacer, moreButton);

        row.setOnMouseEntered(e -> {
            row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 16;");
            moreButton.setStyle(
                    "-fx-background-color: #cbd5e1; " +
                            "-fx-text-fill: #0f172a; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 18; " +
                            "-fx-padding: 0;"
            );
        });

        row.setOnMouseExited(e -> {
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 16;");
            moreButton.setStyle(
                    "-fx-background-color: #e2e8f0; " +
                            "-fx-text-fill: #334155; " +
                            "-fx-font-size: 18px; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 18; " +
                            "-fx-padding: 0;"
            );
        });

        playButton.setOnAction(event -> {
            Song song = getItem();
            if (song != null && onPlay != null) {
                onPlay.accept(song);
            }
            event.consume();
        });

        moreButton.setOnAction(event -> {
            Song song = getItem();
            if (song != null) {
                showActionPopup(song);
            }
            event.consume();
        });

        setOnMousePressed(event -> {
            if (!isEmpty() && getListView() != null) {
                getListView().getSelectionModel().clearSelection();
            }
        });
    }

    @Override
    protected void updateItem(Song song, boolean empty) {
        super.updateItem(song, empty);

        hidePopup();

        if (empty || song == null) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        titleLabel.setText(song.title());
        artistLabel.setText(song.artist());

        setText(null);
        setGraphic(row);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override
    public void updateSelected(boolean selected) {
        super.updateSelected(false);
    }

    private void showActionPopup(Song song) {
        hidePopup();

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);
        popup.setHideOnEscape(true);

        VBox menuCard = new VBox(8);
        menuCard.setPadding(new Insets(10));
        menuCard.setPrefWidth(210);
        menuCard.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 18; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-radius: 18; " +
                        "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.18), 18, 0.18, 0, 6);"
        );

        Button addButton = buildMenuButton("Add to Playlist", false);
        addButton.setOnAction(event -> {
            hidePopup();
            if (onAddToPlaylist != null) {
                onAddToPlaylist.accept(song);
            }
            event.consume();
        });

        String playlistName = playlistNameSupplier != null ? playlistNameSupplier.get() : "this playlist";
        Button removeButton = buildMenuButton("Remove from " + playlistName, true);
        removeButton.setOnAction(event -> {
            hidePopup();
            if (onRemoveFromPlaylist != null) {
                onRemoveFromPlaylist.accept(song);
            }
            event.consume();
        });

        menuCard.getChildren().addAll(addButton, removeButton);
        popup.getContent().add(menuCard);

        Bounds bounds = moreButton.localToScreen(moreButton.getBoundsInLocal());
        if (bounds != null) {
            popup.show(moreButton, bounds.getMaxX() - 200, bounds.getMaxY() + 6);
            activePopup = popup;
        }
    }

    private Button buildMenuButton(String text, boolean destructive) {
        Button button = new Button(text);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40);
        button.setFocusTraversable(false);

        String baseStyle =
                "-fx-background-color: transparent; " +
                        "-fx-background-radius: 12; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 0 12 0 12; " +
                        "-fx-text-fill: " + (destructive ? "#dc2626" : "#0f172a") + ";";

        String hoverStyle =
                "-fx-background-color: " + (destructive ? "#fef2f2" : "#f8fafc") + "; " +
                        "-fx-background-radius: 12; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 0 12 0 12; " +
                        "-fx-text-fill: " + (destructive ? "#dc2626" : "#0f172a") + ";";

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));

        return button;
    }

    private void hidePopup() {
        if (activePopup != null) {
            activePopup.hide();
            activePopup = null;
        }
    }
}
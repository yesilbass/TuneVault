package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Suggested song row.
 *
 * Layout: [♫] [▶] [title / artist] [+] (genre not shown on playlist page)
 *
 * The play button sits between the icon and the song name.
 * The [+] add button is on the far right, invisible at rest and
 * fades in on any row hover.
 */
public class SuggestedSongCell extends ListCell<Song> {

    private final Consumer<Song> onAdd;
    private final Consumer<Song> onPlay;
    private final Consumer<String> onOpenArtist;
    private final Consumer<Song> onOpenAddToPlaylist;

    private final HBox      root       = new HBox(10);
    private final StackPane addWrapper = new StackPane();
    private final Button    addButton  = new Button("+");
    private final Label     titleLabel = new Label();
    private final VBox      textBox    = new VBox(3);
    private final Button    playButton = new Button("▶");

    // ── Add-button styles ─────────────────────────────────────────

    private static final String ADD_HIDDEN =
            "-fx-background-color: transparent; -fx-text-fill: transparent;" +
            "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 16;";

    private static final String ADD_VISIBLE =
            "-fx-background-color: rgba(139,92,246,0.18);" +
            "-fx-text-fill: #c4b5fd;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(139,92,246,0.3);" +
            "-fx-border-radius: 16; -fx-border-width: 1;";

    private static final String ADD_HOVER =
            "-fx-background-color: #8b5cf6;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 16;";

    private static final String ADD_CONFIRM =
            "-fx-background-color: rgba(34,197,94,0.22);" +
            "-fx-text-fill: #86efac;" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(34,197,94,0.3);" +
            "-fx-border-radius: 16; -fx-border-width: 1;";

    // ── Play button (violet family — matches ♫ icon; avoids green-on-lavender in light mode)

    private static final String PLAY_DEFAULT_DARK =
            "-fx-background-color: transparent;"
                    + "-fx-text-fill: #58586e;"
                    + "-fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 15; -fx-padding: 0;";

    private static String playDefaultStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: transparent;"
                    + "-fx-text-fill: " + CellStyleKit.getTextMuted() + ";"
                    + "-fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 15; -fx-padding: 0;";
        }
        return PLAY_DEFAULT_DARK;
    }

    /** Shown while the row is hovered — soft disk, not a second accent color */
    private static String playVisibleStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: rgba(124,58,237,0.14);"
                    + "-fx-text-fill: #5b21b6;"
                    + "-fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 15; -fx-padding: 0;";
        }
        return "-fx-background-color: rgba(139,92,246,0.20);"
                + "-fx-text-fill: #ddd6fe;"
                + "-fx-font-size: 13px; -fx-font-weight: bold;"
                + "-fx-background-radius: 15; -fx-padding: 0;";
    }

    private static String playHoverStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: #7c3aed;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-size: 13px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 15; -fx-padding: 0;";
        }
        return "-fx-background-color: #8b5cf6;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 13px; -fx-font-weight: bold;"
                + "-fx-background-radius: 15; -fx-padding: 0;";
    }

    // ─────────────────────────────────────────────────────────────

    public SuggestedSongCell(
            Consumer<Song> onAdd,
            Consumer<Song> onPlay,
            Consumer<String> onOpenArtist,
            Consumer<Song> onOpenAddToPlaylist) {
        this.onAdd = onAdd;
        this.onPlay = onPlay;
        this.onOpenArtist = onOpenArtist;
        this.onOpenAddToPlaylist = onOpenAddToPlaylist;

        // Icon
        StackPane iconBox = CellStyleKit.iconBox("♫", CellStyleKit.Palette.PURPLE, false);

        // Compact circular play button
        playButton.setPrefSize(30, 30);
        playButton.setMinSize(30, 30);
        playButton.setMaxSize(30, 30);
        playButton.setFocusTraversable(false);
        playButton.setStyle(playDefaultStyle());

        // Labels
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + CellStyleKit.getTextPrimary() + ";");
        textBox.getChildren().add(titleLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        // Fixed-size slot for the add button so layout never shifts
        addWrapper.setPrefSize(34, 34);
        addWrapper.setMinSize(34, 34);
        addWrapper.setMaxSize(34, 34);
        addButton.setPrefSize(34, 34);
        addButton.setMinSize(34, 34);
        addButton.setMaxSize(34, 34);
        addButton.setFocusTraversable(false);
        addButton.setStyle(ADD_HIDDEN);
        addButton.setOpacity(0);
        addWrapper.getChildren().add(addButton);

        // Row: [♫] [▶] [title/meta grows] [+] — single HGrow so the + stays right-aligned
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(8, 14, 8, 14));
        root.setStyle(CellStyleKit.getRowDefault());
        root.getChildren().addAll(iconBox, playButton, textBox, addWrapper);

        // Row hover — reveal both the play button and the + button
        root.setOnMouseEntered(e -> {
            root.setStyle(CellStyleKit.getRowHover());
            playButton.setStyle(playVisibleStyle());
            if (addButton.getText().equals("+")) {
                addButton.setStyle(ADD_VISIBLE);
                fade(addButton, true);
            }
        });
        root.setOnMouseExited(e -> {
            root.setStyle(CellStyleKit.getRowDefault());
            playButton.setStyle(playDefaultStyle());
            if (addButton.getText().equals("+")) {
                fade(addButton, false);
                PauseTransition delay = new PauseTransition(Duration.millis(180));
                delay.setOnFinished(ev -> {
                    if (addButton.getText().equals("+")) addButton.setStyle(ADD_HIDDEN);
                });
                delay.play();
            }
        });

        // Play button hover
        playButton.setOnMouseEntered(e -> playButton.setStyle(playHoverStyle()));
        playButton.setOnMouseExited(e -> {
            if (root.isHover()) {
                playButton.setStyle(playVisibleStyle());
            } else {
                playButton.setStyle(playDefaultStyle());
            }
        });

        // Add button hover
        addButton.setOnMouseEntered(e -> {
            if (addButton.getOpacity() > 0) addButton.setStyle(ADD_HOVER);
            e.consume();
        });
        addButton.setOnMouseExited(e -> {
            if (addButton.getOpacity() > 0) addButton.setStyle(ADD_VISIBLE);
            e.consume();
        });

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

        root.setOnMouseClicked(ev -> {
            if (ev.getButton() != MouseButton.PRIMARY || ev.getClickCount() != 2) return;
            Song s = getItem();
            if (s != null && onPlay != null) {
                onPlay.accept(s);
                ev.consume();
            }
        });

        root.addEventFilter(
                ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                ev -> {
                    Song s = getItem();
                    if (s == null || isEmpty()) {
                        return;
                    }
                    ContextMenu menu =
                            SongContextMenuBuilder.build(
                                    s,
                                    root,
                                    SongContextMenuBuilder.Spec.general());
                    if (onOpenAddToPlaylist != null) {
                        for (var item : menu.getItems()) {
                            if ("Add to playlist".equals(item.getText())) {
                                item.setOnAction(
                                        e -> {
                                            menu.hide();
                                            onOpenAddToPlaylist.accept(s);
                                        });
                                break;
                            }
                        }
                    }
                    menu.show(root, ev.getScreenX(), ev.getScreenY());
                    ev.consume();
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
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold;"
                        + "-fx-text-fill: " + CellStyleKit.getTextPrimary() + ";");
        while (textBox.getChildren().size() > 1) {
            textBox.getChildren().remove(1);
        }
        HBox meta = CellStyleKit.songMetaLine(song.artist(), null, onOpenArtist);
        if (!meta.getChildren().isEmpty()) {
            textBox.getChildren().add(meta);
        }

        addButton.setText("+");
        addButton.setStyle(ADD_HIDDEN);
        addButton.setOpacity(0);
        playButton.setStyle(playDefaultStyle());

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
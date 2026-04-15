package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

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
    private final VBox     textBox     = new VBox(3);
    private final Region   spacer      = new Region();
    private final HBox     row         = new HBox(12);

    // Now-playing indicator — thin left-edge bar
    private final Region   nowPlayingBar = new Region();

    private final Consumer<Song>    onPlay;
    private final Consumer<Song>    onAddToPlaylist;
    private final Consumer<Song>    onRemoveFromPlaylist;
    private final Supplier<String>  playlistNameSupplier;
    private final Consumer<String>  onOpenArtist;

    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private ContextMenu activeMenu;

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

    private static final String PLAY_PLAYING_DARK =
            "-fx-background-color: rgba(139,92,246,0.25);" +
                    "-fx-text-fill: #c4b5fd;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 17;";

    private static String playDefaultStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: transparent;"
                    + "-fx-text-fill: #64748b;"
                    + "-fx-font-size: 14px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        return PLAY_DEFAULT;
    }

    private static String playHoverStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: rgba(124,58,237,0.14);"
                    + "-fx-text-fill: #5b21b6;"
                    + "-fx-font-size: 14px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        return PLAY_HOVER;
    }

    private static String playPlayingStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: #7c3aed;"
                    + "-fx-text-fill: white;"
                    + "-fx-font-size: 12px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        return PLAY_PLAYING_DARK;
    }

    private static String moreDefaultStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: transparent;"
                    + "-fx-text-fill: #64748b;"
                    + "-fx-font-size: 18px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 18; -fx-padding: 0;";
        }
        return MORE_DEFAULT_DARK;
    }

    private static String moreHoverStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: rgba(124,58,237,0.10);"
                    + "-fx-text-fill: #5b21b6;"
                    + "-fx-font-size: 18px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 18; -fx-padding: 0;";
        }
        return MORE_HOVER_DARK;
    }

    private static final String MORE_DEFAULT_DARK =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #58586e;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18; -fx-padding: 0;";

    private static final String MORE_HOVER_DARK =
            "-fx-background-color: rgba(255,255,255,0.09);" +
                    "-fx-text-fill: #a0a0c0;" +
                    "-fx-font-size: 18px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 18; -fx-padding: 0;";

    // ─────────────────────────────────────────────────────────────

    public PlayableSongCell(Consumer<Song> onPlay,
                            Consumer<Song> onAddToPlaylist,
                            Consumer<Song> onRemoveFromPlaylist,
                            Supplier<String> playlistNameSupplier,
                            Consumer<String> onOpenArtist) {
        this.onPlay               = onPlay;
        this.onAddToPlaylist      = onAddToPlaylist;
        this.onRemoveFromPlaylist = onRemoveFromPlaylist;
        this.playlistNameSupplier = playlistNameSupplier;
        this.onOpenArtist         = onOpenArtist;

        // Now-playing bar — hidden by default
        nowPlayingBar.setPrefWidth(3);
        nowPlayingBar.setMinWidth(3);
        nowPlayingBar.setMaxWidth(3);
        nowPlayingBar.setStyle(
                "-fx-background-color: #8b5cf6;" +
                        "-fx-background-radius: 2;");
        nowPlayingBar.setVisible(false);

        // Play button
        playButton.setStyle(playDefaultStyle());
        playButton.setPrefSize(34, 34);
        playButton.setMinSize(34, 34);
        playButton.setMaxSize(34, 34);
        playButton.setFocusTraversable(false);

        // More button
        moreButton.setStyle(moreDefaultStyle());
        moreButton.setPrefSize(36, 36);
        moreButton.setMinSize(36, 36);
        moreButton.setMaxSize(36, 36);
        moreButton.setFocusTraversable(false);

        // Labels
        titleLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                        + CellStyleKit.getTextPrimary() + ";");

        textBox.getChildren().add(titleLabel);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        HBox.setHgrow(spacer,  Priority.ALWAYS);

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(9, 12, 9, 12));
        row.setStyle(CellStyleKit.getRowDefault());
        row.getChildren().addAll(nowPlayingBar, playButton, textBox, spacer, moreButton);

        // Row hover
        row.setOnMouseEntered(e -> {
            Song s = getItem();
            boolean isPlaying = isCurrentTrackInThisPlaylist(s);
                       if (!isPlaying) {
                row.setStyle(CellStyleKit.getRowHover());
                playButton.setStyle(playHoverStyle());
            }
            moreButton.setStyle(moreHoverStyle());
        });
        row.setOnMouseExited(e -> {
            Song s = getItem();
            boolean isPlaying = isCurrentTrackInThisPlaylist(s);
            row.setStyle(isPlaying ? CellStyleKit.getRowPlaying() : CellStyleKit.getRowDefault());
            playButton.setStyle(isPlaying ? playPlayingStyle() : playDefaultStyle());
            moreButton.setStyle(moreDefaultStyle());
        });

        // Actions
        playButton.setOnAction(ev -> {
            Song s = getItem();
            if (s != null && onPlay != null) onPlay.accept(s);
            ev.consume();
        });
        moreButton.setOnAction(
                ev -> {
                    Song s = getItem();
                    if (s != null) {
                        Bounds b = moreButton.localToScreen(moreButton.getBoundsInLocal());
                        if (b != null) {
                            showSongMenu(s, moreButton, b.getMinX(), b.getMaxY() + 6);
                        }
                    }
                    ev.consume();
                });

        row.setOnMouseClicked(ev -> {
            if (ev.getButton() != MouseButton.PRIMARY || ev.getClickCount() != 2) return;
            Song s = getItem();
            if (s != null && onPlay != null) {
                onPlay.accept(s);
                ev.consume();
            }
        });

        row.addEventFilter(
                ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                ev -> {
                    Song s = getItem();
                    if (s == null || isEmpty()) {
                        return;
                    }
                    showSongMenu(s, row, ev.getScreenX(), ev.getScreenY());
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
        hideActionMenu();

        if (empty || song == null) {
            setText(null); setGraphic(null);
            setBackground(Background.EMPTY);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        boolean isPlaying = isCurrentTrackInThisPlaylist(song);

        titleLabel.setText(song.title());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (isPlaying ? CellStyleKit.getAccentTitle() : CellStyleKit.getTextPrimary()) + ";");
        while (textBox.getChildren().size() > 1) {
            textBox.getChildren().remove(1);
        }
        // Genre omitted on playlist page UI; search still matches genre in SongSearchService.
        HBox meta = CellStyleKit.songMetaLine(song.artist(), null, onOpenArtist);
        if (!meta.getChildren().isEmpty()) {
            textBox.getChildren().add(meta);
        }

        nowPlayingBar.setVisible(isPlaying);
        nowPlayingBar.setManaged(isPlaying);
        playButton.setText(isPlaying ? "⏸" : "▶");
        playButton.setStyle(isPlaying ? playPlayingStyle() : playDefaultStyle());
        row.setStyle(isPlaying ? CellStyleKit.getRowPlaying() : CellStyleKit.getRowDefault());
        CellStyleKit.markPlaying(row, isPlaying);

        setText(null); setGraphic(row);
        setBackground(Background.EMPTY);
        setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
    }

    @Override
    public void updateSelected(boolean selected) { super.updateSelected(false); }

    private void showSongMenu(Song song, Node anchor, double screenX, double screenY) {
        hideActionMenu();

        String playlistName =
                playlistNameSupplier != null ? playlistNameSupplier.get() : "this playlist";
        SongContextMenuBuilder.Spec spec;
        if (PlaylistNames.isLikedSongs(playlistName)) {
            spec =
                    new SongContextMenuBuilder.Spec()
                            .playNext(true)
                            .addToPlaylist(true, "Add to Another Playlist")
                            .likeToggle(true);
        } else {
            spec =
                    SongContextMenuBuilder.Spec.forPlaylistRow(
                            playlistName,
                            () -> {
                                if (onRemoveFromPlaylist != null) {
                                    onRemoveFromPlaylist.accept(song);
                                }
                            });
        }
        ContextMenu menu = SongContextMenuBuilder.build(song, anchor, spec);
        if (onAddToPlaylist != null) {
            for (var item : menu.getItems()) {
                if ("Add to Another Playlist".equals(item.getText())) {
                    item.setOnAction(
                            ev -> {
                                hideActionMenu();
                                onAddToPlaylist.accept(song);
                            });
                    break;
                }
            }
        }
        activeMenu = menu;
        menu.show(anchor, screenX, screenY);
    }

    private void hideActionMenu() {
        if (activeMenu != null) {
            activeMenu.hide();
            activeMenu = null;
        }
    }

    private boolean isCurrentTrackInThisPlaylist(Song song) {
        if (song == null || player.getCurrentSong() == null) {
            return false;
        }

        String currentSourcePlaylist = player.getCurrentSourcePlaylistName();
        String thisPlaylist = playlistNameSupplier == null ? "" : playlistNameSupplier.get();
        if (currentSourcePlaylist == null) {
            currentSourcePlaylist = "";
        }
        if (thisPlaylist == null) {
            thisPlaylist = "";
        }

        return !currentSourcePlaylist.isBlank()
                && currentSourcePlaylist.equals(thisPlaylist)
                && player.getCurrentSong().songId() == song.songId();
    }
}
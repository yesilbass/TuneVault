package com.example.tunevaultfx.playlist.cell;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.playlist.SongRowArtGraphic;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.Node;
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

    private static final double ROW_ART_SIZE = 48;
    private static final double INDEX_COL = 28;
    private static final double ALBUM_MIN = 120;
    private static final double ALBUM_PREF = 188;
    private static final double ALBUM_MAX = 220;
    private static final double DUR_COL = 52;

    private final Button playButton = new Button("▶");
    private final Button moreButton = new Button("⋯");
    private final VBox textBox = new VBox(4);
    private final HBox row = new HBox(12);

    private final Region nowPlayingBar = new Region();
    private final Label indexLabel = new Label();
    private final StackPane artHost = new StackPane();
    private final Label albumLabel = new Label();
    private final Label durationLabel = new Label();

    private final Consumer<Song>    onPlay;
    private final Consumer<Song>    onAddToPlaylist;
    private final Consumer<Song>    onRemoveFromPlaylist;
    private final Supplier<String>  playlistNameSupplier;
    private final Consumer<String>  onOpenArtist;
    private final Consumer<Song>    onOpenSong;

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

    /** Current track row while playback is paused — still “live”, but shows ▶ to resume. */
    private static String playPausedOnCurrentRowStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: rgba(124,58,237,0.22);"
                    + "-fx-text-fill: #5b21b6;"
                    + "-fx-font-size: 14px; -fx-font-weight: bold;"
                    + "-fx-background-radius: 17;";
        }
        return "-fx-background-color: rgba(139,92,246,0.14);"
                + "-fx-text-fill: #ddd6fe;"
                + "-fx-font-size: 14px; -fx-font-weight: bold;"
                + "-fx-background-radius: 17;";
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
                            Consumer<String> onOpenArtist,
                            Consumer<Song> onOpenSong) {
        this.onPlay               = onPlay;
        this.onAddToPlaylist      = onAddToPlaylist;
        this.onRemoveFromPlaylist = onRemoveFromPlaylist;
        this.playlistNameSupplier = playlistNameSupplier;
        this.onOpenArtist         = onOpenArtist;
        this.onOpenSong           = onOpenSong;

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

        indexLabel.setMinWidth(INDEX_COL);
        indexLabel.setPrefWidth(INDEX_COL);
        indexLabel.setMaxWidth(INDEX_COL);
        indexLabel.setAlignment(Pos.CENTER_RIGHT);
        indexLabel.getStyleClass().add("playlist-track-index");

        artHost.setMinSize(ROW_ART_SIZE, ROW_ART_SIZE);
        artHost.setPrefSize(ROW_ART_SIZE, ROW_ART_SIZE);
        artHost.setMaxSize(ROW_ART_SIZE, ROW_ART_SIZE);

        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        albumLabel.setMinWidth(ALBUM_MIN);
        albumLabel.setPrefWidth(ALBUM_PREF);
        albumLabel.setMaxWidth(ALBUM_MAX);
        albumLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        albumLabel.getStyleClass().add("playlist-track-album");

        durationLabel.setMinWidth(DUR_COL);
        durationLabel.setPrefWidth(DUR_COL);
        durationLabel.setMaxWidth(DUR_COL);
        durationLabel.setAlignment(Pos.CENTER_RIGHT);
        durationLabel.getStyleClass().add("playlist-track-duration");

        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 12));
        row.setStyle(CellStyleKit.getRowDefault());
        row.getChildren()
                .addAll(
                        nowPlayingBar,
                        indexLabel,
                        artHost,
                        textBox,
                        albumLabel,
                        durationLabel,
                        playButton,
                        moreButton);

        // Row hover
        row.setOnMouseEntered(e -> {
            Song s = getItem();
            boolean isCurrent = isCurrentSongRow(s);
            if (!isCurrent) {
                row.setStyle(CellStyleKit.getRowHover());
                playButton.setStyle(playHoverStyle());
            } else if (player.isPlaying()) {
                playButton.setStyle(playPlayingStyle());
            } else {
                playButton.setStyle(playPausedOnCurrentRowStyle());
            }
            moreButton.setStyle(moreHoverStyle());
        });
        row.setOnMouseExited(e -> {
            Song s = getItem();
            boolean isCurrent = isCurrentSongRow(s);
            row.setStyle(isCurrent ? CellStyleKit.getRowPlaying() : CellStyleKit.getRowDefault());
            if (!isCurrent) {
                playButton.setStyle(playDefaultStyle());
            } else if (player.isPlaying()) {
                playButton.setStyle(playPlayingStyle());
            } else {
                playButton.setStyle(playPausedOnCurrentRowStyle());
            }
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
            if (CellStyleKit.isInteractiveRowChromeTarget(ev.getTarget())) return;
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
            setText(null);
            setGraphic(null);
            setBackground(Background.EMPTY);
            setStyle("-fx-background-color: transparent;");
            return;
        }

        boolean isCurrent = isCurrentSongRow(song);
        boolean audioPlaying = isCurrent && player.isPlaying();

        int idx = getIndex();
        indexLabel.setText(idx >= 0 ? String.format("%02d", idx + 1) : "");

        artHost.getChildren().clear();
        artHost.getChildren().add(SongRowArtGraphic.create(ROW_ART_SIZE, song));

        textBox.getChildren().clear();
        if (onOpenSong != null) {
            Hyperlink titleLink =
                    CellStyleKit.primaryTitleLink(song.title(), () -> onOpenSong.accept(song));
            titleLink.setWrapText(false);
            titleLink.setTextOverrun(OverrunStyle.ELLIPSIS);
            titleLink.setMaxWidth(Double.MAX_VALUE);
            if (isCurrent) {
                titleLink.setStyle(
                        "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: "
                                + CellStyleKit.getAccentTitle()
                                + "; -fx-border-width: 0; -fx-background-color: transparent;");
            } else {
                titleLink.setStyle(
                        "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: "
                                + CellStyleKit.getTextPrimary()
                                + "; -fx-border-width: 0; -fx-background-color: transparent;");
            }
            textBox.getChildren().add(titleLink);
        } else {
            Label titleLabel = new Label(song.title());
            titleLabel.setWrapText(false);
            titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            titleLabel.setStyle(
                    "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: "
                            + (isCurrent ? CellStyleKit.getAccentTitle() : CellStyleKit.getTextPrimary())
                            + ";");
            textBox.getChildren().add(titleLabel);
        }
        HBox meta = CellStyleKit.songMetaLine(song.artist(), song.genre(), onOpenArtist);
        if (!meta.getChildren().isEmpty()) {
            textBox.getChildren().add(meta);
        }

        String album = song.album();
        albumLabel.setText(album != null && !album.isBlank() ? album.trim() : "—");
        albumLabel.setOpacity(album != null && !album.isBlank() ? 1 : 0.55);

        int sec = Math.max(0, song.durationSeconds());
        durationLabel.setText(sec > 0 ? (sec / 60) + ":" + String.format("%02d", sec % 60) : "—");

        nowPlayingBar.setVisible(isCurrent);
        nowPlayingBar.setManaged(isCurrent);
        playButton.setText(audioPlaying ? "⏸" : "▶");
        if (!isCurrent) {
            playButton.setStyle(playDefaultStyle());
        } else if (audioPlaying) {
            playButton.setStyle(playPlayingStyle());
        } else {
            playButton.setStyle(playPausedOnCurrentRowStyle());
        }
        row.setStyle(isCurrent ? CellStyleKit.getRowPlaying() : CellStyleKit.getRowDefault());

        setText(null);
        setGraphic(row);
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

    /** True when this row's song is the global current track (any source). */
    private boolean isCurrentSongRow(Song song) {
        if (song == null || player.getCurrentSong() == null) {
            return false;
        }
        return player.getCurrentSong().songId() == song.songId();
    }
}
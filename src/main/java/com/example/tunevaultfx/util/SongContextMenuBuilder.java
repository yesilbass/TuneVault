package com.example.tunevaultfx.util;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.playlist.service.PlaylistPickerService;
import com.example.tunevaultfx.playlist.service.PlaylistService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 * Right-click / secondary-click and overflow (⋯) menus for song rows.
 */
public final class SongContextMenuBuilder {

    private static final MusicPlayerController PLAYER = MusicPlayerController.getInstance();
    private static final PlaylistService PLAYLIST_SERVICE = new PlaylistService();
    private static final PlaylistPickerService PICKER = new PlaylistPickerService();

    private SongContextMenuBuilder() {}

    public static ContextMenu build(Song song, Node anchor, Spec spec) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("tv-song-actions-menu");
        menu.setAutoHide(true);
        ContextMenuPopupSupport.installThemedPopupHandlers(menu, anchor);

        if (spec.includePlayNext) {
            MenuItem playNext = new MenuItem("Play Next");
            playNext.setOnAction(
                    e -> {
                        PLAYER.addToQueueNext(song);
                        menu.hide();
                    });
            menu.getItems().add(playNext);
        }

        if (spec.includeAddToPlaylist) {
            String label =
                    spec.addToPlaylistMenuLabel != null && !spec.addToPlaylistMenuLabel.isBlank()
                            ? spec.addToPlaylistMenuLabel
                            : "Add to playlist";
            MenuItem add = new MenuItem(label);
            add.setOnAction(
                    e -> {
                        Scene sc = anchor != null ? anchor.getScene() : null;
                        if (sc != null) {
                            PICKER.show(song, sc);
                        }
                        menu.hide();
                    });
            menu.getItems().add(add);
        }

        if (spec.includeLikeToggle) {
            UserProfile profile = SessionManager.getCurrentUserProfile();
            boolean liked = profile != null && profile.isLiked(song);
            MenuItem like =
                    new MenuItem(
                            liked
                                    ? "Remove from " + PlaylistNames.LIKED_SONGS
                                    : "Save to " + PlaylistNames.LIKED_SONGS);
            if (liked) {
                like.getStyleClass().add("tv-menu-destructive");
            }
            like.setOnAction(
                    e -> {
                        PLAYLIST_SERVICE.toggleLikeSong(song);
                        menu.hide();
                    });
            menu.getItems().add(like);
        }

        if (spec.removeFromPlaylistLabel != null
                && spec.onRemoveFromPlaylist != null
                && !spec.removeFromPlaylistLabel.isBlank()) {
            MenuItem remove = new MenuItem("Remove from " + spec.removeFromPlaylistLabel);
            remove.getStyleClass().add("tv-menu-destructive");
            remove.setOnAction(
                    e -> {
                        spec.onRemoveFromPlaylist.run();
                        menu.hide();
                    });
            menu.getItems().add(remove);
        }

        return menu;
    }

    /** Configuration for {@link #build(Song, Node, Spec)}. */
    public static final class Spec {
        boolean includePlayNext = true;
        boolean includeAddToPlaylist = true;
        String addToPlaylistMenuLabel;
        boolean includeLikeToggle = true;
        String removeFromPlaylistLabel;
        Runnable onRemoveFromPlaylist;

        public Spec playNext(boolean v) {
            this.includePlayNext = v;
            return this;
        }

        public Spec addToPlaylist(boolean v, String menuLabelOrNull) {
            this.includeAddToPlaylist = v;
            this.addToPlaylistMenuLabel = menuLabelOrNull;
            return this;
        }

        public Spec likeToggle(boolean v) {
            this.includeLikeToggle = v;
            return this;
        }

        public Spec removeFromPlaylist(String displayName, Runnable onRemove) {
            this.removeFromPlaylistLabel = displayName;
            this.onRemoveFromPlaylist = onRemove;
            return this;
        }

        public static Spec forPlaylistRow(String playlistDisplayName, Runnable onRemove) {
            return new Spec()
                    .playNext(true)
                    .addToPlaylist(true, "Add to Another Playlist")
                    .likeToggle(true)
                    .removeFromPlaylist(playlistDisplayName, onRemove);
        }

        public static Spec general() {
            return new Spec().playNext(true).addToPlaylist(true, null).likeToggle(true);
        }
    }
}

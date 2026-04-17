package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.playlist.service.PlaylistService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.ContextMenuPopupSupport;
import com.example.tunevaultfx.util.ToastUtil;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * Context menu for a library playlist row (sidebar, home tiles, etc.).
 */
public final class PlaylistLibraryContextMenu {

    private PlaylistLibraryContextMenu() {}

    /**
     * @param anchor node that owns the menu (for themed popup scene + clip); also supplies {@link Scene}
     *               for overlays
     * @param refreshLibraryUi run after mutations that should reload playlist lists / tiles (pins,
     *                           rename, delete)
     */
    public static ContextMenu create(
            Node anchor,
            UserProfile profile,
            PlaylistService playlistService,
            String playlistName,
            Runnable refreshLibraryUi) {
        ContextMenu menu = new ContextMenu();
        Scene scene = anchor != null ? anchor.getScene() : null;
        if (profile == null || scene == null || playlistName == null) {
            return menu;
        }

        MenuItem addQueue = new MenuItem("Add to queue");
        addQueue.setOnAction(e -> handleAddPlaylistToQueue(profile, playlistName, scene));

        MenuItem editInfo = new MenuItem("Edit info");
        editInfo.setDisable(PlaylistNames.isLikedSongs(playlistName));
        editInfo.setOnAction(
                e ->
                        RenamePlaylistOverlay.show(
                                scene, profile, playlistService, playlistName, refreshLibraryUi));

        CheckMenuItem pinItem = new CheckMenuItem("Pin playlist");
        pinItem.setSelected(playlistService.isPlaylistPinned(profile, playlistName));
        pinItem.setOnAction(
                e -> {
                    boolean want = pinItem.isSelected();
                    if (want) {
                        if (!playlistService.setPlaylistPinned(profile, playlistName, true)) {
                            pinItem.setSelected(false);
                            ToastUtil.warning(
                                    scene,
                                    "You can pin up to "
                                            + PlaylistNames.MAX_USER_PINNED_PLAYLISTS
                                            + " playlists.");
                        } else {
                            refreshLibraryUi.run();
                        }
                    } else {
                        playlistService.setPlaylistPinned(profile, playlistName, false);
                        refreshLibraryUi.run();
                    }
                });

        MenuItem deleteItem = new MenuItem("Delete playlist");
        deleteItem.setDisable(PlaylistNames.isLikedSongs(playlistName));
        deleteItem.setOnAction(
                e ->
                        PlaylistDeleteOverlay.show(
                                scene, profile, playlistService, playlistName, refreshLibraryUi));

        CheckMenuItem publicItem = new CheckMenuItem("Show on profile (public)");
        if (PlaylistNames.isLikedSongs(playlistName)) {
            publicItem.setDisable(true);
        } else {
            publicItem.setSelected(playlistService.isPlaylistPublic(profile, playlistName));
            publicItem.setOnAction(
                    e -> {
                        boolean on = publicItem.isSelected();
                        if (playlistService.setPlaylistPublic(profile, playlistName, on)) {
                            refreshLibraryUi.run();
                            SessionManager.notifyPlaylistPublicChanged();
                            ToastUtil.info(
                                    scene,
                                    on
                                            ? "Playlist is public — discoverable in search."
                                            : "Playlist is private again.");
                        } else {
                            publicItem.setSelected(!on);
                            ToastUtil.info(scene, "Could not update playlist visibility.");
                        }
                    });
        }

        menu.getItems()
                .addAll(
                        addQueue,
                        editInfo,
                        pinItem,
                        publicItem,
                        new SeparatorMenuItem(),
                        deleteItem);
        ContextMenuPopupSupport.installThemedPopupHandlers(menu, anchor);
        return menu;
    }

    private static void handleAddPlaylistToQueue(
            UserProfile profile, String playlistName, Scene scene) {
        ObservableList<Song> songs = profile.getPlaylists().get(playlistName);
        if (songs == null || songs.isEmpty()) {
            ToastUtil.info(scene, "This playlist has no songs to add.");
            return;
        }
        MusicPlayerController.getInstance().addSongsToUserQueueEnd(songs);
        ToastUtil.info(scene, "Added " + songs.size() + " song(s) to the queue.");
    }
}

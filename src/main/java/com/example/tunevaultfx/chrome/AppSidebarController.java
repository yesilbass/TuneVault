package com.example.tunevaultfx.chrome;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.playlist.CreatePlaylistOverlay;
import com.example.tunevaultfx.playlist.PlaylistCoverGraphic;
import com.example.tunevaultfx.playlist.PlaylistLibraryContextMenu;
import com.example.tunevaultfx.playlist.service.PlaylistService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakMapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.OverrunStyle;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Left column: library panel only (playlist list). Discovery lives elsewhere (e.g. home).
 */
public class AppSidebarController {

    @FXML private VBox sidebarRoot;

    @FXML private ListView<String> libraryListView;

    private final PlaylistService playlistService = new PlaylistService();

    private final MapChangeListener<String, ObservableList<Song>> libraryPlaylistKeysChanged =
            c -> Platform.runLater(this::refreshLibraryItems);

    @FXML
    public void initialize() {
        setupLibraryList();
        refreshLibraryItems();
        UserProfile p = SessionManager.getCurrentUserProfile();
        if (p != null) {
            p.getPlaylists().addListener(new WeakMapChangeListener<>(libraryPlaylistKeysChanged));
            p.getPinnedPlaylistsOrdered()
                    .addListener(
                            (javafx.collections.ListChangeListener<String>)
                                    c -> Platform.runLater(this::refreshLibraryItems));
        }

        sidebarRoot.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) {
                Platform.runLater(this::refreshLibraryItems);
            }
        });
    }

    private void setupLibraryList() {
        Label ph = new Label("Use Create playlist above to add one.");
        ph.getStyleClass().add("sidebar-library-placeholder");
        ph.setWrapText(true);
        libraryListView.setPlaceholder(ph);
        libraryListView.setFixedCellSize(52);
        libraryListView.getSelectionModel().clearSelection();
        libraryListView.setCellFactory(
                lv ->
                        new ListCell<>() {
                            {
                                setOnContextMenuRequested(
                                        ev -> {
                                            String pl = getItem();
                                            if (pl == null || isEmpty()) {
                                                return;
                                            }
                                            ContextMenu menu =
                                                    PlaylistLibraryContextMenu.create(
                                                            this,
                                                            SessionManager.getCurrentUserProfile(),
                                                            playlistService,
                                                            pl,
                                                            AppSidebarController.this::refreshLibraryItems);
                                            menu.show(this, ev.getScreenX(), ev.getScreenY());
                                            ev.consume();
                                        });
                            }

                            @Override
                            protected void updateItem(String name, boolean empty) {
                                super.updateItem(name, empty);
                                if (empty || name == null) {
                                    setText(null);
                                    setGraphic(null);
                                    getStyleClass().remove("liked-songs-cell");
                                    getStyleClass().remove("sidebar-pinned-playlist");
                                    return;
                                }
                                getStyleClass().remove("liked-songs-cell");
                                getStyleClass().remove("sidebar-pinned-playlist");
                                UserProfile p = SessionManager.getCurrentUserProfile();
                                boolean liked = PlaylistNames.isLikedSongs(name);
                                boolean pinned = p != null && p.getPinnedPlaylistsOrdered().contains(name);
                                if (liked) {
                                    getStyleClass().add("liked-songs-cell");
                                }
                                if (pinned) {
                                    getStyleClass().add("sidebar-pinned-playlist");
                                }

                                StackPane art = PlaylistCoverGraphic.create(38, name);
                                StackPane pinHost = new StackPane();
                                pinHost.setMinWidth(22);
                                pinHost.setPrefWidth(22);
                                pinHost.setMaxWidth(22);
                                if (pinned) {
                                    Label pinGlyph = new Label("\uD83D\uDCCC");
                                    pinGlyph.setMouseTransparent(true);
                                    pinGlyph.getStyleClass().add("sidebar-pin-glyph");
                                    pinGlyph.setTooltip(new Tooltip("Pinned to top of library"));
                                    pinHost.getChildren().add(pinGlyph);
                                }

                                Label nameLabel = new Label(name);
                                nameLabel.getStyleClass().add("sidebar-playlist-name");
                                nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
                                nameLabel.setWrapText(false);
                                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                                nameLabel.setMaxWidth(Double.MAX_VALUE);
                                if (liked) {
                                    nameLabel.getStyleClass().add("sidebar-playlist-name-liked");
                                } else if (pinned) {
                                    nameLabel.getStyleClass().add("sidebar-playlist-name-pinned");
                                }
                                Label visBadge = new Label();
                                if (liked) {
                                    visBadge.setVisible(false);
                                    visBadge.setManaged(false);
                                } else {
                                    boolean pub =
                                            p != null && playlistService.isPlaylistPublic(p, name);
                                    visBadge.setText(pub ? "Public" : "Private");
                                    visBadge.getStyleClass()
                                            .addAll(
                                                    "profile-playlist-vis",
                                                    pub
                                                            ? "profile-playlist-vis-public"
                                                            : "profile-playlist-vis-private");
                                    visBadge.setMouseTransparent(true);
                                }
                                HBox textRow = new HBox(8, nameLabel, visBadge);
                                textRow.setAlignment(Pos.CENTER_LEFT);
                                HBox.setHgrow(textRow, Priority.ALWAYS);
                                textRow.setMaxWidth(Double.MAX_VALUE);
                                HBox row = new HBox(8, art, pinHost, textRow);
                                row.setAlignment(Pos.CENTER_LEFT);
                                row.setPadding(new Insets(0, 6, 0, 2));
                                setText(null);
                                setGraphic(row);
                            }
                        });

        libraryListView.setOnMouseClicked(
                ev -> {
                    if (ev.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    String name = libraryListView.getSelectionModel().getSelectedItem();
                    if (name != null) {
                        openPlaylist(name);
                    }
                });
    }

    private void refreshLibraryItems() {
        UserProfile profile = SessionManager.getCurrentUserProfile();
        ObservableList<String> items = FXCollections.observableArrayList();
        if (profile != null && profile.getPlaylists() != null && !profile.getPlaylists().isEmpty()) {
            List<String> names = new ArrayList<>(profile.getPlaylists().keySet());
            PlaylistNames.orderSidebarPlaylists(names, profile.getPinnedPlaylistsOrdered());
            items.setAll(names);
        }
        libraryListView.setItems(items);
        libraryListView.refresh();
    }

    private void openPlaylist(String playlistName) {
        SessionManager.requestPlaylistToOpen(playlistName);
        try {
            navigate(FxmlResources.PLAYLISTS, sidebarRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCreatePlaylistClick() {
        UserProfile profile = SessionManager.getCurrentUserProfile();
        Scene scene = sidebarRoot.getScene();
        if (profile == null || scene == null) {
            return;
        }
        CreatePlaylistOverlay.show(
                scene,
                profile,
                playlistService,
                name -> {
                    SessionManager.requestPlaylistToOpen(name);
                    try {
                        navigate(FxmlResources.PLAYLISTS, sidebarRoot);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void navigate(String fxml, Object source) throws IOException {
        SceneUtil.switchScene((Node) source, fxml);
    }
}

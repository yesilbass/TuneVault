package com.example.tunevaultfx.chrome;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakMapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Left rail: primary navigation plus scrollable playlist list (Spotify-style library).
 */
public class AppSidebarController {

    @FXML private VBox sidebarRoot;

    @FXML private Button navHome;
    @FXML private Button navSearch;
    @FXML private Button navLibrary;
    @FXML private Button navWrapped;
    @FXML private Button navGenre;

    @FXML private ListView<String> libraryListView;

    private final MapChangeListener<String, ObservableList<Song>> libraryPlaylistKeysChanged =
            c -> Platform.runLater(this::refreshLibraryItems);

    @FXML
    public void initialize() {
        setupLibraryList();
        refreshLibraryItems();
        UserProfile p = SessionManager.getCurrentUserProfile();
        if (p != null) {
            p.getPlaylists().addListener(new WeakMapChangeListener<>(libraryPlaylistKeysChanged));
        }

        sidebarRoot.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) {
                Platform.runLater(
                        () -> {
                            updateNavHighlight();
                            refreshLibraryItems();
                        });
            }
        });
    }

    private void setupLibraryList() {
        Label ph = new Label("Create a playlist in Library to see it here.");
        ph.getStyleClass().add("sidebar-library-placeholder");
        ph.setWrapText(true);
        libraryListView.setPlaceholder(ph);
        libraryListView.setFixedCellSize(40);
        libraryListView.getSelectionModel().clearSelection();
        libraryListView.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(String name, boolean empty) {
                                super.updateItem(name, empty);
                                if (empty || name == null) {
                                    setText(null);
                                    setGraphic(null);
                                    getStyleClass().remove("liked-songs-cell");
                                    return;
                                }
                                setText(name);
                                getStyleClass().remove("liked-songs-cell");
                                if ("Liked Songs".equals(name)) {
                                    getStyleClass().add("liked-songs-cell");
                                }
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
            PlaylistNames.sortForDisplay(names);
            items.setAll(names);
        }
        libraryListView.setItems(items);
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
    private void handleBrandClick(MouseEvent e) throws IOException {
        navigate(FxmlResources.MAIN_MENU, e.getSource());
    }

    @FXML
    private void goHome(ActionEvent e) throws IOException {
        navigate(FxmlResources.MAIN_MENU, e.getSource());
    }

    @FXML
    private void goSearch(ActionEvent e) throws IOException {
        navigate(FxmlResources.SEARCH, e.getSource());
    }

    @FXML
    private void goLibrary(ActionEvent e) throws IOException {
        navigate(FxmlResources.PLAYLISTS, e.getSource());
    }

    @FXML
    private void goWrapped(ActionEvent e) throws IOException {
        navigate(FxmlResources.WRAPPED, e.getSource());
    }

    @FXML
    private void goGenre(ActionEvent e) throws IOException {
        navigate(FxmlResources.FIND_YOUR_GENRE, e.getSource());
    }

    private void navigate(String fxml, Object source) throws IOException {
        SceneUtil.switchSceneNoHistory((Node) source, fxml);
    }

    private void updateNavHighlight() {
        String page = SceneUtil.getCurrentPage();
        if (page == null) {
            page = "";
        }

        // Artist profile is opened from many places; highlight the section we came from, not Search.
        String navPage = page;
        if (FxmlResources.ARTIST_PROFILE.equals(page)) {
            String from = SceneUtil.peekHistory();
            navPage = from != null ? from : "";
        }

        setNavActive(navHome, FxmlResources.MAIN_MENU.equals(navPage));
        setNavActive(navSearch, FxmlResources.SEARCH.equals(navPage));
        setNavActive(navLibrary, FxmlResources.PLAYLISTS.equals(navPage));
        setNavActive(navWrapped, FxmlResources.WRAPPED.equals(navPage));
        setNavActive(navGenre, FxmlResources.FIND_YOUR_GENRE.equals(navPage));
    }

    private void setNavActive(Button b, boolean active) {
        b.getStyleClass().removeAll("sidebar-nav-active");
        if (active) {
            b.getStyleClass().add("sidebar-nav-active");
        }
    }
}

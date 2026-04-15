package com.example.tunevaultfx.mainmenu;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.db.UserGenreDiscoveryDAO;
import com.example.tunevaultfx.db.UserGenreDiscoverySummary;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import com.example.tunevaultfx.util.UiMotionUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakMapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Home dashboard: libraries, personalized picks, newest catalog tracks.
 */
public class MainMenuController {

    @FXML private VBox homeHeroSection;
    @FXML private VBox homeLibrariesSection;
    @FXML private VBox forYouFeedCard;
    @FXML private VBox freshTracksFeedCard;
    @FXML private VBox menuContent;
    @FXML private HBox homeFeedRow;
    @FXML private FlowPane playlistChipsContainer;
    @FXML private Label homeGreetingLabel;
    @FXML private Label homeStatsLabel;
    @FXML private Label homeVibeLabel;
    @FXML private Label homeVibeSubLabel;
    @FXML private Label noPlaylistsHint;
    @FXML private ListView<Song> forYouListView;
    @FXML private ListView<Song> freshTracksListView;

    private final RecommendationService recommendationService = new RecommendationService();
    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final SongDAO songDAO = new SongDAO();
    private final UserGenreDiscoveryDAO genreDiscoveryDAO = new UserGenreDiscoveryDAO();

    private final MapChangeListener<String, ObservableList<Song>> homePlaylistKeysChanged =
            c -> Platform.runLater(this::refreshHomeLibraryStrip);

    @FXML
    public void initialize() {
        String user = SessionManager.getCurrentUsername();
        if (homeGreetingLabel != null) {
            homeGreetingLabel.setText(
                    user != null && !user.isBlank()
                            ? "Hey, " + user + " \u2014 here's your space."
                            : "Welcome to TuneVault");
        }

        UserProfile profile = SessionManager.getCurrentUserProfile();
        int playlistCount =
                profile != null && profile.getPlaylists() != null ? profile.getPlaylists().size() : 0;
        int songTotal = countUniqueSavedSongs(profile);
        if (homeStatsLabel != null) {
            homeStatsLabel.setText(
                    playlistCount + " playlist" + (playlistCount != 1 ? "s" : "")
                            + "  \u00B7  "
                            + songTotal + " saved song" + (songTotal != 1 ? "s" : ""));
        }

        buildPlaylistChips(profile);
        loadFeedLists(user);
        applyHomeVibe(user);

        setupHomeListView(forYouListView);
        setupHomeListView(freshTracksListView);

        if (menuContent != null) {
            menuContent.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    return;
                }
                applyResponsiveDensity(newScene.getWidth());
                newScene.widthProperty().addListener((o, oldW, newW) ->
                        applyResponsiveDensity(newW.doubleValue()));
            });
        }

        // Double defer: first layout pass applies sizes; then we hide + stagger (avoids a one-frame flash).
        Platform.runLater(
                () ->
                        Platform.runLater(
                                () -> {
                                    UiMotionUtil.playStaggeredEntrance(
                                            List.of(
                                                    homeHeroSection,
                                                    homeLibrariesSection,
                                                    forYouFeedCard,
                                                    freshTracksFeedCard));
                                    UiMotionUtil.applyHoverLift(forYouFeedCard);
                                    UiMotionUtil.applyHoverLift(freshTracksFeedCard);
                                }));
    }

    private void refreshHomeLibraryStrip() {
        UserProfile profile = SessionManager.getCurrentUserProfile();
        int playlistCount =
                profile != null && profile.getPlaylists() != null ? profile.getPlaylists().size() : 0;
        int songTotal = countUniqueSavedSongs(profile);
        if (homeStatsLabel != null) {
            homeStatsLabel.setText(
                    playlistCount + " playlist" + (playlistCount != 1 ? "s" : "")
                            + "  \u00B7  "
                            + songTotal + " saved song" + (songTotal != 1 ? "s" : ""));
        }
        buildPlaylistChips(profile);
    }

    private void applyHomeVibe(String username) {
        if (homeVibeLabel == null) {
            return;
        }
        if (username == null || username.isBlank()) {
            homeVibeLabel.setVisible(false);
            homeVibeLabel.setManaged(false);
            if (homeVibeSubLabel != null) {
                homeVibeSubLabel.setVisible(false);
                homeVibeSubLabel.setManaged(false);
            }
            return;
        }
        try {
            Optional<UserGenreDiscoverySummary> opt = genreDiscoveryDAO.loadSummary(username);
            if (opt.isEmpty()) {
                homeVibeLabel.getStyleClass().setAll("caption");
                homeVibeLabel.setText(
                        "When you\u2019re ready, use Account \u2192 Profile (or Genre Quiz in the sidebar) "
                                + "to shape recommendations.");
                homeVibeLabel.setVisible(true);
                homeVibeLabel.setManaged(true);
                if (homeVibeSubLabel != null) {
                    homeVibeSubLabel.setText("");
                    homeVibeSubLabel.setVisible(false);
                    homeVibeSubLabel.setManaged(false);
                }
                return;
            }
            UserGenreDiscoverySummary s = opt.get();
            homeVibeLabel.getStyleClass().setAll("home-vibe-line");
            homeVibeLabel.setText("Your vibe: " + s.blendLine());
            homeVibeLabel.setVisible(true);
            homeVibeLabel.setManaged(true);
            if (homeVibeSubLabel != null) {
                String mode = s.quizModeLabel();
                String modeBit =
                        mode.isEmpty()
                                ? "Saved from Find Your Genre."
                                : "Saved from your " + mode + " quiz.";
                homeVibeSubLabel.setText(
                        modeBit
                                + " This nudges picks, search, and autoplay. Update from Account \u2192 Profile or Genre Quiz.");
                homeVibeSubLabel.setVisible(true);
                homeVibeSubLabel.setManaged(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            homeVibeLabel.setText("");
            homeVibeLabel.setVisible(false);
            homeVibeLabel.setManaged(false);
            if (homeVibeSubLabel != null) {
                homeVibeSubLabel.setText("");
                homeVibeSubLabel.setVisible(false);
                homeVibeSubLabel.setManaged(false);
            }
        }
    }

    private static int countUniqueSavedSongs(UserProfile profile) {
        if (profile == null || profile.getPlaylists() == null) {
            return 0;
        }
        Set<Integer> seen = new HashSet<>();
        for (ObservableList<Song> list : profile.getPlaylists().values()) {
            if (list == null) {
                continue;
            }
            for (Song s : list) {
                if (s != null) {
                    seen.add(s.songId());
                }
            }
        }
        return seen.size();
    }

    private void buildPlaylistChips(UserProfile profile) {
        playlistChipsContainer.getChildren().clear();
        if (menuContent != null) {
            playlistChipsContainer.prefWrapLengthProperty()
                    .bind(
                            Bindings.createDoubleBinding(
                                    () -> Math.max(400, menuContent.getWidth() - 80),
                                    menuContent.widthProperty()));
        }
        if (profile == null || profile.getPlaylists() == null || profile.getPlaylists().isEmpty()) {
            noPlaylistsHint.setVisible(true);
            noPlaylistsHint.setManaged(true);
            return;
        }
        noPlaylistsHint.setVisible(false);
        noPlaylistsHint.setManaged(false);

        List<String> names = new ArrayList<>(profile.getPlaylists().keySet());
        PlaylistNames.sortForDisplay(names);

        final int max = 16;
        for (int i = 0; i < Math.min(names.size(), max); i++) {
            String name = names.get(i);
            playlistChipsContainer.getChildren().add(createPlaylistTile(name));
        }
        if (names.size() > max) {
            int more = names.size() - max;
            playlistChipsContainer.getChildren().add(createMorePlaylistsTile(more));
        }
    }

    private HBox createPlaylistTile(String playlistName) {
        Label glyph = new Label(PlaylistNames.glyphForPlaylist(playlistName));
        glyph.getStyleClass().add("home-playlist-tile-glyph");
        StackPane art = new StackPane(glyph);
        art.setMinSize(52, 52);
        art.setPrefSize(52, 52);
        art.setMaxSize(52, 52);
        art.getStyleClass().add("home-playlist-tile-art");

        Label title = new Label(playlistName);
        title.getStyleClass().add("home-playlist-tile-title");
        title.setWrapText(true);
        title.setMaxWidth(146);

        HBox row = new HBox(12, art, title);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("home-playlist-tile");
        row.setPadding(new Insets(8, 12, 8, 10));
        row.setPrefWidth(Region.USE_COMPUTED_SIZE);
        row.setMinWidth(196);
        row.setMaxWidth(236);
        row.setOnMouseClicked(
                ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY) {
                        goToPlaylist(playlistName);
                        ev.consume();
                    }
                });
        return row;
    }

    private HBox createMorePlaylistsTile(int moreCount) {
        Label glyph = new Label("+" + moreCount);
        glyph.getStyleClass().addAll("home-playlist-tile-glyph", "home-playlist-tile-glyph-more");
        StackPane art = new StackPane(glyph);
        art.setMinSize(52, 52);
        art.setPrefSize(52, 52);
        art.setMaxSize(52, 52);
        art.getStyleClass().addAll("home-playlist-tile-art", "home-playlist-tile-art-more");

        Label title = new Label("See all playlists");
        title.getStyleClass().add("home-playlist-tile-title");
        title.setWrapText(true);
        title.setMaxWidth(146);

        HBox row = new HBox(12, art, title);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("home-playlist-tile", "home-playlist-tile-more");
        row.setPadding(new Insets(8, 12, 8, 10));
        row.setMinWidth(196);
        row.setMaxWidth(236);
        row.setOnMouseClicked(
                ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY) {
                        try {
                            openPlaylistsPageFromHome();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        ev.consume();
                    }
                });
        return row;
    }

    private void goToPlaylist(String playlistName) {
        SessionManager.requestPlaylistToOpen(playlistName);
        try {
            SceneUtil.switchSceneNoHistory(menuContent, FxmlResources.PLAYLISTS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openPlaylistsPageFromHome() throws IOException {
        SceneUtil.switchSceneNoHistory(menuContent, FxmlResources.PLAYLISTS);
    }

    private void loadFeedLists(String username) {
        ObservableList<Song> forYou = FXCollections.observableArrayList();
        if (username != null && !username.isBlank()) {
            forYou.setAll(recommendationService.getSuggestedSongsForUser(username, 14));
        }
        forYouListView.setItems(forYou);

        ObservableList<Song> src;
        try {
            if (SessionManager.isSongLibraryReady()) {
                src = FXCollections.observableArrayList(SessionManager.getSongLibrary());
            } else {
                src = songDAO.getAllSongs();
            }
        } catch (Exception e) {
            e.printStackTrace();
            src = FXCollections.observableArrayList();
        }
        List<Song> fresh =
                src.stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingInt(Song::songId).reversed())
                        .limit(14)
                        .collect(Collectors.toList());
        freshTracksListView.setItems(FXCollections.observableArrayList(fresh));
    }

    private void setupHomeListView(ListView<Song> listView) {
        listView.setFixedCellSize(58);
        Label ph = new Label("Nothing here yet.");
        ph.setStyle("-fx-text-fill: " + CellStyleKit.getTextMuted() + "; -fx-font-size: 13px;");
        listView.setPlaceholder(ph);

        listView.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(Song song, boolean empty) {
                                super.updateItem(song, empty);
                                if (empty || song == null) {
                                    setText(null);
                                    setGraphic(null);
                                    return;
                                }

                                Label glyph = new Label("\u266A");
                                glyph.setStyle(
                                        "-fx-font-size: 15px; -fx-text-fill: #a78bfa; -fx-font-weight: bold;");
                                StackPane icon = new StackPane(glyph);
                                icon.setPrefSize(36, 36);
                                icon.setMinSize(36, 36);
                                icon.setMaxSize(36, 36);
                                icon.setStyle(
                                        "-fx-background-color: rgba(139,92,246,0.14);"
                                                + "-fx-background-radius: 10;"
                                                + "-fx-border-color: rgba(139,92,246,0.22);"
                                                + "-fx-border-radius: 10; -fx-border-width: 1;");

                                VBox text =
                                        CellStyleKit.songTextBox(
                                                song.title(),
                                                song.artist(),
                                                null,
                                                MainMenuController.this::openArtistProfile);

                                Region sp = new Region();
                                HBox.setHgrow(sp, Priority.ALWAYS);

                                Button play = new Button("\u25B6");
                                play.setStyle(
                                        "-fx-background-color: transparent; -fx-text-fill: #7c3aed;"
                                                + "-fx-font-size: 12px; -fx-font-weight: bold;");
                                play.setPrefSize(34, 34);
                                play.setFocusTraversable(false);
                                play.setOnAction(e -> player.playSingleSong(song));

                                HBox row = new HBox(12, icon, text, sp, play);
                                row.setAlignment(Pos.CENTER_LEFT);
                                row.setPadding(new Insets(6, 10, 6, 10));

                                row.setOnMouseClicked(
                                        ev -> {
                                            if (ev.getButton() == MouseButton.PRIMARY
                                                    && ev.getClickCount() == 2) {
                                                player.playSingleSong(song);
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
                                            ContextMenu cm =
                                                    SongContextMenuBuilder.build(
                                                            s,
                                                            row,
                                                            SongContextMenuBuilder.Spec.general());
                                            cm.show(row, ev.getScreenX(), ev.getScreenY());
                                            ev.consume();
                                        });

                                setText(null);
                                setGraphic(row);
                            }
                        });
    }

    private void openArtistProfile(String artist) {
        if (artist == null || artist.isBlank()) {
            return;
        }
        SessionManager.setSelectedArtist(artist.trim());
        try {
            Node n = forYouListView != null ? forYouListView : menuContent;
            SceneUtil.switchScene(n, FxmlResources.ARTIST_PROFILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyResponsiveDensity(double width) {
        boolean compact = width < 1400;
        menuContent.setSpacing(compact ? 18 : 26);
        if (homeFeedRow != null) {
            homeFeedRow.setSpacing(width < 1100 ? 16 : 20);
        }
    }

    @FXML
    private void openPlaylistsPage(ActionEvent event) throws IOException {
        SceneUtil.switchSceneNoHistory(menuContent, FxmlResources.PLAYLISTS);
    }
}

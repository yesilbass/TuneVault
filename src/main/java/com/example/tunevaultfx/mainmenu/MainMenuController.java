package com.example.tunevaultfx.mainmenu;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ListeningEventDAO;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.db.UserGenreDiscoveryDAO;
import com.example.tunevaultfx.db.UserGenreDiscoverySummary;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.playlist.PlaylistLibraryContextMenu;
import com.example.tunevaultfx.playlist.service.PlaylistService;
import com.example.tunevaultfx.recommendation.RecommendationService;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import com.example.tunevaultfx.util.UiMotionUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.scene.control.Hyperlink;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakMapChangeListener;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Home dashboard: libraries, personalized picks, top artists by listening.
 */
public class MainMenuController {

    @FXML private VBox homeHeroSection;
    @FXML private VBox homeLibrariesSection;
    @FXML private VBox forYouFeedCard;
    @FXML private VBox freshTracksFeedCard;
    @FXML private VBox menuContent;
    @FXML private HBox homeFeedRow;
    @FXML private HBox homeStatsFooter;
    @FXML private TilePane playlistChipsContainer;
    @FXML private Label homeGreetingLabel;
    @FXML private Label homeStatsLabel;
    @FXML private Label homeVibeLabel;
    @FXML private Label homeVibeSubLabel;
    @FXML private Label noPlaylistsHint;
    @FXML private ListView<Song> forYouListView;
    @FXML private ListView<String> freshTracksListView;
    @FXML private VBox homeTodaySection;
    @FXML private FlowPane homeTodayFlow;
    @FXML private VBox homeTodayTracksHost;
    @FXML private VBox homeTodayArtistsHost;
    @FXML private VBox homeChartsSection;
    @FXML private FlowPane homeChartsFlow;
    @FXML private VBox homeTopSongsHost;
    @FXML private VBox homeTopArtistsHost;

    private final RecommendationService recommendationService = new RecommendationService();
    private final PlaylistService playlistService = new PlaylistService();
    private final MusicPlayerController player = MusicPlayerController.getInstance();
    private final SongDAO songDAO = new SongDAO();
    private final ListeningEventDAO listeningEventDAO = new ListeningEventDAO();
    private final UserGenreDiscoveryDAO genreDiscoveryDAO = new UserGenreDiscoveryDAO();
    private final HomeListeningChartsService homeChartsService = new HomeListeningChartsService();

    private final MapChangeListener<String, ObservableList<Song>> homePlaylistKeysChanged =
            c -> Platform.runLater(this::refreshHomeLibraryStrip);

    private final List<HomeChartsSection.ChartPlayBinding> chartPlayBindings = new ArrayList<>();

    /** Re-applies now-playing chrome on static chart rows when {@link #player} changes. */
    private final List<Runnable> chartRowPlaybackSyncers = new ArrayList<>();

    private final HomeChartsSection.ChartUiHost chartUiHost =
            new HomeChartsSection.ChartUiHost() {
                @Override
                public MusicPlayerController player() {
                    return MainMenuController.this.player;
                }

                @Override
                public void registerChartPlayBinding(Button play, Song song) {
                    chartPlayBindings.add(new HomeChartsSection.ChartPlayBinding(play, song));
                }

                @Override
                public void registerChartRowSync(Runnable syncer) {
                    chartRowPlaybackSyncers.add(syncer);
                }

                @Override
                public void openArtistProfile(String artist) {
                    MainMenuController.this.openArtistProfile(artist);
                }

                @Override
                public void openSongProfile(Song song) {
                    MainMenuController.this.openSongProfile(song);
                }
            };

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
        if (profile != null && profile.getPlaylists() != null) {
            profile.getPlaylists().addListener(new WeakMapChangeListener<>(homePlaylistKeysChanged));
        }
        int playlistCount =
                profile != null && profile.getPlaylists() != null ? profile.getPlaylists().size() : 0;
        int songTotal = countUniqueSavedSongs(profile);
        if (homeStatsLabel != null) {
            homeStatsLabel.setText(
                    playlistCount + " playlist" + (playlistCount != 1 ? "s" : "")
                            + "  \u00B7  "
                            + songTotal + " saved song" + (songTotal != 1 ? "s" : ""));
        }

        bindPlaylistTileSizingOnce();
        buildPlaylistChips(profile);
        loadFeedLists(user);
        applyHomeVibe(user);
        bindHomeChartsFlowWrap();
        bindHomeTodayFlowWrap();
        chartPlayBindings.clear();
        chartRowPlaybackSyncers.clear();
        HomeChartsSection.loadTodayCharts(
                user, homeTodayTracksHost, homeTodayArtistsHost, homeChartsService, chartUiHost);
        HomeChartsSection.loadHomeCharts(
                user, homeTopSongsHost, homeTopArtistsHost, homeChartsService, chartUiHost);

        player.currentSongProperty()
                .addListener((o, a, b) -> Platform.runLater(this::refreshHomePlaybackAffordances));
        player.playingProperty()
                .addListener((o, a, b) -> Platform.runLater(this::refreshHomePlaybackAffordances));

        setupHomeListView(forYouListView);
        setupTopArtistsListView(freshTracksListView);
        Platform.runLater(this::refreshHomePlaybackAffordances);

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
                                                    freshTracksFeedCard,
                                                    homeTodaySection,
                                                    homeChartsSection,
                                                    homeStatsFooter));
                                    UiMotionUtil.applyHoverLift(forYouFeedCard);
                                    UiMotionUtil.applyHoverLift(freshTracksFeedCard);
                                    if (homeTodayTracksHost != null) {
                                        UiMotionUtil.applyHoverLift(homeTodayTracksHost);
                                    }
                                    if (homeTodayArtistsHost != null) {
                                        UiMotionUtil.applyHoverLift(homeTodayArtistsHost);
                                    }
                                    if (homeTopSongsHost != null) {
                                        UiMotionUtil.applyHoverLift(homeTopSongsHost);
                                    }
                                    if (homeTopArtistsHost != null) {
                                        UiMotionUtil.applyHoverLift(homeTopArtistsHost);
                                    }
                                    if (menuContent != null) {
                                        menuContent.setFocusTraversable(true);
                                        menuContent.requestFocus();
                                    }
                                }));
    }

    private void refreshHomePlaybackAffordances() {
        for (HomeChartsSection.ChartPlayBinding b : chartPlayBindings) {
            if (b.button().getScene() == null) {
                continue;
            }
            Song cur = player.getCurrentSong();
            boolean same = cur != null && cur.songId() == b.song().songId();
            b.button().setText(same && player.isPlaying() ? "⏸" : "▶");
        }
        if (forYouListView != null) {
            forYouListView.refresh();
        }
        for (Runnable r : chartRowPlaybackSyncers) {
            r.run();
        }
    }

    private void bindHomeChartsFlowWrap() {
        if (homeChartsFlow == null || menuContent == null) {
            return;
        }
        homeChartsFlow.prefWrapLengthProperty()
                .bind(
                        Bindings.createDoubleBinding(
                                () -> Math.max(340, menuContent.getWidth() - 56),
                                menuContent.widthProperty()));
    }

    private void bindHomeTodayFlowWrap() {
        if (homeTodayFlow == null || menuContent == null) {
            return;
        }
        homeTodayFlow.prefWrapLengthProperty()
                .bind(
                        Bindings.createDoubleBinding(
                                () -> Math.max(340, menuContent.getWidth() - 56),
                                menuContent.widthProperty()));
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
            homeVibeLabel.getStyleClass().setAll("caption");
            homeVibeLabel.setText("Couldn\u2019t load your genre profile. Your music still works as usual.");
            homeVibeLabel.setVisible(true);
            homeVibeLabel.setManaged(true);
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

    /** Five playlist columns; tile width tracks the home column width. */
    private void bindPlaylistTileSizingOnce() {
        if (menuContent == null || playlistChipsContainer == null) {
            return;
        }
        if (playlistChipsContainer.prefTileWidthProperty().isBound()) {
            return;
        }
        final double hgap = playlistChipsContainer.getHgap();
        playlistChipsContainer.setPrefColumns(5);
        playlistChipsContainer.prefTileWidthProperty()
                .bind(
                        Bindings.createDoubleBinding(
                                () -> {
                                    double w = menuContent.getWidth() - 48;
                                    if (w <= 0) {
                                        return 200.0;
                                    }
                                    double cols = 5;
                                    double tile = (w - (cols - 1) * hgap) / cols;
                                    return Math.min(420, Math.max(168, tile));
                                },
                                menuContent.widthProperty()));
        playlistChipsContainer.setPrefTileHeight(76);
    }

    private void buildPlaylistChips(UserProfile profile) {
        playlistChipsContainer.getChildren().clear();
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
        art.setMinSize(56, 56);
        art.setPrefSize(56, 56);
        art.setMaxSize(56, 56);
        art.getStyleClass().add("home-playlist-tile-art");

        Label title = new Label(playlistName);
        title.getStyleClass().add("home-playlist-tile-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox row = new HBox(12, art, title);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("home-playlist-tile");
        row.setPadding(new Insets(10, 14, 10, 12));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setOnMouseClicked(
                ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY) {
                        goToPlaylist(playlistName);
                        ev.consume();
                    }
                });
        row.setOnContextMenuRequested(
                ev -> {
                    UserProfile p = SessionManager.getCurrentUserProfile();
                    if (p == null || row.getScene() == null) {
                        return;
                    }
                    ContextMenu menu =
                            PlaylistLibraryContextMenu.create(
                                    row,
                                    p,
                                    playlistService,
                                    playlistName,
                                    this::refreshHomeLibraryStrip);
                    menu.show(row, ev.getScreenX(), ev.getScreenY());
                    ev.consume();
                });
        return row;
    }

    private HBox createMorePlaylistsTile(int moreCount) {
        Label glyph = new Label("+" + moreCount);
        glyph.getStyleClass().addAll("home-playlist-tile-glyph", "home-playlist-tile-glyph-more");
        StackPane art = new StackPane(glyph);
        art.setMinSize(56, 56);
        art.setPrefSize(56, 56);
        art.setMaxSize(56, 56);
        art.getStyleClass().addAll("home-playlist-tile-art", "home-playlist-tile-art-more");

        Label title = new Label("See all playlists");
        title.getStyleClass().add("home-playlist-tile-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox row = new HBox(12, art, title);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("home-playlist-tile", "home-playlist-tile-more");
        row.setPadding(new Insets(10, 14, 10, 12));
        row.setMaxWidth(Double.MAX_VALUE);
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
            SceneUtil.switchScene(menuContent, FxmlResources.PLAYLISTS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openPlaylistsPageFromHome() throws IOException {
        SceneUtil.switchScene(menuContent, FxmlResources.PLAYLISTS);
    }

    private void loadFeedLists(String username) {
        ObservableList<Song> forYou = FXCollections.observableArrayList();
        if (username != null && !username.isBlank()) {
            forYou.setAll(recommendationService.getSuggestedSongsForUser(username, 5));
        }
        forYouListView.setItems(forYou);

        List<String> topArtists = List.of();
        if (username != null && !username.isBlank()) {
            try {
                topArtists = listeningEventDAO.findTopArtistNamesByListening(username, 5);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        freshTracksListView.setItems(FXCollections.observableArrayList(topArtists));
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
                                glyph.setStyle(HomeFeedStyles.noteGlyphStyle());
                                StackPane icon = new StackPane(glyph);
                                icon.setPrefSize(36, 36);
                                icon.setMinSize(36, 36);
                                icon.setMaxSize(36, 36);
                                icon.setStyle(HomeFeedStyles.noteIconBoxStyle());

                                Song curSong = player.getCurrentSong();
                                boolean current = curSong != null && curSong.songId() == song.songId();

                                Region nowPlayingBar = CellStyleKit.nowPlayingEdgeBar();
                                nowPlayingBar.setVisible(current);
                                nowPlayingBar.setManaged(current);

                                VBox text =
                                        CellStyleKit.songTextBox(
                                                song.title(),
                                                song.artist(),
                                                null,
                                                MainMenuController.this::openArtistProfile,
                                                () -> MainMenuController.this.openSongProfile(song));
                                if (current && !text.getChildren().isEmpty()) {
                                    Node head = text.getChildren().get(0);
                                    String playingTitleStyle =
                                            "-fx-font-size: 14px; -fx-font-weight: bold;-fx-text-fill: "
                                                    + CellStyleKit.getAccentTitle()
                                                    + ";-fx-border-width: 0;"
                                                    + "-fx-background-color: transparent;";
                                    if (head instanceof Label lab) {
                                        lab.setStyle(playingTitleStyle);
                                    } else if (head instanceof Hyperlink h) {
                                        h.setStyle(playingTitleStyle);
                                    }
                                }

                                Region sp = new Region();
                                HBox.setHgrow(sp, Priority.ALWAYS);

                                Button play = new Button("\u25B6");
                                play.setStyle(HomeFeedStyles.playButtonStyle());
                                play.setPrefSize(34, 34);
                                play.setFocusTraversable(false);
                                play.setText(current && player.isPlaying() ? "⏸" : "▶");
                                play.setOnAction(e -> player.playSingleSong(song));

                                HBox row = new HBox(12, nowPlayingBar, icon, text, sp, play);
                                row.setAlignment(Pos.CENTER_LEFT);
                                row.setPadding(new Insets(6, 10, 6, 10));
                                CellStyleKit.markPlaying(row, current);

                                row.setOnMouseClicked(
                                        ev -> {
                                            if (ev.getButton() == MouseButton.PRIMARY
                                                    && ev.getClickCount() == 2
                                                    && !CellStyleKit.isInteractiveRowChromeTarget(
                                                            ev.getTarget())) {
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

    private void setupTopArtistsListView(ListView<String> listView) {
        listView.setFixedCellSize(58);
        Label ph = new Label("Listen to music to see your top artists here.");
        ph.setStyle("-fx-text-fill: " + CellStyleKit.getTextMuted() + "; -fx-font-size: 13px;");
        listView.setPlaceholder(ph);

        listView.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(String artist, boolean empty) {
                                super.updateItem(artist, empty);
                                if (empty || artist == null || artist.isBlank()) {
                                    setText(null);
                                    setGraphic(null);
                                    return;
                                }

                                String initial =
                                        artist.isEmpty() ? "?" : artist.substring(0, 1).toUpperCase();
                                StackPane icon =
                                        CellStyleKit.iconBox(
                                                initial, CellStyleKit.Palette.ROSE, true);
                                VBox text = new VBox(3);
                                Hyperlink nameLink =
                                        CellStyleKit.primaryTitleLink(
                                                artist,
                                                () ->
                                                        MainMenuController.this.openArtistProfile(
                                                                artist));
                                text.getChildren().addAll(nameLink, CellStyleKit.secondary("Artist"));
                                HBox.setHgrow(text, Priority.ALWAYS);
                                Region sp = new Region();
                                HBox.setHgrow(sp, Priority.ALWAYS);

                                HBox row = new HBox(12, icon, text, sp);
                                row.setAlignment(Pos.CENTER_LEFT);
                                row.setPadding(new Insets(6, 10, 6, 10));
                                row.setStyle(CellStyleKit.getRowDefault());
                                CellStyleKit.addHover(row);

                                row.setOnMouseClicked(
                                        ev -> {
                                            if (ev.getButton() == MouseButton.PRIMARY) {
                                                openArtistProfile(artist);
                                                ev.consume();
                                            }
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

    private void openSongProfile(Song song) {
        if (song == null) {
            return;
        }
        SessionManager.setSelectedSong(song);
        try {
            Node n = forYouListView != null ? forYouListView : menuContent;
            SceneUtil.switchScene(n, FxmlResources.SONG_PROFILE);
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

}

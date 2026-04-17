package com.example.tunevaultfx.mainmenu;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.util.CellStyleKit;
import com.example.tunevaultfx.util.SongContextMenuBuilder;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.sql.SQLException;
import java.util.List;

/** Builds “Today” and “All-time” listening chart cards on the home screen. */
public final class HomeChartsSection {

    public record ChartPlayBinding(Button button, Song song) {}

    public interface ChartUiHost {
        MusicPlayerController player();

        void registerChartPlayBinding(Button play, Song song);

        void registerChartRowSync(Runnable syncer);

        void openArtistProfile(String artist);
    }

    private HomeChartsSection() {}

    public static void loadTodayCharts(
            String username,
            VBox homeTodayTracksHost,
            VBox homeTodayArtistsHost,
            HomeListeningChartsService homeChartsService,
            ChartUiHost ui) {
        if (homeTodayTracksHost == null || homeTodayArtistsHost == null) {
            return;
        }
        homeTodayTracksHost.getChildren().clear();
        homeTodayArtistsHost.getChildren().clear();

        if (username == null || username.isBlank()) {
            Label signInTracks = new Label("Sign in to see what you\u2019ve played today.");
            signInTracks.getStyleClass().add("home-charts-column-empty");
            signInTracks.setWrapText(true);
            homeTodayTracksHost
                    .getChildren()
                    .addAll(
                            buildChartCardHeader("Today\u2019s top tracks", "Listening time from today only"),
                            new Separator(),
                            signInTracks);
            Label signInArtists = new Label("Sign in to see which artists you\u2019ve played today.");
            signInArtists.getStyleClass().add("home-charts-column-empty");
            signInArtists.setWrapText(true);
            homeTodayArtistsHost
                    .getChildren()
                    .addAll(
                            buildChartCardHeader("Today\u2019s top artists", "Listening time from today only"),
                            new Separator(),
                            signInArtists);
            return;
        }

        try {
            List<HomeListeningChartsService.ChartSongEntry> songs =
                    homeChartsService.loadTopSongsToday(username, 5);
            List<HomeListeningChartsService.ChartArtistEntry> artists =
                    homeChartsService.loadTopArtistsToday(username, 5);

            if (songs.isEmpty()) {
                Label empty = new Label("Nothing logged today yet \u2014 press play and check back later.");
                empty.getStyleClass().add("home-charts-column-empty");
                empty.setWrapText(true);
                homeTodayTracksHost
                        .getChildren()
                        .addAll(
                                buildChartCardHeader("Today\u2019s top tracks", "Listening time from today only"),
                                new Separator(),
                                empty);
            } else {
                populateSongsChartCard(
                        homeTodayTracksHost,
                        songs,
                        "Today\u2019s top tracks",
                        "Listening time from today only",
                        ui);
            }

            if (artists.isEmpty()) {
                Label empty = new Label("No artist time today yet \u2014 start a playlist.");
                empty.getStyleClass().add("home-charts-column-empty");
                empty.setWrapText(true);
                homeTodayArtistsHost
                        .getChildren()
                        .addAll(
                                buildChartCardHeader("Today\u2019s top artists", "Listening time from today only"),
                                new Separator(),
                                empty);
            } else {
                populateArtistsChartCard(
                        homeTodayArtistsHost,
                        artists,
                        "Today\u2019s top artists",
                        "Listening time from today only",
                        ui);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Couldn\u2019t load today\u2019s charts.");
            err.getStyleClass().add("home-charts-column-empty");
            err.setWrapText(true);
            homeTodayTracksHost
                    .getChildren()
                    .addAll(buildChartCardHeader("Today\u2019s top tracks", ""), new Separator(), err);
            Label errArtists = new Label(err.getText());
            errArtists.getStyleClass().add("home-charts-column-empty");
            errArtists.setWrapText(true);
            homeTodayArtistsHost
                    .getChildren()
                    .addAll(buildChartCardHeader("Today\u2019s top artists", ""), new Separator(), errArtists);
        }
    }

    public static void loadHomeCharts(
            String username,
            VBox homeTopSongsHost,
            VBox homeTopArtistsHost,
            HomeListeningChartsService homeChartsService,
            ChartUiHost ui) {
        if (homeTopSongsHost == null || homeTopArtistsHost == null) {
            return;
        }
        homeTopSongsHost.getChildren().clear();
        homeTopArtistsHost.getChildren().clear();
        homeTopArtistsHost.setVisible(true);
        homeTopArtistsHost.setManaged(true);

        if (username == null || username.isBlank()) {
            showHomeChartsPlaceholder(
                    homeTopSongsHost, homeTopArtistsHost, "Sign in to see charts built from your listening time.");
            return;
        }

        try {
            List<HomeListeningChartsService.ChartSongEntry> songs =
                    homeChartsService.loadTopSongs(username, 5);
            List<HomeListeningChartsService.ChartArtistEntry> artists =
                    homeChartsService.loadTopArtists(username, 5);

            if (songs.isEmpty() && artists.isEmpty()) {
                showHomeChartsPlaceholder(
                        homeTopSongsHost,
                        homeTopArtistsHost,
                        "Your charts will appear here after you listen — we rank tracks and artists by the time you spend with them.");
                return;
            }

            if (!songs.isEmpty()) {
                populateSongsChartCard(
                        homeTopSongsHost, songs, "Top tracks", "By total listening time", ui);
            } else {
                Label empty = new Label("No track data yet — press play on something you love.");
                empty.getStyleClass().add("home-charts-column-empty");
                empty.setWrapText(true);
                homeTopSongsHost
                        .getChildren()
                        .addAll(buildChartCardHeader("Top tracks", "By total listening time"), new Separator(), empty);
            }

            if (!artists.isEmpty()) {
                populateArtistsChartCard(
                        homeTopArtistsHost, artists, "Top artists", "By total listening time", ui);
            } else {
                Label empty = new Label("Artist rankings will show up once you have listening history.");
                empty.getStyleClass().add("home-charts-column-empty");
                empty.setWrapText(true);
                homeTopArtistsHost
                        .getChildren()
                        .addAll(
                                buildChartCardHeader("Top artists", "By total listening time"),
                                new Separator(),
                                empty);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showHomeChartsPlaceholder(
                    homeTopSongsHost, homeTopArtistsHost, "Couldn\u2019t load charts right now. Everything else still works.");
        }
    }

    private static void showHomeChartsPlaceholder(
            VBox homeTopSongsHost, VBox homeTopArtistsHost, String message) {
        homeTopArtistsHost.setVisible(false);
        homeTopArtistsHost.setManaged(false);
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        Label glyph = new Label("\u266B");
        glyph.getStyleClass().add("home-charts-empty-glyph");
        Label headline = new Label("Charts unlock with listening");
        headline.getStyleClass().add("home-charts-empty-title");
        Label body = new Label(message);
        body.getStyleClass().add("home-charts-empty-body");
        body.setWrapText(true);
        box.getChildren().addAll(glyph, headline, body);
        homeTopSongsHost.getChildren().add(box);
    }

    private static VBox buildChartCardHeader(String title, String subtitle) {
        VBox v = new VBox(4);
        Label t = new Label(title);
        t.getStyleClass().add("home-charts-card-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("home-charts-card-subtitle");
        s.setWrapText(true);
        v.getChildren().addAll(t, s);
        return v;
    }

    private static void populateSongsChartCard(
            VBox host,
            List<HomeListeningChartsService.ChartSongEntry> entries,
            String title,
            String subtitle,
            ChartUiHost ui) {
        int maxSec =
                entries.stream().mapToInt(HomeListeningChartsService.ChartSongEntry::listenedSeconds).max().orElse(1);
        host.getChildren().add(buildChartCardHeader(title, subtitle));
        host.getChildren().add(new Separator());
        VBox rows = new VBox(12);
        int rank = 1;
        for (HomeListeningChartsService.ChartSongEntry e : entries) {
            rows.getChildren().add(createSongChartRow(rank++, e, maxSec, ui));
        }
        host.getChildren().add(rows);
    }

    private static void populateArtistsChartCard(
            VBox host,
            List<HomeListeningChartsService.ChartArtistEntry> entries,
            String title,
            String subtitle,
            ChartUiHost ui) {
        int maxSec =
                entries.stream().mapToInt(HomeListeningChartsService.ChartArtistEntry::listenedSeconds).max().orElse(1);
        host.getChildren().add(buildChartCardHeader(title, subtitle));
        host.getChildren().add(new Separator());
        VBox rows = new VBox(12);
        int rank = 1;
        for (HomeListeningChartsService.ChartArtistEntry e : entries) {
            rows.getChildren().add(createArtistChartRow(rank++, e, maxSec, ui));
        }
        host.getChildren().add(rows);
    }

    private static HBox createSongChartRow(
            int rank, HomeListeningChartsService.ChartSongEntry entry, int maxSeconds, ChartUiHost ui) {
        Song song = entry.song();
        int sec = entry.listenedSeconds();
        MusicPlayerController player = ui.player();

        StackPane rankBadge = new StackPane();
        rankBadge.setMinSize(40, 40);
        rankBadge.setPrefSize(40, 40);
        rankBadge.setMaxSize(40, 40);
        rankBadge.getStyleClass().addAll("home-charts-rank", "home-charts-rank--" + rank);
        Label rankLbl = new Label(Integer.toString(rank));
        rankLbl.getStyleClass().add("home-charts-rank-label");
        rankBadge.getChildren().add(rankLbl);

        Label title = new Label(song.title());
        title.getStyleClass().add("home-charts-row-title");
        title.setWrapText(true);

        Region edgeBar = CellStyleKit.nowPlayingEdgeBar();

        Hyperlink artistLink = new Hyperlink(song.artist());
        artistLink.getStyleClass().add("home-charts-row-meta-link");
        artistLink.setOnAction(ev -> ui.openArtistProfile(song.artist()));

        VBox textCol = new VBox(2, title, artistLink);
        textCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label timeLbl = new Label(formatListenTime(sec));
        timeLbl.getStyleClass().add("home-charts-row-time");

        HBox topLine = new HBox(12, textCol, timeLbl);
        topLine.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar((double) sec / Math.max(1, maxSeconds));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(5);
        bar.getStyleClass().add("home-charts-progress");

        VBox rightCol = new VBox(8, topLine, bar);
        rightCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        Button play = new Button("\u25B6");
        play.setStyle(HomeFeedStyles.playButtonStyle());
        play.setPrefSize(36, 36);
        play.setMinSize(36, 36);
        play.setFocusTraversable(false);
        play.setOnAction(ev -> player.playSingleSong(song));
        ui.registerChartPlayBinding(play, song);

        HBox row = new HBox(14, edgeBar, rankBadge, rightCol, play);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 10));
        row.getStyleClass().add("home-charts-row");

        Runnable syncChartRowPlayingChrome =
                () -> {
                    if (row.getScene() == null) {
                        return;
                    }
                    Song cur = player.getCurrentSong();
                    boolean isCur = cur != null && cur.songId() == song.songId();
                    edgeBar.setVisible(isCur);
                    edgeBar.setManaged(isCur);
                    CellStyleKit.markPlaying(row, isCur);
                    if (isCur) {
                        title.setStyle("-fx-text-fill: " + CellStyleKit.getAccentTitle() + ";");
                    } else {
                        title.setStyle(null);
                    }
                };
        ui.registerChartRowSync(syncChartRowPlayingChrome);
        syncChartRowPlayingChrome.run();

        row.setOnMouseClicked(
                ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() == 2) {
                        player.playSingleSong(song);
                        ev.consume();
                    }
                });
        row.addEventFilter(
                ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                ev -> {
                    ContextMenu cm =
                            SongContextMenuBuilder.build(song, row, SongContextMenuBuilder.Spec.general());
                    cm.show(row, ev.getScreenX(), ev.getScreenY());
                    ev.consume();
                });

        return row;
    }

    private static HBox createArtistChartRow(
            int rank, HomeListeningChartsService.ChartArtistEntry entry, int maxSeconds, ChartUiHost ui) {
        String name = entry.artistName();
        int sec = entry.listenedSeconds();

        StackPane rankBadge = new StackPane();
        rankBadge.setMinSize(40, 40);
        rankBadge.setPrefSize(40, 40);
        rankBadge.setMaxSize(40, 40);
        rankBadge.getStyleClass().addAll("home-charts-rank", "home-charts-rank-artist--" + rank);
        Label rankLbl = new Label(Integer.toString(rank));
        rankLbl.getStyleClass().add("home-charts-rank-label");
        rankBadge.getChildren().add(rankLbl);

        Label title = new Label(name);
        title.getStyleClass().add("home-charts-row-title");
        title.setWrapText(true);

        Label meta = new Label("Artist");
        meta.getStyleClass().add("home-charts-row-meta");

        VBox textCol = new VBox(2, title, meta);
        textCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label timeLbl = new Label(formatListenTime(sec));
        timeLbl.getStyleClass().add("home-charts-row-time");

        HBox topLine = new HBox(12, textCol, timeLbl);
        topLine.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar((double) sec / Math.max(1, maxSeconds));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(5);
        bar.getStyleClass().add("home-charts-progress home-charts-progress-artist");

        VBox rightCol = new VBox(8, topLine, bar);
        rightCol.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        HBox row = new HBox(14, rankBadge, rightCol);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 10));
        row.getStyleClass().add("home-charts-row");
        CellStyleKit.addHover(row);

        row.setOnMouseClicked(
                ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY) {
                        ui.openArtistProfile(name);
                        ev.consume();
                    }
                });

        return row;
    }

    static String formatListenTime(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 min";
        }
        if (totalSeconds < 60) {
            return totalSeconds + " sec";
        }
        int minutes = totalSeconds / 60;
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " min" : " min");
        }
        int hours = minutes / 60;
        int m = minutes % 60;
        return hours + "h " + m + "m";
    }
}

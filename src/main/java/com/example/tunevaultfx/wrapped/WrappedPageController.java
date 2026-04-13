package com.example.tunevaultfx.wrapped;

import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

import java.io.IOException;

/**
 * Controls the Wrapped page and displays the user's listening summary.
 */
public class WrappedPageController {

    @FXML
    private Label topSongLabel;

    @FXML
    private Label topSongArtistLabel;

    @FXML
    private Label topArtistLabel;

    @FXML
    private Label topArtistTopSongLabel;

    @FXML
    private Label favoriteGenreLabel;

    @FXML
    private Label totalMinutesLabel;

    @FXML
    private Label summaryLabel;

    @FXML
    private ProgressBar songBar;

    @FXML
    private ProgressBar artistBar;

    @FXML
    private ProgressBar genreBar;

    @FXML
    private Label rangeTitleLabel;

    @FXML
    private Button todayButton;

    @FXML
    private Button overallButton;
    @FXML private VBox wrappedContent;
    @FXML private GridPane statsGrid;
    @FXML private VBox topSongCard;
    @FXML private VBox topArtistCard;
    @FXML private VBox genreCard;
    @FXML private VBox timeCard;
    @FXML private VBox summaryCard;

    private final WrappedStatsService wrappedStatsService = new WrappedStatsService();
    private StatsRange currentRange = StatsRange.DAILY;

    @FXML
    public void initialize() {
        loadStats(currentRange);
        updateRangeButtons();

        Platform.runLater(() -> {
            UiMotionUtil.playStaggeredEntrance(List.of(topSongCard, topArtistCard, genreCard, timeCard, summaryCard));
            UiMotionUtil.applyHoverLift(topSongCard);
            UiMotionUtil.applyHoverLift(topArtistCard);
            UiMotionUtil.applyHoverLift(genreCard);
            UiMotionUtil.applyHoverLift(timeCard);
            UiMotionUtil.applyHoverLift(summaryCard);
        });

        wrappedContent.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) return;
            applyResponsiveDensity(newScene.getWidth());
            newScene.widthProperty().addListener((o, oldW, newW) -> applyResponsiveDensity(newW.doubleValue()));
        });
    }

    private void applyResponsiveDensity(double width) {
        boolean compact = width < 1400;
        statsGrid.setHgap(compact ? 12 : 16);
        statsGrid.setVgap(compact ? 12 : 16);
        wrappedContent.setSpacing(compact ? 14 : 18);
    }

    @FXML
    private void handleShowToday() {
        currentRange = StatsRange.DAILY;
        loadStats(currentRange);
        updateRangeButtons();
    }

    @FXML
    private void handleShowOverall() {
        currentRange = StatsRange.OVERALL;
        loadStats(currentRange);
        updateRangeButtons();
    }

    @FXML
    private void handleBackToMenu(ActionEvent event) throws IOException {
        SceneUtil.goBack((Node) event.getSource());
    }

    private void loadStats(StatsRange range) {
        String username = SessionManager.getCurrentUsername();
        WrappedStats stats = wrappedStatsService.loadStatsForUsername(username, range);

        topSongLabel.setText(stats.getTopSong());
        topArtistLabel.setText(stats.getTopArtist());
        favoriteGenreLabel.setText(stats.getFavoriteGenre());
        totalMinutesLabel.setText(formatDuration(stats.getTotalListeningSeconds()));
        summaryLabel.setText(stats.getSummary());

        if (topSongArtistLabel != null) {
            String topSongArtistText = buildTopSongArtistText(stats);
            topSongArtistLabel.setText(topSongArtistText);
            topSongArtistLabel.setVisible(!topSongArtistText.isBlank());
            topSongArtistLabel.setManaged(!topSongArtistText.isBlank());
        }

        if (topArtistTopSongLabel != null) {
            String topArtistTopSongText = buildTopArtistTopSongText(stats);
            topArtistTopSongLabel.setText(topArtistTopSongText);
            topArtistTopSongLabel.setVisible(!topArtistTopSongText.isBlank());
            topArtistTopSongLabel.setManaged(!topArtistTopSongText.isBlank());
        }

        songBar.setProgress(progressFromSeconds(stats.getTopSongSeconds(), stats.getTotalListeningSeconds()));
        artistBar.setProgress(progressFromSeconds(stats.getTopArtistSeconds(), stats.getTotalListeningSeconds()));
        genreBar.setProgress(progressFromSeconds(stats.getFavoriteGenreSeconds(), stats.getTotalListeningSeconds()));

        if (rangeTitleLabel != null) {
            rangeTitleLabel.setText(range == StatsRange.DAILY ? "Today" : "Overall");
        }
    }

    private String buildTopSongArtistText(WrappedStats stats) {
        String artistName = stats.getTopSongArtist();
        if (artistName == null
                || artistName.isBlank()
                || artistName.equalsIgnoreCase("No listening data today")
                || artistName.equalsIgnoreCase("No listening data yet")) {
            return "";
        }
        return artistName;
    }

    private String buildTopArtistTopSongText(WrappedStats stats) {
        String songTitle = stats.getTopArtistTopSong();
        if (songTitle == null
                || songTitle.isBlank()
                || songTitle.equalsIgnoreCase("No listening data today")
                || songTitle.equalsIgnoreCase("No listening data yet")) {
            return "";
        }
        return "Top track: " + songTitle;
    }

    private void updateRangeButtons() {
        if (todayButton != null) {
            todayButton.getStyleClass().removeAll("btn-toggle-active", "btn-toggle-inactive");
            todayButton.getStyleClass().add(currentRange == StatsRange.DAILY
                    ? "btn-toggle-active" : "btn-toggle-inactive");
            todayButton.setDisable(currentRange == StatsRange.DAILY);
        }

        if (overallButton != null) {
            overallButton.getStyleClass().removeAll("btn-toggle-active", "btn-toggle-inactive");
            overallButton.getStyleClass().add(currentRange == StatsRange.OVERALL
                    ? "btn-toggle-active" : "btn-toggle-inactive");
            overallButton.setDisable(currentRange == StatsRange.OVERALL);
        }
    }

    private double progressFromSeconds(int valueSeconds, int totalSeconds) {
        if (valueSeconds <= 0 || totalSeconds <= 0) {
            return 0;
        }

        return Math.min(1.0, (double) valueSeconds / totalSeconds);
    }

    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + ":" + String.format("%02d:%02d", minutes, seconds);
        }
        return minutes + ":" + String.format("%02d", seconds);
    }
}
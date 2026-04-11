package com.example.tunevaultfx.wrapped;

import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.io.IOException;

/**
 * Controls the Wrapped page and displays the user's listening summary.
 */
public class WrappedPageController {

    @FXML
    private Label topSongLabel;

    @FXML
    private Label topArtistLabel;

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

    private final WrappedStatsService wrappedStatsService = new WrappedStatsService();
    private StatsRange currentRange = StatsRange.DAILY;

    @FXML
    public void initialize() {
        loadStats(currentRange);
        updateRangeButtons();
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
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    private void loadStats(StatsRange range) {
        String username = SessionManager.getCurrentUsername();
        WrappedStats stats = wrappedStatsService.loadStatsForUsername(username, range);

        topSongLabel.setText(stats.getTopSong());
        topArtistLabel.setText(stats.getTopArtist());
        favoriteGenreLabel.setText(stats.getFavoriteGenre());
        totalMinutesLabel.setText(formatDuration(stats.getTotalListeningSeconds()));
        summaryLabel.setText(stats.getSummary());

        songBar.setProgress(progressFromSeconds(stats.getTopSongSeconds(), stats.getTotalListeningSeconds()));
        artistBar.setProgress(progressFromSeconds(stats.getTopArtistSeconds(), stats.getTotalListeningSeconds()));
        genreBar.setProgress(progressFromSeconds(stats.getFavoriteGenreSeconds(), stats.getTotalListeningSeconds()));

        if (rangeTitleLabel != null) {
            rangeTitleLabel.setText(range == StatsRange.DAILY ? "Today" : "Overall");
        }
    }

    private void updateRangeButtons() {
        if (todayButton != null) {
            todayButton.setDisable(currentRange == StatsRange.DAILY);
        }

        if (overallButton != null) {
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
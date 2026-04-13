package com.example.tunevaultfx.mainmenu;

import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.List;

import java.io.IOException;

/**
 * Controls the main menu screen.
 * Handles navigation from the home screen to the app's main features.
 */
public class MainMenuController {

    @FXML
    private Label welcomeLabel;
    @FXML private VBox menuContent;
    @FXML private GridPane featureGrid;
    @FXML private VBox searchCard;
    @FXML private VBox playlistsCard;
    @FXML private VBox wrappedCard;
    @FXML private VBox genreCard;

    @FXML
    private void openSearchPage(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "search-page.fxml");
    }

    @FXML
    public void initialize() {
        String username = SessionManager.getCurrentUsername();
        welcomeLabel.setText(username != null ? "Welcome, " + username : "Welcome");

        Platform.runLater(() -> {
            UiMotionUtil.playStaggeredEntrance(List.of(searchCard, playlistsCard, wrappedCard, genreCard));
            UiMotionUtil.applyHoverLift(searchCard);
            UiMotionUtil.applyHoverLift(playlistsCard);
            UiMotionUtil.applyHoverLift(wrappedCard);
            UiMotionUtil.applyHoverLift(genreCard);
        });

        if (menuContent != null && menuContent.sceneProperty() != null) {
            menuContent.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) return;
                applyResponsiveDensity(newScene.getWidth());
                newScene.widthProperty().addListener((o, oldW, newW) -> applyResponsiveDensity(newW.doubleValue()));
            });
        }
    }

    private void applyResponsiveDensity(double width) {
        boolean compact = width < 1400;
        featureGrid.setHgap(compact ? 14 : 18);
        featureGrid.setVgap(compact ? 14 : 18);
        menuContent.setSpacing(compact ? 16 : 22);

        if (featureGrid.getColumnConstraints().size() >= 2) {
            double cardWidth = compact ? 500 : 576;
            for (ColumnConstraints cc : featureGrid.getColumnConstraints()) {
                cc.setPrefWidth(cardWidth);
            }
        }
    }

    @FXML
    private void openPlaylistsPage(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "playlists-page.fxml");
    }

    @FXML
    private void openNowPlayingPage(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "nowplaying-page.fxml");
    }

    @FXML
    private void openWrappedPage(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "wrapped-page.fxml");
    }

    @FXML
    private void openFindYourGenrePage(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "findyourgenre-page.fxml");
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        SessionManager.logout();
        SceneUtil.clearHistory();
        SceneUtil.switchSceneNoHistory((Node) event.getSource(), "login-page.fxml");
    }
}
package com.example.tunevaultfx.controllers;

import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;

import java.io.IOException;

/**
 * Controls the main menu screen.
 * Handles navigation from the home screen to the app's main features.
 */
public class MainMenuController {

    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize() {
        String username = SessionManager.getCurrentUsername();
        welcomeLabel.setText(username != null ? "Welcome, " + username : "Welcome");
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
        SceneUtil.switchScene((Node) event.getSource(), "login-page.fxml");
    }
}
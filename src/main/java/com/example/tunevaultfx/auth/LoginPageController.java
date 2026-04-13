package com.example.tunevaultfx.auth;

import com.example.tunevaultfx.db.UserDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.User;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controls the login page.
 * Supports login with either username or email.
 */
public class LoginPageController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        Platform.runLater(() -> usernameField.requestFocus());

        usernameField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null || newScene.getProperties().containsKey("loginHandlersInstalled")) {
                return;
            }

            newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    try {
                        handleLogin(new ActionEvent(usernameField, usernameField));
                    } catch (IOException e) {
                        showError("Unable to open the next page.");
                    }
                    event.consume();
                } else if (event.getCode() == KeyCode.ESCAPE) {
                    usernameField.clear();
                    passwordField.clear();
                    clearStatus();
                    usernameField.requestFocus();
                    event.consume();
                }
            });

            newScene.getProperties().put("loginHandlersInstalled", true);
        });
    }

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        clearStatus();

        String loginInput = safeTrim(usernameField.getText());
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (loginInput.isBlank() || password.isBlank()) {
            showError("Please enter your username/email and password.");
            return;
        }

        try {
            User user = userDAO.authenticateUser(loginInput, password);

            if (user == null) {
                showError("Invalid credentials.");
                return;
            }

            MusicPlayerController.getInstance().resetForNewSession();
            SessionManager.startSession(user.getUsername());
            SceneUtil.clearHistory();
            SceneUtil.switchSceneNoHistory((Node) event.getSource(), "main-menu.fxml");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void openCreateAccountPage(ActionEvent event) throws IOException {
        SceneUtil.switchSceneNoHistory((Node) event.getSource(), "create-account-page.fxml");
    }

    @FXML
    private void openForgotPasswordPage(ActionEvent event) throws IOException {
        SceneUtil.switchSceneNoHistory((Node) event.getSource(), "forgot-password-page.fxml");
    }

    @FXML
    private void handleCreateAccountPage(ActionEvent event) throws IOException {
        openCreateAccountPage(event);
    }

    @FXML
    private void handleForgotPasswordPage(ActionEvent event) throws IOException {
        openForgotPasswordPage(event);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void clearStatus() {
        statusLabel.setText("");
        statusLabel.getStyleClass().clear();
    }

    private void showError(String message) {
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setText(message);
    }
}
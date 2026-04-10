package com.example.tunevaultfx.controllers.auth;

import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.User;
import com.example.tunevaultfx.user.UserStore;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Controls the login page.
 * Reads login input, validates the user, and opens the next screen
 * when login is successful.
 */
public class LoginPageController {

    public Button loginButton;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        User user = UserStore.validateLogin(username, password);

        if (user == null) {
            statusLabel.setText("Invalid username or password.");
            return;
        }

        SessionManager.startSession(user.getUsername());
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    @FXML
    private void openCreateAccountPage(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "create-account-page.fxml");
    }

    @FXML
    private void openForgotPasswordPage(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "forgot-password-page.fxml");
    }

    @FXML
    private void handleCreateAccountPage(ActionEvent event) throws IOException {
        openCreateAccountPage(event);
    }

    @FXML
    private void handleForgotPasswordPage(ActionEvent event) throws IOException {
        openForgotPasswordPage(event);
    }
}
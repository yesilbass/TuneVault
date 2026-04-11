package com.example.tunevaultfx.controllers.auth;

import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.User;
import com.example.tunevaultfx.db.UserDAO;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controls the login page.
 * Reads login input, validates the user through the database,
 * and opens the main menu when login is successful.
 */
public class LoginPageController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        try {
            User user = userDAO.authenticateUser(username, password);

            if (user == null) {
                statusLabel.setText("Invalid username or password.");
                return;
            }

            SessionManager.startSession(user.getUsername());
            SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");

        } catch (SQLException e) {
            statusLabel.setText("Database error. Please try again.");
            e.printStackTrace();
        }
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
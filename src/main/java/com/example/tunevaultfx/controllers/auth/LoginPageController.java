package com.example.tunevaultfx.controllers.auth;

import com.example.tunevaultfx.db.UserDAO;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.User;
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
 * Supports login with either username or email.
 */
public class LoginPageController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

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

            SessionManager.startSession(user.getUsername());
            SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("A database error occurred. Please try again.");
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void clearStatus() {
        statusLabel.setText("");
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        statusLabel.setText(message);
    }
}
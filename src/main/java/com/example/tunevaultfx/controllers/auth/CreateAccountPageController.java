package com.example.tunevaultfx.controllers.auth;

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
 * Controls the create account page.
 * Reads account input, validates it, and creates a new user
 * through the database.
 */
public class CreateAccountPageController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleCreateAccount(ActionEvent event) throws IOException {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            statusLabel.setText("Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        try {
            if (userDAO.usernameExists(username)) {
                statusLabel.setText("Username already exists.");
                return;
            }

            if (userDAO.emailExists(email)) {
                statusLabel.setText("Email already exists.");
                return;
            }

            boolean created = userDAO.createUser(username, email, password);

            if (!created) {
                statusLabel.setText("Could not create account.");
                return;
            }

            SceneUtil.switchScene((Node) event.getSource(), "login-page.fxml");

        } catch (SQLException e) {
            statusLabel.setText("Database error. Please try again.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "login-page.fxml");
    }
}
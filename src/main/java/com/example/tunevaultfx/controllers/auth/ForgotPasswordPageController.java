package com.example.tunevaultfx.controllers.auth;

import com.example.tunevaultfx.user.UserDAO;
import com.example.tunevaultfx.util.SceneUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controls the forgot password page.
 * Checks whether the entered email exists in the database
 * and displays a recovery-style message.
 */
public class ForgotPasswordPageController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleRecoverPassword(ActionEvent event) {
        String email = emailField.getText().trim();

        if (email.isBlank()) {
            statusLabel.setText("Please enter your email.");
            return;
        }

        try {
            boolean exists = userDAO.emailRegistered(email);

            if (exists) {
                statusLabel.setText("Email found. Password recovery instructions would be sent here.");
            } else {
                statusLabel.setText("No account found with that email.");
            }
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
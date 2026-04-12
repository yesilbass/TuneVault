package com.example.tunevaultfx.auth;

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
import java.util.regex.Pattern;

/**
 * Controls the forgot password page.
 * Resets a user's password by email.
 */
public class ForgotPasswordPageController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    private void handleSubmit(ActionEvent event) {
        clearStatus();

        String email = safeTrim(emailField.getText()).toLowerCase();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        String validationError = validateResetInput(email, newPassword, confirmPassword);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            if (!userDAO.emailExists(email)) {
                showError("No account was found for that email.");
                return;
            }

            boolean updated = userDAO.updatePasswordByEmail(email, newPassword);
            if (!updated) {
                showError("Password could not be updated. Please try again.");
                return;
            }

            showSuccess("Password updated successfully. You can now log in.");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "login-page.fxml");
    }

    private String validateResetInput(String email, String newPassword, String confirmPassword) {
        if (email.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            return "Please fill in all fields.";
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }

        if (newPassword.length() < 8) {
            return "Password must be at least 8 characters long.";
        }

        if (!containsUppercase(newPassword) || !containsLowercase(newPassword) || !containsDigit(newPassword)) {
            return "Password must include uppercase, lowercase, and a number.";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "Passwords do not match.";
        }

        return null;
    }

    private boolean containsUppercase(String value) {
        return value.chars().anyMatch(Character::isUpperCase);
    }

    private boolean containsLowercase(String value) {
        return value.chars().anyMatch(Character::isLowerCase);
    }

    private boolean containsDigit(String value) {
        return value.chars().anyMatch(Character::isDigit);
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

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px;");
        statusLabel.setText(message);
    }
}
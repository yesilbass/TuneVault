package com.example.tunevaultfx.auth;

import com.example.tunevaultfx.db.UserDAO;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
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
    public void initialize() {
        Platform.runLater(() -> emailField.requestFocus());

        emailField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null || newScene.getProperties().containsKey("forgotPasswordHandlersInstalled")) {
                return;
            }

            newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleSubmit(new ActionEvent(emailField, emailField));
                    event.consume();
                } else if (event.getCode() == KeyCode.ESCAPE) {
                    emailField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    clearStatus();
                    emailField.requestFocus();
                    event.consume();
                }
            });

            newScene.getProperties().put("forgotPasswordHandlersInstalled", true);
        });
    }

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
        SceneUtil.switchSceneNoHistory((Node) event.getSource(), FxmlResources.AUTH_LOGIN);
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
        statusLabel.getStyleClass().clear();
    }

    private void showError(String message) {
        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setText(message);
    }

    private void showSuccess(String message) {
        statusLabel.getStyleClass().setAll("status-success");
        statusLabel.setText(message);
    }
}
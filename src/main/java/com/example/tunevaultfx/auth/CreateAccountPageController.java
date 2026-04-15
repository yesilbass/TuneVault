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
 * Controls the create account page.
 * Validates account input and creates a user in the database.
 */
public class CreateAccountPageController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        Platform.runLater(() -> usernameField.requestFocus());

        usernameField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null || newScene.getProperties().containsKey("createAccountHandlersInstalled")) {
                return;
            }

            newScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    try {
                        handleCreateAccount(new ActionEvent(usernameField, usernameField));
                    } catch (IOException e) {
                        showError("Unable to open the next page.");
                    }
                    event.consume();
                } else if (event.getCode() == KeyCode.ESCAPE) {
                    usernameField.clear();
                    emailField.clear();
                    passwordField.clear();
                    confirmPasswordField.clear();
                    clearStatus();
                    usernameField.requestFocus();
                    event.consume();
                }
            });

            newScene.getProperties().put("createAccountHandlersInstalled", true);
        });
    }

    @FXML
    private void handleCreateAccount(ActionEvent event) throws IOException {
        clearStatus();

        String username = safeTrim(usernameField.getText());
        String email = safeTrim(emailField.getText()).toLowerCase();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        String validationError = validateCreateAccountInput(username, email, password, confirmPassword);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        try {
            if (userDAO.usernameExists(username)) {
                showError("That username is already taken.");
                return;
            }

            if (userDAO.emailExists(email)) {
                showError("That email is already registered.");
                return;
            }

            boolean created = userDAO.createUser(username, email, password);
            if (!created) {
                showError("Account could not be created. Please try again.");
                return;
            }

            SceneUtil.switchSceneNoHistory((Node) event.getSource(), FxmlResources.AUTH_LOGIN);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("A database error occurred. Please try again.");
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) throws IOException {
        SceneUtil.switchSceneNoHistory((Node) event.getSource(), FxmlResources.AUTH_LOGIN);
    }

    private String validateCreateAccountInput(String username,
                                              String email,
                                              String password,
                                              String confirmPassword) {
        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            return "Please fill in all fields.";
        }

        if (username.length() < 3 || username.length() > 30) {
            return "Username must be between 3 and 30 characters.";
        }

        if (!username.matches("[A-Za-z0-9_]+")) {
            return "Username can only contain letters, numbers, and underscores.";
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }

        if (password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }

        if (!containsUppercase(password) || !containsLowercase(password) || !containsDigit(password)) {
            return "Password must include uppercase, lowercase, and a number.";
        }

        if (!password.equals(confirmPassword)) {
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
}
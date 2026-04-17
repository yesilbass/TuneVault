package com.example.tunevaultfx.settings;

import com.example.tunevaultfx.chrome.SearchBarState;
import com.example.tunevaultfx.db.UserDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.ToastUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import com.example.tunevaultfx.util.UiPrefs;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/**
 * App-level preferences: appearance, playback defaults, about.
 */
public class SettingsPageController {

    @FXML private VBox settingsPageRoot;
    @FXML private Button themeToggleButton;
    @FXML private Label themeHintLabel;
    @FXML private CheckBox defaultShuffleCheckBox;
    @FXML private Label aboutVersionLabel;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        if (aboutVersionLabel != null) {
            aboutVersionLabel.setText("Version " + resolveAppVersion());
        }
        if (defaultShuffleCheckBox != null) {
            defaultShuffleCheckBox.setSelected(
                    UiPrefs.prefs().getBoolean(UiPrefs.KEY_DEFAULT_SHUFFLE_ON_LOGIN, false));
        }
        syncThemeUi();
        settingsPageRoot
                .sceneProperty()
                .addListener(
                        (obs, o, n) -> {
                            if (n != null) {
                                Platform.runLater(this::syncThemeUi);
                            }
                        });

        Platform.runLater(
                () -> {
                    if (settingsPageRoot != null) {
                        UiMotionUtil.playStaggeredEntrance(settingsPageRoot.getChildren());
                    }
                });
    }

    @FXML
    private void handleToggleTheme(ActionEvent e) {
        boolean next = !UiPrefs.prefs().getBoolean(UiPrefs.KEY_THEME_LIGHT, false);
        UiPrefs.prefs().putBoolean(UiPrefs.KEY_THEME_LIGHT, next);
        Scene scene = themeToggleButton != null ? themeToggleButton.getScene() : null;
        SceneUtil.applySavedTheme(scene);
        if (scene != null && scene.getRoot() != null) {
            AppTheme.refreshAllListViews(scene.getRoot());
        }
        syncThemeUi();
    }

    @FXML
    private void handleDefaultShuffleChanged(ActionEvent e) {
        if (defaultShuffleCheckBox == null) {
            return;
        }
        boolean on = defaultShuffleCheckBox.isSelected();
        UiPrefs.prefs().putBoolean(UiPrefs.KEY_DEFAULT_SHUFFLE_ON_LOGIN, on);
        MusicPlayerController.getInstance().setDefaultShufflePreference(on);
    }

    private void syncThemeUi() {
        if (themeToggleButton == null) {
            return;
        }
        boolean light = UiPrefs.prefs().getBoolean(UiPrefs.KEY_THEME_LIGHT, false);
        themeToggleButton.setText(light ? "Use dark mode" : "Use light mode");
        if (themeHintLabel != null) {
            themeHintLabel.setText(
                    light
                            ? "Light mode is active. The top bar uses the same setting."
                            : "Dark mode is active. The top bar uses the same setting.");
        }
    }

    private static String resolveAppVersion() {
        String v = SettingsPageController.class.getPackage().getImplementationVersion();
        return v != null && !v.isBlank() ? v : "0.1.0-SNAPSHOT";
    }

    @FXML
    private void handleDeleteAccount(ActionEvent event) {
        String username = SessionManager.getCurrentUsername();
        if (username == null || username.isBlank()) {
            return;
        }
        Scene scene = settingsPageRoot != null ? settingsPageRoot.getScene() : null;
        if (scene == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(scene.getWindow());
        confirm.setTitle("Delete account");
        confirm.setHeaderText("Delete your TuneVault account?");
        confirm.setContentText(
                "This permanently removes your playlists, listening history, saved searches, "
                        + "genre quiz results, and social follows. This cannot be undone.");
        if (confirm.showAndWait().isEmpty()
                || confirm.getResult() != ButtonType.OK) {
            return;
        }

        Dialog<ButtonType> verify = new Dialog<>();
        verify.initOwner(scene.getWindow());
        verify.setTitle("Verify password");
        verify.setHeaderText("Enter your password to confirm");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Label hint =
                new Label(
                        "For your security, re-enter the password for this account "
                                + "(\u201c"
                                + username
                                + "\u201d).");
        hint.setWrapText(true);
        hint.setMaxWidth(420);
        VBox box = new VBox(10, hint, passwordField);
        box.setPadding(new Insets(4, 0, 0, 0));
        verify.getDialogPane().setContent(box);

        ButtonType deleteForever =
                new ButtonType("Delete forever", ButtonBar.ButtonData.OK_DONE);
        verify.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, deleteForever);
        Button deleteBtn = (Button) verify.getDialogPane().lookupButton(deleteForever);
        deleteBtn.disableProperty().bind(passwordField.textProperty().isEmpty());

        if (verify.showAndWait().isEmpty() || verify.getResult() != deleteForever) {
            return;
        }

        String password = passwordField.getText() == null ? "" : passwordField.getText();
        Node source = (Node) event.getSource();

        Task<Boolean> task =
                new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return userDAO.deleteAccountVerified(username, password);
                    }
                };
        task.setOnSucceeded(
                ev -> {
                    if (!Boolean.TRUE.equals(task.getValue())) {
                        ToastUtil.error(scene, "Incorrect password. Your account was not deleted.");
                        return;
                    }
                    SearchBarState.clearSearchSubscriber();
                    SearchBarState.clearQuery();
                    MusicPlayerController.getInstance().resetForNewSession();
                    SessionManager.logout();
                    SceneUtil.clearHistory();
                    try {
                        SceneUtil.switchSceneNoHistory(source, FxmlResources.AUTH_LOGIN);
                    } catch (java.io.IOException ex) {
                        ex.printStackTrace();
                        ToastUtil.error(scene, "Account deleted, but navigation failed. Restart the app.");
                        return;
                    }
                    Scene after = source.getScene();
                    if (after != null) {
                        ToastUtil.success(after, "Your account has been deleted.");
                    }
                });
        task.setOnFailed(
                ev -> {
                    Throwable t = task.getException();
                    if (t != null) {
                        t.printStackTrace();
                    }
                    ToastUtil.error(scene, "Could not delete account. Try again later.");
                });
        Thread thread = new Thread(task, "delete-account");
        thread.setDaemon(true);
        thread.start();
    }
}

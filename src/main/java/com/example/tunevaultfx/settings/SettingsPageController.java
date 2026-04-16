package com.example.tunevaultfx.settings;

import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import com.example.tunevaultfx.util.UiPrefs;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
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
        MusicPlayerController.getInstance().setShuffleEnabled(on);
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
}

package com.example.tunevaultfx.chrome;

import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.ContextMenuPopupSupport;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.UiPrefs;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Thin header: theme toggle and log out (top-right). */
public class AppTopBarController {

    @FXML private HBox topBarRoot;
    @FXML private Button themeToggleBtn;
    @FXML private Button accountMenuBtn;

    private ContextMenu accountMenu;

    @FXML
    public void initialize() {
        syncThemeToggleLabel();
        topBarRoot.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) {
                Platform.runLater(this::syncThemeToggleLabel);
            }
        });
        if (accountMenuBtn != null) {
            accountMenu = new ContextMenu();
            accountMenu.getStyleClass().add("tv-account-dropdown");
            MenuItem profile = new MenuItem("Profile");
            profile.getStyleClass().add("top-bar-account-menu-item");
            profile.setOnAction(
                    e -> {
                        accountMenu.hide();
                        try {
                            SceneUtil.switchScene(accountMenuBtn, FxmlResources.PROFILE);
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
            MenuItem settings = new MenuItem("Settings");
            settings.getStyleClass().add("top-bar-account-menu-item");
            settings.setOnAction(
                    e -> {
                        accountMenu.hide();
                        try {
                            SceneUtil.switchScene(accountMenuBtn, FxmlResources.SETTINGS);
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    });
            accountMenu.getItems().addAll(profile, settings);
            ContextMenuPopupSupport.installThemedPopupHandlers(accountMenu, accountMenuBtn);
        }
    }

    @FXML
    private void handleAccountMenu(ActionEvent e) {
        if (accountMenu == null || accountMenuBtn == null) {
            return;
        }
        if (accountMenu.isShowing()) {
            accountMenu.hide();
        } else {
            accountMenu.show(accountMenuBtn, Side.BOTTOM, 0, 0);
        }
    }

    @FXML
    private void toggleTheme(ActionEvent e) {
        boolean next = !UiPrefs.prefs().getBoolean(UiPrefs.KEY_THEME_LIGHT, false);
        UiPrefs.prefs().putBoolean(UiPrefs.KEY_THEME_LIGHT, next);
        Scene scene = themeToggleBtn.getScene();
        SceneUtil.applySavedTheme(scene);
        syncThemeToggleLabel();
        if (scene != null && scene.getRoot() != null) {
            AppTheme.refreshAllListViews(scene.getRoot());
        }
    }

    @FXML
    private void handleLogout(ActionEvent e) throws IOException {
        SessionManager.logout();
        SceneUtil.clearHistory();
        SceneUtil.switchSceneNoHistory((Node) e.getSource(), FxmlResources.AUTH_LOGIN);
    }

    private void syncThemeToggleLabel() {
        boolean light = UiPrefs.prefs().getBoolean(UiPrefs.KEY_THEME_LIGHT, false);
        themeToggleBtn.setText(light ? "Dark mode" : "Light mode");
    }
}

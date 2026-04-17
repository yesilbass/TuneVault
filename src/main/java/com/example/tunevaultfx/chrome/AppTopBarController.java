package com.example.tunevaultfx.chrome;

import com.example.tunevaultfx.search.FullSearchPageOpener;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.util.ContextMenuPopupSupport;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.UiPrefs;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Header: home, centered search, theme, account menu, log out. */
public final class AppTopBarController {

    @FXML private HBox topBarRoot;
    @FXML private HBox topBarBrand;
    @FXML private Button topBarHomeButton;
    @FXML private Button topBarBackButton;
    @FXML private Button topBarForwardButton;
    @FXML private TextField globalSearchField;
    @FXML private Button searchInlineClearButton;
    @FXML private Button wrappedQuickBtn;
    @FXML private Button genreQuizQuickBtn;
    @FXML private Button themeToggleBtn;
    @FXML private Button accountMenuBtn;

    private ContextMenu accountMenu;
    private TopBarSearchDropdown topBarSearchDropdown;

    @FXML
    public void initialize() {
        syncThemeToggleLabel();
        setupHistoryNavButtons();
        SearchBarState.bindTopBarSearchField(globalSearchField);

        if (topBarHomeButton != null) {
            topBarHomeButton.setTooltip(new Tooltip("Home"));
        }

        topBarSearchDropdown = new TopBarSearchDropdown(globalSearchField);

        // Keep the bar out of the default tab order so the window doesn’t focus it on show (which
        // used to pop the history dropdown immediately after login). The field still accepts focus
        // on mouse click.
        globalSearchField.setFocusTraversable(false);

        if (searchInlineClearButton != null) {
            searchInlineClearButton
                    .visibleProperty()
                    .bind(
                            Bindings.createBooleanBinding(
                                    () -> {
                                        String t = SearchBarState.queryProperty().get();
                                        return t != null && !t.isBlank();
                                    },
                                    SearchBarState.queryProperty()));
            searchInlineClearButton.managedProperty().bind(searchInlineClearButton.visibleProperty());
            searchInlineClearButton.setTooltip(new Tooltip("Clear"));
        }

        globalSearchField.setOnAction(
                e -> {
                    if (topBarSearchDropdown != null) {
                        topBarSearchDropdown.hide();
                    }
                    try {
                        FullSearchPageOpener.open(globalSearchField);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });

        globalSearchField.addEventFilter(
                KeyEvent.KEY_PRESSED,
                e -> SearchBarState.clearSearchDropdownAutoOpenSuppress());

        globalSearchField.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                e -> {
                    if (e.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    SearchBarState.clearSearchDropdownAutoOpenSuppress();
                    if (topBarSearchDropdown != null) {
                        topBarSearchDropdown.onFieldActivated();
                    }
                });

        topBarRoot.sceneProperty()
                .addListener(
                        (obs, oldScene, newScene) -> {
                            if (newScene != null) {
                                Platform.runLater(this::syncThemeToggleLabel);
                            }
                        });
        if (topBarRoot.getScene() != null) {
            Platform.runLater(this::syncThemeToggleLabel);
        }

        if (wrappedQuickBtn != null) {
            wrappedQuickBtn.setTooltip(new Tooltip("Listening highlights"));
        }
        if (genreQuizQuickBtn != null) {
            genreQuizQuickBtn.setTooltip(new Tooltip("Genre profile quiz"));
        }

        if (accountMenuBtn != null) {
            accountMenu = new ContextMenu();
            accountMenu.getStyleClass().add("tv-account-dropdown");
            MenuItem profile = new MenuItem("Profile");
            profile.getStyleClass().add("top-bar-account-menu-item");
            profile.setOnAction(
                    e -> {
                        accountMenu.hide();
                        try {
                            SessionManager.clearProfileViewUsername();
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

    private void setupHistoryNavButtons() {
        if (topBarBackButton != null) {
            topBarBackButton.setTooltip(new Tooltip("Back"));
        }
        if (topBarForwardButton != null) {
            topBarForwardButton.setTooltip(new Tooltip("Forward"));
        }
        SceneUtil.setHistoryRefreshHandler(this::refreshHistoryNavButtons);
        refreshHistoryNavButtons();
    }

    private void refreshHistoryNavButtons() {
        if (topBarBackButton != null) {
            topBarBackButton.setDisable(!SceneUtil.canGoBack());
        }
        if (topBarForwardButton != null) {
            topBarForwardButton.setDisable(!SceneUtil.canGoForward());
        }
    }

    @FXML
    private void handleTopBarBack() {
        if (topBarRoot == null || topBarRoot.getScene() == null) {
            return;
        }
        try {
            SceneUtil.goBack(topBarRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTopBarForward() {
        if (topBarRoot == null || topBarRoot.getScene() == null) {
            return;
        }
        try {
            SceneUtil.goForward(topBarRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBrandClick(MouseEvent e) throws IOException {
        if (topBarSearchDropdown != null) {
            topBarSearchDropdown.hide();
        }
        SceneUtil.switchScene(topBarBrand, FxmlResources.MAIN_MENU);
        e.consume();
    }

    @FXML
    private void handleHomeClick(ActionEvent e) throws IOException {
        if (topBarSearchDropdown != null) {
            topBarSearchDropdown.hide();
        }
        SceneUtil.switchScene((Node) e.getSource(), FxmlResources.MAIN_MENU);
    }

    @FXML
    private void handleGoWrapped(ActionEvent e) throws IOException {
        if (topBarSearchDropdown != null) {
            topBarSearchDropdown.hide();
        }
        SceneUtil.switchScene(wrappedQuickBtn, FxmlResources.WRAPPED);
    }

    @FXML
    private void handleGoGenreQuiz(ActionEvent e) throws IOException {
        if (topBarSearchDropdown != null) {
            topBarSearchDropdown.hide();
        }
        SceneUtil.switchScene(genreQuizQuickBtn, FxmlResources.FIND_YOUR_GENRE);
    }

    @FXML
    private void handleInlineSearchClear(ActionEvent e) {
        e.consume();
        SearchBarState.clearQuery();
        if (globalSearchField != null) {
            globalSearchField.requestFocus();
        }
        if (topBarSearchDropdown != null) {
            Platform.runLater(
                    () -> {
                        if (globalSearchField != null) {
                            globalSearchField.requestFocus();
                        }
                        topBarSearchDropdown.refreshAndShow();
                    });
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
        if (topBarSearchDropdown != null) {
            topBarSearchDropdown.hide();
        }
        SearchBarState.clearSearchSubscriber();
        SearchBarState.clearQuery();
        SessionManager.logout();
        SceneUtil.clearHistory();
        SceneUtil.switchSceneNoHistory((Node) e.getSource(), FxmlResources.AUTH_LOGIN);
    }

    private void syncThemeToggleLabel() {
        boolean light = UiPrefs.prefs().getBoolean(UiPrefs.KEY_THEME_LIGHT, false);
        themeToggleBtn.setText(light ? "Dark mode" : "Light mode");
    }
}

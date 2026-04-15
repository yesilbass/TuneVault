package com.example.tunevaultfx.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.example.tunevaultfx.view.FxmlResources;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
/**
 * Centralized scene switching with navigation history.
 * Every {@link #switchScene} call pushes the current page onto a stack
 * so {@link #goBack} can return the user to where they actually came from.
 */
public final class SceneUtil {

    private static final String CSS_PATH  = "/com/example/tunevaultfx/app.css";
    private static final String FXML_BASE = FxmlResources.CLASSPATH_PREFIX;

    private static final Deque<String> history = new ArrayDeque<>();
    private static String currentPage = null;

    private SceneUtil() {}

    public static void switchScene(Node sourceNode, String fxmlFile) throws IOException {
        if (currentPage != null) {
            history.push(currentPage);
        }
        currentPage = fxmlFile;
        loadScene(sourceNode, fxmlFile);
    }

    /**
     * Go back to the previous page. If there's no history, goes to main-menu.
     */
    public static void goBack(Node sourceNode) throws IOException {
        String target = history.isEmpty() ? FxmlResources.MAIN_MENU : history.pop();
        currentPage = target;
        loadScene(sourceNode, target);
    }

    /**
     * Navigate without pushing to history (used for auth flows where
     * you don't want "back" to return to login after signing in).
     */
    public static void switchSceneNoHistory(Node sourceNode, String fxmlFile) throws IOException {
        currentPage = fxmlFile;
        loadScene(sourceNode, fxmlFile);
    }

    /**
     * Clear all navigation history. Call on logout / session reset.
     */
    public static void clearHistory() {
        history.clear();
        currentPage = null;
    }

    public static boolean hasHistory() {
        return !history.isEmpty();
    }

    /**
     * Page on top of the back stack (where {@link #goBack} would return), without removing it.
     * Used e.g. to highlight the correct sidebar section while on the artist profile.
     */
    public static String peekHistory() {
        return history.isEmpty() ? null : history.peek();
    }

    /** Current FXML path relative to {@link FxmlResources#CLASSPATH_PREFIX} (e.g. {@code search/search-page.fxml}). */
    public static String getCurrentPage() {
        return currentPage;
    }

    /**
     * Applies saved light/dark preference to the scene root (style class + {@link AppTheme}).
     * Does <strong>not</strong> refresh every {@link javafx.scene.control.ListView} — doing that on
     * every navigation caused a visible one-frame flicker. Call {@link AppTheme#refreshAllListViews}
     * only after the user toggles appearance.
     * <p>
     * Call again after code replaces {@link Scene#getRoot()} with an overlay {@code StackPane}
     * wrapper; {@code theme-light} must live on the actual scene root so {@code .root.theme-light}
     * CSS selectors keep matching.
     */
    public static void applySavedTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        boolean light = UiPrefs.prefs().getBoolean(UiPrefs.KEY_THEME_LIGHT, false);
        AppTheme.setLightMode(light);
        Parent root = scene.getRoot();
        root.getStyleClass().removeAll("theme-light");
        if (light) {
            root.getStyleClass().add("theme-light");
        }
    }

    private static void loadScene(Node sourceNode, String fxmlFile) throws IOException {
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        Scene scene = stage.getScene();

        FXMLLoader loader = new FXMLLoader(
                SceneUtil.class.getResource(FXML_BASE + fxmlFile));
        Parent root = loader.load();

        String cssUrl = SceneUtil.class.getResource(CSS_PATH).toExternalForm();
        if (!scene.getStylesheets().contains(cssUrl)) {
            scene.getStylesheets().add(cssUrl);
        }

        scene.setRoot(root);
        applySavedTheme(scene);
    }
}

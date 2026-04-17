package com.example.tunevaultfx.util;

import com.example.tunevaultfx.chrome.SearchBarState;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized scene switching with browser-style navigation: a linear history with back/forward.
 * <p>
 * {@link #switchScene} appends a new page and drops any "forward" tail (same as navigating after
 * using Back). {@link #switchSceneNoHistory} replaces the entire trail with a single page (auth and
 * similar flows).
 */
public final class SceneUtil {

    private static final String CSS_PATH  = "/com/example/tunevaultfx/app.css";
    private static final String FXML_BASE = FxmlResources.CLASSPATH_PREFIX;

    private static final List<String> entries = new ArrayList<>();
    private static int currentIndex = -1;
    private static String currentPage = null;

    /**
     * Latest top bar registers this so back/forward buttons stay in sync after each navigation.
     * Replaced on every new page load since each root has a fresh {@code AppTopBarController}.
     */
    private static Runnable historyRefreshHandler;

    private SceneUtil() {}

    public static void setHistoryRefreshHandler(Runnable handler) {
        historyRefreshHandler = handler;
    }

    private static void notifyHistoryChanged() {
        if (historyRefreshHandler != null) {
            try {
                historyRefreshHandler.run();
            } catch (RuntimeException ignored) {
                // avoid breaking navigation if UI refresh fails
            }
        }
    }

    public static void switchScene(Node sourceNode, String fxmlFile) throws IOException {
        if (sourceNode == null || sourceNode.getScene() == null) {
            throw new IOException("Navigation source has no Scene (already detached?)");
        }
        switchScene(sourceNode.getScene(), fxmlFile);
    }

    /**
     * Same as {@link #switchScene(Node, String)} but uses the stage {@link Scene} directly. Prefer
     * this when the triggering node may be torn down (e.g. popup actions) or when you already hold
     * a stable scene reference from the current pulse.
     */
    public static void switchScene(Scene scene, String fxmlFile) throws IOException {
        if (scene == null) {
            throw new IOException("Scene is null");
        }
        if (currentIndex < 0 || entries.isEmpty()) {
            entries.clear();
            entries.add(fxmlFile);
            currentIndex = 0;
        } else {
            while (entries.size() > currentIndex + 1) {
                entries.remove(entries.size() - 1);
            }
            if (fxmlFile.equals(entries.get(currentIndex))) {
                loadScene(scene, fxmlFile);
                return;
            }
            entries.add(fxmlFile);
            currentIndex++;
        }
        loadScene(scene, fxmlFile);
    }

    /**
     * Replace navigation history with this page only (login, logout, auth sub-pages).
     */
    public static void switchSceneNoHistory(Node sourceNode, String fxmlFile) throws IOException {
        entries.clear();
        entries.add(fxmlFile);
        currentIndex = 0;
        if (sourceNode == null || sourceNode.getScene() == null) {
            throw new IOException("Navigation source has no Scene (already detached?)");
        }
        loadScene(sourceNode.getScene(), fxmlFile);
    }

    public static void goBack(Node sourceNode) throws IOException {
        if (!canGoBack()) {
            return;
        }
        currentIndex--;
        if (sourceNode == null || sourceNode.getScene() == null) {
            throw new IOException("Navigation source has no Scene (already detached?)");
        }
        loadScene(sourceNode.getScene(), entries.get(currentIndex));
    }

    public static void goForward(Node sourceNode) throws IOException {
        if (!canGoForward()) {
            return;
        }
        currentIndex++;
        if (sourceNode == null || sourceNode.getScene() == null) {
            throw new IOException("Navigation source has no Scene (already detached?)");
        }
        loadScene(sourceNode.getScene(), entries.get(currentIndex));
    }

    public static boolean canGoBack() {
        return currentIndex > 0;
    }

    public static boolean canGoForward() {
        return currentIndex >= 0 && currentIndex < entries.size() - 1;
    }

    /**
     * Clear all navigation history. Call on logout / session reset.
     */
    public static void clearHistory() {
        entries.clear();
        currentIndex = -1;
        currentPage = null;
        historyRefreshHandler = null;
        notifyHistoryChanged();
    }

    /**
     * Page you would return to with Back, without changing history.
     */
    public static String peekHistory() {
        if (currentIndex <= 0) {
            return null;
        }
        return entries.get(currentIndex - 1);
    }

    /** Current FXML path relative to {@link FxmlResources#CLASSPATH_PREFIX}. */
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

    private static void loadScene(Scene scene, String fxmlFile) throws IOException {
        // Drop the search subscriber before tearing down the old root so debounced query updates
        // cannot run against a detached SearchPageController (symptom: "typing shows nothing").
        SearchBarState.clearSearchSubscriber();

        if (scene == null) {
            throw new IOException("Scene is null");
        }
        Window window = scene.getWindow();
        if (window == null) {
            throw new IOException("Scene has no Window");
        }

        var fxmlUrl = SceneUtil.class.getResource(FXML_BASE + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Missing FXML resource: " + FXML_BASE + fxmlFile);
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();

        String cssUrl = SceneUtil.class.getResource(CSS_PATH).toExternalForm();
        if (!scene.getStylesheets().contains(cssUrl)) {
            scene.getStylesheets().add(cssUrl);
        }

        scene.setRoot(root);
        applySavedTheme(scene);
        currentPage = fxmlFile;
        notifyHistoryChanged();
    }
}

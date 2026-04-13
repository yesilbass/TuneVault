package com.example.tunevaultfx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

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
    private static final String FXML_BASE = "/com/example/tunevaultfx/";

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
        String target = history.isEmpty() ? "main-menu.fxml" : history.pop();
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
    }
}

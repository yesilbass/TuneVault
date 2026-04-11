package com.example.tunevaultfx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Handles scene switching throughout the application.
 */
public final class SceneUtil {

    private SceneUtil() {
    }

    public static void switchScene(Node sourceNode, String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                SceneUtil.class.getResource("/com/example/tunevaultfx/" + fxmlFile)
        );

        Parent root = loader.load();

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.sizeToScene();

        if (stage.getWidth() < 1180) {
            stage.setWidth(1280);
        }
        if (stage.getHeight() < 780) {
            stage.setHeight(820);
        }

        stage.centerOnScreen();
        stage.show();
    }
}
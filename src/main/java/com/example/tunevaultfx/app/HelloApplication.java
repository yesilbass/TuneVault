package com.example.tunevaultfx.app;

import com.example.tunevaultfx.db.DBConnection;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Application entry point.
 *
 * Changes:
 *  - Stage starts maximized so the app fills the screen immediately on launch.
 *  - Global CSS stylesheet is applied to the initial scene.
 *  - DBConnection pool is shut down cleanly when the app closes.
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader =
                new FXMLLoader(
                        HelloApplication.class.getResource(
                                FxmlResources.CLASSPATH_PREFIX + FxmlResources.AUTH_LOGIN));

        Scene scene = new Scene(loader.load());

        // Apply global dark theme to every node in the scene graph
        scene.getStylesheets().add(
                HelloApplication.class.getResource(
                        "/com/example/tunevaultfx/app.css").toExternalForm());

        SceneUtil.applySavedTheme(scene);

        stage.setTitle("TuneVault");
        stage.setScene(scene);

        // Start maximized — fills the user's screen on every machine regardless
        // of display resolution, no manual resizing needed
        stage.setMaximized(true);

        // Minimum size prevents layout breakage if user un-maximizes
        stage.setMinWidth(1180);
        stage.setMinHeight(780);

        // Shut down HikariCP pool cleanly when the window is closed
        stage.setOnCloseRequest(e -> DBConnection.shutdown());

        stage.show();
    }
}

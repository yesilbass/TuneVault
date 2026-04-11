package com.example.tunevaultfx.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/com/example/tunevaultfx/login-page.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("TuneVault");
        stage.setScene(scene);

        stage.setWidth(1280);
        stage.setHeight(820);
        stage.centerOnScreen();

        stage.show();
    }
}
module com.example.tunevaultfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires java.sql;

    exports com.example.tunevaultfx.app;
    opens com.example.tunevaultfx.app to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.controllers;
    opens com.example.tunevaultfx.controllers to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.controllers.auth;
    opens com.example.tunevaultfx.controllers.auth to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.core;
    opens com.example.tunevaultfx.core to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.util;
    opens com.example.tunevaultfx.util to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.playlist;
    opens com.example.tunevaultfx.playlist to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.findyourgenre;
    opens com.example.tunevaultfx.findyourgenre to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.user;
    opens com.example.tunevaultfx.user to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.session;
    opens com.example.tunevaultfx.session to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.musicplayer;
    opens com.example.tunevaultfx.musicplayer to com.fasterxml.jackson.databind, javafx.fxml;
    exports com.example.tunevaultfx.wrapped;
    opens com.example.tunevaultfx.wrapped to com.fasterxml.jackson.databind, javafx.fxml;
    exports com.example.tunevaultfx.db;
    opens com.example.tunevaultfx.db to com.fasterxml.jackson.databind, javafx.fxml;
}
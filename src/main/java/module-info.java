module com.example.tunevaultfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires javafx.swing;

    requires java.desktop;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires java.sql;

    // Connection pooling
    requires com.zaxxer.hikari;

    // HikariCP uses SLF4J for logging — declare it so the module system
    // doesn't block the reflection HikariCP needs internally
    requires org.slf4j;
    requires java.prefs;

    exports com.example.tunevaultfx.app;
    opens com.example.tunevaultfx.app to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.mainmenu;
    opens com.example.tunevaultfx.mainmenu to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.auth;
    opens com.example.tunevaultfx.auth to com.fasterxml.jackson.databind, javafx.fxml;

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

    exports com.example.tunevaultfx.musicplayer.playback;
    opens com.example.tunevaultfx.musicplayer.playback to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.playlist.service;
    opens com.example.tunevaultfx.playlist.service to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.playlist.cell;
    opens com.example.tunevaultfx.playlist.cell to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.musicplayer.controller;
    opens com.example.tunevaultfx.musicplayer.controller to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.search;
    opens com.example.tunevaultfx.search to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.recommendation;
    opens com.example.tunevaultfx.recommendation to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.chrome;
    opens com.example.tunevaultfx.chrome to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.profile;
    opens com.example.tunevaultfx.profile to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.profile.media;
    opens com.example.tunevaultfx.profile.media to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.settings;
    opens com.example.tunevaultfx.settings to com.fasterxml.jackson.databind, javafx.fxml;

    exports com.example.tunevaultfx.view;
}
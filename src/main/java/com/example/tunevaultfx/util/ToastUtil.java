package com.example.tunevaultfx.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Slide-in toast notifications anchored to the top of the current scene.
 * <p>
 * Usage:
 * <pre>
 *   ToastUtil.success(someNode.getScene(), "Song added to queue");
 *   ToastUtil.error(someNode.getScene(), "Could not save playlist");
 *   ToastUtil.info(someNode.getScene(), "3 songs imported");
 * </pre>
 */
public final class ToastUtil {

    private ToastUtil() {}

    private static final Duration SLIDE_IN  = Duration.millis(250);
    private static final Duration DISPLAY   = Duration.millis(2400);
    private static final Duration SLIDE_OUT = Duration.millis(200);

    public static void success(Scene scene, String message) {
        show(scene, message, "toast", "toast-success");
    }

    public static void error(Scene scene, String message) {
        show(scene, message, "toast", "toast-error");
    }

    public static void info(Scene scene, String message) {
        show(scene, message, "toast", "toast-info");
    }

    private static void show(Scene scene, String message, String... cssClasses) {
        if (scene == null || scene.getRoot() == null) return;

        Label toast = new Label(message);
        toast.getStyleClass().addAll(cssClasses);
        toast.setMouseTransparent(true);
        toast.setMaxWidth(460);
        toast.setWrapText(true);

        StackPane.setAlignment(toast, Pos.TOP_CENTER);
        toast.setTranslateY(-50);
        toast.setOpacity(0);

        if (scene.getRoot() instanceof StackPane sp) {
            sp.getChildren().add(toast);
            animate(toast, sp);
        } else {
            StackPane wrapper = new StackPane();
            wrapper.getChildren().addAll(scene.getRoot(), toast);
            scene.setRoot(wrapper);
            animate(toast, wrapper);
        }
    }

    private static void animate(Label toast, StackPane parent) {
        FadeTransition fadeIn = new FadeTransition(SLIDE_IN, toast);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(SLIDE_IN, toast);
        slideIn.setToY(20);

        ParallelTransition enter = new ParallelTransition(fadeIn, slideIn);

        PauseTransition hold = new PauseTransition(DISPLAY);

        FadeTransition fadeOut = new FadeTransition(SLIDE_OUT, toast);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(SLIDE_OUT, toast);
        slideOut.setToY(-30);

        ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
        exit.setOnFinished(e -> parent.getChildren().remove(toast));

        enter.setOnFinished(e -> hold.play());
        hold.setOnFinished(e -> exit.play());
        enter.play();
    }
}

package com.example.tunevaultfx.musicplayer;

/**
 * Single source of truth for all player button styles.
 *
 * Both MiniPlayerController and ExpandedPlayerController use these constants.
 * Changing the accent colour or button shape now requires editing ONE file.
 *
 * Sizing (prefWidth/prefHeight) is intentionally left to each controller
 * because the mini player uses smaller buttons than the expanded player.
 */
public final class PlayerStyleConstants {

    private PlayerStyleConstants() {}

    // ── Colours (change these to re-theme the entire player) ───────

    private static final String VIOLET_BG       = "rgba(139,92,246,0.18)";
    private static final String VIOLET_BG_HOVER = "rgba(139,92,246,0.28)";
    private static final String VIOLET_TEXT      = "#a78bfa";
    private static final String VIOLET_BORDER    = "rgba(139,92,246,0.28)";

    private static final String ROSE_BG_ON       = "rgba(244,63,94,0.16)";
    private static final String ROSE_TEXT_ON      = "#f43f5e";
    private static final String ROSE_BORDER_ON    = "rgba(244,63,94,0.26)";

    private static final String NEUTRAL_TEXT      = "#5c5c78";
    private static final String NEUTRAL_BG        = "rgba(255,255,255,0.07)";
    private static final String NEUTRAL_BORDER    = "rgba(255,255,255,0.1)";

    // ── Shared radius tokens ────────────────────────────────────────

    /** Round radius used by mini player buttons. */
    public static final String RADIUS_MINI = "18";

    /** Round radius used by expanded player buttons. */
    public static final String RADIUS_FULL = "22";

    // ── Style builders ─────────────────────────────────────────────

    /**
     * Inactive mode button (shuffle off, loop off).
     * @param fontSize e.g. "18px" for mini, "20px" for expanded
     * @param radius   use RADIUS_MINI or RADIUS_FULL
     */
    public static String modeInactive(String fontSize, String radius) {
        return "-fx-background-color: " + NEUTRAL_BG + ";" +
                "-fx-text-fill: " + NEUTRAL_TEXT + ";" +
                "-fx-font-size: " + fontSize + "; -fx-font-weight: bold;" +
                "-fx-background-radius: " + radius + ";" +
                "-fx-border-color: " + NEUTRAL_BORDER + ";" +
                "-fx-border-radius: " + radius + "; -fx-border-width: 1;";
    }

    /**
     * Active mode button (shuffle on, loop on).
     */
    public static String modeActive(String fontSize, String radius) {
        return "-fx-background-color: " + VIOLET_BG + ";" +
                "-fx-text-fill: " + VIOLET_TEXT + ";" +
                "-fx-font-size: " + fontSize + "; -fx-font-weight: bold;" +
                "-fx-background-radius: " + radius + ";" +
                "-fx-border-color: " + VIOLET_BORDER + ";" +
                "-fx-border-radius: " + radius + "; -fx-border-width: 1;";
    }

    /**
     * Like button when the current song IS liked (heart filled, rose colour).
     */
    public static String likeOn(String fontSize, String radius) {
        return "-fx-background-color: " + ROSE_BG_ON + ";" +
                "-fx-text-fill: " + ROSE_TEXT_ON + ";" +
                "-fx-font-size: " + fontSize + "; -fx-font-weight: bold;" +
                "-fx-background-radius: " + radius + ";" +
                "-fx-border-color: " + ROSE_BORDER_ON + ";" +
                "-fx-border-radius: " + radius + "; -fx-border-width: 1;";
    }

    /**
     * Like button when the current song is NOT liked (heart empty, muted).
     */
    public static String likeOff(String fontSize, String radius) {
        return "-fx-background-color: transparent;" +
                "-fx-text-fill: " + NEUTRAL_TEXT + ";" +
                "-fx-font-size: " + fontSize + "; -fx-font-weight: bold;" +
                "-fx-background-radius: " + radius + ";";
    }

    /**
     * Add-to-playlist button style (always muted).
     */
    public static String addButton(String fontSize, String radius) {
        return "-fx-background-color: transparent;" +
                "-fx-text-fill: " + NEUTRAL_TEXT + ";" +
                "-fx-font-size: " + fontSize + "; -fx-font-weight: bold;" +
                "-fx-background-radius: " + radius + ";";
    }

    // ── CSS class names (preferred over inline styles) ─────────────

    public static String modeInactiveClass()  { return "player-btn"; }
    public static String modeActiveClass()    { return "player-btn-active"; }

    public static String likeOnClass()        { return "player-btn-like-on"; }
    public static String likeOffClass()       { return "player-btn-like"; }

    public static String queueActiveClass()   { return "player-btn-queue-active"; }
    public static String queueInactiveClass() { return "player-btn-queue"; }

    public static String addButtonClass()     { return "player-btn"; }
}
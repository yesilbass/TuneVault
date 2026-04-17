package com.example.tunevaultfx.mainmenu;

import com.example.tunevaultfx.util.AppTheme;

/** Inline CSS snippets shared by the home feed list cells and chart rows. */
public final class HomeFeedStyles {

    private HomeFeedStyles() {}

    public static String noteGlyphStyle() {
        String fill = AppTheme.isLightMode() ? "#5b21b6" : "#a78bfa";
        return "-fx-font-size: 15px; -fx-text-fill: " + fill + "; -fx-font-weight: bold;";
    }

    public static String noteIconBoxStyle() {
        if (AppTheme.isLightMode()) {
            return "-fx-background-color: rgba(124,58,237,0.14);"
                    + "-fx-background-radius: 10;"
                    + "-fx-border-color: rgba(124,58,237,0.32);"
                    + "-fx-border-radius: 10; -fx-border-width: 1;";
        }
        return "-fx-background-color: rgba(139,92,246,0.14);"
                + "-fx-background-radius: 10;"
                + "-fx-border-color: rgba(139,92,246,0.22);"
                + "-fx-border-radius: 10; -fx-border-width: 1;";
    }

    public static String playButtonStyle() {
        String fill = AppTheme.isLightMode() ? "#5b21b6" : "#7c3aed";
        return "-fx-background-color: transparent; -fx-text-fill: "
                + fill
                + "; -fx-font-size: 12px; -fx-font-weight: bold;";
    }
}

package com.example.tunevaultfx.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.lang.reflect.Field;

/**
 * Context menus render in a separate
 * {@link Scene}. That scene does not inherit the main window's {@code .root.theme-light}, and the
 * default fill is black — rounded menu skins then show dark corners in light mode unless the fill
 * and stylesheet are synced like the main stage.
 */
public final class ContextMenuPopupSupport {

    /** Popup scenes do not inherit the Stage's stylesheets; they must be attached explicitly. */
    private static final String APP_CSS_URL;

    static {
        var res = ContextMenuPopupSupport.class.getResource("/com/example/tunevaultfx/app.css");
        APP_CSS_URL = res != null ? res.toExternalForm() : "";
    }

    private ContextMenuPopupSupport() {}

    private static final String THEMED_MENU_BUTTON_KEY = "tunevault.themedMenuButtonPopup";
    private static final String MENU_BUTTON_LISTENERS_KEY = "tunevault.menuButtonPopupListeners";

    /**
     * {@link MenuButton} uses an internal {@link ContextMenu} (see {@code MenuButtonSkinBase}) that
     * never gets {@link #installThemedPopupHandlers(ContextMenu, Node)} — popup scenes keep the
     * default black fill and show dark corners behind rounded menu skins in light mode.
     */
    public static void installThemedMenuButtonPopup(MenuButton menuButton, Node anchor) {
        if (menuButton == null) {
            return;
        }
        Node sceneAnchor = anchor != null ? anchor : menuButton;
        Runnable attach =
                () -> {
                    ContextMenu cm = contextMenuFromMenuButtonSkin(menuButton);
                    if (cm == null) {
                        return;
                    }
                    if (Boolean.TRUE.equals(cm.getProperties().get(THEMED_MENU_BUTTON_KEY))) {
                        return;
                    }
                    cm.getProperties().put(THEMED_MENU_BUTTON_KEY, Boolean.TRUE);
                    installThemedPopupHandlers(cm, sceneAnchor);
                };
        if (!Boolean.TRUE.equals(menuButton.getProperties().get(MENU_BUTTON_LISTENERS_KEY))) {
            menuButton.getProperties().put(MENU_BUTTON_LISTENERS_KEY, Boolean.TRUE);
            menuButton.skinProperty().addListener((obs, oldSkin, newSkin) -> attach.run());
            /*
             * ContextMenu show/shown can race the popup Scene on some controls; the MenuButton's
             * ON_SHOWN always runs after the popup window exists — re-sync fill, light styles, clip.
             */
            EventHandler<Event> onMenuButtonShown =
                    e -> {
                        ContextMenu cm = contextMenuFromMenuButtonSkin(menuButton);
                        if (cm == null) {
                            return;
                        }
                        syncTvContextLightStyleClass(cm);
                        Runnable polish =
                                () -> {
                                    preparePopupScene(cm, sceneAnchor);
                                    syncPopupSceneFill(cm);
                                    clipMenuRootToRoundedCorners(cm);
                                };
                        polish.run();
                        Platform.runLater(polish);
                        Platform.runLater(() -> Platform.runLater(polish));
                    };
            menuButton.addEventHandler(MenuButton.ON_SHOWN, onMenuButtonShown);
        }
        if (menuButton.getSkin() != null) {
            attach.run();
        }
        Platform.runLater(attach);
    }

    private static ContextMenu contextMenuFromMenuButtonSkin(MenuButton menuButton) {
        Skin skin = menuButton.getSkin();
        if (skin == null) {
            return null;
        }
        for (Class<?> cl = skin.getClass(); cl != null; cl = cl.getSuperclass()) {
            final Field popupField;
            try {
                popupField = cl.getDeclaredField("popup");
            } catch (NoSuchFieldException e) {
                continue;
            }
            if (!ContextMenu.class.isAssignableFrom(popupField.getType())) {
                continue;
            }
            try {
                popupField.setAccessible(true);
                Object raw = popupField.get(skin);
                return raw instanceof ContextMenu contextMenu ? contextMenu : null;
            } catch (ReflectiveOperationException e) {
                return null;
            }
        }
        return null;
    }

    public static void preparePopupScene(ContextMenu menu, Node anchor) {
        Scene popupScene = menu.getScene();
        if (popupScene == null) {
            return;
        }
        if (!APP_CSS_URL.isEmpty() && !popupScene.getStylesheets().contains(APP_CSS_URL)) {
            popupScene.getStylesheets().add(APP_CSS_URL);
        }
        Scene ownerScene = anchor != null ? anchor.getScene() : null;
        if (ownerScene != null) {
            for (String sheet : ownerScene.getStylesheets()) {
                if (sheet != null
                        && !sheet.isBlank()
                        && !popupScene.getStylesheets().contains(sheet)) {
                    popupScene.getStylesheets().add(sheet);
                }
            }
            Parent popRoot = popupScene.getRoot();
            Parent ownRoot = ownerScene.getRoot();
            if (popRoot != null && ownRoot != null) {
                boolean light = ownRoot.getStyleClass().contains("theme-light");
                popRoot.getStyleClass().removeAll("theme-light");
                if (light) {
                    popRoot.getStyleClass().add("theme-light");
                }
            }
        }
    }

    public static void syncPopupSceneFill(ContextMenu menu) {
        Scene popupScene = menu.getScene();
        if (popupScene != null) {
            popupScene.setFill(Color.TRANSPARENT);
            if (popupScene.getRoot() instanceof Region reg) {
                reg.setStyle("-fx-background-color: transparent;");
            }
        }
    }

    /**
     * Rounded backgrounds still paint a rectangular layout bounds; anti-aliased fringe can show
     * whatever is behind (row tint, scene fill). Clipping the skin root matches the rounded shape.
     */
    public static void clipMenuRootToRoundedCorners(ContextMenu menu) {
        Scene sc = menu.getScene();
        if (sc == null) {
            return;
        }
        Node root = sc.getRoot();
        if (!(root instanceof Region reg)) {
            return;
        }
        double radius = AppTheme.isLightMode() ? 16 : 14;
        double arc = 2 * radius;
        Rectangle clip = new Rectangle();
        clip.setSmooth(true);
        clip.setArcWidth(arc);
        clip.setArcHeight(arc);
        clip.widthProperty().bind(reg.widthProperty());
        clip.heightProperty().bind(reg.heightProperty());
        reg.setClip(clip);
    }

    public static void clearMenuRootClip(ContextMenu menu) {
        Scene sc = menu.getScene();
        if (sc != null && sc.getRoot() instanceof Region reg) {
            var old = reg.getClip();
            reg.setClip(null);
            if (old instanceof Rectangle rect) {
                rect.widthProperty().unbind();
                rect.heightProperty().unbind();
            }
        }
    }

    /** Match {@link com.example.tunevaultfx.app.css} {@code .context-menu.tv-context-light}. */
    public static void syncTvContextLightStyleClass(ContextMenu menu) {
        menu.getStyleClass().remove("tv-context-light");
        if (AppTheme.isLightMode()) {
            menu.getStyleClass().add("tv-context-light");
        }
    }

    public static void polishPopupSurface(ContextMenu menu, Node anchor) {
        preparePopupScene(menu, anchor);
        syncPopupSceneFill(menu);
        clipMenuRootToRoundedCorners(menu);
    }

    /**
     * Attach show/hide handlers so the popup matches app theme (fill, stylesheet, light menu
     * styles, rounded clip).
     */
    public static void installThemedPopupHandlers(ContextMenu menu, Node anchor) {
        menu.setOnShowing(
                e -> {
                    syncTvContextLightStyleClass(menu);
                    preparePopupScene(menu, anchor);
                });
        menu.setOnHidden(e -> clearMenuRootClip(menu));
        menu.setOnShown(
                e -> {
                    Runnable syncPopupSurface =
                            () -> {
                                preparePopupScene(menu, anchor);
                                syncPopupSceneFill(menu);
                                clipMenuRootToRoundedCorners(menu);
                            };
                    syncPopupSurface.run();
                    Platform.runLater(syncPopupSurface);
                    Platform.runLater(() -> Platform.runLater(syncPopupSurface));
                });
    }

}

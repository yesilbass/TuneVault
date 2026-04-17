package com.example.tunevaultfx.search;

import com.example.tunevaultfx.chrome.SearchBarState;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.scene.Node;
import javafx.scene.Scene;

import java.io.IOException;

/**
 * Opens the full Search page for the current top-bar query, with a one-shot hint so the results
 * chrome scrolls to the top and the search field stays the focus target (dropdown / inline flow).
 */
public final class FullSearchPageOpener {

    private FullSearchPageOpener() {}

    public static void open(Node navigationSource) throws IOException {
        if (navigationSource == null || navigationSource.getScene() == null) {
            return;
        }
        open(navigationSource.getScene());
    }

    /**
     * Opens search using the stage scene directly (safe when a control inside a transient popup
     * initiated navigation: the {@link Node} may be removed on the next layout pulse).
     */
    public static void open(Scene hostScene) throws IOException {
        if (hostScene == null) {
            return;
        }
        SearchBarState.requestFullSearchPresentationAfterNextRun();
        SearchBarState.suppressSearchDropdownAutoOpen();
        SceneUtil.switchScene(hostScene, FxmlResources.SEARCH);
    }
}

package com.example.tunevaultfx.chrome;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Shared search query bound to the top-bar {@link TextField} on logged-in screens.
 * Survives navigation so the same text stays when switching pages until the user clears it.
 * <p>
 * The active {@link SearchPageController} registers a debounced subscriber so only one listener
 * is attached to the query property across scene reloads.
 */
public final class SearchBarState {

    private static final StringProperty QUERY = new SimpleStringProperty("");
    private static TextField boundField;
    private static Consumer<String> searchSubscriber;

    /**
     * When true, the next {@link #setSearchSubscriber} run (or subscriber notification) on the
     * search page should scroll results to the top and polish focus — used when opening the full
     * search page from the top bar (Enter) so the page feels intentional, not a stale scroll position.
     */
    private static boolean fullSearchPresentationAfterNextRun;

    /**
     * After Enter opens the full search page, the new header must not
     * immediately reopen the typeahead popup (focus + non-empty query would otherwise feel stuck on
     * the compact list). Cleared when the user clicks or types in the search field.
     */
    private static boolean suppressSearchDropdownAutoOpen;

    /** Coalesces fast typing on the search page without blocking the first navigation + subscribe. */
    private static final Timeline DEBOUNCE =
            new Timeline(new KeyFrame(Duration.millis(120), e -> notifySubscriberLatestQuery()));

    static {
        DEBOUNCE.setCycleCount(1);
        QUERY.addListener(
                (obs, o, n) -> {
                    Consumer<String> sub = searchSubscriber;
                    String raw = n != null ? n : "";
                    if (sub != null && raw.isBlank()) {
                        DEBOUNCE.stop();
                        sub.accept(raw);
                        return;
                    }
                    if (sub == null) {
                        DEBOUNCE.stop();
                        return;
                    }
                    DEBOUNCE.stop();
                    DEBOUNCE.playFromStart();
                });
    }

    private SearchBarState() {}

    private static void notifySubscriberLatestQuery() {
        Consumer<String> sub = searchSubscriber;
        if (sub != null) {
            String v = QUERY.get();
            sub.accept(v != null ? v : "");
        }
    }

    public static StringProperty queryProperty() {
        return QUERY;
    }

    public static TextField getBoundField() {
        return boundField;
    }

    /**
     * Binds the top-bar field to {@link #queryProperty()}, replacing any previous binding
     * from an older header instance after scene switches.
     */
    public static void bindTopBarSearchField(TextField field) {
        if (boundField != null && boundField != field) {
            try {
                boundField.textProperty().unbindBidirectional(QUERY);
            } catch (@SuppressWarnings("unused") RuntimeException ignored) {
                // field may already be partially torn down
            }
        }
        boundField = field;
        if (field != null) {
            field.textProperty().bindBidirectional(QUERY);
        }
    }

    public static void clearQuery() {
        QUERY.set("");
    }

    public static void requestFullSearchPresentationAfterNextRun() {
        fullSearchPresentationAfterNextRun = true;
    }

    /** @return whether the search page should reset chrome scroll / focus after this run */
    public static boolean consumeFullSearchPresentationRequest() {
        boolean v = fullSearchPresentationAfterNextRun;
        fullSearchPresentationAfterNextRun = false;
        return v;
    }

    public static void suppressSearchDropdownAutoOpen() {
        suppressSearchDropdownAutoOpen = true;
    }

    public static void clearSearchDropdownAutoOpenSuppress() {
        suppressSearchDropdownAutoOpen = false;
    }

    public static boolean isSearchDropdownAutoOpenSuppressed() {
        return suppressSearchDropdownAutoOpen;
    }

    /**
     * Only the visible search page should register; replaces any previous subscriber.
     * Immediately applies the current query so results show even if a debounced update
     * fired while {@link #clearSearchSubscriber} had cleared the listener during navigation.
     */
    public static void setSearchSubscriber(Consumer<String> subscriber) {
        searchSubscriber = subscriber;
        DEBOUNCE.stop();
        if (subscriber != null) {
            String v = QUERY.get();
            subscriber.accept(v != null ? v : "");
        }
    }

    public static void clearSearchSubscriber() {
        searchSubscriber = null;
        DEBOUNCE.stop();
    }
}

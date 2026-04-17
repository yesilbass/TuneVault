package com.example.tunevaultfx.chrome;

/**
 * Row markers and compact models for {@link TopBarSearchDropdown}.
 */
public final class SearchDropdownRows {

    private SearchDropdownRows() {}

    public static final Object CLEAR_SENTINEL = new Object();
    public static final Object NO_RESULTS_SENTINEL = new Object();

    public record ArtistHit(String name) {}
}

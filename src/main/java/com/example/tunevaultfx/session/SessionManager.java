package com.example.tunevaultfx.session;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SearchHistoryDAO;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.db.UserProfileDAO;
import com.example.tunevaultfx.search.SearchRecentItem;
import com.example.tunevaultfx.user.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.sql.SQLException;

/**
 * Manages shared session state for the currently logged-in user.
 *
 * Responsibilities:
 *  - Authenticate and start/end sessions.
 *  - Hold the loaded UserProfile (playlists, liked songs).
 *  - Cache the full song library so controllers don't hit the DB on every
 *    page load — library is loaded once in a background thread at login.
 *  - Track navigation helpers (selected song, artist, requested playlist).
 *  - Maintain the recent-search list with DB persistence.
 */
public final class SessionManager {

    // ── Session state ─────────────────────────────────────────────
    private static String      currentUsername;
    private static UserProfile currentUserProfile;
    private static String      requestedPlaylistToOpen;
    private static Song        selectedSong;
    private static String      selectedArtist;

    // ── Shared lists ──────────────────────────────────────────────
    private static final ObservableList<SearchRecentItem> recentSearches =
            FXCollections.observableArrayList();

    /** Full song library — loaded once in background at login. */
    private static volatile ObservableList<Song> songLibrary;

    // ── DAOs ──────────────────────────────────────────────────────
    private static final SearchHistoryDAO searchHistoryDAO = new SearchHistoryDAO();
    private static final UserProfileDAO   userProfileDAO   = new UserProfileDAO();

    private SessionManager() {}

    // ─────────────────────────────────────────────────────────────
    // Session lifecycle
    // ─────────────────────────────────────────────────────────────

    /**
     * Starts a new session for the given username.
     *
     * The user profile (playlists + liked songs) and recent searches are
     * loaded synchronously so they are available immediately when the main
     * menu opens. The full song library is loaded in the background so
     * search and recommendation features are ready within seconds without
     * blocking the UI.
     */
    public static void startSession(String username) {
        currentUsername         = username;
        selectedSong            = null;
        selectedArtist          = null;
        requestedPlaylistToOpen = null;
        songLibrary             = null;

        // ── 1. Load user profile (playlists + liked songs) ────────
        // Must be synchronous: every page depends on this being non-null.
        try {
            currentUserProfile = userProfileDAO.loadProfile(username);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback so the app doesn't crash — user gets an empty profile
            currentUserProfile = new UserProfile(username);
        }

        // ── 2. Load recent search history ─────────────────────────
        try {
            recentSearches.setAll(searchHistoryDAO.loadRecentSearches(username));
        } catch (Exception e) {
            e.printStackTrace();
            recentSearches.clear();
        }

        // ── 3. Pre-warm the song library in the background ────────
        // Controllers fall back to querying the DB directly if this isn't
        // ready yet, so there is no race-condition risk.
        Task<ObservableList<Song>> libraryTask = new Task<>() {
            @Override
            protected ObservableList<Song> call() throws SQLException {
                return new SongDAO().getAllSongs();
            }
        };
        libraryTask.setOnSucceeded(e -> songLibrary = libraryTask.getValue());
        libraryTask.setOnFailed(e ->
                libraryTask.getException().printStackTrace());

        Thread libraryThread = new Thread(libraryTask, "song-library-loader");
        libraryThread.setDaemon(true);
        libraryThread.start();
    }

    /**
     * Clears all session state.
     * Call this on logout before navigating back to the login page.
     */
    public static void logout() {
        currentUsername         = null;
        currentUserProfile      = null;
        requestedPlaylistToOpen = null;
        selectedSong            = null;
        selectedArtist          = null;
        songLibrary             = null;
        recentSearches.clear();
    }

    // ─────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static UserProfile getCurrentUserProfile() {
        return currentUserProfile;
    }

    /**
     * Returns the cached song library if it has finished loading,
     * otherwise returns an empty list. Controllers should fall back to
     * querying SongDAO directly when this returns empty and they need data
     * immediately (e.g. search page on fast first open).
     */
    public static ObservableList<Song> getSongLibrary() {
        return songLibrary != null ? songLibrary : FXCollections.observableArrayList();
    }

    /** Returns true once the background song library load has completed. */
    public static boolean isSongLibraryReady() {
        return songLibrary != null;
    }

    // ─────────────────────────────────────────────────────────────
    // Navigation helpers
    // ─────────────────────────────────────────────────────────────

    public static void requestPlaylistToOpen(String playlistName) {
        requestedPlaylistToOpen = playlistName;
    }

    /** Consumes and returns the requested playlist name (one-shot). */
    public static String consumeRequestedPlaylistToOpen() {
        String value = requestedPlaylistToOpen;
        requestedPlaylistToOpen = null;
        return value;
    }

    public static void setSelectedSong(Song song)     { selectedSong   = song;   }
    public static Song getSelectedSong()              { return selectedSong;      }

    public static void setSelectedArtist(String artist) { selectedArtist = artist; }
    public static String getSelectedArtist()            { return selectedArtist;   }

    // ─────────────────────────────────────────────────────────────
    // Recent searches
    // ─────────────────────────────────────────────────────────────

    public static ObservableList<SearchRecentItem> getRecentSearches() {
        return recentSearches;
    }

    public static void addRecentSearch(SearchRecentItem item) {
        if (item == null || currentUsername == null || currentUsername.isBlank()) return;

        // De-duplicate then prepend
        recentSearches.removeIf(existing -> existing.sameAs(item));
        recentSearches.add(0, item);

        // Cap at 20 entries
        while (recentSearches.size() > 20) {
            recentSearches.remove(recentSearches.size() - 1);
        }

        // Persist in background so UI isn't blocked
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                searchHistoryDAO.addRecentSearch(currentUsername, item);
                return null;
            }
        };
        saveTask.setOnFailed(e -> saveTask.getException().printStackTrace());
        Thread t = new Thread(saveTask, "search-history-save");
        t.setDaemon(true);
        t.start();
    }

    public static void clearRecentSearches() {
        recentSearches.clear();
        if (currentUsername == null || currentUsername.isBlank()) return;

        Task<Void> clearTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                searchHistoryDAO.clearRecentSearches(currentUsername);
                return null;
            }
        };
        clearTask.setOnFailed(e -> clearTask.getException().printStackTrace());
        Thread t = new Thread(clearTask, "search-history-clear");
        t.setDaemon(true);
        t.start();
    }

    /**
     * No-op — kept for binary compatibility.
     * All DB writes happen immediately through PlaylistService and
     * UserProfileDAO, so there is nothing to flush here.
     */
    public static void saveCurrentProfile() {}
}
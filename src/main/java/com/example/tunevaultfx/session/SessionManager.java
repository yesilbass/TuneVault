package com.example.tunevaultfx.session;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.UserProfileDAO;
import com.example.tunevaultfx.user.UserProfile;

/**
 * Manages the current logged-in user and shared session state.
 * Now loads and saves profile data from the database.
 */
public class SessionManager {

    private static String currentUsername;
    private static UserProfile currentUserProfile;
    private static String requestedPlaylistToOpen;
    private static Song selectedSong;

    private static final UserProfileDAO userProfileDAO = new UserProfileDAO();

    private SessionManager() {
    }

    public static void startSession(String username) {
        currentUsername = username;
        try {
            currentUserProfile = userProfileDAO.loadProfile(username);
        } catch (Exception e) {
            e.printStackTrace();
            currentUserProfile = new UserProfile(username);
        }
    }

    public static void logout() {
        currentUsername = null;
        currentUserProfile = null;
        requestedPlaylistToOpen = null;
        selectedSong = null;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static UserProfile getCurrentUserProfile() {
        return currentUserProfile;
    }

    public static void saveCurrentProfile() {
        // Intentionally left blank for playlist/song changes.
        // Playlist and liked-song updates now write directly to the database
        // through PlaylistService and UserProfileDAO.
    }

    public static void requestPlaylistToOpen(String playlistName) {
        requestedPlaylistToOpen = playlistName;
    }

    public static String consumeRequestedPlaylistToOpen() {
        String value = requestedPlaylistToOpen;
        requestedPlaylistToOpen = null;
        return value;
    }

    public static void setSelectedSong(Song song) {
        selectedSong = song;
    }

    public static Song getSelectedSong() {
        return selectedSong;
    }
}
package com.example.tunevaultfx.user;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.session.SessionManager;
import javafx.collections.ObservableList;

/**
 * Handles current-user song actions like likes and playlist additions.
 */
public class UserLibraryService {

    public void toggleLike(Song song) {
        if (song == null) {
            return;
        }

        UserProfile profile = SessionManager.getCurrentUserProfile();
        if (profile != null) {
            profile.toggleLike(song);
            SessionManager.saveCurrentProfile();
        }
    }

    public boolean isLiked(Song song) {
        UserProfile profile = SessionManager.getCurrentUserProfile();
        return profile != null && song != null && profile.isLiked(song);
    }

    public void addSongToPlaylist(String playlistName, Song song) {
        if (playlistName == null || playlistName.isBlank() || song == null) {
            return;
        }

        UserProfile profile = SessionManager.getCurrentUserProfile();
        if (profile == null) {
            return;
        }

        ObservableList<Song> playlist = profile.getPlaylists().get(playlistName);
        if (playlist != null && !playlist.contains(song)) {
            playlist.add(song);
            SessionManager.saveCurrentProfile();
        }
    }
}

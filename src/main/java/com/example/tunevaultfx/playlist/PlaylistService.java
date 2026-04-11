package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ListeningEventDAO;
import com.example.tunevaultfx.db.UserProfileDAO;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;

/**
 * Handles playlist operations such as creating, deleting,
 * adding songs, and removing songs.
 * Uses database-backed updates instead of rewriting the whole profile.
 */
public class PlaylistService {

    private final UserProfileDAO userProfileDAO = new UserProfileDAO();

    public boolean createPlaylist(UserProfile profile, String name) {
        if (profile == null || name == null || name.isBlank()) {
            return false;
        }

        if (profile.getPlaylists().containsKey(name)) {
            return false;
        }

        try {
            boolean created = userProfileDAO.createPlaylist(profile.getUsername(), name);
            if (created) {
                profile.getPlaylists().put(name, FXCollections.observableArrayList());
            }
            return created;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePlaylist(UserProfile profile, String playlistName) {
        if (profile == null || playlistName == null) {
            return false;
        }

        try {
            boolean deleted = userProfileDAO.deletePlaylist(profile.getUsername(), playlistName);
            if (deleted) {
                profile.getPlaylists().remove(playlistName);
            }
            return deleted;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addSongToPlaylist(UserProfile profile, String playlistName, Song song) {
        if (profile == null || playlistName == null || song == null) {
            return false;
        }

        ObservableList<Song> playlistSongs = profile.getPlaylists().get(playlistName);
        if (playlistSongs == null) {
            return false;
        }

        if (playlistSongs.contains(song)) {
            return false;
        }

        try {
            boolean added = userProfileDAO.addSongToPlaylist(profile.getUsername(), playlistName, song);
            if (added) {
                playlistSongs.add(song);
            }
            return added;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeSongFromPlaylist(UserProfile profile, String playlistName, Song song) {
        if (profile == null || playlistName == null || song == null) {
            return false;
        }

        ObservableList<Song> playlistSongs = profile.getPlaylists().get(playlistName);
        if (playlistSongs == null) {
            return false;
        }

        try {
            boolean removed = userProfileDAO.removeSongFromPlaylist(profile.getUsername(), playlistName, song);
            if (removed) {
                playlistSongs.remove(song);
            }
            return removed;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void toggleLikeSong(Song song) {
        UserProfile profile = SessionManager.getCurrentUserProfile();
        if (profile == null || song == null) {
            return;
        }

        boolean wasLiked = profile.isLiked(song);

        try {
            userProfileDAO.toggleLike(profile.getUsername(), song);
            profile.toggleLike(song);

            if (!wasLiked) {
                new ListeningEventDAO().recordLike(profile.getUsername(), song);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isProtectedPlaylist(String playlistName) {
        return "Liked Songs".equals(playlistName);
    }
}
package com.example.tunevaultfx.db;

import com.example.tunevaultfx.core.Song;

import java.sql.*;

/**
 * Handles song-in-playlist database operations only.
 * Does NOT create or delete playlists — that is PlaylistDAO's job.
 *
 * Extracted from UserProfileDAO to keep each class focused on one concern.
 */
public class PlaylistSongDAO {

    private final PlaylistDAO playlistDAO = new PlaylistDAO();

    // ── Package-private helper ─────────────────────────────────────

    boolean playlistContainsSong(int playlistId, int songId) throws SQLException {
        String sql =
                "SELECT 1 FROM playlist_song WHERE playlist_id = ? AND song_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, songId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ── Public operations ──────────────────────────────────────────

    /**
     * Adds a song to a named playlist. Returns false if already present.
     */
    public boolean addSong(String username, String playlistName, Song song)
            throws SQLException {
        if (song == null || song.songId() <= 0) return false;

        Integer userId = playlistDAO.findUserIdByUsername(username);
        if (userId == null) return false;

        Integer playlistId = playlistDAO.findPlaylistId(userId, playlistName);
        if (playlistId == null) return false;

        if (playlistContainsSong(playlistId, song.songId())) return false;

        String sql = "INSERT INTO playlist_song (playlist_id, song_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, song.songId());
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Removes a song from a named playlist.
     */
    public boolean removeSong(String username, String playlistName, Song song)
            throws SQLException {
        if (song == null || song.songId() <= 0) return false;

        Integer userId = playlistDAO.findUserIdByUsername(username);
        if (userId == null) return false;

        Integer playlistId = playlistDAO.findPlaylistId(userId, playlistName);
        if (playlistId == null) return false;

        String sql = "DELETE FROM playlist_song WHERE playlist_id = ? AND song_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, song.songId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Adds the song to Liked Songs if not present, removes it if it is.
     */
    public void toggleLike(String username, Song song) throws SQLException {
        if (song == null || song.songId() <= 0) return;

        Integer userId = playlistDAO.findUserIdByUsername(username);
        if (userId == null) return;

        playlistDAO.ensureLikedSongsPlaylistExists(userId);

        Integer likedSongsId = playlistDAO.findPlaylistId(userId, "Liked Songs");
        if (likedSongsId == null) return;

        if (playlistContainsSong(likedSongsId, song.songId())) {
            String sql = "DELETE FROM playlist_song WHERE playlist_id = ? AND song_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, likedSongsId);
                stmt.setInt(2, song.songId());
                stmt.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO playlist_song (playlist_id, song_id) VALUES (?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, likedSongsId);
                stmt.setInt(2, song.songId());
                stmt.executeUpdate();
            }
        }
    }
}
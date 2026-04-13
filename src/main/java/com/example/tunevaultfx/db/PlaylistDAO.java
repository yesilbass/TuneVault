package com.example.tunevaultfx.db;

import java.sql.*;

/**
 * Handles playlist-level database operations only.
 * Does NOT touch songs inside playlists — that is PlaylistSongDAO's job.
 *
 * Extracted from UserProfileDAO to keep each class focused on one concern.
 */
public class PlaylistDAO {

    // ── Package-private helpers ────────────────────────────────────
    // These are used by PlaylistSongDAO and UserProfileDAO in the same package.

    Integer findUserIdByUsername(String username) throws SQLException {
        String sql = "SELECT user_id FROM app_user WHERE username = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : null;
            }
        }
    }

    Integer findPlaylistId(int userId, String playlistName) throws SQLException {
        String sql = "SELECT playlist_id FROM playlist WHERE user_id = ? AND name = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, playlistName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("playlist_id") : null;
            }
        }
    }

    boolean playlistExists(int userId, String playlistName) throws SQLException {
        return findPlaylistId(userId, playlistName) != null;
    }

    boolean isSystemPlaylist(int playlistId) throws SQLException {
        String sql = "SELECT is_system_playlist FROM playlist WHERE playlist_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("is_system_playlist");
            }
        }
    }

    // ── Public operations ──────────────────────────────────────────

    /**
     * Ensures Liked Songs exists for the user. Safe to call multiple times.
     */
    public void ensureLikedSongsPlaylistExists(int userId) throws SQLException {
        String insertSql = """
                INSERT INTO playlist (user_id, name, is_system_playlist)
                SELECT ?, 'Liked Songs', TRUE
                WHERE NOT EXISTS (
                    SELECT 1 FROM playlist WHERE user_id = ? AND name = 'Liked Songs'
                )
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }

        String updateSql =
                "UPDATE playlist SET is_system_playlist = TRUE " +
                        "WHERE user_id = ? AND name = 'Liked Songs'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Creates a new user playlist. Returns false if it already exists.
     */
    public boolean createPlaylist(String username, String playlistName) throws SQLException {
        Integer userId = findUserIdByUsername(username);
        if (userId == null || playlistName == null || playlistName.isBlank()) return false;
        if (playlistExists(userId, playlistName)) return false;

        String sql =
                "INSERT INTO playlist (user_id, name, is_system_playlist) VALUES (?, ?, FALSE)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, playlistName);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a playlist and all its songs. Refuses to delete system playlists.
     */
    public boolean deletePlaylist(String username, String playlistName) throws SQLException {
        Integer userId = findUserIdByUsername(username);
        if (userId == null) return false;

        Integer playlistId = findPlaylistId(userId, playlistName);
        if (playlistId == null) return false;

        if (isSystemPlaylist(playlistId)) return false;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM playlist_song WHERE playlist_id = ?")) {
                    stmt.setInt(1, playlistId);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM playlist WHERE playlist_id = ?")) {
                    stmt.setInt(1, playlistId);
                    int deleted = stmt.executeUpdate();
                    conn.commit();
                    return deleted == 1;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
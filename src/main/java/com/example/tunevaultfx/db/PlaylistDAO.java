package com.example.tunevaultfx.db;

import com.example.tunevaultfx.core.PlaylistNames;
import com.example.tunevaultfx.core.Song;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "SELECT user_id FROM app_user WHERE LOWER(username) = LOWER(?) LIMIT 1";
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
        String insertSql =
                """
                INSERT INTO playlist (user_id, name, is_system_playlist)
                SELECT ?, ?, TRUE
                WHERE NOT EXISTS (
                    SELECT 1 FROM playlist WHERE user_id = ? AND name = ?
                )
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, PlaylistNames.LIKED_SONGS);
            stmt.setInt(3, userId);
            stmt.setString(4, PlaylistNames.LIKED_SONGS);
            stmt.executeUpdate();
        }

        String updateSql =
                "UPDATE playlist SET is_system_playlist = TRUE WHERE user_id = ? AND name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, PlaylistNames.LIKED_SONGS);
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
        if (PlaylistNames.isLikedSongs(playlistName)) {
            return false;
        }
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

    public List<String> loadPinnedPlaylistNames(int userId) throws SQLException {
        List<String> out = new ArrayList<>();
        String sql =
                "SELECT name FROM playlist WHERE user_id = ? AND pin_order IS NOT NULL ORDER BY pin_order";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String n = rs.getString("name");
                    if (n != null) {
                        out.add(n);
                    }
                }
            }
        }
        return out;
    }

    /**
     * Sets pin_order 1..n for the given playlist names (max 3), including system playlists such as
     * {@link PlaylistNames#LIKED_SONGS}. Clears pins for all other playlists for this user.
     */
    public void replaceUserPlaylistPins(int userId, List<String> orderedPinnedNames) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement clear =
                        conn.prepareStatement("UPDATE playlist SET pin_order = NULL WHERE user_id = ?")) {
                    clear.setInt(1, userId);
                    clear.executeUpdate();
                }
                int slot = 1;
                if (orderedPinnedNames != null) {
                    for (String name : orderedPinnedNames) {
                        if (name == null
                                || name.isBlank()
                                || slot > PlaylistNames.MAX_USER_PINNED_PLAYLISTS) {
                            continue;
                        }
                        try (PreparedStatement upd =
                                conn.prepareStatement(
                                        "UPDATE playlist SET pin_order = ? WHERE user_id = ? AND name = ?")) {
                            upd.setInt(1, slot);
                            upd.setInt(2, userId);
                            upd.setString(3, name);
                            if (upd.executeUpdate() == 1) {
                                slot++;
                            }
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean renamePlaylist(String username, String oldName, String newName) throws SQLException {
        if (oldName == null || newName == null) {
            return false;
        }
        String o = oldName.trim();
        String n = newName.trim();
        if (o.isEmpty() || n.isEmpty()) {
            return false;
        }
        if (PlaylistNames.isLikedSongs(o) || PlaylistNames.isLikedSongs(n)) {
            return false;
        }
        if (o.equals(n)) {
            return true;
        }

        Integer userId = findUserIdByUsername(username);
        if (userId == null) {
            return false;
        }
        if (!playlistExists(userId, o) || playlistExists(userId, n)) {
            return false;
        }

        Integer playlistId = findPlaylistId(userId, o);
        if (playlistId == null || isSystemPlaylist(playlistId)) {
            return false;
        }

        String sql = "UPDATE playlist SET name = ? WHERE playlist_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, n);
            stmt.setInt(2, playlistId);
            return stmt.executeUpdate() == 1;
        }
    }

    /** Non-system playlists only; Liked Songs cannot be public. */
    public boolean setPlaylistPublic(String username, String playlistName, boolean isPublic) throws SQLException {
        if (playlistName == null || PlaylistNames.isLikedSongs(playlistName)) {
            return false;
        }
        Integer userId = findUserIdByUsername(username);
        if (userId == null) {
            return false;
        }
        Integer playlistId = findPlaylistId(userId, playlistName.trim());
        if (playlistId == null || isSystemPlaylist(playlistId)) {
            return false;
        }
        String sql = "UPDATE playlist SET is_public = ? WHERE playlist_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isPublic);
            stmt.setInt(2, playlistId);
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean isPlaylistPublic(String username, String playlistName) throws SQLException {
        if (playlistName == null || playlistName.isBlank()) {
            return false;
        }
        Integer userId = findUserIdByUsername(username);
        if (userId == null) {
            return false;
        }
        Integer playlistId = findPlaylistId(userId, playlistName.trim());
        if (playlistId == null || isSystemPlaylist(playlistId)) {
            return false;
        }
        String sql = "SELECT is_public FROM playlist WHERE playlist_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("is_public");
            }
        }
    }

    public List<String> listPublicPlaylistNamesForUser(String ownerUsername) throws SQLException {
        Integer userId = findUserIdByUsername(ownerUsername == null ? "" : ownerUsername.trim());
        if (userId == null) {
            return List.of();
        }
        String sql =
                """
                SELECT name FROM playlist
                WHERE user_id = ? AND is_public = TRUE AND is_system_playlist = FALSE
                ORDER BY name ASC
                """;
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    String n = rs.getString("name");
                    if (n != null && !n.isBlank()) {
                        out.add(n);
                    }
                }
                return out;
            }
        }
    }

    public List<PublicPlaylistSearchRow> searchPublicPlaylists(String rawQuery, int limit) throws SQLException {
        if (rawQuery == null || rawQuery.isBlank()) {
            return List.of();
        }
        String needle = rawQuery.trim().replace("%", "").replace("_", "");
        if (needle.isBlank()) {
            return List.of();
        }
        int lim = Math.max(1, Math.min(limit, 50));
        String sql =
                """
                SELECT u.username AS owner_name, p.name AS playlist_name,
                       COUNT(ps.song_id) AS track_count
                FROM playlist p
                JOIN app_user u ON u.user_id = p.user_id
                LEFT JOIN playlist_song ps ON ps.playlist_id = p.playlist_id
                WHERE p.is_public = TRUE AND p.is_system_playlist = FALSE
                  AND LOWER(p.name) LIKE LOWER(CONCAT(?, '%'))
                GROUP BY p.playlist_id, u.username, p.name
                ORDER BY p.name ASC
                LIMIT """
                        + lim;
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, needle);
            try (ResultSet rs = stmt.executeQuery()) {
                List<PublicPlaylistSearchRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(
                            new PublicPlaylistSearchRow(
                                    rs.getString("owner_name"),
                                    rs.getString("playlist_name"),
                                    rs.getInt("track_count")));
                }
                return out;
            }
        }
    }

    /**
     * Songs in a playlist only if it is {@code is_public} and not a system playlist.
     */
    public List<Song> loadPublicPlaylistSongs(String ownerUsername, String playlistName) throws SQLException {
        Integer userId = findUserIdByUsername(ownerUsername == null ? "" : ownerUsername.trim());
        if (userId == null || playlistName == null || playlistName.isBlank()) {
            return List.of();
        }
        Integer playlistId = findPlaylistId(userId, playlistName.trim());
        if (playlistId == null) {
            return List.of();
        }
        String check =
                "SELECT is_public, is_system_playlist FROM playlist WHERE playlist_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement cstmt = conn.prepareStatement(check)) {
            cstmt.setInt(1, playlistId);
            try (ResultSet crs = cstmt.executeQuery()) {
                if (!crs.next() || !crs.getBoolean("is_public") || crs.getBoolean("is_system_playlist")) {
                    return List.of();
                }
            }
        }

        String sql =
                """
                SELECT s.song_id,
                       s.title,
                       COALESCE(a.name, '') AS artist_name,
                       '' AS album_name,
                       COALESCE(g.genre_name, '') AS genre_name,
                       COALESCE(s.duration_seconds, 0) AS duration_seconds
                FROM playlist_song ps
                JOIN song s ON s.song_id = ps.song_id
                LEFT JOIN artist a ON a.artist_id = s.artist_id
                LEFT JOIN genre g ON g.genre_id = s.genre_id
                WHERE ps.playlist_id = ?
                ORDER BY s.title ASC, s.song_id ASC
                """;
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Song> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(
                            new Song(
                                    rs.getInt("song_id"),
                                    rs.getString("title"),
                                    rs.getString("artist_name"),
                                    rs.getString("album_name"),
                                    rs.getString("genre_name"),
                                    rs.getInt("duration_seconds")));
                }
                return out;
            }
        }
    }
}
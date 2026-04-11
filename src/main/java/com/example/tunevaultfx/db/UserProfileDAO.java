package com.example.tunevaultfx.db;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.user.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads and updates a user's music data from the database.
 * Uses targeted database operations instead of deleting and recreating everything.
 */
public class UserProfileDAO {

    public UserProfile loadProfile(String username) throws SQLException {
        Integer userId = findUserIdByUsername(username);
        if (userId == null) {
            return new UserProfile(username);
        }

        UserProfile profile = new UserProfile(username);
        profile.getPlaylists().clear();

        ensureLikedSongsPlaylistExists(userId);

        String sql = """
                SELECT p.playlist_id,
                       p.name AS playlist_name,
                       p.is_system_playlist,
                       s.song_id,
                       s.title,
                       COALESCE(a.name, '') AS artist_name,
                       '' AS album_name,
                       COALESCE(s.duration_seconds, 0) AS duration_seconds
                FROM playlist p
                LEFT JOIN playlist_song ps ON ps.playlist_id = p.playlist_id
                LEFT JOIN song s ON s.song_id = ps.song_id
                LEFT JOIN artist a ON a.artist_id = s.artist_id
                WHERE p.user_id = ?
                ORDER BY p.is_system_playlist DESC, p.playlist_id, ps.song_id
                """;

        Map<String, ObservableList<Song>> loadedPlaylists = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String playlistName = rs.getString("playlist_name");
                    loadedPlaylists.computeIfAbsent(playlistName, k -> FXCollections.observableArrayList());

                    Object songIdObj = rs.getObject("song_id");
                    if (songIdObj != null) {
                        loadedPlaylists.get(playlistName).add(new Song(
                                rs.getInt("song_id"),
                                rs.getString("title"),
                                rs.getString("artist_name"),
                                rs.getString("album_name"),
                                rs.getInt("duration_seconds")
                        ));
                    }
                }
            }
        }

        if (!loadedPlaylists.containsKey("Liked Songs")) {
            loadedPlaylists.put("Liked Songs", FXCollections.observableArrayList());
        }

        profile.getPlaylists().putAll(loadedPlaylists);
        return profile;
    }

    public boolean createPlaylist(String username, String playlistName) throws SQLException {
        Integer userId = findUserIdByUsername(username);
        if (userId == null || playlistName == null || playlistName.isBlank()) {
            return false;
        }

        if (playlistExists(userId, playlistName)) {
            return false;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO playlist (user_id, name, is_system_playlist) VALUES (?, ?, FALSE)")) {
            stmt.setInt(1, userId);
            stmt.setString(2, playlistName);
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean deletePlaylist(String username, String playlistName) throws SQLException {
        Integer userId = findUserIdByUsername(username);
        if (userId == null) {
            return false;
        }

        Integer playlistId = findPlaylistId(userId, playlistName);
        if (playlistId == null) {
            return false;
        }

        if (isSystemPlaylist(playlistId)) {
            return false;
        }

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

    public boolean addSongToPlaylist(String username, String playlistName, Song song) throws SQLException {
        Integer userId = findUserIdByUsername(username);
        if (userId == null || song == null || song.songId() <= 0) {
            return false;
        }

        Integer playlistId = findPlaylistId(userId, playlistName);
        if (playlistId == null) {
            return false;
        }

        if (playlistContainsSong(playlistId, song.songId())) {
            return false;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO playlist_song (playlist_id, song_id) VALUES (?, ?)")) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, song.songId());
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean removeSongFromPlaylist(String username, String playlistName, Song song) throws SQLException {
        Integer userId = findUserIdByUsername(username);
        if (userId == null || song == null || song.songId() <= 0) {
            return false;
        }

        Integer playlistId = findPlaylistId(userId, playlistName);
        if (playlistId == null) {
            return false;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM playlist_song WHERE playlist_id = ? AND song_id = ?")) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, song.songId());
            return stmt.executeUpdate() > 0;
        }
    }

    public void toggleLike(String username, Song song) throws SQLException {
        if (song == null || song.songId() <= 0) {
            return;
        }

        Integer userId = findUserIdByUsername(username);
        if (userId == null) {
            return;
        }

        ensureLikedSongsPlaylistExists(userId);
        Integer likedSongsId = findPlaylistId(userId, "Liked Songs");
        if (likedSongsId == null) {
            return;
        }

        if (playlistContainsSong(likedSongsId, song.songId())) {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM playlist_song WHERE playlist_id = ? AND song_id = ?")) {
                stmt.setInt(1, likedSongsId);
                stmt.setInt(2, song.songId());
                stmt.executeUpdate();
            }
        } else {
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO playlist_song (playlist_id, song_id) VALUES (?, ?)")) {
                stmt.setInt(1, likedSongsId);
                stmt.setInt(2, song.songId());
                stmt.executeUpdate();
            }
        }
    }

    private void ensureLikedSongsPlaylistExists(int userId) throws SQLException {
        String sql = """
                INSERT INTO playlist (user_id, name, is_system_playlist)
                SELECT ?, 'Liked Songs', TRUE
                WHERE NOT EXISTS (
                    SELECT 1 FROM playlist WHERE user_id = ? AND name = 'Liked Songs'
                )
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE playlist SET is_system_playlist = TRUE WHERE user_id = ? AND name = 'Liked Songs'")) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private Integer findUserIdByUsername(String username) throws SQLException {
        String sql = "SELECT user_id FROM app_user WHERE username = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : null;
            }
        }
    }

    private Integer findPlaylistId(int userId, String playlistName) throws SQLException {
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

    private boolean playlistExists(int userId, String playlistName) throws SQLException {
        return findPlaylistId(userId, playlistName) != null;
    }

    private boolean playlistContainsSong(int playlistId, int songId) throws SQLException {
        String sql = "SELECT 1 FROM playlist_song WHERE playlist_id = ? AND song_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            stmt.setInt(2, songId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isSystemPlaylist(int playlistId) throws SQLException {
        String sql = "SELECT is_system_playlist FROM playlist WHERE playlist_id = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("is_system_playlist");
            }
        }
    }
}
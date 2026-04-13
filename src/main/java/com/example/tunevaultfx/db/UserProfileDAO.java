package com.example.tunevaultfx.db;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.user.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a user's full profile from the database and delegates all
 * playlist / song mutations to PlaylistDAO and PlaylistSongDAO.
 *
 * Reduced from 300 lines to ~100 lines by extracting two focused DAOs.
 */
public class UserProfileDAO {

    private final PlaylistDAO    playlistDAO    = new PlaylistDAO();
    private final PlaylistSongDAO playlistSongDAO = new PlaylistSongDAO();

    // ── Profile loading ────────────────────────────────────────────

    /**
     * Loads the complete UserProfile (all playlists + songs) for a user.
     * Guaranteed to always contain "Liked Songs", even if empty.
     */
    public UserProfile loadProfile(String username) throws SQLException {
        Integer userId = playlistDAO.findUserIdByUsername(username);
        if (userId == null) return new UserProfile(username);

        playlistDAO.ensureLikedSongsPlaylistExists(userId);

        String sql = """
                SELECT p.name AS playlist_name,
                       s.song_id,
                       s.title,
                       COALESCE(a.name, '')           AS artist_name,
                       ''                             AS album_name,
                       COALESCE(g.genre_name, '')     AS genre_name,
                       COALESCE(s.duration_seconds,0) AS duration_seconds
                FROM playlist p
                LEFT JOIN playlist_song ps ON ps.playlist_id = p.playlist_id
                LEFT JOIN song          s  ON s.song_id      = ps.song_id
                LEFT JOIN artist        a  ON a.artist_id    = s.artist_id
                LEFT JOIN genre         g  ON g.genre_id     = s.genre_id
                WHERE p.user_id = ?
                ORDER BY p.is_system_playlist DESC, p.playlist_id, ps.song_id
                """;

        Map<String, ObservableList<Song>> loaded = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("playlist_name");
                    loaded.computeIfAbsent(name, k -> FXCollections.observableArrayList());

                    if (rs.getObject("song_id") != null) {
                        loaded.get(name).add(new Song(
                                rs.getInt("song_id"),
                                rs.getString("title"),
                                rs.getString("artist_name"),
                                rs.getString("album_name"),
                                rs.getString("genre_name"),
                                rs.getInt("duration_seconds")));
                    }
                }
            }
        }

        // Always guarantee Liked Songs is present
        loaded.putIfAbsent("Liked Songs", FXCollections.observableArrayList());

        UserProfile profile = new UserProfile(username);
        profile.getPlaylists().clear();
        profile.getPlaylists().putAll(loaded);
        return profile;
    }

    // ── Delegating operations ──────────────────────────────────────
    // Public API stays identical to before — callers (PlaylistService etc.)
    // do not need to change.

    public boolean createPlaylist(String username, String playlistName) throws SQLException {
        return playlistDAO.createPlaylist(username, playlistName);
    }

    public boolean deletePlaylist(String username, String playlistName) throws SQLException {
        return playlistDAO.deletePlaylist(username, playlistName);
    }

    public boolean addSongToPlaylist(String username, String playlistName, Song song)
            throws SQLException {
        return playlistSongDAO.addSong(username, playlistName, song);
    }

    public boolean removeSongFromPlaylist(String username, String playlistName, Song song)
            throws SQLException {
        return playlistSongDAO.removeSong(username, playlistName, song);
    }

    public void toggleLike(String username, Song song) throws SQLException {
        playlistSongDAO.toggleLike(username, song);
    }
}
package com.example.tunevaultfx.db;

import com.example.tunevaultfx.core.Song;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Saves and updates listening analytics in real time.
 */
public class ListeningEventDAO {

    public Integer startListeningSession(String username, Song song) {
        if (username == null || username.isBlank() || song == null || song.songId() <= 0) {
            return null;
        }

        try {
            Integer userId = findUserIdByUsername(username);
            if (userId == null) {
                return null;
            }

            int duration = Math.max(song.durationSeconds(), 0);

            String sql = """
                INSERT INTO listening_event (
                    user_id,
                    song_id,
                    action_type,
                    played_seconds,
                    song_duration_seconds,
                    completion_ratio,
                    count_as_play
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, song.songId());
                stmt.setString(3, "PLAY");
                stmt.setInt(4, 0);
                stmt.setInt(5, duration);
                stmt.setDouble(6, 0.0);
                stmt.setBoolean(7, false);
                stmt.executeUpdate();

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void updateListeningSession(Integer eventId,
                                       int playedSeconds,
                                       int songDurationSeconds,
                                       double completionRatio,
                                       boolean countAsPlay) {
        if (eventId == null) {
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE listening_event SET played_seconds = ?, song_duration_seconds = ?, completion_ratio = ?, count_as_play = ? WHERE event_id = ?")) {
            stmt.setInt(1, Math.max(0, playedSeconds));
            stmt.setInt(2, Math.max(0, songDurationSeconds));
            stmt.setDouble(3, Math.max(0.0, Math.min(1.0, completionRatio)));
            stmt.setBoolean(4, countAsPlay);
            stmt.setInt(5, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void finalizeListeningSession(Integer eventId,
                                         String actionType,
                                         int playedSeconds,
                                         int songDurationSeconds,
                                         double completionRatio,
                                         boolean countAsPlay) {
        if (eventId == null) {
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE listening_event SET action_type = ?, played_seconds = ?, song_duration_seconds = ?, completion_ratio = ?, count_as_play = ? WHERE event_id = ?")) {
            stmt.setString(1, actionType);
            stmt.setInt(2, Math.max(0, playedSeconds));
            stmt.setInt(3, Math.max(0, songDurationSeconds));
            stmt.setDouble(4, Math.max(0.0, Math.min(1.0, completionRatio)));
            stmt.setBoolean(5, countAsPlay);
            stmt.setInt(6, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void recordLike(String username, Song song) {
        recordSimpleInteraction(username, song, "LIKE");
    }

    public void recordUnlike(String username, Song song) {
        recordSimpleInteraction(username, song, "UNLIKE");
    }

    public void recordPlaylistAdd(String username, Song song) {
        recordSimpleInteraction(username, song, "PLAYLIST_ADD");
    }

    public void recordPlaylistRemove(String username, Song song) {
        recordSimpleInteraction(username, song, "PLAYLIST_REMOVE");
    }

    /**
     * Aggregate listening totals for profile / Wrapped-style UI.
     */
    public Optional<ListeningProfileStats> loadListeningProfileStats(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN le.count_as_play THEN 1 ELSE 0 END), 0) AS play_count,
                       COALESCE(SUM(le.played_seconds), 0) AS listened_seconds
                FROM app_user u
                LEFT JOIN listening_event le ON le.user_id = u.user_id
                WHERE u.username = ?
                GROUP BY u.user_id
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(
                        new ListeningProfileStats(
                                rs.getInt("play_count"), rs.getLong("listened_seconds")));
            }
        }
    }

    public List<UserBehaviorEvent> getUserBehaviorEvents(String username) {
        List<UserBehaviorEvent> events = new ArrayList<>();

        if (username == null || username.isBlank()) {
            return events;
        }

        String sql = """
                SELECT s.song_id,
                       COALESCE(a.name, '') AS artist_name,
                       COALESCE(g.genre_name, '') AS genre_name,
                       le.action_type,
                       COALESCE(le.completion_ratio, 0.0) AS completion_ratio
                FROM listening_event le
                JOIN app_user u ON u.user_id = le.user_id
                JOIN song s ON s.song_id = le.song_id
                LEFT JOIN artist a ON a.artist_id = s.artist_id
                LEFT JOIN genre g ON g.genre_id = s.genre_id
                WHERE u.username = ?
                ORDER BY le.event_timestamp DESC, le.event_id DESC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    events.add(new UserBehaviorEvent(
                            rs.getInt("song_id"),
                            rs.getString("artist_name"),
                            rs.getString("genre_name"),
                            rs.getString("action_type"),
                            rs.getDouble("completion_ratio")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return events;
    }

    private void recordSimpleInteraction(String username, Song song, String actionType) {
        if (username == null || username.isBlank() || song == null || song.songId() <= 0) {
            return;
        }

        try {
            Integer userId = findUserIdByUsername(username);
            if (userId == null) {
                return;
            }

            int duration = Math.max(song.durationSeconds(), 0);

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO listening_event (user_id, song_id, action_type, played_seconds, song_duration_seconds, completion_ratio, count_as_play) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setInt(1, userId);
                stmt.setInt(2, song.songId());
                stmt.setString(3, actionType);
                stmt.setInt(4, 0);
                stmt.setInt(5, duration);
                stmt.setDouble(6, 0.0);
                stmt.setBoolean(7, false);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    public record UserBehaviorEvent(
            int songId,
            String artistName,
            String genreName,
            String actionType,
            double completionRatio
    ) {
    }

    public record ListeningProfileStats(int countedPlays, long listenedSeconds) {}
}
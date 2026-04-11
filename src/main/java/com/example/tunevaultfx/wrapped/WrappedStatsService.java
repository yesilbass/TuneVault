package com.example.tunevaultfx.wrapped;

import com.example.tunevaultfx.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reads Wrapped analytics values from the database.
 * Daily and overall Wrapped are based on actual listened seconds.
 */
public class WrappedStatsService {

    public WrappedStats loadStatsForUsername(String username, StatsRange range) {
        if (username == null || username.isBlank()) {
            return WrappedStats.empty();
        }

        try {
            Integer userId = findUserIdByUsername(username);
            if (userId == null) {
                return WrappedStats.empty();
            }

            StatValue topSong = queryTopSong(userId, range);
            StatValue topArtist = queryTopArtist(userId, range);
            StatValue favoriteGenre = queryFavoriteGenre(userId, range);
            int totalSeconds = queryTotalListeningSeconds(userId, range);

            if (topSong.seconds == 0 && topArtist.seconds == 0 && favoriteGenre.seconds == 0 && totalSeconds == 0) {
                return emptyForRange(range);
            }

            String prefix = range == StatsRange.DAILY ? "Today" : "Overall";

            String summary = prefix
                    + " top song: " + topSong.label
                    + ". Top artist: " + topArtist.label
                    + ". Favorite genre: " + favoriteGenre.label
                    + ". Total listening time: " + formatDuration(totalSeconds) + ".";

            return new WrappedStats(
                    topSong.label,
                    topSong.seconds,
                    topArtist.label,
                    topArtist.seconds,
                    favoriteGenre.label,
                    favoriteGenre.seconds,
                    totalSeconds,
                    summary
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return emptyForRange(range);
        }
    }

    private WrappedStats emptyForRange(StatsRange range) {
        if (range == StatsRange.DAILY) {
            return new WrappedStats(
                    "No listening data today",
                    0,
                    "No listening data today",
                    0,
                    "No listening data today",
                    0,
                    0,
                    "No listening data for today yet. Play some songs to build today’s Wrapped."
            );
        }

        return WrappedStats.empty();
    }

    private Integer findUserIdByUsername(String username) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT user_id FROM app_user WHERE username = ? LIMIT 1")) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : null;
            }
        }
    }

    private StatValue queryTopSong(int userId, StatsRange range) throws SQLException {
        String sql = """
                SELECT s.title AS label, COALESCE(SUM(le.played_seconds), 0) AS total_seconds
                FROM listening_event le
                JOIN song s ON s.song_id = le.song_id
                WHERE le.user_id = ?
                """ + dateFilter(range) + """
                GROUP BY s.song_id, s.title
                ORDER BY total_seconds DESC, s.title ASC
                LIMIT 1
                """;

        return querySingleStat(userId, range, sql);
    }

    private StatValue queryTopArtist(int userId, StatsRange range) throws SQLException {
        String sql = """
                SELECT a.name AS label, COALESCE(SUM(le.played_seconds), 0) AS total_seconds
                FROM listening_event le
                JOIN song s ON s.song_id = le.song_id
                JOIN artist a ON a.artist_id = s.artist_id
                WHERE le.user_id = ?
                """ + dateFilter(range) + """
                GROUP BY a.artist_id, a.name
                ORDER BY total_seconds DESC, a.name ASC
                LIMIT 1
                """;

        return querySingleStat(userId, range, sql);
    }

    private StatValue queryFavoriteGenre(int userId, StatsRange range) throws SQLException {
        String sql = """
                SELECT g.genre_name AS label, COALESCE(SUM(le.played_seconds), 0) AS total_seconds
                FROM listening_event le
                JOIN song s ON s.song_id = le.song_id
                JOIN genre g ON g.genre_id = s.genre_id
                WHERE le.user_id = ?
                """ + dateFilter(range) + """
                GROUP BY g.genre_id, g.genre_name
                ORDER BY total_seconds DESC, g.genre_name ASC
                LIMIT 1
                """;

        return querySingleStat(userId, range, sql);
    }

    private int queryTotalListeningSeconds(int userId, StatsRange range) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(le.played_seconds), 0) AS total_seconds
                FROM listening_event le
                WHERE le.user_id = ?
                """ + dateFilter(range);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_seconds");
                }
                return 0;
            }
        }
    }

    private StatValue querySingleStat(int userId, StatsRange range, String sql) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StatValue(
                            rs.getString("label"),
                            rs.getInt("total_seconds")
                    );
                }

                String emptyLabel = range == StatsRange.DAILY
                        ? "No listening data today"
                        : "No listening data yet";

                return new StatValue(emptyLabel, 0);
            }
        }
    }

    private String dateFilter(StatsRange range) {
        if (range == StatsRange.DAILY) {
            return "\n AND DATE(le.event_timestamp) = CURRENT_DATE ";
        }
        return "";
    }

    private String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + ":" + String.format("%02d:%02d", minutes, seconds);
        }
        return minutes + ":" + String.format("%02d", seconds);
    }

    private record StatValue(String label, int seconds) {
    }
}
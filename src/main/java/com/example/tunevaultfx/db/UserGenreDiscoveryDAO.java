package com.example.tunevaultfx.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persists the Find Your Genre quiz outcome and supplies normalized genre boosts
 * for {@link com.example.tunevaultfx.recommendation.RecommendationEngine}.
 *
 * <p>Each quiz completion <em>merges</em> into the existing weights rather than
 * overwriting them, so repeated quiz sessions stack up and produce a richer,
 * more reliable genre profile over time. The reset button in Settings is the
 * only thing that wipes the profile clean.</p>
 *
 * <p>{@code quiz_session_count} is incremented on every save so the next quiz
 * session serves a fresh set of questions (cycling through 5 sessions).</p>
 */
public final class UserGenreDiscoveryDAO {

    public Map<String, Double> loadBoostWeights(String username) throws SQLException {
        Integer userId = findUserId(username);
        if (userId == null) {
            return Map.of();
        }
        String sql = """
                SELECT weights_boost FROM user_genre_discovery WHERE user_id = ? LIMIT 1
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Map.of();
                }
                return parseWeights(rs.getString("weights_boost"));
            }
        }
    }

    public boolean hasSavedProfile(String username) throws SQLException {
        return loadSummary(username).isPresent();
    }

    /**
     * Loads saved quiz labels for home / settings UI (not the numeric boosts).
     */
    public Optional<UserGenreDiscoverySummary> loadSummary(String username) throws SQLException {
        Integer userId = findUserId(username);
        if (userId == null) {
            return Optional.empty();
        }
        try {
            return loadSummaryFull(userId);
        } catch (SQLException e) {
            if (isUnknownColumnError(e, "quiz_mode")) {
                return loadSummaryWithoutQuizMode(userId);
            }
            throw e;
        }
    }

    /**
     * Merges new quiz boosts into the user's existing genre profile and increments
     * {@code quiz_session_count} so the next quiz serves a different question set.
     *
     * <p>Merging strategy: existing weights and new weights are summed per genre,
     * then the combined map is re-normalized to [0, 1]. This means each completed
     * quiz shifts the profile proportionally — early sessions have high influence
     * but are gradually refined by later ones.</p>
     *
     * @param mode            QUICK or FULL (stored for analytics / UI)
     * @param topGenre        display label for the top result
     * @param secondGenre     display label for the second result (nullable)
     * @param thirdGenre      display label for the third result (nullable)
     * @param normalizedBoosts genre key → normalized boost value from this session
     */
    public void save(String username,
                     String mode,
                     String topGenre,
                     String secondGenre,
                     String thirdGenre,
                     Map<String, Double> normalizedBoosts) throws SQLException {
        Integer userId = findUserId(username);
        if (userId == null) {
            throw new SQLException("Unknown user: " + username);
        }

        String m = mode == null || mode.isBlank() ? "FULL" : mode.trim().toUpperCase();

        // Load existing weights to merge with
        Map<String, Double> existing = loadBoostWeightsForUser(userId);
        Map<String, Double> merged   = mergeWeights(existing, normalizedBoosts);
        String serialized            = serializeWeights(merged);

        String sql = """
                INSERT INTO user_genre_discovery
                    (user_id, top_genre, second_genre, third_genre, quiz_mode, weights_boost, quiz_session_count)
                VALUES (?, ?, ?, ?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE
                    top_genre          = VALUES(top_genre),
                    second_genre       = VALUES(second_genre),
                    third_genre        = VALUES(third_genre),
                    quiz_mode          = VALUES(quiz_mode),
                    weights_boost      = VALUES(weights_boost),
                    quiz_session_count = quiz_session_count + 1
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, topGenre == null ? "" : topGenre);
            stmt.setString(3, secondGenre);
            stmt.setString(4, thirdGenre);
            stmt.setString(5, m);
            stmt.setString(6, serialized);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes the saved Find Your Genre profile for this user, resetting both
     * the genre weights and the session counter back to zero.
     * Does not touch listening events, playlists, or any other profile data.
     *
     * @return true if a row was removed
     */
    public boolean deleteForUser(String username) throws SQLException {
        Integer userId = findUserId(username);
        if (userId == null) {
            return false;
        }
        String sql = "DELETE FROM user_genre_discovery WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Merges two weight maps by summing values per genre key, then re-normalizing
     * the result to a [0, 1] scale so the combined map stays consistent.
     */
    private static Map<String, Double> mergeWeights(Map<String, Double> existing,
                                                    Map<String, Double> incoming) {
        Map<String, Double> merged = new LinkedHashMap<>(existing);
        for (var e : incoming.entrySet()) {
            String k = e.getKey() == null ? "" : e.getKey().trim().toLowerCase();
            if (!k.isEmpty() && e.getValue() != null && e.getValue() > 0) {
                merged.merge(k, e.getValue(), Double::sum);
            }
        }
        // Re-normalize so values stay in [0, 1]
        double max = merged.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if (max > 0.0) {
            merged.replaceAll((k, v) -> v / max);
        }
        return merged;
    }

    private Map<String, Double> loadBoostWeightsForUser(int userId) throws SQLException {
        String sql = "SELECT weights_boost FROM user_genre_discovery WHERE user_id = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Map.of();
                }
                return parseWeights(rs.getString("weights_boost"));
            }
        }
    }

    private Optional<UserGenreDiscoverySummary> loadSummaryFull(int userId) throws SQLException {
        String sql = """
                SELECT top_genre, second_genre, third_genre, quiz_mode
                FROM user_genre_discovery
                WHERE user_id = ?
                LIMIT 1
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return mapSummaryRow(rs, true);
            }
        }
    }

    private Optional<UserGenreDiscoverySummary> loadSummaryWithoutQuizMode(int userId)
            throws SQLException {
        String sql = """
                SELECT top_genre, second_genre, third_genre
                FROM user_genre_discovery
                WHERE user_id = ?
                LIMIT 1
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return mapSummaryRow(rs, false);
            }
        }
    }

    private static Optional<UserGenreDiscoverySummary> mapSummaryRow(ResultSet rs,
                                                                     boolean readQuizMode)
            throws SQLException {
        String quiz = readQuizMode ? rs.getString("quiz_mode") : null;
        if (readQuizMode && rs.wasNull()) {
            quiz = null;
        }
        UserGenreDiscoverySummary s = new UserGenreDiscoverySummary(
                rs.getString("top_genre"),
                rs.getString("second_genre"),
                rs.getString("third_genre"),
                quiz);
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    private Integer findUserId(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return null;
        }
        String sql = "SELECT user_id FROM app_user WHERE username = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("user_id") : null;
            }
        }
    }

    private static boolean isUnknownColumnError(SQLException e, String columnName) {
        if ("42S22".equals(e.getSQLState())) {
            return true;
        }
        String m = e.getMessage();
        return m != null && m.contains("Unknown column") && columnName != null
                && m.contains(columnName);
    }

    static Map<String, Double> parseWeights(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, Double> out = new LinkedHashMap<>();
        for (String part : raw.split("\\|")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int colon = p.indexOf(':');
            if (colon <= 0 || colon >= p.length() - 1) continue;
            String key = p.substring(0, colon).trim().toLowerCase();
            try {
                double v = Double.parseDouble(p.substring(colon + 1).trim());
                if (!key.isEmpty() && v > 0) {
                    out.put(key, v);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static String serializeWeights(Map<String, Double> boosts) {
        if (boosts == null || boosts.isEmpty()) {
            return "";
        }
        return boosts.entrySet().stream()
                .filter(e -> e.getKey() != null && !e.getKey().isBlank()
                        && e.getValue() != null && e.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey().trim().toLowerCase() + ":" + round4(e.getValue()))
                .collect(Collectors.joining("|"));
    }

    private static double round4(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
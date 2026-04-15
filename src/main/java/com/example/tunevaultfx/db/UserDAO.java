package com.example.tunevaultfx.db;

import com.example.tunevaultfx.user.User;
import com.example.tunevaultfx.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAO {

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM app_user WHERE username = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM app_user WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean createUser(String username, String email, String password) throws SQLException {
        String sql = "INSERT INTO app_user (username, email, password_hash) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email.toLowerCase());
            stmt.setString(3, PasswordUtil.hashPassword(password));
            return stmt.executeUpdate() == 1;
        }
    }

    public User authenticateUser(String loginInput, String password) throws SQLException {
        String sql = """
                SELECT user_id, username, email
                FROM app_user
                WHERE (username = ? OR LOWER(email) = LOWER(?))
                  AND password_hash = ?
                LIMIT 1
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, loginInput);
            stmt.setString(2, loginInput);
            stmt.setString(3, PasswordUtil.hashPassword(password));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            null
                    );
                }
                return null;
            }
        }
    }

    public boolean updatePasswordByEmail(String email, String newPassword) throws SQLException {
        String sql = "UPDATE app_user SET password_hash = ? WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, PasswordUtil.hashPassword(newPassword));
            stmt.setString(2, email);
            return stmt.executeUpdate() == 1;
        }
    }

    public boolean emailRegistered(String email) throws SQLException {
        return emailExists(email);
    }

    /** Public profile fields only (password not loaded). */
    public Optional<User> findByUsername(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try {
            return findByUsernameWithProfileMedia(username.trim());
        } catch (SQLException e) {
            if (isUnknownColumnError(e)) {
                return findByUsernameLegacy(username.trim());
            }
            throw e;
        }
    }

    private Optional<User> findByUsernameWithProfileMedia(String username) throws SQLException {
        String sql = """
                SELECT user_id, username, email, profile_avatar_key
                FROM app_user
                WHERE username = ?
                LIMIT 1
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPublicUserRow(rs));
            }
        }
    }

    /** When app_user has not been migrated with profile_avatar_key yet. */
    private Optional<User> findByUsernameLegacy(String username) throws SQLException {
        String sql = """
                SELECT user_id, username, email
                FROM app_user
                WHERE username = ?
                LIMIT 1
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(
                        new User(
                                rs.getInt("user_id"),
                                rs.getString("username"),
                                rs.getString("email"),
                                null,
                                null));
            }
        }
    }

    private static boolean isUnknownColumnError(SQLException e) {
        if ("42S22".equals(e.getSQLState())) {
            return true;
        }
        String m = e.getMessage();
        return m != null && m.contains("Unknown column") && m.contains("profile_");
    }

    public void updateProfileAvatarKey(int userId, String relativeKeyOrNull) throws SQLException {
        String sql = "UPDATE app_user SET profile_avatar_key = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (relativeKeyOrNull == null || relativeKeyOrNull.isBlank()) {
                stmt.setNull(1, java.sql.Types.VARCHAR);
            } else {
                stmt.setString(1, relativeKeyOrNull.trim());
            }
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    private static User mapPublicUserRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String av = rs.getString("profile_avatar_key");
        if (rs.wasNull()) {
            av = null;
        }
        return new User(id, username, email, null, av);
    }
}

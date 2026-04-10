package com.example.tunevaultfx.user;

import com.example.tunevaultfx.db.DBConnection;
import com.example.tunevaultfx.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Handles database operations related to user accounts.
 * Used for checking existing users, creating accounts, and authenticating login.
 */
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
        String sql = "SELECT 1 FROM app_user WHERE email = ? LIMIT 1";

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
            stmt.setString(2, email);
            stmt.setString(3, PasswordUtil.hashPassword(password));

            return stmt.executeUpdate() == 1;
        }
    }

    public User authenticateUser(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, email FROM app_user WHERE username = ? AND password_hash = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, PasswordUtil.hashPassword(password));

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

    public boolean emailRegistered(String email) throws SQLException {
        return emailExists(email);
    }
}
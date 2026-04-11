package com.example.tunevaultfx.user;

/**
 * Represents one user account.
 * Stores the user id, username, email, and password.
 */
public class User {
    private int userId;
    private String username;
    private String email;
    private String password;

    public User() {
    }

    public User(int userId, String username, String email, String password) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public User(String username, String email, String password) {
        this(0, username, email, password);
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String toFileString() {
        return username + "," + email + "," + password;
    }

    public static User fromFileString(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 3) {
            return null;
        }
        return new User(parts[0], parts[1], parts[2]);
    }
}
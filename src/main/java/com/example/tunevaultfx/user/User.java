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
    /** Relative path under app profile-media dir, or null */
    private String profileAvatarKey;

    public User() {
    }

    public User(int userId, String username, String email, String password) {
        this(userId, username, email, password, null);
    }

    public User(
            int userId,
            String username,
            String email,
            String password,
            String profileAvatarKey) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.profileAvatarKey = profileAvatarKey;
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

    public String getProfileAvatarKey() {
        return profileAvatarKey;
    }
}

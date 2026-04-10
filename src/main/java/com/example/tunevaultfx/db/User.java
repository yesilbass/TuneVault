package com.example.tunevaultfx.model;

public class User {
    private final Integer userId;
    private final String username;
    private final String email;
    private final String password;

    public User(String username, String email, String password) {
        this(null, username, email, password);
    }

    public User(Integer userId, String username, String email, String password) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public Integer getUserId() {
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
}

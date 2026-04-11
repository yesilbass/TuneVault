package com.example.tunevaultfx.user;

import com.example.tunevaultfx.core.Song;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents one user's music data inside the app.
 * Stores playlists and liked songs.
 */
public class UserProfile {

    private final String username;
    private final Map<String, ObservableList<Song>> playlists = new LinkedHashMap<>();

    public UserProfile(String username) {
        this.username = username;
        playlists.put("Liked Songs", FXCollections.observableArrayList());
    }

    public String getUsername() {
        return username;
    }

    public Map<String, ObservableList<Song>> getPlaylists() {
        return playlists;
    }

    public ObservableList<Song> getLikedSongs() {
        return playlists.computeIfAbsent("Liked Songs", k -> FXCollections.observableArrayList());
    }

    public boolean isLiked(Song song) {
        return getLikedSongs().contains(song);
    }

    public void toggleLike(Song song) {
        ObservableList<Song> liked = getLikedSongs();
        if (liked.contains(song)) {
            liked.remove(song);
        } else {
            liked.add(song);
        }
    }
}
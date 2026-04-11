package com.example.tunevaultfx.db;

import com.example.tunevaultfx.core.Song;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Loads songs from the database.
 */
public class SongDAO {

    public ObservableList<Song> getAllSongs() throws SQLException {
        ObservableList<Song> songs = FXCollections.observableArrayList();

        String sql = """
                SELECT s.song_id,
                       s.title,
                       COALESCE(a.name, '') AS artist_name,
                       '' AS album_name,
                       COALESCE(s.duration_seconds, 0) AS duration_seconds
                FROM song s
                LEFT JOIN artist a ON a.artist_id = s.artist_id
                ORDER BY s.title
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                songs.add(new Song(
                        rs.getInt("song_id"),
                        rs.getString("title"),
                        rs.getString("artist_name"),
                        rs.getString("album_name"),
                        rs.getInt("duration_seconds")
                ));
            }
        }

        return songs;
    }
}
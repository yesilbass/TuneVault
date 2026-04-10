package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.Song;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
/**
 * Handles searching and filtering songs based on user input.
 * Used by the playlists page search panel.
 */
public class SongSearchService {

    public ObservableList<Song> filterSongs(ObservableList<Song> sourceSongs, String searchText) {
        ObservableList<Song> results = FXCollections.observableArrayList();

        if (sourceSongs == null) {
            return results;
        }

        String search = searchText == null ? "" : searchText.trim().toLowerCase();

        for (Song song : sourceSongs) {
            if (song == null) {
                continue;
            }

            if (search.isEmpty()
                    || song.title().toLowerCase().contains(search)
                    || song.artist().toLowerCase().contains(search)
                    || song.album().toLowerCase().contains(search)) {
                results.add(song);
            }
        }

        return results;
    }
}
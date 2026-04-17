package com.example.tunevaultfx.playlist.service;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.playlist.PlaylistSummary;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.TimeUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
/**
 * Builds summary information for the selected playlist,
 * such as song count, total duration, and the list of songs.
 */
public class PlaylistSelectionService {

    public PlaylistSummary buildSummary(UserProfile profile, String playlistName) {
        if (profile == null || playlistName == null) {
            return new PlaylistSummary(
                    "No playlist selected",
                    FXCollections.observableArrayList(),
                    0,
                    0,
                    "0 minutes"
            );
        }

        ObservableList<Song> songs = profile.getPlaylists().get(playlistName);
        if (songs == null) {
            songs = FXCollections.observableArrayList();
        }

        int totalSeconds = songs.stream().mapToInt(Song::durationSeconds).sum();

        return new PlaylistSummary(
                playlistName,
                songs,
                songs.size(),
                totalSeconds,
                TimeUtil.formatPlaylistTotalDuration(totalSeconds)
        );
    }
}
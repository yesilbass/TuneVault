package com.example.tunevaultfx.playlist.service;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.user.UserProfile;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;

/**
 * Recommends songs to add to a playlist based on:
 * 1. Genres already present in the playlist.
 * 2. Artists already present in the playlist.
 * 3. Songs the user has liked (via Liked Songs playlist).
 *
 * Returns up to 4 suggestions that are not already in the target playlist.
 */
public class SongSuggestionService {

    private static final int MAX_SUGGESTIONS = 4;

    /**
     * @param allSongs       Full song library.
     * @param profile        Current user's profile (for liked songs).
     * @param playlistName   Name of the playlist to suggest for.
     * @return Up to 4 recommended songs not already in the playlist.
     */
    public ObservableList<Song> suggest(ObservableList<Song> allSongs,
                                        UserProfile profile,
                                        String playlistName) {

        if (allSongs == null || allSongs.isEmpty() || profile == null || playlistName == null) {
            return FXCollections.observableArrayList();
        }

        ObservableList<Song> playlistSongs = profile.getPlaylists().get(playlistName);
        ObservableList<Song> likedSongs = profile.getLikedSongs();

        if (playlistSongs == null) {
            playlistSongs = FXCollections.observableArrayList();
        }

        Set<Integer> alreadyInPlaylist = new HashSet<>();
        for (Song s : playlistSongs) {
            if (s != null) alreadyInPlaylist.add(s.songId());
        }

        // Collect genre/artist signals from the playlist itself and liked songs
        Map<String, Integer> genreScore = new HashMap<>();
        Map<String, Integer> artistScore = new HashMap<>();

        for (Song s : playlistSongs) {
            if (s == null) continue;
            if (s.genre() != null && !s.genre().isBlank()) {
                genreScore.merge(s.genre().toLowerCase(), 3, Integer::sum);
            }
            if (s.artist() != null && !s.artist().isBlank()) {
                artistScore.merge(s.artist().toLowerCase(), 2, Integer::sum);
            }
        }

        for (Song s : likedSongs) {
            if (s == null) continue;
            if (s.genre() != null && !s.genre().isBlank()) {
                genreScore.merge(s.genre().toLowerCase(), 1, Integer::sum);
            }
            if (s.artist() != null && !s.artist().isBlank()) {
                artistScore.merge(s.artist().toLowerCase(), 1, Integer::sum);
            }
        }

        // Score every song not already in the playlist
        List<ScoredSong> candidates = new ArrayList<>();

        for (Song s : allSongs) {
            if (s == null || alreadyInPlaylist.contains(s.songId())) continue;

            int score = 0;

            if (s.genre() != null && !s.genre().isBlank()) {
                score += genreScore.getOrDefault(s.genre().toLowerCase(), 0);
            }
            if (s.artist() != null && !s.artist().isBlank()) {
                score += artistScore.getOrDefault(s.artist().toLowerCase(), 0);
            }

            // Bonus if song is liked
            for (Song liked : likedSongs) {
                if (liked != null && liked.songId() == s.songId()) {
                    score += 5;
                    break;
                }
            }

            candidates.add(new ScoredSong(s, score));
        }

        // Sort descending by score, then alphabetically for ties
        candidates.sort((a, b) -> {
            int cmp = Integer.compare(b.score(), a.score());
            if (cmp != 0) return cmp;
            return a.song().title().compareToIgnoreCase(b.song().title());
        });

        ObservableList<Song> result = FXCollections.observableArrayList();
        for (int i = 0; i < Math.min(MAX_SUGGESTIONS, candidates.size()); i++) {
            result.add(candidates.get(i).song());
        }

        return result;
    }

    private record ScoredSong(Song song, int score) {}
}

package com.example.tunevaultfx.recommendation;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Public API for all recommendation and search-ranking features.
 *
 * Reduced from 260 lines to ~100 lines by extracting:
 *  - RecommendationEngine  — core profile building + song scoring
 *  - SearchRankingService  — search-specific ranking logic
 *  - RecommendationProfile — shared data record
 *
 * Callers (PlaylistsPageController, SearchPageController,
 * MusicPlayerController) do not need to change — the method signatures
 * are identical to before.
 */
public class RecommendationService {

    private final SongDAO              songDAO              = new SongDAO();
    private final RecommendationEngine engine               = new RecommendationEngine();
    private final SearchRankingService searchRankingService = new SearchRankingService();

    // ── Playlist suggestions ───────────────────────────────────────

    /**
     * Suggests songs for a user based purely on their listening history,
     * with a novelty penalty for overplayed songs.
     */
    public ObservableList<Song> getSuggestedSongsForUser(String username, int limit) {
        try {
            ObservableList<Song> allSongs = songDAO.getAllSongs();
            RecommendationProfile profile = engine.buildProfileForUser(username);

            if (profile.songAffinity().isEmpty()) return fallback(allSongs, limit);

            record ScoredSong(Song song, double score) {}
            List<ScoredSong> scored = new ArrayList<>();

            for (Song song : allSongs) {
                if (song == null) continue;
                double s = engine.scoreForUser(profile, song);
                if (s > 0.0) scored.add(new ScoredSong(song, s));
            }

            scored.sort(Comparator.comparingDouble(ScoredSong::score).reversed());
            return scored.stream().limit(limit).map(ScoredSong::song)
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

        } catch (SQLException e) {
            e.printStackTrace();
            return FXCollections.observableArrayList();
        }
    }

    /**
     * Suggests songs that fit a specific playlist, weighted by both the
     * playlist's artist/genre distribution and the user's taste.
     * Songs already in the playlist are excluded.
     */
    public ObservableList<Song> getSuggestedSongsForPlaylist(String username,
                                                             ObservableList<Song> playlistSongs,
                                                             int limit) {
        try {
            ObservableList<Song> allSongs = songDAO.getAllSongs();
            RecommendationProfile profile = engine.buildProfileForUser(username);

            Map<String, Double> artistWeights = new HashMap<>();
            Map<String, Double> genreWeights  = new HashMap<>();
            Set<Integer>        existing       = new HashSet<>();

            if (playlistSongs != null) {
                for (Song s : playlistSongs) {
                    if (s == null) continue;
                    existing.add(s.songId());
                    artistWeights.merge(engine.normalize(s.artist()), 1.0, Double::sum);
                    genreWeights.merge(engine.normalize(s.genre()),   1.0, Double::sum);
                }
            }

            engine.normalizeMap(artistWeights);
            engine.normalizeMap(genreWeights);

            record ScoredSong(Song song, double score) {}
            List<ScoredSong> scored = new ArrayList<>();

            for (Song song : allSongs) {
                if (song == null || existing.contains(song.songId())) continue;
                double s = engine.scoreForPlaylist(profile, artistWeights, genreWeights, song);
                if (s > 0.0) scored.add(new ScoredSong(song, s));
            }

            scored.sort(Comparator.comparingDouble(ScoredSong::score).reversed());
            return scored.stream().limit(limit).map(ScoredSong::song)
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

        } catch (SQLException e) {
            e.printStackTrace();
            return FXCollections.observableArrayList();
        }
    }

    // ── Search ranking — delegates to SearchRankingService ────────

    public ObservableList<Song> getRankedSearchSongs(String username,
                                                     String query,
                                                     ObservableList<Song> allSongs,
                                                     int limit) {
        return searchRankingService.getRankedSearchSongs(username, query, allSongs, limit);
    }

    public ObservableList<String> getRankedSearchArtists(String username,
                                                         String query,
                                                         ObservableList<Song> allSongs,
                                                         int limit) {
        return searchRankingService.getRankedSearchArtists(username, query, allSongs, limit);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private ObservableList<Song> fallback(ObservableList<Song> all, int limit) {
        return all.stream().limit(limit)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
}
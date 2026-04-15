package com.example.tunevaultfx.recommendation;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.UserGenreDiscoveryDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ranks songs and artists for a search query, personalised by the user's
 * listening behaviour.
 *
 * Extracted from RecommendationService because search ranking is a completely
 * separate concern from playlist suggestions. Changes to one should never
 * require touching the other.
 */
public class SearchRankingService {

    private final RecommendationEngine   engine            = new RecommendationEngine();
    private final UserGenreDiscoveryDAO genreDiscoveryDAO = new UserGenreDiscoveryDAO();

    /**
     * Returns songs matching {@code query}, ranked so the best fit for
     * THIS user appears first.
     *
     * Ranking factors (descending priority):
     *  1. Text match quality (title start > artist start > contains)
     *  2. User affinity (liked artists / genres float to the top)
     *  3. Strongly disliked songs are excluded
     */
    public ObservableList<Song> getRankedSearchSongs(String username,
                                                     String query,
                                                     ObservableList<Song> allSongs,
                                                     int limit) {
        String normalizedQuery = engine.normalize(query);
        if (normalizedQuery.isBlank() || allSongs == null)
            return FXCollections.observableArrayList();

        RecommendationProfile profile =
                engine.buildProfileForUser(username, loadGenreBoost(username));

        record ScoredSong(Song song, double score) {}
        List<ScoredSong> ranked = new ArrayList<>();

        for (Song song : allSongs) {
            if (song == null) continue;
            if (profile.strongNegativeSongIds().contains(song.songId())) continue;

            double textScore = songTextMatchScore(song, normalizedQuery);
            if (textScore <= 0.0) continue;

            // Blend text relevance with personal affinity
            double userAffinity =
                    profile.artistAffinity().getOrDefault(engine.normalize(song.artist()), 0.0) * 0.3 +
                            profile.genreAffinity().getOrDefault(engine.normalize(song.genre()), 0.0) * 0.2;

            ranked.add(new ScoredSong(song, textScore + userAffinity));
        }

        ranked.sort(Comparator.comparingDouble(ScoredSong::score).reversed());

        return ranked.stream()
                .limit(limit)
                .map(ScoredSong::song)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    /**
     * Returns artist names matching {@code query}, ranked by text quality
     * and the user's artist/genre affinity.
     */
    public ObservableList<String> getRankedSearchArtists(String username,
                                                         String query,
                                                         ObservableList<Song> allSongs,
                                                         int limit) {
        String normalizedQuery = engine.normalize(query);
        if (normalizedQuery.isBlank() || allSongs == null)
            return FXCollections.observableArrayList();

        RecommendationProfile profile =
                engine.buildProfileForUser(username, loadGenreBoost(username));
        Map<String, Double> ranked = new HashMap<>();

        for (Song song : allSongs) {
            if (song == null || song.artist() == null || song.artist().isBlank()) continue;

            String artist = song.artist();
            double textScore = artistTextMatchScore(artist, normalizedQuery);
            if (textScore <= 0.0) continue;

            double affinity =
                    profile.artistAffinity().getOrDefault(engine.normalize(artist), 0.0) * 1.5 +
                            profile.genreAffinity().getOrDefault(engine.normalize(song.genre()), 0.0);

            double finalScore = (textScore * 0.65) + (affinity * 0.35);
            ranked.merge(artist, finalScore, Math::max);
        }

        return ranked.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    private Map<String, Double> loadGenreBoost(String username) {
        try {
            return genreDiscoveryDAO.loadBoostWeights(username);
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    // ── Text match scoring ─────────────────────────────────────────

    private double songTextMatchScore(Song song, String query) {
        String title  = engine.normalize(song.title());
        String artist = engine.normalize(song.artist());
        String album  = engine.normalize(song.album());
        String genre  = engine.normalize(song.genre());

        double score = 0.0;
        if (title.startsWith(query))               score = Math.max(score, 10.0);
        if (artist.startsWith(query))              score = Math.max(score, 9.0);
        if (title.contains(query))                 score = Math.max(score, 7.0);
        if (artist.contains(query))                score = Math.max(score, 6.5);
        if (!genre.isBlank()  && genre.contains(query))  score = Math.max(score, 5.0);
        if (!album.isBlank()  && album.contains(query))  score = Math.max(score, 4.0);
        return score;
    }

    private double artistTextMatchScore(String artistName, String query) {
        String artist = engine.normalize(artistName);
        if (artist.startsWith(query)) return 10.0;
        if (artist.contains(query))   return 7.0;
        return 0.0;
    }
}
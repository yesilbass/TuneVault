package com.example.tunevaultfx.recommendation;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.db.UserGenreDiscoveryDAO;
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

    private final SongDAO                 songDAO              = new SongDAO();
    private final UserGenreDiscoveryDAO   genreDiscoveryDAO  = new UserGenreDiscoveryDAO();
    private final RecommendationEngine    engine               = new RecommendationEngine();
    private final SearchRankingService    searchRankingService = new SearchRankingService();

    // ── Playlist suggestions ───────────────────────────────────────

    /**
     * Suggests songs for a user based purely on their listening history,
     * with a novelty penalty for overplayed songs.
     */
    public ObservableList<Song> getSuggestedSongsForUser(String username, int limit) {
        try {
            ObservableList<Song> allSongs = songDAO.getAllSongs();
            RecommendationProfile profile =
                    engine.buildProfileForUser(username, loadGenreBoost(username));

            if (profile.songAffinity().isEmpty()
                    && profile.artistAffinity().isEmpty()
                    && profile.genreAffinity().isEmpty()) {
                return fallback(allSongs, limit);
            }

            record ScoredSong(Song song, double score) {}
            List<ScoredSong> scored = new ArrayList<>();

            for (Song song : allSongs) {
                if (song == null) continue;
                double s = engine.scoreForUser(profile, song);
                if (s > 0.0) scored.add(new ScoredSong(song, s));
            }

            scored.sort(Comparator.comparingDouble(ScoredSong::score).reversed());
            if (scored.isEmpty()) {
                return FXCollections.observableArrayList(fallbackShuffle(allSongs, limit));
            }
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
            RecommendationProfile profile =
                    engine.buildProfileForUser(username, loadGenreBoost(username));

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
            List<Song> picks = scored.stream().limit(limit).map(ScoredSong::song).toList();
            if (picks.isEmpty() && !allSongs.isEmpty()) {
                return FXCollections.observableArrayList(
                        shuffleExcluding(allSongs, existing, limit));
            }
            return FXCollections.observableArrayList(picks);

        } catch (SQLException e) {
            e.printStackTrace();
            return FXCollections.observableArrayList();
        }
    }

    /**
     * Pulls more songs for endless autoplay: taste-ranked first, then shuffled catalog.
     * If every id is excluded (tiny library), repeats from the full catalog.
     */
    public ObservableList<Song> getAutoplayContinuation(String username,
                                                        Song anchor,
                                                        Set<Integer> alreadyQueuedIds,
                                                        int limit) {
        try {
            ObservableList<Song> allSongs = songDAO.getAllSongs();
            if (allSongs.isEmpty()) {
                return FXCollections.observableArrayList();
            }

            Set<Integer> exclude = new HashSet<>();
            if (alreadyQueuedIds != null) {
                exclude.addAll(alreadyQueuedIds);
            }
            if (anchor != null && anchor.songId() > 0) {
                exclude.add(anchor.songId());
            }

            RecommendationProfile profile =
                    engine.buildProfileForUser(username, loadGenreBoost(username));
            record ScoredSong(Song song, double score) {}
            List<ScoredSong> scored = new ArrayList<>();

            for (Song song : allSongs) {
                if (song == null || exclude.contains(song.songId())) {
                    continue;
                }
                double s = engine.scoreForUser(profile, song);
                if (s > 0.0) {
                    scored.add(new ScoredSong(song, s));
                }
            }

            scored.sort(Comparator.comparingDouble(ScoredSong::score).reversed());
            List<Song> out = new ArrayList<>(scored.stream().limit(limit).map(ScoredSong::song).toList());

            if (out.size() < limit) {
                out.addAll(shuffleExcluding(allSongs, unionIds(exclude, out), limit - out.size()));
            }

            if (out.isEmpty()) {
                out.addAll(shuffleExcluding(allSongs, exclude, limit));
            }

            return FXCollections.observableArrayList(out);
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

    private Map<String, Double> loadGenreBoost(String username) {
        try {
            return genreDiscoveryDAO.loadBoostWeights(username);
        } catch (SQLException e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    private ObservableList<Song> fallback(ObservableList<Song> all, int limit) {
        return FXCollections.observableArrayList(fallbackShuffle(all, limit));
    }

    private List<Song> fallbackShuffle(ObservableList<Song> all, int limit) {
        return shuffleExcluding(all, Collections.emptySet(), limit);
    }

    private Set<Integer> unionIds(Set<Integer> base, List<Song> songs) {
        Set<Integer> u = new HashSet<>(base);
        for (Song s : songs) {
            if (s != null && s.songId() > 0) {
                u.add(s.songId());
            }
        }
        return u;
    }

    /**
     * Random songs from {@code allSongs} omitting {@code excludeIds}. If that pool is empty,
     * shuffles the full list so playback can always continue with a non-empty catalog.
     */
    private List<Song> shuffleExcluding(ObservableList<Song> allSongs,
                                        Set<Integer> excludeIds,
                                        int limit) {
        List<Song> pool = allSongs.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.songId() <= 0 || excludeIds == null || !excludeIds.contains(s.songId()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (pool.isEmpty()) {
            pool = allSongs.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        }
        Collections.shuffle(pool);
        return pool.stream().limit(Math.max(0, limit)).collect(Collectors.toList());
    }
}
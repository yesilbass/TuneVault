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
 * Callers: playlist suggestions pass the playlist display name so picks stay
 * consistent per playlist but differ across playlists when the in-playlist signal is thin.
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
     *
     * @param playlistDisplayName playlist title (e.g. sidebar selection); used with content to
     *                          diversify ordering when the playlist signal is weak so empty or
     *                          similar playlists do not all show identical picks.
     */
    public ObservableList<Song> getSuggestedSongsForPlaylist(String username,
                                                             String playlistDisplayName,
                                                             ObservableList<Song> playlistSongs,
                                                             int limit) {
        try {
            String playlistKey = playlistDisplayName == null ? "" : playlistDisplayName;
            ObservableList<Song> allSongs = songDAO.getAllSongs();
            RecommendationProfile profile =
                    engine.buildProfileForUser(username, loadGenreBoost(username));

            Map<String, Double> artistWeights = new HashMap<>();
            Map<String, Double> genreWeights  = new HashMap<>();
            Set<Integer>        existing       = new HashSet<>();
            Set<String>         distinctArtists = new HashSet<>();
            Set<String>         distinctGenres  = new HashSet<>();
            int                 playlistSongCount = 0;

            if (playlistSongs != null) {
                for (Song s : playlistSongs) {
                    if (s == null) continue;
                    playlistSongCount++;
                    existing.add(s.songId());
                    String a = engine.normalize(s.artist());
                    String g = engine.normalize(s.genre());
                    if (!a.isEmpty()) {
                        distinctArtists.add(a);
                        artistWeights.merge(a, 1.0, Double::sum);
                    }
                    if (!g.isEmpty()) {
                        distinctGenres.add(g);
                        genreWeights.merge(g, 1.0, Double::sum);
                    }
                }
            }

            engine.normalizeMap(artistWeights);
            engine.normalizeMap(genreWeights);

            long scopeKey = stablePlaylistScopeKey(username, playlistKey, existing);
            double diversityStrength =
                    playlistSuggestionDiversityStrength(
                            playlistSongCount, distinctArtists.size(), distinctGenres.size());

            record ScoredSong(Song song, double score) {}
            List<ScoredSong> scored = new ArrayList<>();

            for (Song song : allSongs) {
                if (song == null || existing.contains(song.songId())) continue;
                double base = engine.scoreForPlaylist(
                        profile, artistWeights, genreWeights, playlistSongCount, song);
                if (base == Double.NEGATIVE_INFINITY) {
                    continue;
                }
                double s = base + diversityShift(scopeKey, song.songId(), diversityStrength);
                if (s > 0.0) scored.add(new ScoredSong(song, s));
            }

            scored.sort(Comparator.comparingDouble(ScoredSong::score).reversed());
            List<Song> picks =
                    new ArrayList<>(scored.stream().limit(limit).map(ScoredSong::song).toList());

            Set<Integer> pickedIds = new HashSet<>(existing);
            for (Song s : picks) {
                if (s != null && s.songId() > 0) {
                    pickedIds.add(s.songId());
                }
            }
            if (picks.size() < limit && !allSongs.isEmpty()) {
                picks.addAll(shuffleExcluding(allSongs, pickedIds, limit - picks.size(), scopeKey));
            }
            if (picks.isEmpty() && !allSongs.isEmpty()) {
                return FXCollections.observableArrayList(
                        shuffleExcluding(allSongs, existing, limit, scopeKey));
            }
            return FXCollections.observableArrayList(picks);

        } catch (SQLException e) {
            e.printStackTrace();
            return FXCollections.observableArrayList();
        }
    }

    /** Stable key so the same playlist always gets the same “jitter”, different playlists differ. */
    private static long stablePlaylistScopeKey(
            String username, String playlistDisplayName, Set<Integer> memberIds) {
        int u = username == null ? 0 : username.hashCode();
        int p = playlistDisplayName == null ? 0 : playlistDisplayName.hashCode();
        int contentHash;
        if (memberIds == null || memberIds.isEmpty()) {
            contentHash = 0;
        } else {
            int[] ids =
                    memberIds.stream().mapToInt(Integer::intValue).filter(i -> i > 0).sorted().toArray();
            contentHash = Arrays.hashCode(ids);
        }
        return ((long) u << 32) ^ (p & 0xFFFFFFFFL) ^ ((long) contentHash << 16);
    }

    /**
     * How much to perturb scores when the playlist does not yet define a rich artist/genre mix.
     * Strong playlists (e.g. Liked Songs with many tracks) keep this low so ranking stays sharp.
     */
    private static double playlistSuggestionDiversityStrength(
            int nSongs, int distinctArtists, int distinctGenres) {
        if (nSongs <= 0) {
            return 0.58;
        }
        int breadth = Math.max(distinctArtists, distinctGenres);
        if (nSongs >= 40 && breadth >= 12) {
            return 0.025;
        }
        if (nSongs >= 20 && breadth >= 8) {
            return 0.045;
        }
        if (nSongs >= 12 && breadth >= 5) {
            return 0.08;
        }
        if (nSongs >= 6 && breadth >= 3) {
            return 0.14;
        }
        if (nSongs >= 3) {
            return 0.28;
        }
        return 0.42;
    }

    /** Deterministic, playlist-scoped tie-break in {@code [-strength, strength]}. */
    private static double diversityShift(long scopeKey, int songId, double strength) {
        if (strength <= 1e-9) {
            return 0.0;
        }
        long z = scopeKey ^ ((long) songId * 0x9E3779B97F4A7C15L);
        z ^= z >>> 33;
        z *= 0xff51afd7ed558ccdL;
        z ^= z >>> 33;
        double u = (z & ((1L << 53) - 1)) / (double) (1L << 53);
        return (u - 0.5) * 2.0 * strength;
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
                out.addAll(shuffleExcluding(allSongs, unionIds(exclude, out), limit - out.size(), null));
            }

            if (out.isEmpty()) {
                out.addAll(shuffleExcluding(allSongs, exclude, limit, null));
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
        return shuffleExcluding(all, Collections.emptySet(), limit, null);
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
     *
     * @param shuffleSeed if non-null, used for a reproducible shuffle per playlist context
     */
    private List<Song> shuffleExcluding(
            ObservableList<Song> allSongs, Set<Integer> excludeIds, int limit, Long shuffleSeed) {
        List<Song> pool = allSongs.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.songId() <= 0 || excludeIds == null || !excludeIds.contains(s.songId()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (pool.isEmpty()) {
            pool = allSongs.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
        }
        if (shuffleSeed != null) {
            Collections.shuffle(pool, new Random(shuffleSeed));
        } else {
            Collections.shuffle(pool);
        }
        return pool.stream().limit(Math.max(0, limit)).collect(Collectors.toList());
    }
}
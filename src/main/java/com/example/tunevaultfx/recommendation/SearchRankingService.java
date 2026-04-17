package com.example.tunevaultfx.recommendation;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.UserGenreDiscoveryDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ranks songs and artists for a search query, personalised by the user's listening behaviour.
 *
 * <p>Catalog search blends songs and artists in one ordering. Text matching is <strong>prefix
 * only</strong> on title, artist, and album (genre is not used for matching). Results are ordered
 * first by match kind (title before artist before album, with standalone artist hits alongside
 * artist-on-song matches), then by a score that blends text strength with light personalization.
 */
public class SearchRankingService {

    private static final int PRIORITY_TITLE = 400;
    private static final int PRIORITY_ARTIST = 300;
    private static final int PRIORITY_ALBUM = 100;

    private final RecommendationEngine   engine            = new RecommendationEngine();
    private final UserGenreDiscoveryDAO genreDiscoveryDAO = new UserGenreDiscoveryDAO();

    private record ScoredCandidate(RankedSearchRow row, double raw, int textPriority) {}

    /**
     * Songs and artists in one list, best match first for this user (mixed types).
     */
    public List<RankedSearchRow> getRankedCatalogSearchRows(
            String username, String query, ObservableList<Song> allSongs, int limit) {
        List<ScoredCandidate> candidates = collectScoredCandidates(username, query, allSongs);
        if (candidates.isEmpty()) {
            return List.of();
        }
        candidates.sort(
                Comparator.comparingInt(ScoredCandidate::textPriority)
                        .reversed()
                        .thenComparing(
                                Comparator.comparingDouble(ScoredCandidate::raw).reversed()));
        return candidates.stream().limit(limit).map(ScoredCandidate::row).toList();
    }

    /**
     * Songs only, same scoring as {@link #getRankedCatalogSearchRows} (for callers that need a song list).
     */
    public ObservableList<Song> getRankedSearchSongs(
            String username, String query, ObservableList<Song> allSongs, int limit) {
        List<ScoredCandidate> songs =
                collectScoredCandidates(username, query, allSongs).stream()
                        .filter(c -> c.row instanceof RankedSearchRow.SongHit)
                        .sorted(
                                Comparator.comparingInt(ScoredCandidate::textPriority)
                                        .reversed()
                                        .thenComparing(
                                                Comparator.comparingDouble(ScoredCandidate::raw)
                                                        .reversed()))
                        .limit(limit)
                        .toList();
        return songs.stream()
                .map(c -> ((RankedSearchRow.SongHit) c.row).song())
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    /**
     * Artists only, same scoring as {@link #getRankedCatalogSearchRows}.
     */
    public ObservableList<String> getRankedSearchArtists(
            String username, String query, ObservableList<Song> allSongs, int limit) {
        List<ScoredCandidate> artists =
                collectScoredCandidates(username, query, allSongs).stream()
                        .filter(c -> c.row instanceof RankedSearchRow.ArtistHit)
                        .sorted(
                                Comparator.comparingInt(ScoredCandidate::textPriority)
                                        .reversed()
                                        .thenComparing(
                                                Comparator.comparingDouble(ScoredCandidate::raw)
                                                        .reversed()))
                        .limit(limit)
                        .toList();
        return artists.stream()
                .map(c -> ((RankedSearchRow.ArtistHit) c.row).artistName())
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    private List<ScoredCandidate> collectScoredCandidates(
            String username, String query, ObservableList<Song> allSongs) {
        String normalizedQuery = engine.normalize(query);
        if (normalizedQuery.isBlank() || allSongs == null) {
            return List.of();
        }

        RecommendationProfile profile =
                engine.buildProfileForUser(username, loadGenreBoost(username));

        List<ScoredCandidate> songRows = new ArrayList<>();
        Map<String, Double> artistBestRaw = new HashMap<>();

        for (Song song : allSongs) {
            if (song == null) {
                continue;
            }

            int textPriority = songTextMatchPriority(song, normalizedQuery);
            double textScore = songTextMatchScore(song, normalizedQuery);
            if (textScore > 0.0 && textPriority > 0) {
                double personalize = songPersonalizationBlend(profile, song);
                double strongDislikePenalty =
                        profile.strongNegativeSongIds().contains(song.songId()) ? 6.0 : 0.0;
                double raw = textScore + personalize - strongDislikePenalty;
                songRows.add(new ScoredCandidate(new RankedSearchRow.SongHit(song), raw, textPriority));
            }

            if (song.artist() == null || song.artist().isBlank()) {
                continue;
            }
            String artist = song.artist();
            double artistText = artistTextMatchScore(artist, normalizedQuery);
            if (artistText <= 0.0) {
                continue;
            }
            double artistPersonal = artistPersonalizationFromSong(profile, artist, song);
            double artistRaw = artistText + artistPersonal;
            artistBestRaw.merge(artist, artistRaw, Math::max);
        }

        List<ScoredCandidate> out = new ArrayList<>(songRows);
        for (var e : artistBestRaw.entrySet()) {
            out.add(
                    new ScoredCandidate(
                            new RankedSearchRow.ArtistHit(e.getKey()), e.getValue(), PRIORITY_ARTIST));
        }
        return out;
    }

    /**
     * Title prefix ranks above artist prefix, then album. Same band as {@link #PRIORITY_ARTIST} for
     * standalone artist rows so “ar…” surfaces song titles first, then artists.
     */
    private int songTextMatchPriority(Song song, String query) {
        if (query.isEmpty()) {
            return 0;
        }
        String title = engine.normalize(song.title());
        String artist = engine.normalize(song.artist());
        String album = engine.normalize(song.album());
        if (title.startsWith(query)) {
            return PRIORITY_TITLE;
        }
        if (artist.startsWith(query)) {
            return PRIORITY_ARTIST;
        }
        if (!album.isBlank() && album.startsWith(query)) {
            return PRIORITY_ALBUM;
        }
        return 0;
    }

    /** Taste nudge within the same prefix tier; genre is minor so it cannot reorder title/artist matches. */
    private double songPersonalizationBlend(RecommendationProfile profile, Song song) {
        return profile.artistAffinity().getOrDefault(engine.normalize(song.artist()), 0.0) * 0.55
                + profile.genreAffinity().getOrDefault(engine.normalize(song.genre()), 0.0) * 0.08
                + profile.songAffinity().getOrDefault(song.songId(), 0.0) * 0.50;
    }

    private double artistPersonalizationFromSong(RecommendationProfile profile, String artist, Song song) {
        return profile.artistAffinity().getOrDefault(engine.normalize(artist), 0.0) * 0.55
                + profile.genreAffinity().getOrDefault(engine.normalize(song.genre()), 0.0) * 0.08;
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

    /**
     * Prefix-only on title, artist, album — genre excluded so catalogue search stays name-forward.
     */
    private double songTextMatchScore(Song song, String query) {
        return songTextPrefixScore(song, query);
    }

    private double songTextPrefixScore(Song song, String query) {
        if (query.isEmpty()) {
            return 0.0;
        }
        String title = engine.normalize(song.title());
        String artist = engine.normalize(song.artist());
        String album = engine.normalize(song.album());

        double score = 0.0;
        if (title.startsWith(query)) {
            score = Math.max(score, 10.0);
        }
        if (artist.startsWith(query)) {
            score = Math.max(score, 9.0);
        }
        if (!album.isBlank() && album.startsWith(query)) {
            score = Math.max(score, 7.0);
        }
        return score;
    }

    private double artistTextMatchScore(String artistName, String query) {
        return artistTextPrefixScore(artistName, query);
    }

    private double artistTextPrefixScore(String artistName, String query) {
        if (query.isEmpty()) {
            return 0.0;
        }
        String artist = engine.normalize(artistName);
        return artist.startsWith(query) ? 10.0 : 0.0;
    }
}

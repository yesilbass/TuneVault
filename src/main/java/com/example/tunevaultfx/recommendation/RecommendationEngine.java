package com.example.tunevaultfx.recommendation;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.ListeningEventDAO;
import com.example.tunevaultfx.db.ListeningEventDAO.UserBehaviorEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core recommendation engine.
 *
 * Responsibilities:
 *  1. Build a RecommendationProfile from a user's listening history.
 *  2. Score individual songs against that profile.
 *
 * This class has NO knowledge of playlists, search, or UI.
 * Extracted from RecommendationService which previously mixed this
 * low-level logic with high-level orchestration.
 */
class RecommendationEngine {

    private final ListeningEventDAO listeningEventDAO = new ListeningEventDAO();

    // ── Profile building ───────────────────────────────────────────

    RecommendationProfile buildProfileForUser(String username) {
        return buildProfileForUser(username, null);
    }

    /**
     * @param genreDiscoveryBoost normalized genre keys (e.g. from Find Your Genre quiz); merged into
     *                            genre affinity so recommendations reflect declared taste even with thin history.
     */
    RecommendationProfile buildProfileForUser(String username, Map<String, Double> genreDiscoveryBoost) {
        List<UserBehaviorEvent> events = listeningEventDAO.getUserBehaviorEvents(username);
        RecommendationProfile base =
                events.isEmpty() ? RecommendationProfile.empty() : buildProfile(events);
        if (genreDiscoveryBoost == null || genreDiscoveryBoost.isEmpty()) {
            return base;
        }
        return mergeGenreDiscovery(base, genreDiscoveryBoost);
    }

    private RecommendationProfile mergeGenreDiscovery(RecommendationProfile base,
                                                        Map<String, Double> boost) {
        Map<String, Double> genres = new HashMap<>(base.genreAffinity());
        for (var e : boost.entrySet()) {
            String k = normalize(e.getKey());
            if (!k.isEmpty()) {
                genres.merge(k, e.getValue(), Double::sum);
            }
        }
        normalizeMap(genres);
        return new RecommendationProfile(
                new HashMap<>(base.songAffinity()),
                new HashMap<>(base.artistAffinity()),
                genres,
                new HashSet<>(base.strongNegativeSongIds()));
    }

    RecommendationProfile buildProfile(List<UserBehaviorEvent> events) {
        Map<Integer, Double> songAffinity   = new HashMap<>();
        Map<String,  Double> artistAffinity = new HashMap<>();
        Map<String,  Double> genreAffinity  = new HashMap<>();
        Set<Integer>         strongNegative = new HashSet<>();

        for (UserBehaviorEvent event : events) {
            double weight = weightFor(event.actionType(), event.completionRatio());

            songAffinity.merge(event.songId(), weight, Double::sum);
            artistAffinity.merge(normalize(event.artistName()), weight, Double::sum);
            genreAffinity.merge(normalize(event.genreName()), weight, Double::sum);

            if (weight <= -4.0) strongNegative.add(event.songId());
        }

        normalizeMap(artistAffinity);
        normalizeMap(genreAffinity);

        return new RecommendationProfile(
                songAffinity, artistAffinity, genreAffinity, strongNegative);
    }

    // ── Song scoring ───────────────────────────────────────────────

    /**
     * Scores a song purely against user behaviour data.
     * Higher is better. Negative means the user dislikes it.
     */
    double scoreForUser(RecommendationProfile profile, Song song) {
        if (profile.strongNegativeSongIds().contains(song.songId())) return Double.NEGATIVE_INFINITY;

        double songScore   = profile.songAffinity().getOrDefault(song.songId(), 0.0);
        double artistScore = profile.artistAffinity().getOrDefault(normalize(song.artist()), 0.0);
        double genreScore  = profile.genreAffinity().getOrDefault(normalize(song.genre()), 0.0);

        // Songs heard too many times get a novelty penalty
        double noveltyPenalty = songScore > 3.0 ? 2.5 : 0.0;

        return (artistScore * 2.2) + (genreScore * 1.8) + (songScore * 0.6) - noveltyPenalty;
    }

    /**
     * Scores a song against both user behaviour AND the playlist's
     * artist/genre distribution.
     */
    double scoreForPlaylist(RecommendationProfile profile,
                            Map<String, Double> playlistArtistWeights,
                            Map<String, Double> playlistGenreWeights,
                            Song song) {
        if (profile.strongNegativeSongIds().contains(song.songId())) return Double.NEGATIVE_INFINITY;

        double userSong    = profile.songAffinity().getOrDefault(song.songId(), 0.0);
        double userArtist  = profile.artistAffinity().getOrDefault(normalize(song.artist()), 0.0);
        double userGenre   = profile.genreAffinity().getOrDefault(normalize(song.genre()), 0.0);
        double plArtist    = playlistArtistWeights.getOrDefault(normalize(song.artist()), 0.0);
        double plGenre     = playlistGenreWeights.getOrDefault(normalize(song.genre()), 0.0);

        return (userSong   * 0.5)
                + (userArtist * 1.6)
                + (userGenre  * 1.3)
                + (plArtist   * 2.4)
                + (plGenre    * 2.0);
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Weights for each action type, incorporating playback completion ratio.
     */
    double weightFor(String actionType, double completionRatio) {
        if (actionType == null) return 0.0;
        return switch (actionType) {
            case "LIKE"                   ->  5.0;
            case "PLAYLIST_ADD"           ->  4.0;
            case "PLAY"                   ->  3.0 + completionRatio;
            case "PLAY_PARTIAL_POSITIVE"  ->  2.0 + (completionRatio * 0.5);
            case "PLAY_PARTIAL"           ->  1.0;
            case "PLAY_SHORT"             -> -1.0;
            case "STOPPED_MID"            -> -1.5;
            case "SKIP"                   -> -2.0;
            case "SKIP_EARLY"             -> -4.5;
            case "PLAYLIST_REMOVE"        -> -3.0;
            case "UNLIKE"                 -> -5.0;
            default                       ->  0.0;
        };
    }

    void normalizeMap(Map<String, Double> map) {
        double max = map.values().stream().mapToDouble(Math::abs).max().orElse(0.0);
        if (max <= 0.0) return;
        map.replaceAll((k, v) -> v / max);
    }

    String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
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
     *                            The quiz weight decays as real listening data accumulates — at
     *                            {@value #QUIZ_HANDOFF_PLAYS} qualified plays the quiz has faded out entirely.
     */
    RecommendationProfile buildProfileForUser(String username, Map<String, Double> genreDiscoveryBoost) {
        List<UserBehaviorEvent> events = listeningEventDAO.getUserBehaviorEvents(username);
        RecommendationProfile base =
                events.isEmpty() ? RecommendationProfile.empty() : buildProfile(events);
        if (genreDiscoveryBoost == null || genreDiscoveryBoost.isEmpty()) {
            return base;
        }
        int qualifiedPlays = listeningEventDAO.countQualifiedPlays(username);
        return mergeGenreDiscovery(base, genreDiscoveryBoost, qualifiedPlays);
    }

    /**
     * Number of qualified plays (count_as_play = 1) at which listening history reaches its
     * maximum influence. Beyond this point the quiz floor still applies.
     */
    private static final int QUIZ_HANDOFF_PLAYS = 50;

    /**
     * Minimum quiz contribution regardless of how much listening history exists.
     * The quiz never drops below this weight so it always has influence — the user
     * can retake the quiz at any time to steer recommendations, and resets wipe the
     * profile entirely. Value of 0.30 means listening history caps at 70%.
     */
    private static final double QUIZ_FLOOR = 0.30;

    /**
     * Blends the quiz genre boost with the listening-derived genre affinity.
     *
     * <p>The quiz acts as a strong prior when the user is new, then its weight decays
     * linearly as real listening data accumulates — but never below {@value #QUIZ_FLOOR}:</p>
     * <ul>
     *   <li>0 qualified plays  → 100% quiz, 0% listening</li>
     *   <li>25 qualified plays → 65% quiz, 35% listening</li>
     *   <li>50+ qualified plays → 30% quiz (floor), 70% listening</li>
     * </ul>
     *
     * <p>This keeps the quiz permanently relevant — retaking it always has an impact,
     * and resetting it via Settings gives a clean slate.</p>
     *
     * @param qualifiedPlays number of songs the user has genuinely listened to (count_as_play = 1)
     */
    private RecommendationProfile mergeGenreDiscovery(RecommendationProfile base,
                                                      Map<String, Double> boost,
                                                      int qualifiedPlays) {
        // listeningWeight grows from 0.0 → (1 - QUIZ_FLOOR) as qualified plays accumulate
        double maxListeningWeight = 1.0 - QUIZ_FLOOR;
        double listeningWeight    = Math.min(maxListeningWeight,
                (double) qualifiedPlays / QUIZ_HANDOFF_PLAYS * maxListeningWeight);
        double quizWeight         = 1.0 - listeningWeight;

        Map<String, Double> genres = new HashMap<>(base.genreAffinity());

        // Scale down the quiz contribution proportionally — when listeningWeight = 1.0
        // quizWeight = 0.0 so the boost adds nothing and listening history wins entirely.
        for (var e : boost.entrySet()) {
            String k = normalize(e.getKey());
            if (!k.isEmpty()) {
                genres.merge(k, e.getValue() * quizWeight, Double::sum);
            }
        }

        // Scale down listening history contribution when quiz should dominate
        // (new users with 0 plays get a pure quiz-driven profile)
        if (listeningWeight < 1.0) {
            genres.replaceAll((k, v) -> {
                double listeningPart = base.genreAffinity().getOrDefault(k, 0.0) * listeningWeight;
                double quizPart      = boost.getOrDefault(k, 0.0) * quizWeight;
                return listeningPart + quizPart;
            });
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

        normalizeSongMap(songAffinity);
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

        // Song affinity is max-normalized like artist/genre; very high relative play still gets a
        // small novelty nudge so "For you" is not only the single most-played track.
        double noveltyPenalty =
                songScore >= 0.92 ? 0.32 : (songScore >= 0.78 ? 0.14 : 0.0);

        return (artistScore * 2.2) + (genreScore * 1.8) + (songScore * 0.6) - noveltyPenalty;
    }

    /**
     * Scores a song against both user behaviour AND the playlist's
     * artist/genre distribution. When the playlist has tracks, blends playlist match
     * vs user taste using {@link RecommendationConstants#PLAYLIST_VS_USER_BLEND_PLAYLIST_SHARE}
     * (Spotify-style: mostly “sounds like this playlist”, still personalized).
     */
    double scoreForPlaylist(RecommendationProfile profile,
                            Map<String, Double> playlistArtistWeights,
                            Map<String, Double> playlistGenreWeights,
                            int playlistSongCount,
                            Song song) {
        if (profile.strongNegativeSongIds().contains(song.songId())) return Double.NEGATIVE_INFINITY;

        double userSong    = profile.songAffinity().getOrDefault(song.songId(), 0.0);
        double userArtist  = profile.artistAffinity().getOrDefault(normalize(song.artist()), 0.0);
        double userGenre   = profile.genreAffinity().getOrDefault(normalize(song.genre()), 0.0);
        double plArtist    = playlistArtistWeights.getOrDefault(normalize(song.artist()), 0.0);
        double plGenre     = playlistGenreWeights.getOrDefault(normalize(song.genre()), 0.0);

        double userPart = (userSong * 0.5) + (userArtist * 1.6) + (userGenre * 1.3);
        double plPart   = (plArtist * 2.4) + (plGenre * 2.0);

        if (playlistSongCount <= 0) {
            return userPart;
        }
        double w = RecommendationConstants.PLAYLIST_VS_USER_BLEND_PLAYLIST_SHARE;
        return w * plPart + (1.0 - w) * userPart;
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

    /** Same scaling as {@link #normalizeMap(Map)} so per-song play counts do not dwarf genre/artist. */
    void normalizeSongMap(Map<Integer, Double> map) {
        double max = map.values().stream().mapToDouble(Math::abs).max().orElse(0.0);
        if (max <= 0.0) return;
        map.replaceAll((k, v) -> v / max);
    }

    String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
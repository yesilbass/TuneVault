package com.example.tunevaultfx.recommendation;

/**
 * Shared tuning for recommendation UIs — single place for counts and blend ratios
 * so playlist suggestions stay consistent and easy to adjust.
 */
public final class RecommendationConstants {

    private RecommendationConstants() {}

    /**
     * Exact number of tracks in the Playlists page &quot;Suggested for you&quot; list
     * (filled from ranked picks, then catalog shuffle if needed).
     */
    public static final int PLAYLIST_PAGE_SUGGESTION_COUNT = 5;

    /**
     * How much playlist artist/genre signal counts vs user taste (listening + genre quiz)
     * when scoring candidates for {@link RecommendationService#getSuggestedSongsForPlaylist}.
     * 0.72 ≈ 70% playlist-shaped, 30% user/global profile.
     */
    public static final double PLAYLIST_VS_USER_BLEND_PLAYLIST_SHARE = 0.72;
}

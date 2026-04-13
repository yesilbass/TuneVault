package com.example.tunevaultfx.recommendation;

import java.util.Map;
import java.util.Set;

/**
 * Immutable snapshot of a user's music tastes derived from their
 * listening history.
 *
 * Extracted from RecommendationService so both RecommendationEngine
 * and RecommendationService can use it without circular dependency.
 *
 * All maps use normalized (lowercase, trimmed) keys.
 */
public record RecommendationProfile(
        Map<Integer, Double> songAffinity,
        Map<String, Double>  artistAffinity,
        Map<String, Double>  genreAffinity,
        Set<Integer>         strongNegativeSongIds
) {
    /** Returns an empty profile for users with no listening history. */
    public static RecommendationProfile empty() {
        return new RecommendationProfile(
                Map.of(), Map.of(), Map.of(), Set.of()
        );
    }
}
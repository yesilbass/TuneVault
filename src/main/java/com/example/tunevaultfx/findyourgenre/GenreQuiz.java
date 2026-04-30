package com.example.tunevaultfx.findyourgenre;

import com.example.tunevaultfx.db.QuizQuestionDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Find Your Genre — question bank, scoring, and boost conversion for recommendations.
 *
 * <p>Questions are loaded from the {@code quiz_question} / {@code quiz_answer} database
 * tables via {@link QuizQuestionDAO}. If the tables are absent or empty (e.g. a pre-migration
 * schema), the built-in hardcoded bank is used as a fallback so the quiz always works.</p>
 *
 * <p>Design goals: short readable prompts (ADHD-friendly), no timers, clear progress,
 * optional skip, Quick vs Full length. Genre labels align with typical catalog spellings
 * and {@link com.example.tunevaultfx.recommendation.RecommendationEngine#normalize(String)}.</p>
 */
public final class GenreQuiz {

    private static final Logger LOG = Logger.getLogger(GenreQuiz.class.getName());

    public enum QuizMode {
        /** Five questions — same flow, faster win. */
        QUICK,
        /** Full personality pass — richer signal for recommendations. */
        FULL
    }

    private GenreQuiz() {}

    /**
     * Returns shuffled questions for the given mode and session number, loaded from the DB.
     * Falls back to the built-in hardcoded bank only if the quiz tables are missing or empty.
     *
     * @param mode          QUICK (5 random questions) or FULL (all 10, shuffled)
     * @param sessionNumber 1–5; determines which set of 10 questions is served
     */
    public static List<Question> questionsFor(QuizMode mode, int sessionNumber) {
        try {
            QuizQuestionDAO dao = new QuizQuestionDAO();
            List<Question> dbQuestions = dao.loadQuestions(mode, sessionNumber);
            if (!dbQuestions.isEmpty()) {
                return dbQuestions;
            }
            LOG.warning("quiz_question table is empty or missing — using built-in fallback questions.");
        } catch (SQLException e) {
            LOG.log(Level.WARNING,
                    "Could not load quiz questions from DB (table may not exist yet). Using built-in fallback.",
                    e);
        }
        // Fallback: hardcoded bank, shuffled
        List<Question> all = new ArrayList<>(allQuestions());
        Collections.shuffle(all);
        if (mode == QuizMode.QUICK) {
            return List.copyOf(all.subList(0, Math.min(5, all.size())));
        }
        return List.copyOf(all);
    }

    /** Convenience overload — uses session 1 (e.g. for fallback/testing). */
    public static List<Question> questionsFor(QuizMode mode) {
        return questionsFor(mode, 1);
    }

    public static int questionCount(QuizMode mode, int sessionNumber) {
        return questionsFor(mode, sessionNumber).size();
    }

    public static int questionCount(QuizMode mode) {
        return questionsFor(mode).size();
    }

    /**
     * Turns raw tallies into normalized boosts (0–1 scale) for merging into {@link com.example.tunevaultfx.recommendation.RecommendationProfile}.
     */
    public static Map<String, Double> toRecommendationBoosts(Map<String, Integer> scoreTally) {
        if (scoreTally == null || scoreTally.isEmpty()) {
            return Map.of();
        }
        int max = scoreTally.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        Map<String, Double> out = new LinkedHashMap<>();
        for (var e : scoreTally.entrySet()) {
            if (e.getKey() == null || e.getKey().isBlank()) {
                continue;
            }
            double v = (e.getValue() * 1.0 / Math.max(1, max)) * 0.92;
            if (v > 0) {
                out.put(e.getKey().trim().toLowerCase(), v);
            }
        }
        return out;
    }

    /** Top genres by score, for results UI and DB summary columns. */
    public static List<String> topGenres(Map<String, Integer> scores, int limit) {
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        return scores.entrySet().stream()
                .filter(e -> e.getKey() != null && !e.getKey().isBlank())
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> formatGenreTitle(e.getKey()))
                .collect(Collectors.toList());
    }

    /** Lowercase keys for matching against {@link com.example.tunevaultfx.core.Song#genre()}. */
    public static List<String> topGenreKeys(Map<String, Integer> scores, int limit) {
        if (scores == null || scores.isEmpty()) {
            return List.of();
        }
        return scores.entrySet().stream()
                .filter(e -> e.getKey() != null && !e.getKey().isBlank())
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> e.getKey().trim().toLowerCase())
                .collect(Collectors.toList());
    }

    public static String formatGenreTitle(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        if (t.length() <= 3) {
            return t.toUpperCase();
        }
        return Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase();
    }

    private static List<Question> allQuestions() {
        List<Question> q = new ArrayList<>();
        // 1 — energy (weighted: louder choices count a bit more)
        q.add(new Question(
                "What energy do you want from music right now?",
                "Soft & calm", "Steady groove", "Up & dancey", "Loud & intense",
                "Classical", "R&B", "Pop", "Metal",
                1, 1, 2, 2));
        // 2 — entry point
        q.add(Question.q(
                "What hooks you first?",
                "The words", "The rhythm", "The melody", "The atmosphere",
                "Folk", "Hip Hop", "Pop", "Electronic"));
        // 3 — space
        q.add(Question.q(
                "Where do you listen most?",
                "Cozy at home", "Night drive", "With friends", "Deep focus",
                "Indie", "Rock", "Pop", "Electronic"));
        // 4 — “color” metaphor (abstract → inclusive)
        q.add(Question.q(
                "Pick a sound “colour”:",
                "Warm & smoky", "Cool & smooth", "Bright & sparkly", "Raw & organic",
                "Jazz", "R&B", "Pop", "Folk"));
        // 5 — era
        q.add(Question.q(
                "Old or new sounds?",
                "Timeless classics", "Throwback eras", "Fresh releases", "Mix it all",
                "Classical", "Rock", "Pop", "Electronic"));
        // 6 — structure
        q.add(new Question(
                "Song structure you like:",
                "Catchy & tight", "Room to breathe", "Long builds", "Loops that hit",
                "Pop", "Jazz", "Electronic", "Hip Hop",
                2, 1, 2, 1));
        // 7 — vocals
        q.add(Question.q(
                "How important are vocals?",
                "Sing-along essential", "Nice when they’re there", "Background is fine", "Instrumentals rule",
                "Pop", "Indie", "Rock", "Electronic"));
        // 8 — social
        q.add(Question.q(
                "Social vibe when you press play:",
                "Solo reset", "Small hangout", "Big crowd energy", "Study / work flow",
                "Classical", "Indie", "Rock", "Electronic"));
        // 9 — emotion
        q.add(Question.q(
                "Emotional lane today:",
                "Happy boost", "Soft & reflective", "Angry catharsis", "Hopeful glow",
                "Pop", "R&B", "Metal", "Indie"));
        // 10 — instrument bias
        q.add(Question.q(
                "Secret-weapon instrument:",
                "Strings", "Drums & bass", "Synths & pads", "Guitar forward",
                "Classical", "Hip Hop", "Electronic", "Rock"));
        return List.copyOf(q);
    }

    /**
     * One quiz step: prompt, four options, four genre tags, optional per-answer weights.
     */
    public record Question(
            String prompt,
            String option1, String option2, String option3, String option4,
            String genre1, String genre2, String genre3, String genre4,
            int weight1, int weight2, int weight3, int weight4
    ) {
        public static Question q(String prompt,
                                 String o1, String o2, String o3, String o4,
                                 String g1, String g2, String g3, String g4) {
            return new Question(prompt, o1, o2, o3, o4, g1, g2, g3, g4, 1, 1, 1, 1);
        }

        public String genreFor(int index) {
            return switch (index) {
                case 0 -> genre1;
                case 1 -> genre2;
                case 2 -> genre3;
                default -> genre4;
            };
        }

        public String optionFor(int index) {
            return switch (index) {
                case 0 -> option1;
                case 1 -> option2;
                case 2 -> option3;
                default -> option4;
            };
        }

        public int weightFor(int index) {
            int w = switch (index) {
                case 0 -> weight1;
                case 1 -> weight2;
                case 2 -> weight3;
                default -> weight4;
            };
            return Math.max(1, w);
        }
    }
}
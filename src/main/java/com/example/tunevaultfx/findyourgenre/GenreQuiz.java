package com.example.tunevaultfx.findyourgenre;

import java.util.List;

/**
 * Contains all quiz questions and their genre mappings.
 *
 * Extracted from FindYourGenrePageController so that:
 *  - Adding or editing a question touches only this file, not the controller.
 *  - The controller stays focused on UI logic only.
 *
 * Each Question maps its four answer options to four genre labels.
 * Whichever genre accumulates the most points wins.
 */
public final class GenreQuiz {

    private GenreQuiz() {}

    public static List<Question> getQuestions() {
        return List.of(
                new Question(
                        "What kind of night sounds best to you?",
                        "Big party",       "Long drive",       "Coffee shop",    "Loud concert",
                        "Pop",             "Synthwave",         "Jazz",           "Rock"),

                new Question(
                        "Pick a music vibe:",
                        "Catchy",          "Dreamy",            "Emotional",      "Heavy",
                        "Pop",             "Indie",             "R&B",            "Rock"),

                new Question(
                        "What instrument grabs your attention most?",
                        "Vocals",          "Synths",            "Saxophone",      "Guitar",
                        "Pop",             "Synthwave",         "Jazz",           "Rock"),

                new Question(
                        "Which setting fits your taste most?",
                        "Dance floor",     "Late-night city",   "Lounge",         "Festival stage",
                        "Pop",             "Synthwave",         "Jazz",           "Rock")
        );
    }

    // ── Question record ────────────────────────────────────────────

    /**
     * One quiz question with four answer/genre pairs.
     * Adding a new question means adding one entry to getQuestions() — nothing else.
     */
    public record Question(
            String prompt,
            String option1, String option2, String option3, String option4,
            String genre1,  String genre2,  String genre3,  String genre4
    ) {
        /** Returns the genre that corresponds to the chosen answer index (0–3). */
        public String genreFor(int index) {
            return switch (index) {
                case 0  -> genre1;
                case 1  -> genre2;
                case 2  -> genre3;
                default -> genre4;
            };
        }

        /** Returns the display text for the given answer index (0–3). */
        public String optionFor(int index) {
            return switch (index) {
                case 0  -> option1;
                case 1  -> option2;
                case 2  -> option3;
                default -> option4;
            };
        }
    }
}
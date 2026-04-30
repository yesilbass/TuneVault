package com.example.tunevaultfx.db;

import com.example.tunevaultfx.findyourgenre.GenreQuiz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads genre-discovery quiz questions and their answers from the database.
 *
 * <p>Questions are organised into 5 sessions of 10 questions each (50 total).
 * The session served to the user is determined by their {@code quiz_session_count}
 * in {@code user_genre_discovery} (mod 5, so it cycles back after 5 completions).</p>
 *
 * <p>QUICK mode picks 5 random questions from the full 10 in the session.
 * FULL mode uses all 10, also shuffled so order varies between attempts.</p>
 */
public final class QuizQuestionDAO {

    private static final int QUESTIONS_PER_SESSION = 10;
    private static final int QUICK_QUESTION_COUNT  = 5;
    private static final int TOTAL_SESSIONS        = 5;

    /**
     * Returns the next session number (1–5) for the given user based on how
     * many times they have completed the quiz. Cycles back to 1 after session 5.
     * Returns 1 for new users or users with no saved profile.
     */
    public int nextSessionFor(String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return 1;
        }
        String sql = """
                SELECT ugd.quiz_session_count
                FROM user_genre_discovery ugd
                JOIN app_user u ON u.user_id = ugd.user_id
                WHERE u.username = ?
                LIMIT 1
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 1;
                }
                int count = rs.getInt("quiz_session_count");
                // count is the number of completions so far; next session = (count % 5) + 1
                return (count % TOTAL_SESSIONS) + 1;
            }
        }
    }

    /**
     * Loads and shuffles quiz questions for the given session and mode.
     *
     * <ul>
     *   <li>FULL — all 10 questions from the session, shuffled.</li>
     *   <li>QUICK — 5 randomly selected questions from the same 10.</li>
     * </ul>
     *
     * @param mode          QUICK or FULL
     * @param sessionNumber 1–5
     * @return shuffled list of {@link GenreQuiz.Question}, never null
     * @throws SQLException on any DB error
     */
    public List<GenreQuiz.Question> loadQuestions(GenreQuiz.QuizMode mode, int sessionNumber)
            throws SQLException {
        int session = Math.max(1, Math.min(TOTAL_SESSIONS, sessionNumber));

        // 1. Load all question rows for this session
        List<QuestionRow> rows = loadQuestionRows(session);
        if (rows.isEmpty()) {
            return List.of();
        }

        // 2. Load answers in bulk
        Map<Integer, List<AnswerRow>> answersByQuestion = loadAnswers(rows);

        // 3. Assemble GenreQuiz.Question records
        List<GenreQuiz.Question> questions = new ArrayList<>(rows.size());
        for (QuestionRow qr : rows) {
            List<AnswerRow> answers = answersByQuestion.getOrDefault(qr.id(), List.of());
            if (answers.size() < 4) {
                continue; // skip incomplete questions
            }
            AnswerRow a1 = answers.get(0);
            AnswerRow a2 = answers.get(1);
            AnswerRow a3 = answers.get(2);
            AnswerRow a4 = answers.get(3);
            questions.add(new GenreQuiz.Question(
                    qr.prompt(),
                    a1.text(), a2.text(), a3.text(), a4.text(),
                    a1.genreName(), a2.genreName(), a3.genreName(), a4.genreName(),
                    a1.weight(), a2.weight(), a3.weight(), a4.weight()
            ));
        }

        // 4. Shuffle — both modes get a random order
        Collections.shuffle(questions);

        // 5. QUICK takes first 5 after shuffle
        if (mode == GenreQuiz.QuizMode.QUICK) {
            return List.copyOf(questions.subList(0, Math.min(QUICK_QUESTION_COUNT, questions.size())));
        }
        return List.copyOf(questions);
    }

    private static List<QuestionRow> loadQuestionRows(int sessionNumber) throws SQLException {
        String sql = """
                SELECT question_id, prompt
                FROM quiz_question
                WHERE is_active = 1 AND session_number = ?
                ORDER BY display_order ASC
                """;
        List<QuestionRow> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new QuestionRow(rs.getInt("question_id"), rs.getString("prompt")));
                }
            }
        }
        return rows;
    }

    private static Map<Integer, List<AnswerRow>> loadAnswers(List<QuestionRow> questions)
            throws SQLException {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        String sql = "SELECT question_id, answer_text, genre_name, weight, answer_order "
                + "FROM quiz_answer "
                + "WHERE question_id IN (" + placeholders + ") "
                + "ORDER BY question_id ASC, answer_order ASC";

        Map<Integer, List<AnswerRow>> map = new LinkedHashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < questions.size(); i++) {
                ps.setInt(i + 1, questions.get(i).id());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qid = rs.getInt("question_id");
                    map.computeIfAbsent(qid, k -> new ArrayList<>())
                            .add(new AnswerRow(
                                    rs.getString("answer_text"),
                                    rs.getString("genre_name"),
                                    Math.max(1, rs.getInt("weight"))
                            ));
                }
            }
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // Private data holders
    // -------------------------------------------------------------------------

    private record QuestionRow(int id, String prompt) {}
    private record AnswerRow(String text, String genreName, int weight) {}
}
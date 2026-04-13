package com.example.tunevaultfx.findyourgenre;

import com.example.tunevaultfx.util.SceneUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controls the Find Your Genre quiz page.
 *
 * Reduced from 120 lines to ~70 lines by extracting all quiz data
 * into GenreQuiz. This controller now only handles UI state transitions.
 *
 * To add a new question: edit GenreQuiz.getQuestions() only.
 */
public class FindYourGenrePageController {

    @FXML private Label  questionLabel;
    @FXML private Label  progressLabel;
    @FXML private Label  resultLabel;

    @FXML private Button answerButton1;
    @FXML private Button answerButton2;
    @FXML private Button answerButton3;
    @FXML private Button answerButton4;
    @FXML private Button restartButton;

    private final List<GenreQuiz.Question> questions = GenreQuiz.getQuestions();
    private final Map<String, Integer>     scores    = new HashMap<>();
    private int currentIndex = 0;

    // ── Lifecycle ─────────────────────────────────────────────────

    @FXML
    public void initialize() {
        resetQuiz();
    }

    // ── Answer handlers ───────────────────────────────────────────

    @FXML private void handleAnswer1() { chooseAnswer(0); }
    @FXML private void handleAnswer2() { chooseAnswer(1); }
    @FXML private void handleAnswer3() { chooseAnswer(2); }
    @FXML private void handleAnswer4() { chooseAnswer(3); }
    @FXML private void handleRestartQuiz() { resetQuiz(); }

    @FXML
    private void handleBackToMenu(ActionEvent event) throws IOException {
        SceneUtil.switchScene((Node) event.getSource(), "main-menu.fxml");
    }

    // ── Quiz logic ────────────────────────────────────────────────

    private void chooseAnswer(int answerIndex) {
        GenreQuiz.Question question = questions.get(currentIndex);
        String genre = question.genreFor(answerIndex);
        scores.merge(genre, 1, Integer::sum);
        currentIndex++;

        if (currentIndex >= questions.size()) showResult();
        else loadQuestion();
    }

    private void loadQuestion() {
        GenreQuiz.Question q = questions.get(currentIndex);
        progressLabel.setText("Question " + (currentIndex + 1) + " of " + questions.size());
        questionLabel.setText(q.prompt());
        answerButton1.setText(q.option1());
        answerButton2.setText(q.option2());
        answerButton3.setText(q.option3());
        answerButton4.setText(q.option4());
        resultLabel.setText("");
        restartButton.setVisible(false);
        setAnswerButtonsDisabled(false);
    }

    private void showResult() {
        String best = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Pop");

        progressLabel.setText("Quiz Complete");
        questionLabel.setText("Your best genre match is…");
        resultLabel.setText(best + " \uD83C\uDFB5");

        setAnswerButtonsDisabled(true);
        restartButton.setVisible(true);
    }

    private void resetQuiz() {
        scores.clear();
        currentIndex = 0;
        loadQuestion();
    }

    private void setAnswerButtonsDisabled(boolean disabled) {
        answerButton1.setDisable(disabled);
        answerButton2.setDisable(disabled);
        answerButton3.setDisable(disabled);
        answerButton4.setDisable(disabled);
    }
}
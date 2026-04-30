package com.example.tunevaultfx.findyourgenre;

import com.example.tunevaultfx.core.Song;
import com.example.tunevaultfx.db.SongDAO;
import com.example.tunevaultfx.db.QuizQuestionDAO;
import com.example.tunevaultfx.db.UserGenreDiscoveryDAO;
import com.example.tunevaultfx.musicplayer.controller.MusicPlayerController;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.util.ToastUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Find Your Genre — fast, low-pressure quiz with Quick/Full modes, skip, keyboard shortcuts,
 * DB persistence, and recommendation integration via {@link UserGenreDiscoveryDAO}.
 */
public class FindYourGenrePageController {

    private static final String RESULTS_CARD_STYLE = "genre-quiz-card--results";

    private static final String SUBTITLE_DEFAULT =
            "Choose Quick or Full, then Start quiz. Quick taps, no timer, no wrong answers — you can redo anytime.";
    private static final String SUBTITLE_RESULTS =
            "Here’s what we heard from your picks. Play a mix to hear it in your library, or run the quiz again — nothing is permanent.";

    @FXML private Label pageSubtitleLabel;
    @FXML private Label progressLabel;
    @FXML private Label questionLabel;
    @FXML private ProgressBar quizProgressBar;

    @FXML private Button answerButton1;
    @FXML private Button answerButton2;
    @FXML private Button answerButton3;
    @FXML private Button answerButton4;
    @FXML private Button skipQuestionButton;
    @FXML private Button restartButton;
    @FXML private Button playMixButton;
    @FXML private Button startQuizButton;

    @FXML private VBox quizContainer;
    @FXML private VBox genreQuizHero;
    @FXML private VBox genreQuizSideRail;
    @FXML private VBox quizCard;
    @FXML private VBox quizProgressBlock;
    @FXML private VBox lengthPanel;
    @FXML private VBox quickModeOffer;
    @FXML private VBox fullModeOffer;
    @FXML private VBox quizQuestionSection;
    @FXML private VBox quizInteractiveSection;
    @FXML private VBox resultSection;
    @FXML private Label resultsEyebrowLabel;
    @FXML private Label resultHeadlineLabel;
    @FXML private Label resultSummaryLabel;
    @FXML private Label savedHintLabel;
    @FXML private FlowPane resultChips;
    @FXML private HBox resultActionsRow;

    @FXML private RadioButton modeQuickRadio;
    @FXML private RadioButton modeFullRadio;

    private final ToggleGroup modeGroup = new ToggleGroup();

    private final UserGenreDiscoveryDAO genreDiscoveryDAO = new UserGenreDiscoveryDAO();
    private final QuizQuestionDAO quizQuestionDAO = new QuizQuestionDAO();

    /** Session number (1–5) for the current quiz run, loaded from DB on start. */
    private int currentSessionNumber = 1;
    private final SongDAO songDAO = new SongDAO();
    private final MusicPlayerController player = MusicPlayerController.getInstance();

    private List<GenreQuiz.Question> questions = List.of();
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    private int currentIndex = 0;
    private boolean quizComplete = false;
    /** True before the user confirms length with Start quiz (and after Try again). */
    private boolean awaitingStart = true;

    @FXML
    public void initialize() {
        modeQuickRadio.setToggleGroup(modeGroup);
        modeFullRadio.setToggleGroup(modeGroup);

        enterPickLengthState();

        modeGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null) {
                return;
            }
            if (awaitingStart) {
                syncPickLengthProgressHint();
            }
            syncModeOfferHighlight();
        });
        wireModeOfferClicks();
        syncModeOfferHighlight();

        Platform.runLater(() -> {
            Scene scene = quizContainer.getScene();
            applyResponsiveDensity(scene != null ? scene.getWidth() : 1280);
            List<Node> entrance = new ArrayList<>();
            if (genreQuizHero != null) {
                entrance.add(genreQuizHero);
            }
            if (quizCard != null) {
                entrance.add(quizCard);
            }
            if (genreQuizSideRail != null) {
                entrance.add(genreQuizSideRail);
            }
            UiMotionUtil.playStaggeredEntrance(entrance);
            UiMotionUtil.applyHoverLift(quizCard);
        });

        quizContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            applyResponsiveDensity(newScene.getWidth());
            newScene.widthProperty().addListener((o, oldW, newW) ->
                    applyResponsiveDensity(newW.doubleValue()));

            if (newScene.getProperties().containsKey("genreQuizKeysInstalled")) {
                return;
            }
            newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSceneKey);
            newScene.getProperties().put("genreQuizKeysInstalled", true);
        });
    }

    private void handleSceneKey(KeyEvent event) {
        if (awaitingStart || quizComplete || questions.isEmpty()) {
            return;
        }
        KeyCode c = event.getCode();
        int idx = switch (c) {
            case DIGIT1, NUMPAD1 -> 0;
            case DIGIT2, NUMPAD2 -> 1;
            case DIGIT3, NUMPAD3 -> 2;
            case DIGIT4, NUMPAD4 -> 3;
            default -> -1;
        };
        if (idx >= 0) {
            chooseAnswer(idx);
            event.consume();
        }
    }

    private void applyResponsiveDensity(double width) {
        boolean showSideRail = width >= 960;
        if (genreQuizSideRail != null) {
            genreQuizSideRail.setVisible(showSideRail);
            genreQuizSideRail.setManaged(showSideRail);
        }
        if (quizCard != null) {
            quizCard.setPrefWidth(Region.USE_COMPUTED_SIZE);
            quizCard.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void wireModeOfferClicks() {
        if (quickModeOffer != null) {
            quickModeOffer.setOnMouseClicked(
                    e -> {
                        modeQuickRadio.setSelected(true);
                        e.consume();
                    });
        }
        if (fullModeOffer != null) {
            fullModeOffer.setOnMouseClicked(
                    e -> {
                        modeFullRadio.setSelected(true);
                        e.consume();
                    });
        }
    }

    private void syncModeOfferHighlight() {
        if (quickModeOffer != null) {
            quickModeOffer.getStyleClass().remove("genre-quiz-mode-offer--on");
        }
        if (fullModeOffer != null) {
            fullModeOffer.getStyleClass().remove("genre-quiz-mode-offer--on");
        }
        if (modeQuickRadio != null && modeQuickRadio.isSelected() && quickModeOffer != null) {
            quickModeOffer.getStyleClass().add("genre-quiz-mode-offer--on");
        }
        if (modeFullRadio != null && modeFullRadio.isSelected() && fullModeOffer != null) {
            fullModeOffer.getStyleClass().add("genre-quiz-mode-offer--on");
        }
    }

    @FXML private void handleAnswer1() { chooseAnswer(0); }
    @FXML private void handleAnswer2() { chooseAnswer(1); }
    @FXML private void handleAnswer3() { chooseAnswer(2); }
    @FXML private void handleAnswer4() { chooseAnswer(3); }

    @FXML private void handleSkipQuestion() {
        if (awaitingStart || quizComplete || questions.isEmpty()) {
            return;
        }
        currentIndex++;
        if (currentIndex >= questions.size()) {
            showResult();
        } else {
            loadQuestion();
        }
    }

    @FXML private void handleRestartQuiz() {
        // Do NOT clear scores — quiz results stack across sessions.
        // The next session will serve a fresh set of questions.
        // Use Settings > Reset Quiz Results to wipe the profile entirely.
        enterPickLengthState(false);
    }

    @FXML
    private void handleStartQuiz() {
        if (!awaitingStart) {
            return;
        }
        // Load the session number for this user from the DB
        String user = SessionManager.getCurrentUsername();
        if (user != null && !user.isBlank()) {
            try {
                currentSessionNumber = quizQuestionDAO.nextSessionFor(user);
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
                currentSessionNumber = 1;
            }
        }
        questions = GenreQuiz.questionsFor(currentMode(), currentSessionNumber);
        if (questions.isEmpty()) {
            questionLabel.setText("No questions loaded for this mode.");
            return;
        }
        awaitingStart = false;
        setStartQuizButtonVisible(false);
        setLengthPanelVisible(false);
        setQuizProgressVisible(true);
        setQuizQuestionVisible(true);
        setQuizInteractiveVisible(true);
        setModeSelectionEnabled(false);
        currentIndex = 0;
        if (pageSubtitleLabel != null) {
            pageSubtitleLabel.setText(SUBTITLE_DEFAULT);
        }
        loadQuestion();
    }

    @FXML private void handlePlayMix() {
        if (scores.isEmpty()) {
            return;
        }
        List<String> keys = GenreQuiz.topGenreKeys(scores, 5);
        if (keys.isEmpty()) {
            return;
        }

        ObservableList<Song> lib;
        try {
            if (SessionManager.isSongLibraryReady()) {
                lib = SessionManager.getSongLibrary();
            } else {
                lib = songDAO.getAllSongs();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ToastUtil.error(quizCard.getScene(), "Could not load songs for your mix.");
            return;
        }

        List<Song> mix = lib.stream()
                .filter(s -> s != null && genreMatchesAny(s, keys))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(mix);
        if (mix.isEmpty()) {
            mix = new ArrayList<>(lib);
            Collections.shuffle(mix);
        }
        mix = mix.stream().limit(40).collect(Collectors.toList());
        if (mix.isEmpty()) {
            ToastUtil.info(quizCard.getScene(), "No songs in your library yet — add tracks to hear a mix.");
            return;
        }
        player.playQueue(FXCollections.observableArrayList(mix), 0, "Genre mix");
        ToastUtil.success(quizCard.getScene(), "Playing your genre mix");
    }

    private static boolean genreMatchesAny(Song s, List<String> normalizedKeys) {
        String g = normalizeGenre(s.genre());
        if (g.isEmpty()) {
            return false;
        }
        for (String k : normalizedKeys) {
            if (g.equals(k) || g.contains(k) || k.contains(g)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeGenre(String g) {
        return g == null ? "" : g.trim().toLowerCase();
    }

    private void chooseAnswer(int answerIndex) {
        if (awaitingStart || quizComplete || questions.isEmpty()) {
            return;
        }
        GenreQuiz.Question q = questions.get(currentIndex);
        scores.merge(q.genreFor(answerIndex), q.weightFor(answerIndex), Integer::sum);
        currentIndex++;
        if (currentIndex >= questions.size()) {
            showResult();
        } else {
            loadQuestion();
        }
    }

    private void loadQuestion() {
        quizComplete = false;
        setResultsCardStyle(false);
        setStartQuizButtonVisible(false);
        setQuizProgressVisible(true);
        setQuizQuestionVisible(!awaitingStart);
        setQuizInteractiveVisible(!awaitingStart);
        setLengthPanelVisible(awaitingStart);
        resultSection.setVisible(false);
        resultSection.setManaged(false);
        if (pageSubtitleLabel != null && !awaitingStart) {
            pageSubtitleLabel.setText(SUBTITLE_DEFAULT);
        }

        GenreQuiz.Question q = questions.get(currentIndex);
        int total = questions.size();
        progressLabel.setText("Question " + (currentIndex + 1) + " of " + total);
        quizProgressBar.setProgress(total == 0 ? 0 : (currentIndex + 1.0) / total);
        questionLabel.setText(q.prompt());
        answerButton1.setText(q.optionFor(0));
        answerButton2.setText(q.optionFor(1));
        answerButton3.setText(q.optionFor(2));
        answerButton4.setText(q.optionFor(3));
        setAnswerButtonsDisabled(false);
        skipQuestionButton.setDisable(false);
        restartButton.setDisable(false);
        playMixButton.setDisable(true);
        setModeSelectionEnabled(false);
    }

    private void showResult() {
        quizComplete = true;
        setStartQuizButtonVisible(false);
        setLengthPanelVisible(false);
        setQuizInteractiveVisible(false);

        setResultsCardStyle(true);
        setQuizProgressVisible(false);
        setQuizQuestionVisible(false);
        if (pageSubtitleLabel != null) {
            pageSubtitleLabel.setText(SUBTITLE_RESULTS);
        }

        GenreQuizAnalysis.AnalysisResult analysis = GenreQuizAnalysis.analyze(scores);
        List<String> top = analysis.topGenresDisplay();
        resultChips.getChildren().clear();

        if (resultsEyebrowLabel != null) {
            resultsEyebrowLabel.setText("YOUR GENRE PROFILE");
        }

        if (top.size() > 1) {
            for (String g : top) {
                Label chip = new Label(g);
                chip.getStyleClass().addAll("genre-result-chip", "genre-result-chip-hero");
                resultChips.getChildren().add(chip);
            }
        }
        if (resultChips != null) {
            boolean showChips = top.size() > 1;
            resultChips.setVisible(showChips);
            resultChips.setManaged(showChips);
        }
        resultHeadlineLabel.setText(analysis.headline());
        resultSummaryLabel.setText(analysis.summaryForUi());

        playMixButton.setDisable(analysis.isEmptySignal());
        restartButton.setDisable(false);
        setModeSelectionEnabled(true);

        resultSection.setVisible(true);
        resultSection.setManaged(true);

        persistDiscovery(top, analysis);

        Platform.runLater(() -> {
            List<Node> entrance = new ArrayList<>(List.of(
                    resultsEyebrowLabel,
                    resultHeadlineLabel,
                    resultSummaryLabel));
            if (top.size() > 1 && resultChips != null) {
                entrance.add(resultChips);
            }
            entrance.add(savedHintLabel);
            entrance.add(resultActionsRow);
            UiMotionUtil.playStaggeredEntrance(entrance);
        });
    }

    private void persistDiscovery(List<String> topDisplay, GenreQuizAnalysis.AnalysisResult analysis) {
        String user = SessionManager.getCurrentUsername();
        if (user == null || user.isBlank()) {
            savedHintLabel.setText("Sign in to save your mix to your account.");
            return;
        }

        GenreQuiz.QuizMode mode = currentMode();
        Map<String, Double> boosts = analysis.recommendationBoosts();
        if (boosts.isEmpty()) {
            savedHintLabel.setText("");
            return;
        }

        String one;
        if (!topDisplay.isEmpty()) {
            one = topDisplay.get(0);
        } else {
            one = GenreQuiz.formatGenreTitle(
                    Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey());
        }
        String two = topDisplay.size() > 1 ? topDisplay.get(1) : null;
        String three = topDisplay.size() > 2 ? topDisplay.get(2) : null;

        try {
            genreDiscoveryDAO.save(user, mode.name(), one, two, three, boosts);
            savedHintLabel.setText(
                    "Saved to your profile — recommendations, search, Wrapped, and autoplay use this taste signal.");
        } catch (SQLException e) {
            e.printStackTrace();
            savedHintLabel.setText("Could not save to the database. Your mix still plays locally.");
            ToastUtil.error(quizCard.getScene(), "Could not save genre profile. Check your database connection.");
        }
    }

    private GenreQuiz.QuizMode currentMode() {
        return modeFullRadio.isSelected() ? GenreQuiz.QuizMode.FULL : GenreQuiz.QuizMode.QUICK;
    }

    private void enterPickLengthState() {
        enterPickLengthState(true);
    }

    /**
     * @param clearScores true on first load / full reset; false on Try Again
     *                    so this session's scores are not lost before they are persisted.
     */
    private void enterPickLengthState(boolean clearScores) {
        if (clearScores) {
            scores.clear();
        }
        currentIndex = 0;
        quizComplete = false;
        awaitingStart = true;
        questions = List.of();

        setResultsCardStyle(false);
        setLengthPanelVisible(true);
        setQuizProgressVisible(true);
        setQuizQuestionVisible(false);
        setQuizInteractiveVisible(false);
        resultSection.setVisible(false);
        resultSection.setManaged(false);
        resultSummaryLabel.setText("");
        resultHeadlineLabel.setText("");
        savedHintLabel.setText("");
        resultChips.getChildren().clear();
        if (resultChips != null) {
            resultChips.setVisible(true);
            resultChips.setManaged(true);
        }

        if (pageSubtitleLabel != null) {
            pageSubtitleLabel.setText(SUBTITLE_DEFAULT);
        }
        if (resultsEyebrowLabel != null) {
            resultsEyebrowLabel.setText("YOUR GENRE PROFILE");
        }

        quizProgressBar.setProgress(0);
        questionLabel.setText("Pick a length above, then tap Start quiz.");
        answerButton1.setText("");
        answerButton2.setText("");
        answerButton3.setText("");
        answerButton4.setText("");
        setAnswerButtonsDisabled(true);
        skipQuestionButton.setDisable(true);
        playMixButton.setDisable(true);

        setModeSelectionEnabled(true);
        setStartQuizButtonVisible(true);
        syncPickLengthProgressHint();
        syncModeOfferHighlight();
    }

    private void syncPickLengthProgressHint() {
        if (!awaitingStart) {
            return;
        }
        int n = GenreQuiz.questionCount(currentMode());
        boolean quick = currentMode() == GenreQuiz.QuizMode.QUICK;
        progressLabel.setText((quick ? "Quick" : "Full") + " — " + n + " question" + (n == 1 ? "" : "s"));
        quizProgressBar.setProgress(0);
    }

    private void setStartQuizButtonVisible(boolean visible) {
        if (startQuizButton == null) {
            return;
        }
        startQuizButton.setVisible(visible);
        startQuizButton.setManaged(visible);
    }

    private void setLengthPanelVisible(boolean visible) {
        if (lengthPanel == null) {
            return;
        }
        lengthPanel.setVisible(visible);
        lengthPanel.setManaged(visible);
    }

    private void setQuizProgressVisible(boolean visible) {
        if (quizProgressBlock == null) {
            return;
        }
        quizProgressBlock.setVisible(visible);
        quizProgressBlock.setManaged(visible);
    }

    private void setQuizQuestionVisible(boolean visible) {
        if (quizQuestionSection == null) {
            return;
        }
        quizQuestionSection.setVisible(visible);
        quizQuestionSection.setManaged(visible);
    }

    private void setQuizInteractiveVisible(boolean visible) {
        if (quizInteractiveSection == null) {
            return;
        }
        quizInteractiveSection.setVisible(visible);
        quizInteractiveSection.setManaged(visible);
    }

    private void setResultsCardStyle(boolean results) {
        if (quizCard == null) {
            return;
        }
        ObservableList<String> classes = quizCard.getStyleClass();
        if (results) {
            if (!classes.contains(RESULTS_CARD_STYLE)) {
                classes.add(RESULTS_CARD_STYLE);
            }
        } else {
            classes.remove(RESULTS_CARD_STYLE);
        }
    }

    private void setModeSelectionEnabled(boolean enabled) {
        modeQuickRadio.setDisable(!enabled);
        modeFullRadio.setDisable(!enabled);
    }

    private void setAnswerButtonsDisabled(boolean disabled) {
        answerButton1.setDisable(disabled);
        answerButton2.setDisable(disabled);
        answerButton3.setDisable(disabled);
        answerButton4.setDisable(disabled);
    }
}
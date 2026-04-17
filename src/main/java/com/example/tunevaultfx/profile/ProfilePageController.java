package com.example.tunevaultfx.profile;

import com.example.tunevaultfx.db.ListeningEventDAO;
import com.example.tunevaultfx.db.PlaylistDAO;
import com.example.tunevaultfx.db.UserDAO;
import com.example.tunevaultfx.db.UserFollowDAO;
import com.example.tunevaultfx.db.UserGenreDiscoveryDAO;
import com.example.tunevaultfx.db.UserGenreDiscoverySummary;
import com.example.tunevaultfx.profile.media.ProfileAvatarCropDialog;
import com.example.tunevaultfx.profile.media.ProfileMediaStorage;
import com.example.tunevaultfx.session.SessionManager;
import com.example.tunevaultfx.user.User;
import com.example.tunevaultfx.util.AppTheme;
import com.example.tunevaultfx.user.UserProfile;
import com.example.tunevaultfx.util.SceneUtil;
import com.example.tunevaultfx.util.ToastUtil;
import com.example.tunevaultfx.util.UiMotionUtil;
import com.example.tunevaultfx.view.FxmlResources;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

/**
 * Account profile: own settings, or another member's public view (search / social).
 */
public class ProfilePageController {

    @FXML private VBox profilePageRoot;
    @FXML private ImageView profileAvatarImage;
    @FXML private Label profileAvatarInitial;
    @FXML private Label profileDisplayName;
    @FXML private Label profileHandleLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileAccountIdLabel;
    @FXML private Label statPlaylistsValue;
    @FXML private Label statSavedValue;
    @FXML private Label statListeningValue;
    @FXML private Label statListeningHint;
    @FXML private Label statCaptionPlaylists;
    @FXML private Label statCaptionSaved;
    @FXML private FlowPane genreChipsFlow;
    @FXML private Label genreSummaryLabel;
    @FXML private VBox goToGenreQuizBlock;
    @FXML private VBox clearGenreQuizBlock;
    @FXML private Button clearGenreQuizButton;
    @FXML private Label listeningSummaryLabel;
    @FXML private Button changeAvatarButton;
    @FXML private Button removeAvatarButton;
    @FXML private Button followUserButton;
    @FXML private VBox socialSection;
    @FXML private ListView<String> followingListView;
    @FXML private ListView<String> followersListView;

    private final UserDAO userDAO = new UserDAO();
    private final UserGenreDiscoveryDAO genreDiscoveryDAO = new UserGenreDiscoveryDAO();
    private final ListeningEventDAO listeningEventDAO = new ListeningEventDAO();
    private final UserFollowDAO userFollowDAO = new UserFollowDAO();
    private final PlaylistDAO playlistDAO = new PlaylistDAO();
    private final ProfileMediaPersistence mediaPersistence = new ProfileMediaPersistence(userDAO);

    private String sessionUsername;
    /** Profile being rendered (self or another user). */
    private String subjectUsername;
    private boolean viewingSelf;
    private User loadedUser;

    @FXML
    public void initialize() {
        setupAvatarClip();

        sessionUsername = SessionManager.getCurrentUsername();
        String view = SessionManager.getProfileViewUsername();
        if (sessionUsername != null
                && view != null
                && !view.isBlank()
                && view.trim().equalsIgnoreCase(sessionUsername.trim())) {
            SessionManager.clearProfileViewUsername();
            view = null;
        }

        if (sessionUsername == null || sessionUsername.isBlank()) {
            applySignedOutPlaceholder();
            hideCommunitySections();
            return;
        }

        subjectUsername =
                (view != null && !view.isBlank()) ? view.trim() : sessionUsername.trim();
        viewingSelf = subjectUsername.equalsIgnoreCase(sessionUsername);

        loadIdentity(subjectUsername, viewingSelf);
        applyStatCaptions(viewingSelf);
        loadStatsRow(subjectUsername, viewingSelf);
        loadSocial(subjectUsername);
        wireSocialNavigation();
        // Listening copy only; third stat tile for others is already "Following" from loadStatsRow.
        loadListeningBlock(subjectUsername, viewingSelf);
        loadTasteSection(subjectUsername, viewingSelf);
        configureFollowButton();

        changeAvatarButton.setOnAction(e -> pickAndApplyAvatar());
        removeAvatarButton.setOnAction(e -> clearAvatar());
        changeAvatarButton.setDisable(!viewingSelf);
        removeAvatarButton.setDisable(!viewingSelf);

        Platform.runLater(
                () -> {
                    if (profilePageRoot != null) {
                        UiMotionUtil.playStaggeredEntrance(
                                profilePageRoot.getChildren().stream().toList());
                    }
                });
    }

    private void hideCommunitySections() {
        setSectionVisible(socialSection, false);
        if (followUserButton != null) {
            followUserButton.setVisible(false);
            followUserButton.setManaged(false);
        }
    }

    private static void setSectionVisible(VBox section, boolean on) {
        if (section != null) {
            section.setVisible(on);
            section.setManaged(on);
        }
    }

    private void setupAvatarClip() {
        profileAvatarImage.setClip(new Circle(64, 64, 64));
    }

    private void applySignedOutPlaceholder() {
        profileAvatarImage.setImage(null);
        profileAvatarInitial.setText("?");
        profileAvatarInitial.setVisible(true);
        profileDisplayName.setText("Not signed in");
        profileHandleLabel.setText("");
        profileEmailLabel.setText("Sign in to view your TuneVault profile.");
        profileAccountIdLabel.setText("");
        statPlaylistsValue.setText("\u2014");
        statSavedValue.setText("\u2014");
        statListeningValue.setText("\u2014");
        statListeningHint.setText("");
        genreChipsFlow.getChildren().clear();
        Label ph = new Label("Unavailable");
        ph.getStyleClass().add("profile-genre-chip-muted");
        genreChipsFlow.getChildren().add(ph);
        genreSummaryLabel.setText("");
        listeningSummaryLabel.setText("Listening history is tied to your account.");
        changeAvatarButton.setDisable(true);
        removeAvatarButton.setDisable(true);
        if (goToGenreQuizBlock != null) {
            goToGenreQuizBlock.setVisible(false);
            goToGenreQuizBlock.setManaged(false);
        }
        if (clearGenreQuizBlock != null) {
            clearGenreQuizBlock.setVisible(false);
            clearGenreQuizBlock.setManaged(false);
        }
    }

    private void applyStatCaptions(boolean self) {
        if (statCaptionPlaylists != null) {
            statCaptionPlaylists.setText(self ? "Playlists" : "Public playlists");
        }
        if (statCaptionSaved != null) {
            statCaptionSaved.setText(self ? "Saved songs" : "Followers");
        }
        statListeningHint.setText(self ? "Counted plays" : "Following");
    }

    private void loadStatsRow(String subject, boolean self) {
        if (self) {
            UserProfile profile = SessionManager.getCurrentUserProfile();
            int playlistCount =
                    profile != null && profile.getPlaylists() != null ? profile.getPlaylists().size() : 0;
            statPlaylistsValue.setText(String.valueOf(playlistCount));
            statSavedValue.setText(String.valueOf(ProfileLibraryStats.countUniqueSavedSongs(profile)));
            try {
                Optional<ListeningEventDAO.ListeningProfileStats> opt =
                        listeningEventDAO.loadListeningProfileStats(subject);
                if (opt.isEmpty()) {
                    statListeningValue.setText("0");
                } else {
                    statListeningValue.setText(
                            ProfileTextFormat.formatNumber(opt.get().countedPlays()));
                }
            } catch (SQLException e) {
                statListeningValue.setText("\u2014");
            }
            return;
        }

        try {
            int pub = playlistDAO.listPublicPlaylistNamesForUser(subject).size();
            statPlaylistsValue.setText(String.valueOf(pub));
            statSavedValue.setText(String.valueOf(userFollowDAO.countFollowers(subject)));
            statListeningValue.setText(String.valueOf(userFollowDAO.countFollowing(subject)));
        } catch (SQLException e) {
            e.printStackTrace();
            statPlaylistsValue.setText("\u2014");
            statSavedValue.setText("\u2014");
            statListeningValue.setText("\u2014");
        }
    }

    private void loadSocial(String subject) {
        if (followingListView == null || followersListView == null) {
            return;
        }
        try {
            followingListView.setItems(
                    FXCollections.observableArrayList(userFollowDAO.listFollowingUsernames(subject, 200)));
            followersListView.setItems(
                    FXCollections.observableArrayList(userFollowDAO.listFollowerUsernames(subject, 200)));
        } catch (SQLException e) {
            e.printStackTrace();
            followingListView.setItems(FXCollections.observableArrayList());
            followersListView.setItems(FXCollections.observableArrayList());
        }
    }

    private void wireSocialNavigation() {
        if (followingListView != null) {
            followingListView.setOnMouseClicked(
                    e -> {
                        if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {
                            return;
                        }
                        openProfileUser(followingListView.getSelectionModel().getSelectedItem());
                    });
        }
        if (followersListView != null) {
            followersListView.setOnMouseClicked(
                    e -> {
                        if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {
                            return;
                        }
                        openProfileUser(followersListView.getSelectionModel().getSelectedItem());
                    });
        }
    }

    private void openProfileUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        SessionManager.setProfileViewUsername(username.trim());
        try {
            SceneUtil.switchScene(profilePageRoot, FxmlResources.PROFILE);
        } catch (IOException ex) {
            ex.printStackTrace();
            ToastUtil.error(scene(), "Could not open profile.");
        }
    }

    private void configureFollowButton() {
        if (followUserButton == null) {
            return;
        }
        if (viewingSelf) {
            followUserButton.setVisible(false);
            followUserButton.setManaged(false);
            return;
        }
        followUserButton.setVisible(true);
        followUserButton.setManaged(true);
        refreshFollowButtonLabel();
        followUserButton.setOnAction(
                e -> {
                    try {
                        if (userFollowDAO.isFollowing(sessionUsername, subjectUsername)) {
                            userFollowDAO.unfollow(sessionUsername, subjectUsername);
                        } else {
                            userFollowDAO.follow(sessionUsername, subjectUsername);
                        }
                        refreshFollowButtonLabel();
                        loadSocial(subjectUsername);
                        loadStatsRow(subjectUsername, false);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        ToastUtil.error(scene(), "Could not update follow state.");
                    }
                });
    }

    private void refreshFollowButtonLabel() {
        if (followUserButton == null || viewingSelf) {
            return;
        }
        try {
            boolean f = userFollowDAO.isFollowing(sessionUsername, subjectUsername);
            followUserButton.setText(f ? "Following" : "Follow");
        } catch (SQLException e) {
            followUserButton.setText("Follow");
        }
    }

    private void loadIdentity(String username, boolean self) {
        profileDisplayName.setText(username);
        profileHandleLabel.setText("@" + username.replaceAll("\\s+", "").toLowerCase(Locale.ROOT));
        profileAvatarInitial.setText(ProfileTextFormat.initialsFor(username));

        try {
            Optional<User> row = userDAO.findByUsername(username);
            if (row.isPresent()) {
                loadedUser = row.get();
                User u = loadedUser;
                if (self) {
                    profileEmailLabel.setText(u.getEmail() != null ? u.getEmail() : "\u2014");
                    profileAccountIdLabel.setText(
                            "Account ID \u00B7 " + u.getUserId() + "  \u00B7  Sign-in email is not public.");
                } else {
                    profileEmailLabel.setText("TuneVault member");
                    profileAccountIdLabel.setText("");
                }
                applyAvatarImage(u.getProfileAvatarKey());
                changeAvatarButton.setVisible(self);
                changeAvatarButton.setManaged(self);
                removeAvatarButton.setVisible(self);
                removeAvatarButton.setManaged(self);
            } else {
                loadedUser = null;
                profileEmailLabel.setText(self ? "\u2014" : "Unknown member");
                profileAccountIdLabel.setText("");
                applyAvatarImage(null);
                changeAvatarButton.setVisible(self);
                changeAvatarButton.setManaged(self);
                removeAvatarButton.setVisible(self);
                removeAvatarButton.setManaged(self);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            loadedUser = null;
            profileEmailLabel.setText("Could not load account (database may need migration).");
            profileAccountIdLabel.setText("");
            applyAvatarImage(null);
        }
    }

    private void applyAvatarImage(String relativeKey) {
        Path p = ProfileMediaStorage.resolveFile(relativeKey);
        if (p != null && Files.isRegularFile(p)) {
            Image img = new Image(p.toUri().toString(), false);
            if (!img.isError()) {
                profileAvatarImage.setImage(img);
                profileAvatarInitial.setVisible(false);
                return;
            }
        }
        profileAvatarImage.setImage(null);
        profileAvatarInitial.setVisible(true);
    }

    private void pickAndApplyAvatar() {
        if (!viewingSelf || loadedUser == null) {
            return;
        }
        File f = showOpenDialog("Choose profile photo");
        if (f == null) {
            return;
        }
        Stage owner = ownerStage();
        if (owner == null) {
            return;
        }
        Optional<Path> cropped;
        try {
            cropped = ProfileAvatarCropDialog.showAndExport(owner, f.toPath());
        } catch (IOException ex) {
            ex.printStackTrace();
            ToastUtil.error(scene(), ex.getMessage() != null ? ex.getMessage() : "Could not open image.");
            return;
        }
        if (cropped.isEmpty()) {
            return;
        }
        try {
            mediaPersistence.saveAvatarFromCroppedTemp(loadedUser, cropped.get());
            reloadUserAndRefreshAvatar();
            ToastUtil.success(scene(), "Profile photo updated");
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
            ToastUtil.error(scene(), ex.getMessage() != null ? ex.getMessage() : "Could not save photo.");
        }
    }

    private void clearAvatar() {
        if (!viewingSelf || loadedUser == null) {
            return;
        }
        try {
            mediaPersistence.clearAvatar(loadedUser);
            reloadUserAndRefreshAvatar();
            ToastUtil.info(scene(), "Profile photo removed");
        } catch (IOException | SQLException ex) {
            ex.printStackTrace();
            ToastUtil.error(scene(), "Could not remove photo.");
        }
    }

    private void reloadUserAndRefreshAvatar() throws SQLException {
        Optional<User> row = userDAO.findByUsername(sessionUsername);
        loadedUser = row.orElse(null);
        if (loadedUser != null && viewingSelf) {
            applyAvatarImage(loadedUser.getProfileAvatarKey());
            removeAvatarButton.setDisable(
                    loadedUser.getProfileAvatarKey() == null
                            || loadedUser.getProfileAvatarKey().isBlank());
        }
    }

    private File showOpenDialog(String title) {
        Stage stage = ownerStage();
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        return stage != null ? fc.showOpenDialog(stage) : null;
    }

    private Stage ownerStage() {
        return profilePageRoot.getScene() != null ? (Stage) profilePageRoot.getScene().getWindow() : null;
    }

    private javafx.scene.Scene scene() {
        return profilePageRoot != null ? profilePageRoot.getScene() : null;
    }

    private void loadListeningBlock(String username, boolean self) {
        if (!self) {
            listeningSummaryLabel.setText(
                    "Listening activity stays private. Only this member sees their own play counts.");
            return;
        }
        try {
            Optional<ListeningEventDAO.ListeningProfileStats> opt =
                    listeningEventDAO.loadListeningProfileStats(username);
            if (opt.isEmpty()) {
                statListeningValue.setText("0");
                statListeningHint.setText("Counted plays");
                listeningSummaryLabel.setText(
                        "No listening data yet. Play a few tracks — stats will show up after sessions complete.");
                return;
            }
            ListeningEventDAO.ListeningProfileStats s = opt.get();
            statListeningValue.setText(ProfileTextFormat.formatNumber(s.countedPlays()));
            statListeningHint.setText("Counted plays");
            listeningSummaryLabel.setText(
                    "You have "
                            + ProfileTextFormat.formatNumber(s.countedPlays())
                            + " counted play(s) and about "
                            + ProfileTextFormat.formatListeningDuration(s.listenedSeconds())
                            + " of tracked listening time in TuneVault.");
        } catch (SQLException e) {
            e.printStackTrace();
            statListeningValue.setText("\u2014");
            statListeningHint.setText("");
            listeningSummaryLabel.setText("Listening stats could not be loaded.");
        }
    }

    private void loadTasteSection(String username, boolean self) {
        genreChipsFlow.getChildren().clear();
        boolean showTakeGenreQuiz = false;
        try {
            Optional<UserGenreDiscoverySummary> opt = genreDiscoveryDAO.loadSummary(username);
            if (opt.isEmpty()) {
                showTakeGenreQuiz = true;
                Label chip = new Label("Not set");
                chip.getStyleClass().add("profile-genre-chip-muted");
                genreChipsFlow.getChildren().add(chip);
                genreSummaryLabel.setText(
                        self
                                ? "You haven\u2019t saved a genre profile yet. Take the quiz below when you want "
                                        + "recommendations and search to lean into a style."
                                : "This member hasn\u2019t shared a genre quiz profile.");
            } else {
                UserGenreDiscoverySummary s = opt.get();
                for (String part : s.blendParts()) {
                    Label chip = new Label(part);
                    chip.getStyleClass().add("profile-genre-chip");
                    genreChipsFlow.getChildren().add(chip);
                }
                String mode = s.quizModeLabel();
                String modePhrase =
                        mode.isEmpty()
                                ? "Saved from Find Your Genre."
                                : "Saved from a " + mode + " quiz.";
                genreSummaryLabel.setText(
                        self
                                ? modePhrase
                                        + " Retake Find Your Genre from the top bar anytime; your library and history stay as they are."
                                : modePhrase + " Used for their recommendations when they\u2019re signed in.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            genreChipsFlow.getChildren().clear();
            Label err = new Label("Could not load");
            err.getStyleClass().add("profile-genre-chip-muted");
            genreChipsFlow.getChildren().add(err);
            genreSummaryLabel.setText(ProfileGenreMessages.loadFailureHint(e));
        } finally {
            boolean hasSavedProfile = false;
            try {
                hasSavedProfile =
                        username != null
                                && !username.isBlank()
                                && genreDiscoveryDAO.hasSavedProfile(username);
            } catch (SQLException ignored) {
                // leave false
            }
            boolean showQuizUi = self && showTakeGenreQuiz;
            boolean showClear = self && hasSavedProfile;
            if (goToGenreQuizBlock != null) {
                goToGenreQuizBlock.setVisible(showQuizUi);
                goToGenreQuizBlock.setManaged(showQuizUi);
            }
            if (clearGenreQuizBlock != null) {
                clearGenreQuizBlock.setVisible(showClear);
                clearGenreQuizBlock.setManaged(showClear);
            }
            if (self) {
                updateClearGenreQuizButton(sessionUsername);
            } else if (clearGenreQuizButton != null) {
                clearGenreQuizButton.setDisable(true);
            }
        }
    }

    @FXML
    private void handleGoToGenreQuiz() {
        if (profilePageRoot == null || !viewingSelf) {
            return;
        }
        try {
            SceneUtil.switchScene(profilePageRoot, FxmlResources.FIND_YOUR_GENRE);
        } catch (IOException ex) {
            ex.printStackTrace();
            ToastUtil.error(scene(), "Could not open Find Your Genre.");
        }
    }

    @FXML
    private void handleClearGenreQuiz() {
        if (sessionUsername == null || sessionUsername.isBlank()) {
            return;
        }
        ProfileGenreClearConfirmOverlay.show(
                scene(),
                () -> {
                    try {
                        boolean removed = genreDiscoveryDAO.deleteForUser(sessionUsername);
                        if (!removed) {
                            ToastUtil.info(scene(), "No saved genre quiz to clear.");
                        } else {
                            ToastUtil.success(
                                    scene(),
                                    "Genre quiz cleared. Recommendations now use your listening only.");
                        }
                        loadTasteSection(sessionUsername, true);
                        if (profilePageRoot != null) {
                            AppTheme.refreshAllListViews(profilePageRoot);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        ToastUtil.error(
                                scene(), "Could not clear genre quiz. Check your database connection.");
                    }
                });
    }

    private void updateClearGenreQuizButton(String accountUsername) {
        if (clearGenreQuizButton == null) {
            return;
        }
        if (accountUsername == null || accountUsername.isBlank()) {
            clearGenreQuizButton.setDisable(true);
            return;
        }
        try {
            clearGenreQuizButton.setDisable(!genreDiscoveryDAO.hasSavedProfile(accountUsername));
        } catch (SQLException e) {
            clearGenreQuizButton.setDisable(true);
        }
    }
}

package com.example.tunevaultfx.view;

/**
 * FXML locations under classpath {@code /com/example/tunevaultfx/}, mirroring Java packages.
 * Pass path constants (suffix only) to {@link com.example.tunevaultfx.util.SceneUtil} navigation methods.
 */
public final class FxmlResources {

    /** Prefix for {@link Class#getResource(String)} absolute paths. */
    public static final String CLASSPATH_PREFIX = "/com/example/tunevaultfx/";

    private FxmlResources() {}

    public static final String CHROME_TOP_BAR = "chrome/app-top-bar.fxml";
    public static final String CHROME_SIDEBAR = "chrome/app-sidebar.fxml";

    public static final String MUSIC_MINI_PLAYER = "musicplayer/controller/mini-player.fxml";
    public static final String MUSIC_QUEUE_PANEL = "musicplayer/controller/queue-panel.fxml";
    public static final String MUSIC_EXPANDED_PLAYER = "musicplayer/controller/expanded-page.fxml";

    public static final String AUTH_LOGIN = "auth/login-page.fxml";
    public static final String AUTH_CREATE_ACCOUNT = "auth/create-account-page.fxml";
    public static final String AUTH_FORGOT_PASSWORD = "auth/forgot-password-page.fxml";

    public static final String MAIN_MENU = "mainmenu/main-menu.fxml";

    public static final String PROFILE = "profile/profile-page.fxml";
    public static final String SETTINGS = "settings/settings-page.fxml";

    public static final String FIND_YOUR_GENRE = "findyourgenre/findyourgenre-page.fxml";
    public static final String WRAPPED = "wrapped/wrapped-page.fxml";

    public static final String SEARCH = "search/search-page.fxml";
    public static final String ARTIST_PROFILE = "profile/artist-profile-page.fxml";

    public static final String PLAYLISTS = "playlist/playlists-page.fxml";
}

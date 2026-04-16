# TuneVault

TuneVault is a JavaFX desktop music app project.

It currently includes:

- account creation, login, and password reset
- playlist creation and playlist editing
- song search and artist browsing
- shared playback controls through a mini player
- an expanded player overlay and queue panel
- wrapped-style listening stats
- a "Find Your Genre" quiz
- recommendation and search-ranking features

The project is organized by responsibility so it is easier to read, maintain, and extend.

### Documentation in this repository

- **[CODEMAP.md](CODEMAP.md)** — package layout, major classes, and where to start reading the code.
- **[CONTRIBUTING.md](CONTRIBUTING.md)** — license expectations, how to contribute if authorized, and bug-report etiquette.
- **[SECURITY.md](SECURITY.md)** — how to report vulnerabilities privately and disclosure expectations.
- **[CHANGELOG.md](CHANGELOG.md)** — notable changes and release history.
- **[VERSIONING.md](VERSIONING.md)** — when to bump version numbers (milestones, not every commit).
- **[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)** — open-source components used by the app and license pointers.
- **[LICENSE](LICENSE)** — proprietary terms; all rights reserved (this is not open source).

---

## What this project is

TuneVault is designed like a small music application for learning and practicing:

- Java
- JavaFX
- FXML
- controller-based UI structure
- database access with DAOs
- shared state management
- project organization

Important: the current project is **database-backed**, not file-based.

---

## Main features

### Authentication

Users can:

- create an account
- sign in with username or email
- reset a password by email

### Playlists

Users can:

- create playlists
- delete playlists
- add songs to playlists
- remove songs from playlists
- search songs while editing playlists
- play songs from playlists
- view recommended songs for a playlist

### Search

Users can:

- search songs
- search artists
- open artist pages
- use recent searches
- queue songs with "Play Next"

### Music playback

The shared playback system includes:

- play / pause
- previous / next
- shuffle
- loop
- like / unlike
- progress tracking and seeking
- user queue support
- autoplay recommendations when a queue ends

### Player UI

The app includes:

- a shared mini player across screens
- an expanded player overlay
- a queue panel for upcoming songs

### Wrapped

The app includes a wrapped-style listening summary with daily and overall views.

### Find Your Genre

The app includes a short quiz that maps answers to a genre result.

---

## Current package structure

The main Java packages are:

- `app` - startup and JavaFX launch
- `auth` - login, account creation, and password reset controllers
- `mainmenu` - main menu screen controller
- `musicplayer` - playback helpers and player styling
- `musicplayer.controller` - shared playback UI controllers
- `musicplayer.playback` - lower-level playback state and queue logic
- `playlist` - playlist page and playlist models
- `playlist.cell` - custom playlist/search/suggestion row renderers
- `playlist.service` - reusable playlist feature logic
- `search` - search page, artist page, and recent-search model
- `recommendation` - recommendation and ranking logic
- `wrapped` - listening summary feature
- `findyourgenre` - quiz feature
- `db` - database access objects and connection setup
- `session` - current-session state
- `user` - user and profile domain models
- `util` - shared helpers such as navigation, alerts, toasts, styling, and motion

If you want a beginner-friendly package-by-package explanation, see **[CODEMAP.md](CODEMAP.md)**.

---

## How the app flows

1. The app starts from `Launcher.java` and `HelloApplication.java`.
2. `auth/login-page.fxml` is loaded first.
3. After login, `SessionManager` loads the current profile and recent search state.
4. The user reaches the main menu.
5. From the main menu, the user can open search, playlists, wrapped, or the genre quiz.
6. Playback is shared across pages through `MusicPlayerController`.
7. The mini player, expanded player overlay, and queue panel all read from the same shared playback state.

---

## Data and persistence

TuneVault uses a MySQL database.

Key persistence classes:

- `DBConnection` provides pooled connections through HikariCP
- `UserDAO` handles account queries
- `UserProfileDAO` loads a user's playlists and liked songs
- `SongDAO` loads songs
- `SearchHistoryDAO` stores recent searches
- `ListeningEventDAO` stores listening activity data

Database settings can be overridden with:

- `TUNEVAULT_DB_URL`
- `TUNEVAULT_DB_USER`
- `TUNEVAULT_DB_PASSWORD`

---

## UI structure

FXML views live under `src/main/resources/com/example/tunevaultfx/`, in subfolders that mirror Java packages (e.g. `auth/`, `search/`, `chrome/`). Navigation paths are centralized in `com.example.tunevaultfx.view.FxmlResources`.

Current views include:

- `auth/login-page.fxml`, `auth/create-account-page.fxml`, `auth/forgot-password-page.fxml`
- `chrome/app-top-bar.fxml`, `chrome/app-sidebar.fxml`
- `mainmenu/main-menu.fxml`
- `search/search-page.fxml`, `profile/artist-profile-page.fxml`
- `playlist/playlists-page.fxml`
- `wrapped/wrapped-page.fxml`
- `findyourgenre/findyourgenre-page.fxml`
- `profile/profile-page.fxml`, `settings/settings-page.fxml`
- `musicplayer/controller/mini-player.fxml`, `expanded-page.fxml`, `queue-panel.fxml`

Global styling is centralized in `app.css`.

---

## Design ideas used in the codebase

The project tries to separate responsibilities clearly:

- **controllers** manage user interaction and screen updates
- **services** contain reusable feature logic
- **DAOs** talk to the database
- **models** store data
- **utilities** handle shared app-wide helpers

Some especially important shared helpers are:

- `SceneUtil` for page switching and navigation history
- `SessionManager` for logged-in user state
- `MusicPlayerController` for shared playback state
- `ToastUtil` and `AlertUtil` for user feedback

---

## Good places to start reading

If you are new to the project, this order works well:

1. `app`
2. `auth`
3. `session`
4. `mainmenu`
5. `musicplayer.controller`
6. `playlist`
7. `search`
8. `db`
9. `util`

That path helps you understand startup, login, session state, navigation, playback, screens, and persistence in a sensible order.

---

## Notes for future developers

When adding a new feature:

1. Put screen logic in the right controller instead of a random helper.
2. Put reusable business logic in a service.
3. Put persistence logic in a DAO, not in the UI controller.
4. Reuse `SessionManager`, `SceneUtil`, and `MusicPlayerController` instead of duplicating state.
5. Keep CSS styling centralized in `app.css` when possible.

---

## Summary

TuneVault is a JavaFX music app focused on both user-facing features and clean project organization.

The goal is not just to make the app work, but to keep the structure understandable for the next person reading the code.
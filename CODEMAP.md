# TuneVault Code Map

This file explains what the current codebase actually contains.

It is written for a beginner who wants to understand the project structure before reading every class.

Important: this document reflects the current code, not an older version of the project. Some classes that might sound reasonable, such as `NowPlayingPageController` or `SongDetailsController`, do not currently exist in the codebase.

---

## Big picture

TuneVault is a JavaFX desktop music app with:

- account login and account creation
- playlist management
- search and artist browsing
- a shared mini player and an expanded player overlay
- a queue panel
- wrapped-style listening stats
- a genre quiz
- recommendation and search-ranking logic

The app is **database-backed**, not file-based:

- users are stored through `UserDAO`
- profiles/playlists are loaded through `UserProfileDAO`
- songs come from `SongDAO`
- recent search history and listening events are also stored in the database

---

## Package overview

### `app`

This package starts the app.

#### `HelloApplication.java`
Responsible for:

- starting JavaFX
- loading `auth/login-page.fxml`
- creating the main window
- applying the global stylesheet
- shutting down the database connection pool on app exit

Think of it as:
> The class that opens the app window.

#### `Launcher.java`
Responsible for:

- providing the normal Java `main` method
- launching `HelloApplication`

Think of it as:
> The plain Java entry point.

---

### `auth`

This package contains the account-related page controllers.

#### `LoginPageController.java`
Responsible for:

- reading username/email and password
- validating login
- starting the app session
- sending the user to the main menu after login

Think of it as:
> The sign-in screen controller.

#### `CreateAccountPageController.java`
Responsible for:

- reading sign-up input
- validating username, email, and password rules
- creating new accounts through `UserDAO`

Think of it as:
> The sign-up screen controller.

#### `ForgotPasswordPageController.java`
Responsible for:

- validating reset input
- updating a password by email
- returning the user to login

Think of it as:
> The password reset screen controller.

---

### `mainmenu`

#### `MainMenuController.java`
Responsible for:

- greeting the current user
- showing the feature cards on the home page
- navigating to playlists, search, wrapped, and the genre quiz
- logging the user out

Think of it as:
> The home page controller.

---

### `musicplayer`

This package contains shared playback logic and player styling helpers.

#### `PlayerStyleConstants.java`
Responsible for:

- centralizing player-related style names and fallback style builders
- keeping mini-player and expanded-player button states consistent

Think of it as:
> A style helper just for player controls.

#### `ShuffleManager.java`
Responsible for:

- generating and navigating shuffle order

Think of it as:
> The helper that remembers random playback order.

#### `ListeningSessionTracker.java`
Responsible for:

- tracking how long the user listened to a song
- recording skip/finish progress

Think of it as:
> The helper that watches listening sessions.

---

### `musicplayer.controller`

This package contains the shared playback controllers.

#### `MusicPlayerController.java`
Responsible for:

- coordinating playback across the whole app
- tracking the current song, queue, autoplay, shuffle, loop, and liked state
- exposing shared playback state to UI controllers

Think of it as:
> The main playback brain of the application.

#### `MiniPlayerController.java`
Responsible for:

- controlling the mini player shown across screens
- handling play/pause, next/previous, shuffle, loop, like, queue, and add-to-playlist
- opening related pages such as artist profile or the current playlist
- attaching the queue panel and expanded player overlay into the current scene

Think of it as:
> The small player bar controller.

#### `ExpandedPlayerController.java`
Responsible for:

- controlling the large now-playing overlay
- showing the current song and large playback controls
- opening artist pages from the expanded player

Think of it as:
> The large player overlay controller.

#### `QueuePanelController.java`
Responsible for:

- controlling the slide-out queue panel
- showing upcoming songs
- letting the user play from the queue, remove queued items, and reorder queued songs

Think of it as:
> The queue sidebar controller.

---

### `musicplayer.playback`

This package breaks playback into smaller focused classes.

#### `PlaybackState.java`
Responsible for:

- storing current song state and playback properties

Think of it as:
> The current playback snapshot.

#### `PlaybackQueue.java`
Responsible for:

- holding the active queue and queue index
- knowing whether playback is continuous or single-song

Think of it as:
> The low-level queue object.

#### `PlaybackLifecycleService.java`
Responsible for:

- starting playback
- stopping playback
- reacting when queues or playlists change

Think of it as:
> The helper that starts and stops songs safely.

#### `PlaybackNavigator.java`
Responsible for:

- next/previous logic
- shuffle and loop navigation

Think of it as:
> The helper that decides what song comes next.

---

### `core`

This package contains small app-wide core models.

#### `Song.java`
Responsible for:

- representing one song
- storing `songId`, title, artist, album, genre, and duration

Think of it as:
> One song object.

#### `DemoLibrary.java`
Responsible for:

- providing demo/sample songs if needed by the project

Think of it as:
> Built-in sample library data.

---

### `playlist`

This package contains playlist screens and playlist-related models.

#### `PlaylistsPageController.java`
Responsible for:

- showing playlists
- selecting a playlist
- showing songs in the selected playlist
- handling song search/add/remove flows inside playlists
- refreshing playlist suggestions

Think of it as:
> The main playlist screen controller.

#### `PlaylistSummary.java`
Responsible for:

- storing display-ready summary data for one playlist
- keeping playlist name, songs, count, total duration, and formatted duration together

Think of it as:
> The summary object for one playlist view.

---

### `playlist.cell`

This package contains custom `ListCell` classes used by the playlist UI.

#### `PlayableSongCell.java`
Responsible for:

- rendering a playlist song row
- showing play state, metadata, and row actions

Think of it as:
> The custom UI for one playlist song row.

#### `SearchSongToggleCell.java`
Responsible for:

- rendering search results in the playlist screen
- showing whether a song is already in the selected playlist

Think of it as:
> The custom row used when adding songs to a playlist.

#### `SuggestedSongCell.java`
Responsible for:

- rendering recommended songs in the playlist suggestions section

Think of it as:
> The custom row for playlist suggestions.

---

### `playlist.service`

This package contains reusable playlist logic.

#### `PlaylistService.java`
Responsible for:

- creating playlists
- deleting playlists
- adding/removing songs
- toggling liked songs
- writing those changes to the database

Think of it as:
> The main playlist action service.

#### `PlaylistSelectionService.java`
Responsible for:

- building summary data for the selected playlist

Think of it as:
> The helper that prepares playlist data for display.

#### `SongSearchService.java`
Responsible for:

- filtering songs by title, artist, album, or genre

Think of it as:
> The song search helper for playlist flows.

#### `PlaylistPickerService.java`
Responsible for:

- showing the UI that lets the user pick a playlist to add a song into

Think of it as:
> The helper that asks “which playlist should this song go into?”

---

### `search`

This package contains the general search feature.

#### `SearchPageController.java`
Responsible for:

- loading and filtering the song library
- showing song and artist search results
- maintaining recent searches
- handling search-result actions such as play and “Play Next”

Think of it as:
> The main search screen controller.

#### `ArtistProfileController.java`
Responsible for:

- showing all songs by one selected artist
- loading artist songs in the background
- letting the user play that artist’s songs as a queue

Think of it as:
> The artist page controller.

#### `SearchRecentItem.java`
Responsible for:

- representing one recent search item
- storing whether it was a song or artist search

Think of it as:
> One row in the recent-search list.

---

### `recommendation`

This package contains recommendation and ranking logic.

#### `RecommendationService.java`
Responsible for:

- serving as the public API for recommendations and search ranking
- returning suggested songs for a user
- returning suggested songs for a playlist
- delegating search ranking to `SearchRankingService`

Think of it as:
> The recommendation entry point used by controllers.

#### `RecommendationEngine.java`
Responsible for:

- the main scoring logic behind recommendations

Think of it as:
> The engine that scores songs.

#### `SearchRankingService.java`
Responsible for:

- ranking songs and artists for search results

Think of it as:
> The helper that sorts search results.

#### `RecommendationProfile.java`
Responsible for:

- storing the normalized preference/profile data used during recommendation

Think of it as:
> The user taste profile used by the recommendation engine.

---

### `wrapped`

This package contains the listening-summary feature.

#### `WrappedPageController.java`
Responsible for:

- showing wrapped-style listening stats
- switching between daily and overall ranges
- displaying top song, top artist, favorite genre, total time, and summary text

Think of it as:
> The listening summary screen controller.

#### `WrappedStatsService.java`
Responsible for:

- loading wrapped data for a username and range

Think of it as:
> The data loader for the wrapped page.

#### `WrappedStats.java`
Responsible for:

- storing all wrapped page result values

Think of it as:
> The wrapped summary data object.

#### `StatsRange.java`
Responsible for:

- representing whether wrapped data is daily or overall

Think of it as:
> The range selector enum for wrapped data.

---

### `findyourgenre`

This package contains the quiz feature.

#### `FindYourGenrePageController.java`
Responsible for:

- moving through quiz questions
- collecting answers
- showing the final genre result

Think of it as:
> The quiz screen controller.

#### `GenreQuiz.java`
Responsible for:

- holding the quiz questions and answer-to-genre mapping

Think of it as:
> The quiz data source.

#### `Question.java`
Responsible for:

- representing one quiz question and its answers

Think of it as:
> One quiz question object.

---

### `db`

This package contains all database access classes.

This is one of the most important corrections from the old document:
the app does **not** use file-based stores anymore. It uses a MySQL database and HikariCP connection pooling.

#### `DBConnection.java`
Responsible for:

- creating pooled database connections
- reading optional DB config overrides from environment variables/system properties
- shutting down the connection pool cleanly

Think of it as:
> The shared database connection provider.

#### `UserDAO.java`
Responsible for:

- user existence checks
- account creation
- login authentication
- password updates

Think of it as:
> The DAO for account records.

#### `UserProfileDAO.java`
Responsible for:

- loading a user’s playlists and liked songs
- delegating playlist and playlist-song mutations to smaller DAOs

Think of it as:
> The DAO that loads the music profile for one user.

#### `SongDAO.java`
Responsible for:

- loading songs from the database

Think of it as:
> The DAO for songs.

#### `PlaylistDAO.java` and `PlaylistSongDAO.java`
Responsible for:

- lower-level playlist table operations
- lower-level playlist-song relation operations

Think of them as:
> The focused DAOs behind playlist persistence.

#### `SearchHistoryDAO.java`
Responsible for:

- saving and loading recent searches

Think of it as:
> The DAO for search history.

#### `ListeningEventDAO.java`
Responsible for:

- recording listening actions such as likes, unlikes, adds, removes, skips, and listens

Think of it as:
> The DAO for listening analytics.

---

### `session`

This package contains current-session state.

#### `SessionManager.java`
Responsible for:

- starting and ending the current session
- holding the logged-in username and loaded `UserProfile`
- caching the song library
- tracking selected song, selected artist, and requested playlist
- managing recent searches

Think of it as:
> The central session state manager.

---

### `user`

This package contains user-related domain classes.

#### `User.java`
Responsible for:

- representing one account record

Think of it as:
> One user object.

#### `UserProfile.java`
Responsible for:

- storing one user’s playlists and liked songs in memory
- providing helpers like `isLiked()` and `toggleLike()`

Think of it as:
> The in-memory music profile for the current user.

#### `UserLibraryService.java`
Responsible for:

- convenience operations around the current user’s library
- checking liked state
- adding a song to a playlist for the current user

Think of it as:
> A helper for common library actions.

---

### `view`

#### `FxmlResources.java`
Responsible for:

- classpath-relative paths to every FXML root (mirroring resource folders under `com/example/tunevaultfx/`)

Think of it as:
> One place to reference screen FXML paths so navigation does not scatter string literals.

---

### `util`

This package contains shared helpers used across the app.

#### `SceneUtil.java`
Responsible for:

- switching FXML pages
- reusing the current JavaFX scene
- maintaining navigation history so “Back” returns to the real previous page

Think of it as:
> The central navigation helper.

#### `AlertUtil.java`
Responsible for:

- showing information alerts

Think of it as:
> A small pop-up helper.

#### `ToastUtil.java`
Responsible for:

- showing temporary toast notifications

Think of it as:
> A small non-blocking message helper.

#### `TimeUtil.java`
Responsible for:

- formatting durations like `3:45`

Think of it as:
> The time formatting helper.

#### `CellStyleKit.java`
Responsible for:

- building consistent list-row UI pieces

Think of it as:
> A reusable style helper for song rows.

#### `UiMotionUtil.java`
Responsible for:

- lightweight entrance and hover animations

Think of it as:
> The animation helper.

#### `PasswordUtil.java`
Responsible for:

- hashing passwords before storing them

Think of it as:
> The password helper.

---

## FXML screens currently present

FXML files live under package-style folders (e.g. `auth/`, `search/`). Scene roots and paths are defined in `com.example.tunevaultfx.view.FxmlResources`.

- `auth/login-page.fxml`, `auth/create-account-page.fxml`, `auth/forgot-password-page.fxml`
- `chrome/app-top-bar.fxml`, `chrome/app-sidebar.fxml`
- `mainmenu/main-menu.fxml`
- `search/search-page.fxml`, `profile/artist-profile-page.fxml`
- `playlist/playlists-page.fxml`
- `wrapped/wrapped-page.fxml`
- `findyourgenre/findyourgenre-page.fxml`
- `profile/profile-page.fxml`, `settings/settings-page.fxml`
- `musicplayer/controller/mini-player.fxml`, `musicplayer/controller/expanded-page.fxml`, `musicplayer/controller/queue-panel.fxml`

---

## How to understand the project quickly

If you are new to the project, this reading order works well:

1. `app`
2. `auth`
3. `session`
4. `mainmenu`
5. `musicplayer.controller`
6. `playlist` and `playlist.service`
7. `search`
8. `recommendation`
9. `db`
10. `view` (FXML path constants) and `util`

This takes you from:

- startup
- to login/session state
- to UI screens
- to playback
- to feature logic
- to persistence

---

## Responsibility rule

A simple way to understand most classes:

- **Controller** = talks to the screen
- **Service** = contains feature logic used by controllers
- **DAO** = talks to the database
- **Model/data class** = stores information
- **Utility** = helps many parts of the app with common tasks

If one class starts doing too many of these at once, it may need to be split.

---

## Final note

The best question to ask about any file is:

> What is this file mainly responsible for?

If that answer is clear, the codebase becomes much easier to read, debug, and extend.
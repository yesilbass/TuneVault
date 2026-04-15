-- TuneVaultFX — MySQL schema matching the DAO layer (UserDAO, PlaylistDAO, etc.)
--
-- Apply once per environment:
--   mysql -u root -p < src/main/resources/db/schema.sql
-- Or create database manually and: mysql -u root -p tune_vault_db < schema.sql
--
-- Connection defaults: jdbc:mysql://localhost:3306/tune_vault_db
-- Overrides: TUNEVAULT_DB_URL, TUNEVAULT_DB_USER, TUNEVAULT_DB_PASSWORD

CREATE DATABASE IF NOT EXISTS tune_vault_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE tune_vault_db;

-- ---------------------------------------------------------------------------
-- Core entities
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS app_user (
    user_id       INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash CHAR(64)     NOT NULL COMMENT 'SHA-256 hex from PasswordUtil',
    profile_avatar_key VARCHAR(512) NULL COMMENT 'Relative path under ~/.tunevaultfx/profile-media',
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT uq_app_user_email    UNIQUE (email)
) ENGINE=InnoDB;

-- Existing databases: run once if columns are missing (ignore errors if already applied):
-- ALTER TABLE app_user ADD COLUMN profile_avatar_key VARCHAR(512) NULL COMMENT 'Relative path under ~/.tunevaultfx/profile-media';

CREATE TABLE IF NOT EXISTS artist (
    artist_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS genre (
    genre_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    genre_name VARCHAR(128) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS song (
    song_id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(512) NOT NULL,
    artist_id         INT UNSIGNED NOT NULL,
    genre_id          INT UNSIGNED NOT NULL,
    duration_seconds  INT UNSIGNED NOT NULL DEFAULT 0,
    CONSTRAINT fk_song_artist FOREIGN KEY (artist_id) REFERENCES artist (artist_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_song_genre FOREIGN KEY (genre_id) REFERENCES genre (genre_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS playlist (
    playlist_id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id             INT UNSIGNED NOT NULL,
    name                VARCHAR(255) NOT NULL,
    is_system_playlist  TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_playlist_user_name UNIQUE (user_id, name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS playlist_song (
    playlist_id INT UNSIGNED NOT NULL,
    song_id     INT UNSIGNED NOT NULL,
    PRIMARY KEY (playlist_id, song_id),
    CONSTRAINT fk_ps_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (playlist_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ps_song FOREIGN KEY (song_id) REFERENCES song (song_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS listening_event (
    event_id               INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id                INT UNSIGNED NOT NULL,
    song_id                INT UNSIGNED NOT NULL,
    action_type            VARCHAR(32)  NOT NULL,
    played_seconds         INT UNSIGNED NOT NULL DEFAULT 0,
    song_duration_seconds  INT UNSIGNED NOT NULL DEFAULT 0,
    completion_ratio       DOUBLE       NOT NULL DEFAULT 0,
    count_as_play          TINYINT(1)   NOT NULL DEFAULT 0,
    event_timestamp        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_le_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_le_song FOREIGN KEY (song_id) REFERENCES song (song_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_le_user_time (user_id, event_timestamp)
) ENGINE=InnoDB;

-- item_type values from SearchRecentItem.Type.name(): SONG, ARTIST
CREATE TABLE IF NOT EXISTS search_history (
    search_history_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id           INT UNSIGNED NOT NULL,
    item_type         VARCHAR(16)  NOT NULL,
    song_id           INT UNSIGNED NULL,
    artist_name       VARCHAR(255) NULL,
    searched_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sh_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sh_song FOREIGN KEY (song_id) REFERENCES song (song_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_sh_user_searched (user_id, searched_at)
) ENGINE=InnoDB;

-- Genre Discovery quiz — boosts recommendation genre affinity (merged in RecommendationEngine)
-- Note: No FOREIGN KEY on user_id — Error 3780 appears if app_user.user_id type differs (e.g. legacy INT
-- vs INT UNSIGNED). The app enforces user_id via DAO; delete orphans manually if you drop users in SQL.
CREATE TABLE IF NOT EXISTS user_genre_discovery (
    user_id       INT UNSIGNED PRIMARY KEY,
    top_genre     VARCHAR(128) NOT NULL,
    second_genre  VARCHAR(128) NULL,
    third_genre   VARCHAR(128) NULL,
    quiz_mode     VARCHAR(16)  NOT NULL DEFAULT 'FULL' COMMENT 'QUICK or FULL',
    weights_boost VARCHAR(768) NOT NULL COMMENT 'pipe-separated normalized boosts, e.g. pop:0.9|rock:0.4',
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ugd_user (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Optional demo catalog (uncomment to seed one track for local testing)
-- ---------------------------------------------------------------------------
--
-- INSERT INTO artist (name) VALUES ('Demo Artist');
-- SET @demo_artist_id = LAST_INSERT_ID();
-- INSERT INTO genre (genre_name) VALUES ('Pop');
-- SET @demo_genre_id = LAST_INSERT_ID();
-- INSERT INTO song (title, artist_id, genre_id, duration_seconds)
--     VALUES ('Demo Track', @demo_artist_id, @demo_genre_id, 180);

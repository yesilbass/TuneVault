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
    is_public           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1 = visible on profile & search (non-system only)',
    pin_order           TINYINT UNSIGNED NULL COMMENT '1–3 user pins; NULL = unpinned (Liked Songs stays unpinned)',
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_playlist_user_name UNIQUE (user_id, name)
) ENGINE=InnoDB;

-- User A follows user B (for profiles & discovery — not artist follows)
CREATE TABLE IF NOT EXISTS user_follow (
    follower_user_id INT UNSIGNED NOT NULL,
    followee_user_id INT UNSIGNED NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_user_id, followee_user_id),
    CONSTRAINT fk_user_follow_follower FOREIGN KEY (follower_user_id) REFERENCES app_user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_follow_followee FOREIGN KEY (followee_user_id) REFERENCES app_user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_user_follow_followee (followee_user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_follows_artist (
    user_id    INT UNSIGNED NOT NULL,
    artist_id  INT UNSIGNED NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, artist_id),
    CONSTRAINT fk_ufa_user FOREIGN KEY (user_id) REFERENCES app_user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ufa_artist FOREIGN KEY (artist_id) REFERENCES artist (artist_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_ufa_artist (artist_id)
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

-- ---------------------------------------------------------------------------
-- Genre Discovery quiz results
--   quiz_session_count: incremented each completion so the next run serves
--   a fresh session (cycles 1–5 via mod in QuizQuestionDAO).
--   weights_boost: pipe-separated normalized boosts merged across all sessions.
--   Resetting via Settings deletes this row entirely, wiping both weights
--   and the session counter.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_genre_discovery (
    user_id            INT UNSIGNED PRIMARY KEY,
    top_genre          VARCHAR(128) NOT NULL,
    second_genre       VARCHAR(128) NULL,
    third_genre        VARCHAR(128) NULL,
    quiz_mode          VARCHAR(16)  NOT NULL DEFAULT 'FULL' COMMENT 'QUICK or FULL',
    weights_boost      VARCHAR(768) NOT NULL COMMENT 'pipe-separated normalized boosts, e.g. pop:0.9|rock:0.4',
    quiz_session_count TINYINT UNSIGNED NOT NULL DEFAULT 0
                       COMMENT 'Number of completions; determines next session (mod 5)',
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ugd_user (user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Genre Discovery Quiz — question bank
--   session_number: 1–5, groups 10 questions per session.
--   Both QUICK and FULL draw from the same session; QUICK picks 5 at random.
--   display_order: sort within a session (1–10).
--   is_active: set to 0 to hide a question without deleting it.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS quiz_question (
    question_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    prompt         VARCHAR(512) NOT NULL,
    quiz_mode      ENUM('QUICK','FULL') NOT NULL DEFAULT 'FULL'
                   COMMENT 'Kept for legacy compatibility; session + shuffle replaces mode filtering',
    display_order  TINYINT UNSIGNED NOT NULL DEFAULT 0,
    is_active      TINYINT(1) NOT NULL DEFAULT 1,
    session_number TINYINT UNSIGNED NOT NULL DEFAULT 1
                   COMMENT '1–5 question session group; cycles back after 5 completions'
) ENGINE=InnoDB;

-- quiz_answer: four answers per question, each mapped to a genre name.
--   genre_name must match genre.genre_name (case-insensitive compare in app).
--   weight: scoring multiplier (1 = normal, 2 = double-scored).
CREATE TABLE IF NOT EXISTS quiz_answer (
    answer_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    question_id  INT UNSIGNED NOT NULL,
    answer_text  VARCHAR(255) NOT NULL,
    genre_name   VARCHAR(128) NOT NULL COMMENT 'Must match a genre.genre_name value',
    weight       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 = normal, 2 = double-scored',
    answer_order TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1–4 display order within the question',
    CONSTRAINT fk_qa_question FOREIGN KEY (question_id) REFERENCES quiz_question (question_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_qa_question (question_id)
) ENGINE=InnoDB;
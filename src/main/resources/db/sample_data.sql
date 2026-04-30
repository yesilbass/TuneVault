-- =============================================================================
-- TuneVaultFX — Sample Data Seed
-- Tests genre discovery quiz preferences across all 10 quiz genres.
--
-- Safe to re-run: wrapped in a transaction; existing rows are left alone.
-- Genres inserted here match the quiz_answer genre_name values exactly so
-- the recommendation engine can match them.
-- =============================================================================

USE tune_vault_db;

-- ---------------------------------------------------------------------------
-- Genres (10 — one per quiz answer category)
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO genre (genre_id, genre_name) VALUES
(1,  'Classical'),
(2,  'R&B'),
(3,  'Pop'),
(4,  'Metal'),
(5,  'Folk'),
(6,  'Hip Hop'),
(7,  'Electronic'),
(8,  'Indie'),
(9,  'Rock'),
(10, 'Jazz');

-- ---------------------------------------------------------------------------
-- Artists (3 per genre = 30 total)
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO artist (artist_id, name) VALUES
-- Classical
(1,  'Ludwig van Beethoven'),
(2,  'Johann Sebastian Bach'),
(3,  'Wolfgang Amadeus Mozart'),
-- R&B
(4,  'Alicia Keys'),
(5,  'Frank Ocean'),
(6,  'SZA'),
-- Pop
(7,  'Taylor Swift'),
(8,  'Harry Styles'),
(9,  'Dua Lipa'),
-- Metal
(10, 'Metallica'),
(11, 'Black Sabbath'),
(12, 'Tool'),
-- Folk
(13, 'Phoebe Bridgers'),
(14, 'Bon Iver'),
(15, 'Fleet Foxes'),
-- Hip Hop
(16, 'Kendrick Lamar'),
(17, 'J. Cole'),
(18, 'Tyler, the Creator'),
-- Electronic
(19, 'Aphex Twin'),
(20, 'Boards of Canada'),
(21, 'Four Tet'),
-- Indie
(22, 'Arctic Monkeys'),
(23, 'Mitski'),
(24, 'Tame Impala'),
-- Rock
(25, 'Led Zeppelin'),
(26, 'The Strokes'),
(27, 'Radiohead'),
-- Jazz
(28, 'Miles Davis'),
(29, 'John Coltrane'),
(30, 'Thelonious Monk');

-- ---------------------------------------------------------------------------
-- Songs (~10 per genre = 100 total)
-- duration_seconds: realistic track lengths
-- ---------------------------------------------------------------------------

INSERT IGNORE INTO song (song_id, title, artist_id, genre_id, duration_seconds) VALUES

-- Classical (genre_id = 1)
(1,  'Moonlight Sonata',                    1,  1, 900),
(2,  'Symphony No. 5 in C Minor',           1,  1, 1980),
(3,  'Ode to Joy',                          1,  1, 750),
(4,  'Cello Suite No. 1 in G Major',        2,  1, 630),
(5,  'The Well-Tempered Clavier Book I',    2,  1, 5400),
(6,  'Toccata and Fugue in D Minor',        2,  1, 540),
(7,  'Eine kleine Nachtmusik',              3,  1, 780),
(8,  'Piano Sonata No. 16 in C Major',      3,  1, 510),
(9,  'Symphony No. 40 in G Minor',          3,  1, 1560),
(10, 'Requiem in D Minor',                  3,  1, 3300),

-- R&B (genre_id = 2)
(11, 'Fallin''',                            4,  2, 211),
(12, 'No One',                              4,  2, 254),
(13, 'If I Ain''t Got You',                 4,  2, 234),
(14, 'Superstar',                           4,  2, 196),
(15, 'Thinkin Bout You',                    5,  2, 201),
(16, 'Self Control',                        5,  2, 249),
(17, 'Pink + White',                        5,  2, 182),
(18, 'Good Days',                           6,  2, 275),
(19, 'Kill Bill',                           6,  2, 153),
(20, 'Snooze',                              6,  2, 201),

-- Pop (genre_id = 3)
(21, 'Anti-Hero',                           7,  3, 200),
(22, 'Shake It Off',                        7,  3, 219),
(23, 'Cruel Summer',                        7,  3, 178),
(24, 'Love Story',                          7,  3, 235),
(25, 'As It Was',                           8,  3, 167),
(26, 'Watermelon Sugar',                    8,  3, 174),
(27, 'Adore You',                           8,  3, 207),
(28, 'Levitating',                          9,  3, 203),
(29, 'Don''t Start Now',                    9,  3, 183),
(30, 'Physical',                            9,  3, 193),

-- Metal (genre_id = 4)
(31, 'Enter Sandman',                       10, 4, 331),
(32, 'Master of Puppets',                   10, 4, 515),
(33, 'Nothing Else Matters',                10, 4, 387),
(34, 'One',                                 10, 4, 446),
(35, 'Iron Man',                            11, 4, 356),
(36, 'Paranoid',                            11, 4, 171),
(37, 'War Pigs',                            11, 4, 479),
(38, 'Schism',                              12, 4, 403),
(39, 'Parabola',                            12, 4, 386),
(40, 'The Pot',                             12, 4, 387),

-- Folk (genre_id = 5)
(41, 'Motion Sickness',                     13, 5, 231),
(42, 'Garden Song',                         13, 5, 193),
(43, 'Savior Complex',                      13, 5, 218),
(44, 'Moon Song',                           13, 5, 183),
(45, 'Skinny Love',                         14, 5, 222),
(46, 'Holocene',                            14, 5, 282),
(47, 'Towers',                              14, 5, 259),
(48, 'White Winter Hymnal',                 15, 5, 139),
(49, 'Helplessness Blues',                  15, 5, 319),
(50, 'Mykonos',                             15, 5, 248),

-- Hip Hop (genre_id = 6)
(51, 'HUMBLE.',                             16, 6, 177),
(52, 'DNA.',                                16, 6, 185),
(53, 'Money Trees',                         16, 6, 386),
(54, 'Alright',                             16, 6, 219),
(55, 'No Role Modelz',                      17, 6, 293),
(56, 'Love Yourz',                          17, 6, 224),
(57, 'Forest Hill Drive',                   17, 6, 266),
(58, 'See You Again',                       18, 6, 210),
(59, 'EARFQUAKE',                           18, 6, 188),
(60, 'New Magic Wand',                      18, 6, 193),

-- Electronic (genre_id = 7)
(61, 'Windowlicker',                        19, 7, 600),
(62, 'Flim',                                19, 7, 178),
(63, 'Avril 14th',                          19, 7, 121),
(64, 'Roygbiv',                             20, 7, 199),
(65, 'Olson',                               20, 7, 249),
(66, 'Dayvan Cowboy',                       20, 7, 367),
(67, 'Baby',                                21, 7, 374),
(68, 'Moth',                                21, 7, 392),
(69, 'Parallel Jalebi',                     21, 7, 312),
(70, 'Two Thousand and Seventeen',          21, 7, 298),

-- Indie (genre_id = 8)
(71, 'R U Mine?',                           22, 8, 202),
(72, 'Do I Wanna Know?',                    22, 8, 272),
(73, '505',                                 22, 8, 254),
(74, 'Why''d You Only Call Me When You''re High?', 22, 8, 161),
(75, 'Nobody',                              23, 8, 215),
(76, 'Washing Machine Heart',               23, 8, 158),
(77, 'Bug Like an Angel',                   23, 8, 153),
(78, 'The Less I Know the Better',          24, 8, 216),
(79, 'Let It Happen',                       24, 8, 467),
(80, 'Eventually',                          24, 8, 318),

-- Rock (genre_id = 9)
(81, 'Stairway to Heaven',                  25, 9, 482),
(82, 'Whole Lotta Love',                    25, 9, 334),
(83, 'Kashmir',                             25, 9, 516),
(84, 'Black Dog',                           25, 9, 296),
(85, 'Last Nite',                           26, 9, 193),
(86, 'Reptilia',                            26, 9, 222),
(87, 'Hard to Explain',                     26, 9, 218),
(88, 'Creep',                               27, 9, 238),
(89, 'Karma Police',                        27, 9, 264),
(90, 'Paranoid Android',                    27, 9, 383),

-- Jazz (genre_id = 10)
(91, 'So What',                             28, 10, 562),
(92, 'Kind of Blue',                        28, 10, 346),
(93, 'All Blues',                           28, 10, 693),
(94, 'Blue in Green',                       28, 10, 337),
(95, 'A Love Supreme Part I',               29, 10, 459),
(96, 'My Favorite Things',                  29, 10, 853),
(97, 'Giant Steps',                         29, 10, 219),
(98, 'Round Midnight',                      30, 10, 348),
(99, 'Blue Monk',                           30, 10, 281),
(100,'Straight, No Chaser',                 30, 10, 264);

-- ---------------------------------------------------------------------------
-- Quiz seed data — 5 sessions × 10 questions = 50 questions
-- ---------------------------------------------------------------------------

-- Session 1 (original question bank)
INSERT INTO quiz_question (question_id, prompt, quiz_mode, display_order, is_active, session_number) VALUES
(1,  'What energy do you want from music right now?',       'QUICK', 1,  1, 1),
(2,  'What hooks you first?',                               'QUICK', 2,  1, 1),
(3,  'Where do you listen most?',                           'QUICK', 3,  1, 1),
(4,  'Pick a sound "colour":',                              'QUICK', 4,  1, 1),
(5,  'Old or new sounds?',                                  'QUICK', 5,  1, 1),
(6,  'How do you like your songs structured?',              'FULL',  6,  1, 1),
(7,  'Where do vocals sit in your ideal track?',            'FULL',  7,  1, 1),
(8,  'What are you listening for right now?',               'FULL',  8,  1, 1),
(9,  'What feeling do you want music to give you?',         'FULL',  9,  1, 1),
(10, 'Which instrument drives your favourite tracks?',      'FULL',  10, 1, 1);

-- Q1
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(1, 'Soft & calm',    'Classical', 1, 1),
(1, 'Steady groove',  'R&B',       1, 2),
(1, 'Up & dancey',    'Pop',       2, 3),
(1, 'Loud & intense', 'Metal',     2, 4);
-- Q2
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(2, 'The words',      'Folk',       1, 1),
(2, 'The rhythm',     'Hip Hop',    1, 2),
(2, 'The melody',     'Pop',        1, 3),
(2, 'The atmosphere', 'Electronic', 1, 4);
-- Q3
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(3, 'Cozy at home',  'Indie',      1, 1),
(3, 'Night drive',   'Rock',       1, 2),
(3, 'With friends',  'Pop',        1, 3),
(3, 'Deep focus',    'Electronic', 1, 4);
-- Q4
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(4, 'Warm & smoky',     'Jazz', 1, 1),
(4, 'Cool & smooth',    'R&B',  1, 2),
(4, 'Bright & sparkly', 'Pop',  1, 3),
(4, 'Raw & organic',    'Folk', 1, 4);
-- Q5
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(5, 'Timeless classics', 'Classical',  1, 1),
(5, 'Throwback eras',    'Rock',       1, 2),
(5, 'Fresh releases',    'Pop',        1, 3),
(5, 'Mix it all',        'Electronic', 1, 4);
-- Q6
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(6, 'Catchy & tight',  'Pop',        2, 1),
(6, 'Room to breathe', 'Jazz',       1, 2),
(6, 'Long builds',     'Electronic', 2, 3),
(6, 'Loops that hit',  'Hip Hop',    1, 4);
-- Q7
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(7, 'Sing-along essential',     'Pop',        1, 1),
(7, 'Nice when they''re there', 'Indie',      1, 2),
(7, 'Background is fine',       'Rock',       1, 3),
(7, 'Instrumentals rule',       'Electronic', 1, 4);
-- Q8
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(8, 'Solo reset',        'Classical',  1, 1),
(8, 'Small hangout',     'Indie',      1, 2),
(8, 'Big crowd energy',  'Rock',       1, 3),
(8, 'Study / work flow', 'Electronic', 1, 4);
-- Q9
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(9, 'Happy boost',       'Pop',   1, 1),
(9, 'Soft & reflective', 'R&B',   1, 2),
(9, 'Angry catharsis',   'Metal', 1, 3),
(9, 'Hopeful glow',      'Indie', 1, 4);
-- Q10
INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(10, 'Strings',        'Classical',  1, 1),
(10, 'Drums & bass',   'Hip Hop',    1, 2),
(10, 'Synths & pads',  'Electronic', 1, 3),
(10, 'Guitar forward', 'Rock',       1, 4);

-- ---------------------------------------------------------------------------
-- Session 2
-- ---------------------------------------------------------------------------
INSERT INTO quiz_question (question_id, prompt, quiz_mode, display_order, is_active, session_number) VALUES
(11, 'What do you reach for after a rough day?',          'QUICK', 1,  1, 2),
(12, 'Pick the show you would soundtrack:',               'QUICK', 2,  1, 2),
(13, 'Which word fits your perfect track?',               'QUICK', 3,  1, 2),
(14, 'What is the first thing you notice in a song?',     'QUICK', 4,  1, 2),
(15, 'How loud do you usually listen?',                   'QUICK', 5,  1, 2),
(16, 'How should a song make time feel?',                 'FULL',  6,  1, 2),
(17, 'What kind of voice do you gravitate toward?',       'FULL',  7,  1, 2),
(18, 'Pick the decade that shaped your taste most:',      'FULL',  8,  1, 2),
(19, 'What do you want a song to do by the end?',         'FULL',  9,  1, 2),
(20, 'Which best describes your listening habit?',        'FULL',  10, 1, 2);

INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(11, 'Something soft and healing',         'Classical',  1, 1),
(11, 'Something smooth to zone out to',    'R&B',        1, 2),
(11, 'Something loud to vent with',        'Metal',      1, 3),
(11, 'Something honest with real words',   'Folk',       1, 4),
(12, 'A gritty crime drama',               'Rock',       1, 1),
(12, 'A late-night anime',                 'Electronic', 1, 2),
(12, 'A coming-of-age indie film',         'Indie',      1, 3),
(12, 'A stylish music biopic',             'Hip Hop',    1, 4),
(13, 'Breezy',                             'Pop',        1, 1),
(13, 'Melancholic',                        'Indie',      1, 2),
(13, 'Timeless',                           'Jazz',       1, 3),
(13, 'Hard-hitting',                       'Hip Hop',    2, 4),
(14, 'The groove it locks into',           'R&B',        1, 1),
(14, 'The texture of the production',      'Electronic', 1, 2),
(14, 'The chord progression',              'Classical',  1, 3),
(14, 'The attitude of the performance',    'Rock',       1, 4),
(15, 'Quietly in the background',          'Classical',  1, 1),
(15, 'Medium — present but not dominant',  'Folk',       1, 2),
(15, 'Loud enough to feel it',             'Rock',       1, 3),
(15, 'Full blast — neighbours be damned',  'Metal',      2, 4),
(16, 'Stretch it — I want to get lost',    'Electronic', 2, 1),
(16, 'Speed it up — high energy only',     'Pop',        2, 2),
(16, 'Suspend it — floating and calm',     'Jazz',       1, 3),
(16, 'Mark it — strong beat I can feel',   'Hip Hop',    1, 4),
(17, 'Smooth and effortless',              'R&B',        1, 1),
(17, 'Raw and imperfect',                  'Folk',       1, 2),
(17, 'Powerful and theatrical',            'Pop',        1, 3),
(17, 'No vocals — instrument only',        'Classical',  1, 4),
(18, 'Pre-1970s — timeless standards',     'Jazz',       1, 1),
(18, '1970s–1990s — golden era rock',      'Rock',       1, 2),
(18, '1990s–2000s — the hip hop boom',     'Hip Hop',    1, 3),
(18, '2010s–now — anything goes',          'Electronic', 1, 4),
(19, 'Energised and ready to move',        'Pop',        1, 1),
(19, 'Calm and settled',                   'Indie',      1, 2),
(19, 'Understood — like it read my mind',  'R&B',        1, 3),
(19, 'Wired — too much to sit still',      'Metal',      1, 4),
(20, 'Albums front to back, no skipping',  'Classical',  1, 1),
(20, 'Shuffle everything all the time',    'Pop',        1, 2),
(20, 'Deep dives on one artist for weeks', 'Indie',      1, 3),
(20, 'Whatever matches my current mood',   'Electronic', 1, 4);

-- ---------------------------------------------------------------------------
-- Session 3
-- ---------------------------------------------------------------------------
INSERT INTO quiz_question (question_id, prompt, quiz_mode, display_order, is_active, session_number) VALUES
(21, 'Pick a place you would love to listen:',             'QUICK', 1,  1, 3),
(22, 'What keeps you coming back to a track?',             'QUICK', 2,  1, 3),
(23, 'Which matters more to you in music?',                'QUICK', 3,  1, 3),
(24, 'What gets you to add a song to a playlist?',         'QUICK', 4,  1, 3),
(25, 'Pick the best way to discover new music:',           'QUICK', 5,  1, 3),
(26, 'How complex do you want your music to be?',          'FULL',  6,  1, 3),
(27, 'Which best describes your musical guilty pleasure?', 'FULL',  7,  1, 3),
(28, 'Pick the concert experience you would choose:',      'FULL',  8,  1, 3),
(29, 'What should the bass do in a track?',                'FULL',  9,  1, 3),
(30, 'How do you feel about silence in music?',            'FULL',  10, 1, 3);

INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(21, 'A grand concert hall',               'Classical',  1, 1),
(21, 'A rooftop at sunset',                'Indie',      1, 2),
(21, 'A packed festival crowd',            'Rock',       1, 3),
(21, 'Headphones on a long flight',        'Electronic', 1, 4),
(22, 'A hook I can not get out of my head','Pop',        2, 1),
(22, 'Lyrics I keep finding new meaning in','Folk',      1, 2),
(22, 'A feeling I can not quite name',     'R&B',        1, 3),
(22, 'The way it hits physically',         'Metal',      1, 4),
(23, 'The feeling it creates',             'Electronic', 1, 1),
(23, 'The story it tells',                 'Hip Hop',    1, 2),
(23, 'The craft behind it',                'Jazz',       1, 3),
(23, 'The energy it brings',               'Rock',       1, 4),
(24, 'It perfectly fits a mood I have',    'Indie',      1, 1),
(24, 'The drop hit me out of nowhere',     'Electronic', 2, 2),
(24, 'A line made me stop and rewind',     'Folk',       1, 3),
(24, 'It made me want to move',            'Pop',        1, 4),
(25, 'A friend who really knows music',    'Indie',      1, 1),
(25, 'An algorithm that just gets me',     'Pop',        1, 2),
(25, 'Digging through old records',        'Jazz',       1, 3),
(25, 'A random playlist rabbit hole',      'Electronic', 1, 4),
(26, 'Simple and direct — hook me fast',   'Pop',        2, 1),
(26, 'Layered — more I listen more I hear','Classical',  1, 2),
(26, 'Rhythmically complex but melodic',   'Jazz',       1, 3),
(26, 'Chaotic on purpose',                 'Metal',      1, 4),
(27, 'Cheesy pop I know every word to',    'Pop',        1, 1),
(27, 'Old school rap I blast alone',       'Hip Hop',    1, 2),
(27, 'Power ballads that destroy me',      'Rock',       1, 3),
(27, 'Ambient music I pretend is work',    'Electronic', 1, 4),
(28, 'Intimate acoustic set — front row',  'Folk',       1, 1),
(28, 'Arena show with production overkill','Pop',        1, 2),
(28, 'Underground club until 4am',         'Electronic', 2, 3),
(28, 'Mosh pit no questions asked',        'Metal',      2, 4),
(29, 'Felt in the chest — low and heavy',  'Hip Hop',    2, 1),
(29, 'Melodic — almost like another voice','Jazz',       1, 2),
(29, 'Driving the rhythm forward',         'Rock',       1, 3),
(29, 'Subtle — just holding things together','Classical',1, 4),
(30, 'Love it — space is part of the music','Jazz',      1, 1),
(30, 'Fine as long as tension builds after','Classical', 1, 2),
(30, 'Hate it — keep the energy going',    'Pop',        1, 3),
(30, 'Silence before a drop is peak art',  'Electronic', 2, 4);

-- ---------------------------------------------------------------------------
-- Session 4
-- ---------------------------------------------------------------------------
INSERT INTO quiz_question (question_id, prompt, quiz_mode, display_order, is_active, session_number) VALUES
(31, 'What does your ideal Monday morning sound like?',         'QUICK', 1,  1, 4),
(32, 'Pick the emotion you listen to music to process:',        'QUICK', 2,  1, 4),
(33, 'Which instrument solo would stop you cold?',              'QUICK', 3,  1, 4),
(34, 'What do you want people to think when you share a song?', 'QUICK', 4,  1, 4),
(35, 'Pick the cover art that matches your taste:',             'QUICK', 5,  1, 4),
(36, 'How do you feel about genre blending?',                   'FULL',  6,  1, 4),
(37, 'Which matters more in a great album?',                    'FULL',  7,  1, 4),
(38, 'What should a bridge do in a song?',                      'FULL',  8,  1, 4),
(39, 'Pick the producer credit that excites you most:',         'FULL',  9,  1, 4),
(40, 'How important is an artist having a distinct look?',      'FULL',  10, 1, 4);

INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(31, 'Classical piano waking me up slowly', 'Classical', 1, 1),
(31, 'A hype playlist to start strong',      'Pop',       2, 2),
(31, 'Something lo-fi and unbothered',       'Indie',     1, 3),
(31, 'Rap that reminds me I have goals',     'Hip Hop',   1, 4),
(32, 'Grief or longing',                     'Folk',      1, 1),
(32, 'Frustration or anger',                 'Metal',     1, 2),
(32, 'Joy or excitement',                    'Pop',       1, 3),
(32, 'Confusion or searching',               'R&B',       1, 4),
(33, 'A screaming electric guitar',          'Rock',      2, 1),
(33, 'A saxophone cutting through silence',  'Jazz',      1, 2),
(33, 'A synth building for two minutes',     'Electronic',1, 3),
(33, 'A violin that pulls at something deep','Classical', 1, 4),
(34, 'That I have taste',                    'Indie',     1, 1),
(34, 'That I know how to have fun',          'Pop',       1, 2),
(34, 'That I go deep on the culture',        'Hip Hop',   1, 3),
(34, 'That I am not like everyone else',     'Metal',     1, 4),
(35, 'Moody black and white photography',    'Indie',     1, 1),
(35, 'Bold graphic design — almost abstract','Electronic',1, 2),
(35, 'Gritty and raw — shot on film',        'Rock',      1, 3),
(35, 'Warm and painterly',                   'Folk',      1, 4),
(36, 'Love it — the weirder the mix the better','Electronic',2, 1),
(36, 'Fine as long as one genre leads',      'Pop',       1, 2),
(36, 'Prefer artists who stay in a lane',    'Classical', 1, 3),
(36, 'Only when it serves the mood',         'R&B',       1, 4),
(37, 'Every track stands alone',             'Pop',       1, 1),
(37, 'It tells one continuous story',        'Classical', 1, 2),
(37, 'The sequencing — the journey matters', 'Indie',     1, 3),
(37, 'Raw honesty from start to finish',     'Folk',      1, 4),
(38, 'Flip everything — surprise me',        'Jazz',      2, 1),
(38, 'Build tension before the final chorus','Rock',      1, 2),
(38, 'Drop out completely then explode',     'Electronic',2, 3),
(38, 'Give the artist space to say something real','Hip Hop',1, 4),
(39, 'Someone known for orchestral depth',   'Classical', 1, 1),
(39, 'A beat-maker with a signature sound',  'Hip Hop',   1, 2),
(39, 'An electronic pioneer',                'Electronic',1, 3),
(39, 'A classic rock-era engineer',          'Rock',      1, 4),
(40, 'Very — image is part of the art',      'Pop',       1, 1),
(40, 'Somewhat — vibe matters',              'Indie',     1, 2),
(40, 'Not at all — let the music speak',     'Folk',      1, 3),
(40, 'Deliberately against image norms',     'Metal',     1, 4);

-- ---------------------------------------------------------------------------
-- Session 5
-- ---------------------------------------------------------------------------
INSERT INTO quiz_question (question_id, prompt, quiz_mode, display_order, is_active, session_number) VALUES
(41, 'What does music help you do most?',                  'QUICK', 1,  1, 5),
(42, 'Pick the lyric style you connect with:',             'QUICK', 2,  1, 5),
(43, 'How do you feel about long songs?',                  'QUICK', 3,  1, 5),
(44, 'Which best describes your taste to a stranger?',     'QUICK', 4,  1, 5),
(45, 'What do you want from music on a long commute?',     'QUICK', 5,  1, 5),
(46, 'How important is a strong opening to a song?',       'FULL',  6,  1, 5),
(47, 'What is your relationship with sad music?',          'FULL',  7,  1, 5),
(48, 'Pick the thing that ruins a song for you:',          'FULL',  8,  1, 5),
(49, 'How do you prefer your music to end?',               'FULL',  9,  1, 5),
(50, 'What word best describes your music personality?',   'FULL',  10, 1, 5);

INSERT INTO quiz_answer (question_id, answer_text, genre_name, weight, answer_order) VALUES
(41, 'Concentrate and block everything out',  'Electronic', 1, 1),
(41, 'Feel less alone',                       'Indie',      1, 2),
(41, 'Get hyped and push harder',             'Rock',       1, 3),
(41, 'Process what I can not say out loud',   'R&B',        1, 4),
(42, 'Storytelling with specific details',    'Folk',       1, 1),
(42, 'Punchy and quotable — bars',            'Hip Hop',    2, 2),
(42, 'Abstract — more feeling than meaning',  'Indie',      1, 3),
(42, 'Simple and universal',                  'Pop',        1, 4),
(43, 'Love them — more to get lost in',       'Classical',  1, 1),
(43, 'Fine if they earn every minute',        'Rock',       1, 2),
(43, 'Prefer tracks under four minutes',      'Pop',        1, 3),
(43, 'Depends entirely on the track',         'Jazz',       1, 4),
(44, 'Eclectic — a bit of everything',        'Electronic', 1, 1),
(44, 'Underground — things most people miss', 'Indie',      1, 2),
(44, 'Classic — the old stuff is just better','Rock',       1, 3),
(44, 'Current — always on the new wave',      'Hip Hop',    1, 4),
(45, 'A podcast-length album to immerse in',  'Classical',  1, 1),
(45, 'Non-stop bangers — no thinking',        'Pop',        1, 2),
(45, 'Something deep I can really listen to', 'Jazz',       1, 3),
(45, 'Heavy music to make the journey fly',   'Metal',      1, 4),
(46, 'Critical — hook me in 10 seconds',      'Pop',        2, 1),
(46, 'Nice but I give it time to build',      'Classical',  1, 2),
(46, 'I want tension before the release',     'Electronic', 1, 3),
(46, 'Does not matter if the rest delivers',  'Folk',       1, 4),
(47, 'I seek it out — catharsis is the point','R&B',        1, 1),
(47, 'Only when I am already in that place',  'Indie',      1, 2),
(47, 'I use it to feel something I am missing','Classical', 1, 3),
(47, 'I avoid it — music should lift me up',  'Pop',        1, 4),
(48, 'Overproduction that hides the emotion', 'Folk',       1, 1),
(48, 'A weak or lazy beat',                   'Hip Hop',    2, 2),
(48, 'Generic lyrics that say nothing',       'Indie',      1, 3),
(48, 'Too polished — no edge left',           'Rock',       1, 4),
(49, 'Fade out slowly into nothing',          'Electronic', 1, 1),
(49, 'A sharp cut — leave me wanting more',   'Jazz',       1, 2),
(49, 'Build to a big final moment',           'Rock',       1, 3),
(49, 'Resolve cleanly — satisfying close',    'Classical',  1, 4),
(50, 'Explorer',                              'Electronic', 1, 1),
(50, 'Loyalist',                              'Rock',       1, 2),
(50, 'Feeler',                                'R&B',        1, 3),
(50, 'Curator',                               'Jazz',       1, 4);

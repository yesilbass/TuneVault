package com.example.tunevaultfx.wrapped;

/**
 * Holds Wrapped analytics values for one user.
 */
public class WrappedStats {

    private final String topSong;
    private final int topSongSeconds;
    private final String topArtist;
    private final int topArtistSeconds;
    private final String favoriteGenre;
    private final int favoriteGenreSeconds;
    private final int totalListeningSeconds;
    private final String summary;

    public WrappedStats(String topSong,
                        int topSongSeconds,
                        String topArtist,
                        int topArtistSeconds,
                        String favoriteGenre,
                        int favoriteGenreSeconds,
                        int totalListeningSeconds,
                        String summary) {
        this.topSong = topSong;
        this.topSongSeconds = topSongSeconds;
        this.topArtist = topArtist;
        this.topArtistSeconds = topArtistSeconds;
        this.favoriteGenre = favoriteGenre;
        this.favoriteGenreSeconds = favoriteGenreSeconds;
        this.totalListeningSeconds = totalListeningSeconds;
        this.summary = summary;
    }

    public static WrappedStats empty() {
        return new WrappedStats(
                "No listening data yet",
                0,
                "No listening data yet",
                0,
                "No listening data yet",
                0,
                0,
                "Start playing songs to generate your Wrapped statistics."
        );
    }

    public String getTopSong() {
        return topSong;
    }

    public int getTopSongSeconds() {
        return topSongSeconds;
    }

    public String getTopArtist() {
        return topArtist;
    }

    public int getTopArtistSeconds() {
        return topArtistSeconds;
    }

    public String getFavoriteGenre() {
        return favoriteGenre;
    }

    public int getFavoriteGenreSeconds() {
        return favoriteGenreSeconds;
    }

    public int getTotalListeningSeconds() {
        return totalListeningSeconds;
    }

    public String getSummary() {
        return summary;
    }
}
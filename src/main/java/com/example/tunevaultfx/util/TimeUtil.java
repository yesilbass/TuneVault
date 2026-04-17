package com.example.tunevaultfx.util;

/**
 * Utility class for formatting time values (track clocks, playlist totals, etc.).
 */
public class TimeUtil {

    private TimeUtil() {
    }

    /** Per-track style: {@code m:ss}. */
    public static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + String.format("%02d", seconds);
    }

    /**
     * Whole-playlist length for headers: hours and minutes only (no seconds). Total track seconds are
     * rounded to the nearest minute before splitting into hours/minutes.
     */
    public static String formatPlaylistTotalDuration(int totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 minutes";
        }
        long totalMinutes = Math.round(totalSeconds / 60.0);
        if (totalMinutes == 0) {
            return "Under 1 minute";
        }
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes == 1 ? "1 minute" : minutes + " minutes";
        }
        if (minutes == 0) {
            return hours == 1 ? "1 hour" : hours + " hours";
        }
        String hourPart = hours == 1 ? "1 hour" : hours + " hours";
        String minPart = minutes == 1 ? "1 minute" : minutes + " minutes";
        return hourPart + " " + minPart;
    }
}
package com.example.tunevaultfx.playlist;

import com.example.tunevaultfx.core.Song;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.Locale;

/**
 * Small rounded “art” tile for playlist rows — gradient + initial/glyph, stable per song identity
 * (no image URLs in {@link Song} today).
 */
public final class SongRowArtGraphic {

    private SongRowArtGraphic() {}

    public static StackPane create(double side, Song song) {
        if (song == null) {
            song = new Song("", "", "", "", 0);
        }
        String glyphText = glyphForSong(song);
        Label glyph = new Label(glyphText);
        glyph.getStyleClass().add("playlist-track-art-glyph");
        glyph.setStyle(String.format("-fx-font-size: %.0fpx;", Math.max(10, side * 0.34)));

        StackPane stack = new StackPane(glyph);
        stack.setAlignment(Pos.CENTER);
        stack.setMinSize(side, side);
        stack.setPrefSize(side, side);
        stack.setMaxSize(side, side);
        stack.getStyleClass().add("playlist-track-art");
        int idx = Math.floorMod(stableKey(song), 8);
        stack.getStyleClass().add("playlist-cover-palette-" + idx);
        return stack;
    }

    private static int stableKey(Song song) {
        return (song.title() + "\0" + song.artist() + "\0" + song.songId()).hashCode();
    }

    private static String glyphForSong(Song song) {
        String t = song.title();
        if (t != null && !t.isBlank()) {
            String trim = t.trim();
            int cp = trim.codePointAt(0);
            return new String(Character.toChars(cp)).toUpperCase(Locale.ROOT);
        }
        return "\u266A";
    }
}

package com.example.tunevaultfx.recommendation;

import com.example.tunevaultfx.core.Song;

/**
 * One row in a blended song / artist search result list, ordered by text match plus personal taste.
 */
public sealed interface RankedSearchRow permits RankedSearchRow.SongHit, RankedSearchRow.ArtistHit {

    record SongHit(Song song) implements RankedSearchRow {}

    record ArtistHit(String artistName) implements RankedSearchRow {}
}

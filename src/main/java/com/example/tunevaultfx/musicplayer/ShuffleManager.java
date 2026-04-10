package com.example.tunevaultfx.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages shuffled playback order without repeating songs in the same cycle.
 */
public class ShuffleManager {

    private final List<Integer> shuffleOrder = new ArrayList<>();
    private int shufflePosition = -1;

    public void reset() {
        shuffleOrder.clear();
        shufflePosition = -1;
    }

    public boolean isEmpty() {
        return shuffleOrder.isEmpty();
    }

    public void createShuffleOrderStartingFrom(int queueSize, int startIndex) {
        reset();

        if (queueSize <= 0) {
            return;
        }

        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < queueSize; i++) {
            if (i != startIndex) {
                remaining.add(i);
            }
        }

        Collections.shuffle(remaining);

        if (startIndex >= 0 && startIndex < queueSize) {
            shuffleOrder.add(startIndex);
            shuffleOrder.addAll(remaining);
            shufflePosition = 0;
        } else {
            shuffleOrder.addAll(remaining);
        }
    }

    public Integer nextIndex(int queueSize, int currentIndex, boolean loopEnabled) {
        if (queueSize <= 0) {
            return null;
        }

        if (shuffleOrder.isEmpty()) {
            createShuffleOrderStartingFrom(queueSize, currentIndex);
        }

        if (shufflePosition + 1 < shuffleOrder.size()) {
            shufflePosition++;
            return shuffleOrder.get(shufflePosition);
        }

        if (loopEnabled) {
            createShuffleOrderStartingFrom(queueSize, -1);
            if (!shuffleOrder.isEmpty()) {
                shufflePosition = 0;
                return shuffleOrder.get(shufflePosition);
            }
        }

        return null;
    }

    public Integer previousIndex(int queueSize, int currentIndex, boolean loopEnabled) {
        if (queueSize <= 0) {
            return null;
        }

        if (shuffleOrder.isEmpty()) {
            createShuffleOrderStartingFrom(queueSize, currentIndex);
        }

        if (shufflePosition > 0) {
            shufflePosition--;
            return shuffleOrder.get(shufflePosition);
        }

        if (loopEnabled && !shuffleOrder.isEmpty()) {
            shufflePosition = shuffleOrder.size() - 1;
            return shuffleOrder.get(shufflePosition);
        }

        return null;
    }
}
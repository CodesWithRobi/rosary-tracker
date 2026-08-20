package com.example.rosarytracker.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Room entity representing the current state of the rosary tracker.
 */
@Entity(tableName = "rosary_state")
public class RosaryState {

    @PrimaryKey
    public int id = 1;

    /** Current mystery index (0-19). */
    public int currentMysteryIndex;

    /** Number of full sets (5 mysteries each) completed today. */
    public int todayCompletions;

    public String lastResetDate;
    public boolean audioEnabled;
    public String mode;
    public int targetRosaries;
    public boolean isPlaying;

    /** Minutes to wait before auto-advancing to the next mystery. Default is 2. */
    public int mysteryTimerMinutes = 2;

    public MysterySet getCurrentSet() {
        validateIndex();
        return MysterySet.fromGlobalIndex(currentMysteryIndex);
    }

    public int getCurrentLocalIndex() {
        validateIndex();
        return MysterySet.toLocalIndex(currentMysteryIndex);
    }

    /** Ensures currentMysteryIndex is within valid range (0-19). */
    private void validateIndex() {
        if (currentMysteryIndex < 0 || currentMysteryIndex > 19) {
            currentMysteryIndex = 0;
        }
    }

    public String getCurrentMysteryName() {
        MysterySet set = getCurrentSet();
        int localIndex = getCurrentLocalIndex();
        String mysteryName = set.getMystery(localIndex);
        String ordinal = getOrdinal(localIndex + 1);
        return ordinal + " " + set.getDisplayName() + " " + mysteryName;
    }

    public void advanceToNext() {
        validateIndex();
        if ("LITURGICAL".equals(mode)) {
            int[] sequence = MysterySet.getLiturgicalSequence();
            int currentPos = -1;
            for (int i = 0; i < 20; i++) {
                if (sequence[i] == currentMysteryIndex) {
                    currentPos = i;
                    break;
                }
            }
            int nextPos = (currentPos + 1) % 20;
            currentMysteryIndex = sequence[nextPos];
            // Completion is tracked every 5 steps in the sequence
            if (nextPos % 5 == 0 && nextPos != 0) {
                todayCompletions++;
            } else if (nextPos == 0) {
                // Wrapped full 20 mysteries
                todayCompletions++;
            }
        } else {
            currentMysteryIndex = (currentMysteryIndex + 1) % 20;
            if (currentMysteryIndex % 5 == 0) {
                todayCompletions++;
            }
        }
    }

    public void goToPrevious() {
        validateIndex();
        if ("LITURGICAL".equals(mode)) {
            int[] sequence = MysterySet.getLiturgicalSequence();
            int currentPos = -1;
            for (int i = 0; i < 20; i++) {
                if (sequence[i] == currentMysteryIndex) {
                    currentPos = i;
                    break;
                }
            }
            int prevPos = (currentPos - 1 + 20) % 20;
            currentMysteryIndex = sequence[prevPos];
        } else {
            currentMysteryIndex = (currentMysteryIndex - 1 + 20) % 20;
        }
    }

    public void checkAndResetDaily() {
        String today = getTodayDateString();
        if (!today.equals(lastResetDate)) {
            todayCompletions = 0;
            lastResetDate = today;
            if ("LITURGICAL".equals(mode)) {
                currentMysteryIndex = MysterySet.toGlobalIndex(MysterySet.getLiturgicalSet(), 0);
            }
        }
    }

    private static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(Calendar.getInstance().getTime());
    }

    private static String getOrdinal(int n) {
        if (n >= 11 && n <= 13) return n + "th";
        switch (n % 10) {
            case 1: return n + "st";
            case 2: return n + "nd";
            case 3: return n + "rd";
            default: return n + "th";
        }
    }
}

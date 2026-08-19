package com.example.rosarytracker.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Room entity representing the current state of the rosary tracker.
 * Singleton row (id=1) that persists across app restarts.
 */
@Entity(tableName = "rosary_state")
public class RosaryState {

    @PrimaryKey
    public int id = 1;

    /** Current mystery index (0-19). 0=1st Joyful, 19=5th Glorious. */
    public int currentMysteryIndex;

    /** Number of full rosaries completed today. */
    public int todayCompletions;

    /** ISO date string of last reset (e.g. "2026-08-19"). Used to detect new day. */
    public String lastResetDate;

    /** Whether Bishop Barron audio is enabled. */
    public boolean audioEnabled;

    /** Mode: "LITURGICAL" (day's mysteries first) or "CUSTOM" (user picks order). */
    public String mode;

    /** Target number of rosaries per day (1-4). */
    public int targetRosaries;

    /** Whether playback is currently active. */
    public boolean isPlaying;

    /**
     * Returns the current MysterySet based on currentMysteryIndex.
     * Maps 0-4→JOYFUL, 5-9→LUMINOUS, 10-14→SORROWFUL, 15-19→GLORIOUS.
     */
    public MysterySet getCurrentSet() {
        return MysterySet.fromGlobalIndex(currentMysteryIndex);
    }

    /**
     * Returns the local mystery index (0-4) within the current set.
     */
    public int getCurrentLocalIndex() {
        return MysterySet.toLocalIndex(currentMysteryIndex);
    }

    /**
     * Returns a display string like "3rd Joyful — The Nativity".
     */
    public String getCurrentMysteryName() {
        MysterySet set = getCurrentSet();
        int localIndex = getCurrentLocalIndex();
        String mysteryName = set.getMystery(localIndex);
        String ordinal = getOrdinal(localIndex + 1);
        return ordinal + " " + set.getDisplayName() + " — " + mysteryName;
    }

    /**
     * Advances to the next mystery. Wraps from 19 back to 0 and increments completions.
     */
    public void advanceToNext() {
        int prevIndex = currentMysteryIndex;
        currentMysteryIndex = (currentMysteryIndex + 1) % 20;
        // If we wrapped around (went from 19 to 0), a full rosary was completed
        if (currentMysteryIndex == 0 && prevIndex == 19) {
            todayCompletions++;
        }
    }

    /**
     * Goes back to the previous mystery. Wraps from 0 to 19.
     */
    public void goToPrevious() {
        currentMysteryIndex = (currentMysteryIndex - 1 + 20) % 20;
    }

    /**
     * Checks if today is a new day and resets completions to 0 if so.
     */
    public void checkAndResetDaily() {
        String today = getTodayDateString();
        if (!today.equals(lastResetDate)) {
            todayCompletions = 0;
            lastResetDate = today;
        }
    }

    /**
     * Returns today's date as ISO string (e.g. "2026-08-19").
     * Uses SimpleDateFormat for API 24+ compatibility (java.time.LocalDate requires API 26).
     */
    private static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(Calendar.getInstance().getTime());
    }

    /**
     * Returns ordinal string for a number (1→"1st", 2→"2nd", 3→"3rd", etc).
     */
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

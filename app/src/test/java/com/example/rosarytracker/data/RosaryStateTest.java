package com.example.rosarytracker.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.junit.Test;

/**
 * Regression tests for RosaryState transitions.
 *
 * Bug 1 regression: the service used to write a stale cached state back to the
 * database after skip operations, clobbering settings the user had changed in the
 * UI (timer, mode, target rosaries, audio). These tests pin the invariant that
 * advance/goToPrevious never touch settings, and that completions are counted
 * exactly once per completed set.
 */
public class RosaryStateTest {

    private RosaryState stateWithSettings() {
        RosaryState state = new RosaryState();
        state.currentMysteryIndex = 0;
        state.mode = "CUSTOM";
        state.targetRosaries = 3;
        state.mysteryTimerMinutes = 5;
        state.audioEnabled = true;
        return state;
    }

    @Test
    public void advanceToNext_preservesAllSettings() {
        RosaryState state = stateWithSettings();
        state.advanceToNext();
        assertEquals("CUSTOM", state.mode);
        assertEquals(3, state.targetRosaries);
        assertEquals(5, state.mysteryTimerMinutes);
        assertTrue(state.audioEnabled);
        assertEquals(1, state.currentMysteryIndex);
    }

    @Test
    public void goToPrevious_preservesAllSettings() {
        RosaryState state = stateWithSettings();
        state.currentMysteryIndex = 7;
        state.goToPrevious();
        assertEquals("CUSTOM", state.mode);
        assertEquals(3, state.targetRosaries);
        assertEquals(5, state.mysteryTimerMinutes);
        assertTrue(state.audioEnabled);
        assertEquals(6, state.currentMysteryIndex);
    }

    @Test
    public void advanceToNext_customMode_incrementsCompletionAtSetBoundary() {
        RosaryState state = stateWithSettings();
        state.currentMysteryIndex = 4;
        state.advanceToNext();
        assertEquals(5, state.currentMysteryIndex);
        assertEquals(1, state.todayCompletions);
    }

    @Test
    public void advanceToNext_wrapsAfterFinalMystery() {
        RosaryState state = stateWithSettings();
        state.currentMysteryIndex = 19;
        state.advanceToNext();
        assertEquals(0, state.currentMysteryIndex);
        assertEquals(1, state.todayCompletions);
    }

    @Test
    public void liturgicalMode_followsDailySequenceAndCountsCompletion() {
        RosaryState state = new RosaryState();
        state.mode = "LITURGICAL";
        state.currentMysteryIndex = MysterySet.toGlobalIndex(MysterySet.getLiturgicalSet(), 0);

        int[] seq = MysterySet.getLiturgicalSequence();
        assertEquals(seq[0], state.currentMysteryIndex);

        // 5 advances = one completed set, completion counted exactly once
        for (int i = 1; i <= 5; i++) {
            state.advanceToNext();
        }
        assertEquals(seq[5], state.currentMysteryIndex);
        assertEquals(1, state.todayCompletions);
    }

    @Test
    public void checkAndResetDaily_resetsCompletionsWhenDateChanged() {
        RosaryState state = stateWithSettings();
        state.todayCompletions = 3;
        state.lastResetDate = "2000-01-01";
        state.checkAndResetDaily();
        assertEquals(0, state.todayCompletions);
        assertNotEquals("2000-01-01", state.lastResetDate);
    }

    @Test
    public void checkAndResetDaily_keepsCompletionsSameDay() {
        RosaryState state = stateWithSettings();
        state.todayCompletions = 3;
        state.lastResetDate = today();
        state.checkAndResetDaily();
        assertEquals(3, state.todayCompletions);
        assertEquals(today(), state.lastResetDate);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
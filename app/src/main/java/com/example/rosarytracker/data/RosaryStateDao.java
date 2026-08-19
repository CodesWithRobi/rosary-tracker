package com.example.rosarytracker.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Data access object for RosaryState.
 * Room auto-generates the implementation from these annotated method signatures.
 */
@Dao
public interface RosaryStateDao {

    /**
     * Insert or replace the singleton state row (id=1).
     * Room handles this via @Insert annotation — no manual SQL needed.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(RosaryState state);

    /**
     * Get the current state. Returns null if no state exists yet (first launch).
     */
    @Query("SELECT * FROM rosary_state WHERE id = 1")
    RosaryState getState();

    /**
     * Update just the current mystery index field.
     */
    @Query("UPDATE rosary_state SET currentMysteryIndex = :index WHERE id = 1")
    void updateMysteryIndex(int index);

    /**
     * Update just the playing state field.
     */
    @Query("UPDATE rosary_state SET isPlaying = :playing WHERE id = 1")
    void updatePlayingState(boolean playing);

    /**
     * Update today's completion count.
     */
    @Query("UPDATE rosary_state SET todayCompletions = :count WHERE id = 1")
    void updateCompletions(int count);

    /**
     * Delete all state (full reset).
     */
    @Query("DELETE FROM rosary_state")
    void deleteAll();
}

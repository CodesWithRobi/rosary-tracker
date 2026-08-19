package com.example.rosarytracker.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Room database for persisting rosary state across restarts.
 * Single table: rosary_state (singleton row with id=1).
 */
@Database(entities = {RosaryState.class}, version = 1, exportSchema = false)
public abstract class RosaryDatabase extends RoomDatabase {

    private static volatile RosaryDatabase INSTANCE;

    /**
     * Returns the singleton database instance using double-checked locking.
     */
    public static RosaryDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RosaryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            RosaryDatabase.class,
                            "rosary_tracker.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Returns the DAO for rosary state operations.
     * Room auto-generates the implementation from this abstract method.
     */
    public abstract RosaryStateDao rosaryStateDao();

    /**
     * Creates a default RosaryState with sensible initial values for first launch.
     */
    public static RosaryState createDefaultState() {
        RosaryState state = new RosaryState();
        state.currentMysteryIndex = 0;
        state.todayCompletions = 0;
        state.lastResetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Calendar.getInstance().getTime());
        state.audioEnabled = false;
        state.mode = "LITURGICAL";
        state.targetRosaries = 1;
        state.isPlaying = false;
        return state;
    }
}

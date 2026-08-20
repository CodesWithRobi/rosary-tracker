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
 */
@Database(entities = {RosaryState.class}, version = 1, exportSchema = false)
public abstract class RosaryDatabase extends RoomDatabase {

    private static volatile RosaryDatabase INSTANCE;

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

    public abstract RosaryStateDao rosaryStateDao();

    /**
     * Creates a default RosaryState with sensible initial values for first launch.
     */
    public static RosaryState createDefaultState() {
        RosaryState state = new RosaryState();
        state.todayCompletions = 0;
        state.lastResetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Calendar.getInstance().getTime());
        state.audioEnabled = true;
        state.mode = "LITURGICAL";
        state.targetRosaries = 1;
        state.isPlaying = false;
        
        // Initialize to today's liturgical mystery set
        state.currentMysteryIndex = MysterySet.toGlobalIndex(MysterySet.getLiturgicalSet(), 0);

        return state;
    }
}

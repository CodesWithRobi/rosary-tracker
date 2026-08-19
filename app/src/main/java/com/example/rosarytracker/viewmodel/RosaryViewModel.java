package com.example.rosarytracker.viewmodel;

import android.app.Application;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.rosarytracker.data.RosaryDatabase;
import com.example.rosarytracker.data.RosaryState;
import com.example.rosarytracker.service.RosaryPlaybackService;
import java.util.concurrent.Executors;

/**
 * ViewModel that bridges the UI with the RosaryPlaybackService and Room database.
 * Survives configuration changes (screen rotation) and holds UI state.
 */
public class RosaryViewModel extends AndroidViewModel {

    private final RosaryDatabase database;
    private final MutableLiveData<RosaryState> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlayingLiveData = new MutableLiveData<>(false);

    public RosaryViewModel(@NonNull Application application) {
        super(application);
        database = RosaryDatabase.getInstance(application);
    }

    /**
     * Returns observable rosary state. UI should observe this to update display.
     */
    public LiveData<RosaryState> getState() {
        return stateLiveData;
    }

    /**
     * Returns whether playback is active. UI observes this to toggle play/pause icon.
     */
    public LiveData<Boolean> isPlaying() {
        return isPlayingLiveData;
    }

    /**
     * Loads the current state from the database on a background thread.
     */
    public void loadState() {
        Executors.newSingleThreadExecutor().execute(() -> {
            RosaryState state = database.rosaryStateDao().getState();
            if (state == null) {
                state = RosaryDatabase.createDefaultState();
            }
            state.checkAndResetDaily();
            stateLiveData.postValue(state);
        });
    }

    /**
     * Saves state to the database on a background thread.
     */
    public void saveState(RosaryState state) {
        Executors.newSingleThreadExecutor().execute(() ->
                database.rosaryStateDao().insertOrUpdate(state));
    }

    /**
     * Starts playback by launching RosaryPlaybackService as a foreground service.
     */
    public void startPlayback() {
        Intent intent = new Intent(getApplication(), RosaryPlaybackService.class);
        ContextCompat.startForegroundService(getApplication(), intent);
        isPlayingLiveData.postValue(true);
    }

    /**
     * Stops playback by stopping RosaryPlaybackService.
     */
    public void stopPlayback() {
        Intent intent = new Intent(getApplication(), RosaryPlaybackService.class);
        getApplication().stopService(intent);
        isPlayingLiveData.postValue(false);
    }

    /**
     * Updates the rosary count target (1-4) and saves to database.
     */
    public void setTargetRosaries(int count) {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.targetRosaries = count;
            saveState(state);
            stateLiveData.postValue(state);
        }
    }

    /**
     * Toggles between LITURGICAL and CUSTOM mode.
     */
    public void toggleMode() {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.mode = "LITURGICAL".equals(state.mode) ? "CUSTOM" : "LITURGICAL";
            saveState(state);
            stateLiveData.postValue(state);
        }
    }

    /**
     * Toggles audio on/off.
     */
    public void toggleAudio() {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.audioEnabled = !state.audioEnabled;
            saveState(state);
            stateLiveData.postValue(state);
        }
    }

    /**
     * Returns the display string for the current mystery.
     */
    public String getCurrentMysteryDisplay() {
        RosaryState state = stateLiveData.getValue();
        return state != null ? state.getCurrentMysteryName() : "No mystery selected";
    }

    /**
     * Returns completion text like "2 of 3 rosaries completed".
     */
    public String getCompletionDisplay() {
        RosaryState state = stateLiveData.getValue();
        if (state == null) return "0 of 1 rosaries today";
        return state.todayCompletions + " of " + state.targetRosaries + " rosaries today";
    }
}

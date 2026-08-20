package com.example.rosarytracker.viewmodel;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.example.rosarytracker.data.MysterySet;
import com.example.rosarytracker.data.RosaryDatabase;
import com.example.rosarytracker.data.RosaryState;
import com.example.rosarytracker.service.RosaryPlaybackService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * ViewModel that bridges the UI with the RosaryPlaybackService and Room database.
 * Refined for precise mystery-level tracking, countdowns, and user-defined timers.
 */
@UnstableApi
public class RosaryViewModel extends AndroidViewModel {

    private final RosaryDatabase database;
    private final MutableLiveData<RosaryState> stateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlayingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Long> currentPositionLiveData = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> durationLiveData = new MutableLiveData<>(120000L);
    private final MutableLiveData<String> countdownLiveData = new MutableLiveData<>("02:00");

    private MediaController mediaController;
    private final Handler positionHandler = new Handler(Looper.getMainLooper());
    private final Runnable positionUpdater = new Runnable() {
        @Override
        public void run() {
            MediaController controller = mediaController;
            if (controller != null && controller.isPlaying()) {
                long pos = controller.getCurrentPosition();
                long dur = controller.getDuration();

                RosaryState state = stateLiveData.getValue();
                if (state != null) {
                    long targetDur;
                    if (state.audioEnabled && dur > 0) {
                        // Audio mode: one file per mystery, so the file's own
                        // duration drives the countdown.
                        targetDur = dur;
                    } else {
                        // Silent mode (or duration not known yet): the timer drives it.
                        targetDur = state.mysteryTimerMinutes * 60000L;
                    }
                    long relPos = pos;

                    durationLiveData.postValue(targetDur);
                    currentPositionLiveData.postValue(relPos);
                    countdownLiveData.postValue(formatTime(Math.max(0, targetDur - relPos)));
                }
            }
            positionHandler.postDelayed(this, 1000);
        }
    };

    public RosaryViewModel(@NonNull Application application) {
        super(application);
        database = RosaryDatabase.getInstance(application);
        initMediaController();
        positionHandler.post(positionUpdater);
    }

    private void initMediaController() {
        SessionToken sessionToken = new SessionToken(getApplication(),
                new ComponentName(getApplication(), RosaryPlaybackService.class));
        ListenableFuture<MediaController> controllerFuture =
                new MediaController.Builder(getApplication(), sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        isPlayingLiveData.postValue(isPlaying);
                    }

                    @Override
                    public void onMediaMetadataChanged(MediaMetadata metadata) {
                        // The service advances mysteries from anywhere (auto-advance,
                        // notification buttons, in-app buttons) and writes to the DB.
                        // Reload on every metadata change so the UI always reflects
                        // the latest mystery: name, beads, artwork, progress.
                        loadState();
                    }
                });
                isPlayingLiveData.postValue(mediaController.isPlaying());
            } catch (ExecutionException | InterruptedException e) {
                // Ignore
            }
        }, MoreExecutors.directExecutor());
    }

    public LiveData<RosaryState> getState() { return stateLiveData; }
    public LiveData<Boolean> isPlaying() { return isPlayingLiveData; }
    public LiveData<Long> getCurrentPosition() { return currentPositionLiveData; }
    public LiveData<Long> getDuration() { return durationLiveData; }
    public LiveData<String> getCountdown() { return countdownLiveData; }

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

    public void saveState(RosaryState state) {
        Executors.newSingleThreadExecutor().execute(() ->
                database.rosaryStateDao().insertOrUpdate(state));
    }

    public void startPlayback() {
        if (mediaController != null && mediaController.isConnected()) {
            mediaController.play();
        } else {
            Intent intent = new Intent(getApplication(), RosaryPlaybackService.class);
            ContextCompat.startForegroundService(getApplication(), intent);
        }
    }

    public void stopPlayback() {
        if (mediaController != null) {
            mediaController.pause();
        }
    }

    public void seekTo(long relPos) {
        if (mediaController != null && mediaController.isConnected()) {
            mediaController.seekTo(relPos);
        }
    }

    public void resetProgress() {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.todayCompletions = 0;
            saveState(state);
            stateLiveData.postValue(state);
        }
    }

    public void setTargetRosaries(int count) {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.targetRosaries = count;
            saveState(state);
            stateLiveData.postValue(state);
        }
    }

    public void setMysteryTimerMinutes(int minutes) {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.mysteryTimerMinutes = minutes;
            saveState(state);
            stateLiveData.postValue(state);
            notifyServiceSettingsChanged();
        }
    }

    public void toggleMode() {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.mode = "LITURGICAL".equals(state.mode) ? "CUSTOM" : "LITURGICAL";
            if ("LITURGICAL".equals(state.mode)) {
                state.currentMysteryIndex = MysterySet.toGlobalIndex(MysterySet.getLiturgicalSet(), 0);
            }
            saveState(state);
            stateLiveData.postValue(state);
            notifyServiceSettingsChanged();
        }
    }

    public void toggleAudio() {
        RosaryState state = stateLiveData.getValue();
        if (state != null) {
            state.audioEnabled = !state.audioEnabled;
            saveState(state);
            stateLiveData.postValue(state);
            notifyServiceSettingsChanged();
        }
    }

    /**
     * Notify the service that settings changed. The service will reload state from DB.
     * This avoids race conditions — the service is the source of truth while running.
     */
    private void notifyServiceSettingsChanged() {
        Intent intent = new Intent(getApplication(), RosaryPlaybackService.class);
        intent.setAction(RosaryPlaybackService.ACTION_SETTINGS_CHANGED);
        getApplication().startService(intent);
    }

    public String getCompletionDisplay() {
        RosaryState state = stateLiveData.getValue();
        if (state == null) return "0 of 1 rosaries today";
        return state.todayCompletions + " of " + state.targetRosaries + " rosaries today";
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "00:00";
        long totalSecs = ms / 1000;
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        return String.format(java.util.Locale.US, "%02d:%02d", mins, secs);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mediaController != null) {
            mediaController.release();
        }
        positionHandler.removeCallbacks(positionUpdater);
    }
}

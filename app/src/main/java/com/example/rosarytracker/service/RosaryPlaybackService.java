package com.example.rosarytracker.service;

import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.example.rosarytracker.data.RosaryDatabase;
import com.example.rosarytracker.data.RosaryState;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service that hosts the MediaSession.
 * Tricks Android into thinking music is playing so lock screen controls and
 * headphone buttons work for navigating Rosary mysteries.
 */
public class RosaryPlaybackService extends MediaSessionService {

    private MediaSession mediaSession;
    private ExoPlayer player;
    private Player.Listener playerListener;
    private RosaryDatabase database;
    private RosaryState currentState;
    private Handler autoAdvanceHandler;
    private RosaryNotificationManager notificationManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /** Auto-advance delay in milliseconds (2 minutes default). */
    private static final long AUTO_ADVANCE_DELAY_MS = 2 * 60 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this).build();

        // Initialize notification manager
        notificationManager = new RosaryNotificationManager(this);

        // Initialize auto-advance handler
        autoAdvanceHandler = new Handler(Looper.getMainLooper());

        // Initialize database
        database = RosaryDatabase.getInstance(this);

        // Add player listener
        playerListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (currentState == null) return;
                currentState.isPlaying = isPlaying;
                if (isPlaying) {
                    scheduleAutoAdvance();
                } else {
                    cancelAutoAdvance();
                }
                showForegroundNotification();
                saveState();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    skipToNext();
                    player.prepare();
                }
            }

            @Override
            public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                if (reason == Player.DISCONTINUITY_REASON_SKIP) {
                    if (newPosition.mediaItemIndex > oldPosition.mediaItemIndex) {
                        skipToNext();
                    } else {
                        skipToPrevious();
                    }
                }
            }
        };
        player.addListener(playerListener);

        // Build MediaSession
        mediaSession = new MediaSession.Builder(this, player).build();

        // Initial load of state
        loadState(() -> {
            if (currentState != null && currentState.isPlaying) {
                startPlaybackInternal();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Essential: startForeground must be called within 10s of startForegroundService
        showForegroundNotification();

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_NEXT":
                    skipToNext();
                    break;
                case "ACTION_PREVIOUS":
                    skipToPrevious();
                    break;
                case "ACTION_PLAY_PAUSE":
                    handlePlayPause();
                    break;
            }
        } else if (intent != null) {
            // Initial start from ViewModel
            if (currentState != null) {
                startPlaybackInternal();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void handlePlayPause() {
        if (player.isPlaying()) {
            pausePlayback();
        } else {
            startPlaybackInternal();
        }
    }

    @Override
    public void onDestroy() {
        if (currentState != null) saveState();
        cancelAutoAdvance();
        executorService.shutdown();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            if (playerListener != null) player.removeListener(playerListener);
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    private void skipToNext() {
        if (currentState == null) return;
        currentState.advanceToNext();
        updateAndNotify();
    }

    private void skipToPrevious() {
        if (currentState == null) return;
        currentState.goToPrevious();
        updateAndNotify();
    }

    private void updateAndNotify() {
        updateMediaMetadata();
        showForegroundNotification();
        saveState();
        if (player.isPlaying()) scheduleAutoAdvance();
    }

    private void startPlaybackInternal() {
        if (currentState == null) return;
        currentState.isPlaying = true;
        player.setRepeatMode(Player.REPEAT_MODE_ALL);
        updateMediaMetadata();
        player.prepare();
        player.play();
        showForegroundNotification();
        saveState();
        scheduleAutoAdvance();
    }

    private void pausePlayback() {
        if (currentState == null) return;
        currentState.isPlaying = false;
        player.pause();
        cancelAutoAdvance();
        showForegroundNotification();
        saveState();
    }

    private void scheduleAutoAdvance() {
        cancelAutoAdvance();
        autoAdvanceHandler.postDelayed(this::skipToNext, AUTO_ADVANCE_DELAY_MS);
    }

    private void cancelAutoAdvance() {
        autoAdvanceHandler.removeCallbacksAndMessages(null);
    }

    private void updateMediaMetadata() {
        if (currentState == null) return;
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(currentState.getCurrentMysteryName())
                .setArtist("Rosary Tracker")
                .setAlbumTitle(currentState.getCurrentSet().getDisplayName() + " Mysteries")
                .build();

        // Use a valid placeholder URI for the fake playback
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri("http://example.com/rosary")
                .setMediaMetadata(metadata)
                .build();
        player.setMediaItem(mediaItem);
    }

    private void showForegroundNotification() {
        if (notificationManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(RosaryNotificationManager.NOTIFICATION_ID,
                    notificationManager.buildNotification(currentState, mediaSession),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(RosaryNotificationManager.NOTIFICATION_ID,
                    notificationManager.buildNotification(currentState, mediaSession));
        }
    }

    private void saveState() {
        if (currentState == null) return;
        executorService.execute(() -> database.rosaryStateDao().insertOrUpdate(currentState));
    }

    private void loadState(Runnable onLoaded) {
        executorService.execute(() -> {
            RosaryState loaded = database.rosaryStateDao().getState();
            if (loaded == null) {
                loaded = RosaryDatabase.createDefaultState();
            }
            loaded.checkAndResetDaily();
            currentState = loaded;
            if (onLoaded != null) {
                new Handler(Looper.getMainLooper()).post(onLoaded);
            }
        });
    }
}

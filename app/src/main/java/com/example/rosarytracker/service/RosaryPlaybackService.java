package com.example.rosarytracker.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.example.rosarytracker.MainActivity;
import com.example.rosarytracker.data.MysterySet;
import com.example.rosarytracker.data.RosaryDatabase;
import com.example.rosarytracker.data.RosaryState;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Foreground service that hosts the MediaSession.
 * Tracks Rosary mysteries and plays optional Bishop Barron reflections.
 *
 * State handling: the Room database is the single source of truth. Every mutation
 * goes through {@link #mutateState} which reads the latest row, applies the change,
 * and writes it back on the single-thread executor. This prevents the stale-write
 * bug where a long-lived cached copy of the state clobbered settings the user had
 * changed in the UI.
 */
@UnstableApi
public class RosaryPlaybackService extends MediaSessionService {

private MediaSession mediaSession;
    private Player player;
    private Player.Listener playerListener;
    private RosaryDatabase database;
    private volatile RosaryState currentState;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean destroyed = false;

    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_SETTINGS_CHANGED = "ACTION_SETTINGS_CHANGED";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize ExoPlayer with AudioFocus and WakeLock
        ExoPlayer exoPlayer = new ExoPlayer.Builder(this)
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setHandleAudioBecomingNoisy(true)
                .build();

        // Wrap the player so seek-to-next and seek-to-previous are always available.
        // The app plays a single media item, so ExoPlayer never reports
        // COMMAND_SEEK_TO_NEXT as available (hasNextMediaItem() is always false).
        // MediaSession intersects the session-declared commands with the player's
        // real commands before granting them to controllers, so without this wrapper
        // the system media controls (lock screen, quick settings, notification) keep
        // the next button disabled and reject the command before any callback runs.
        // Real players like Spotify/Google Music don't hit this because they play
        // multi-item playlists.
        player = new ForwardingPlayer(exoPlayer) {
            @Override
            public Player.Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .build();
            }

            @Override
            public boolean isCommandAvailable(@Player.Command int command) {
                return command == Player.COMMAND_SEEK_TO_NEXT
                        || command == Player.COMMAND_SEEK_TO_PREVIOUS
                        || super.isCommandAvailable(command);
            }

            @Override
            public void seekToNext() {
                skipToNext();
            }

            @Override
            public void seekToPrevious() {
                skipToPrevious();
            }
        };

        database = RosaryDatabase.getInstance(this);

        playerListener = new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (currentState == null) return;
                // Persist the playing flag on a fresh state read, never a stale cache.
                mutateState(state -> state.isPlaying = isPlaying, null);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    // Each mystery is its own audio file (or a timer-clipped track in
                    // silent mode). When a file finishes, advance to the next mystery.
                    // REPEAT_MODE_OFF guarantees STATE_ENDED fires at the boundary.
                    skipToNext();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (currentState != null && currentState.audioEnabled) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(getApplicationContext(), "Audio reflection unavailable. Switching to tracker mode.", Toast.LENGTH_LONG).show()
                    );
                    mutateState(state -> state.audioEnabled = false, RosaryPlaybackService.this::updateAndNotify);
                }
            }
        };
        player.addListener(playerListener);

        // Media3's default MediaNotificationProvider owns the notification lifecycle
        // (startForeground when active, in-place updates, session-routed actions with
        // the system's animated play/pause button). The previous manual
        // startForeground() calls fought the provider and caused the notification to
        // flicker: every button press re-posted it.
        // setSessionActivity makes tapping the notification open the app.
        Intent contentIntent = new Intent(this, MainActivity.class);
        PendingIntent contentPending = PendingIntent.getActivity(this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mediaSession = new MediaSession.Builder(this, player)
                .setBitmapLoader(new RosaryArtworkBitmapLoader(this))
                .setSessionActivity(contentPending)
                .build();

        loadState(() -> {
            if (currentState != null && currentState.isPlaying) {
                startPlaybackInternal();
            } else {
                updateMediaMetadata();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_NEXT: skipToNext(); break;
                case ACTION_PREVIOUS: skipToPrevious(); break;
                case ACTION_PLAY_PAUSE: handlePlayPause(); break;
                case ACTION_STOP: stopSelf(); break;
                case ACTION_SETTINGS_CHANGED:
                    loadState(() -> {
                        boolean wasPlaying = player.isPlaying();
                        updateMediaMetadata();
                        if (wasPlaying || (currentState != null && currentState.isPlaying)) {
                            player.prepare();
                            player.play();
                        }
                    });
                    break;
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
    public void onTaskRemoved(Intent rootIntent) {
        if (!player.isPlaying()) {
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        destroyed = true;
        // Persist isPlaying=false on a fresh read so the next launch doesn't resume
        // a session that is gone. Graceful shutdown lets this task drain.
        if (currentState != null) {
            executorService.execute(() -> {
                try {
                    RosaryState s = database.rosaryStateDao().getState();
                    if (s == null) s = RosaryDatabase.createDefaultState();
                    s.isPlaying = false;
                    database.rosaryStateDao().insertOrUpdate(s);
                } catch (Exception ignored) {
                    // Best effort — don't crash on destroy
                }
            });
        }
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

    // ─── State mutation (single source of truth: the DB) ──────────

    /**
     * Read the latest state from the DB, apply a mutation, and write it back,
     * all on the single-thread executor so service writes never interleave.
     * Runs {@code onDone} on the main thread afterwards (if the service is alive).
     * This replaces the old pattern of mutating a state object cached from
     * onCreate, which overwrote UI changes (timer, mode, target) with stale values.
     */
    private void mutateState(Consumer<RosaryState> mutation, @Nullable Runnable onDone) {
        executorService.execute(() -> {
            RosaryState state = database.rosaryStateDao().getState();
            if (state == null) {
                state = RosaryDatabase.createDefaultState();
            }
            state.checkAndResetDaily();
            currentState = state;
            mutation.accept(state);
            database.rosaryStateDao().insertOrUpdate(state);
            if (onDone != null && !destroyed) {
                new Handler(Looper.getMainLooper()).post(onDone);
            }
        });
    }

    // ─── Skip Logic ───────────────────────────────────────────────

    private void skipToNext() {
        if (currentState == null) return;

        boolean[] crossedBoundary = {false};
        boolean[] reachedTarget = {false};

        mutateState(state -> {
            int prevLocalIndex = state.getCurrentLocalIndex();
            state.advanceToNext();
            int newLocalIndex = state.getCurrentLocalIndex();

            // Detect set boundary: was at index 4, now at index 0 → set completed
            if (prevLocalIndex == 4 && newLocalIndex == 0
                    && state.todayCompletions >= state.targetRosaries) {
                reachedTarget[0] = true;
            } else if (prevLocalIndex == 4 && newLocalIndex == 0) {
                crossedBoundary[0] = true;
            }
        }, () -> {
            if (reachedTarget[0]) {
                vibrateDouble();
                vibrateTriple();
                Toast.makeText(getApplicationContext(), "Daily Rosary Target Reached!", Toast.LENGTH_SHORT).show();
                // Auto-stop after reaching daily target
                pausePlayback();
                return;
            }
            if (crossedBoundary[0]) {
                vibrateDouble();
            } else {
                vibrateSingle();
            }
            updateAndNotify();
        });
    }

    private void skipToPrevious() {
        if (currentState == null) return;
        mutateState(RosaryState::goToPrevious, () -> {
            vibrateSingle();
            updateAndNotify();
        });
    }

    private void updateAndNotify() {
        if (player == null) return;
        // A file that just ended (STATE_ENDED) still counts as "was playing":
        // playback must continue into the next mystery's file. A manual next while
        // paused (STATE_READY, not playing) must not start playback.
        boolean wasPlaying = player.isPlaying() || player.getPlaybackState() == Player.STATE_ENDED;
        updateMediaMetadata();
        if (wasPlaying) {
            player.play();
        }
    }

    // ─── Vibration ────────────────────────────────────────────────

    private void vibrateSingle() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(150);
            }
        }
    }

    private void vibrateDouble() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            long[] pattern = {0, 200, 100, 200};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        }
    }

    private void vibrateTriple() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            long[] pattern = {0, 150, 100, 150, 100, 400};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        }
    }

    // ─── Playback ─────────────────────────────────────────────────

    private void startPlaybackInternal() {
        if (currentState == null) return;
        mutateState(state -> state.isPlaying = true, () -> {
            // REPEAT_MODE_OFF so each mystery file ends with STATE_ENDED, which is
            // the trigger for advancing to the next mystery (both modes).
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
            updateMediaMetadata();
            player.prepare();
            player.play();
        });
    }

    private void pausePlayback() {
        if (currentState == null) return;
        // onIsPlayingChanged(false) persists the flag and stops the handlers.
        player.pause();
    }

    // ─── Media Metadata (Fix #3: avoid re-buffer) ─────────────────

    private String lastLoadedUri = null;

    private void updateMediaMetadata() {
        if (currentState == null) return;

        player.setVolume(currentState.audioEnabled ? 1f : 0f);

        // Local bundled WebP artwork, resolved by RosaryArtworkBitmapLoader.
        // The rosary:// scheme keeps the URI inside the app, no network needed.
        // The notification (Media3 default provider) shows title = mystery name and
        // content text = artist, so the artist carries today's progress.
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(currentState.getCurrentMysteryName())
                .setArtist(currentState.todayCompletions + " of " + currentState.targetRosaries + " rosaries today")
                .setAlbumTitle(currentState.getCurrentSet().getDisplayName() + " Mysteries")
                .setArtworkUri(Uri.parse("rosary://drawable/"
                        + currentState.getCurrentSet().getArtworkResId(currentState.getCurrentLocalIndex())))
                .build();

        // Bundled per-mystery audio (res/raw). ExoPlayer resolves
        // android.resource:// URIs natively. Audio mode plays one file per mystery;
        // silent mode reuses one track clipped to the timer.
        String uri = currentState.audioEnabled
                ? "android.resource://" + getPackageName() + "/"
                        + currentState.getCurrentSet().getAudioResId(currentState.getCurrentLocalIndex())
                : "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3";

        // Build the media item. Only silent mode gets a timer clip: audio mode plays
        // each mystery file to its natural end. (Setting the clip end to C.TIME_UNSET
        // crashes: setEndPositionMs multiplies ms->us and the overflowed value fails
        // Media3's precondition check.)
        MediaItem.Builder itemBuilder = new MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(metadata);
        if (!currentState.audioEnabled) {
            itemBuilder.setClippingConfiguration(
                new MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(currentState.mysteryTimerMinutes * 60000L)
                    .build()
            );
        }

        if (uri.equals(lastLoadedUri)) {
            // Same audio file — update metadata only, no re-buffer gap.
            player.replaceMediaItem(player.getCurrentMediaItemIndex(), itemBuilder.build());
            // After a clip/file ended, the player sits at STATE_ENDED; prepare the
            // replacement so the next play() starts the new item.
            if (player.getPlaybackState() == Player.STATE_ENDED) {
                player.prepare();
            }
        } else {
            // Different audio file — full replace, start from the beginning.
            lastLoadedUri = uri;
            player.setMediaItem(itemBuilder.build());
            player.prepare();
        }
    }

    // ─── Database ─────────────────────────────────────────────────

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
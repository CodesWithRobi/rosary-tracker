package com.example.rosarytracker.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;
import com.example.rosarytracker.MainActivity;
import com.example.rosarytracker.data.RosaryState;

/**
 * Manages the MediaStyle notification for lock screen and notification shade.
 */
public class RosaryNotificationManager {

    public static final String CHANNEL_ID = "rosary_playback_channel";
    private static final String CHANNEL_NAME = "Rosary Playback";
    static final int NOTIFICATION_ID = 1;

    private final Context context;
    private final NotificationManager notificationManager;

    public RosaryNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            channel.setSound(null, null); // Keep it silent
            notificationManager.createNotificationChannel(channel);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public Notification buildNotification(RosaryState state, MediaSession mediaSession) {
        Intent contentIntent = new Intent(context, MainActivity.class);
        PendingIntent contentPending = PendingIntent.getActivity(context, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(contentPending)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // On Android 14+, foreground services of type mediaPlayback MUST use MediaStyle.
        // We set basic title/text first, then try to apply MediaStyle if session is ready.
        String title = (state != null) ? state.getCurrentMysteryName() : "Rosary Tracker";
        builder.setContentTitle(title)
               .setContentText("Rosary Tracker");

        if (mediaSession != null) {
            // Action intents
            Intent prevIntent = new Intent(context, RosaryPlaybackService.class).setAction("ACTION_PREVIOUS");
            PendingIntent prevPending = PendingIntent.getService(context, 1, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent nextIntent = new Intent(context, RosaryPlaybackService.class).setAction("ACTION_NEXT");
            PendingIntent nextPending = PendingIntent.getService(context, 2, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent playPauseIntent = new Intent(context, RosaryPlaybackService.class).setAction("ACTION_PLAY_PAUSE");
            PendingIntent playPausePending = PendingIntent.getService(context, 3, playPauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Add actions
            builder.addAction(android.R.drawable.ic_media_previous, "Previous", prevPending);
            
            boolean isPlaying = (state != null && state.isPlaying);
            builder.addAction(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                    isPlaying ? "Pause" : "Play", playPausePending);
            
            builder.addAction(android.R.drawable.ic_media_next, "Next", nextPending);

            // Apply MediaStyle
            builder.setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0, 1, 2));
        }

        return builder.build();
    }

    public void showNotification(RosaryState state, MediaSession mediaSession) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state, mediaSession));
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}

package com.example.rosarytracker.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.media3.common.util.BitmapLoader;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSourceBitmapLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * BitmapLoader for the MediaSession that resolves the app's bundled WebP
 * artwork (rosary://drawable/&lt;resId&gt;) straight from resources, so the
 * notification and lock screen artwork renders instantly with no network.
 * Any other URI falls back to the default DataSource loader.
 */
@UnstableApi
public class RosaryArtworkBitmapLoader implements BitmapLoader {

    private static final String SCHEME_ROSARY = "rosary";

    private final Context context;
    private final DataSourceBitmapLoader fallback;

    public RosaryArtworkBitmapLoader(Context context) {
        this.context = context.getApplicationContext();
        this.fallback = new DataSourceBitmapLoader(this.context);
    }

    @Override
    public boolean supportsMimeType(String mimeType) {
        return fallback.supportsMimeType(mimeType);
    }

    @Override
    public ListenableFuture<Bitmap> decodeBitmap(byte[] data) {
        return fallback.decodeBitmap(data);
    }

    @Override
    public ListenableFuture<Bitmap> loadBitmap(Uri uri) {
        if (SCHEME_ROSARY.equals(uri.getScheme())) {
            String resIdString = uri.getLastPathSegment();
            try {
                int resId = Integer.parseInt(resIdString);
                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
                if (bitmap != null) {
                    return Futures.immediateFuture(bitmap);
                }
                return Futures.immediateFailedFuture(
                        new IllegalStateException("Failed to decode drawable resource " + resId));
            } catch (NumberFormatException e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return fallback.loadBitmap(uri);
    }
}
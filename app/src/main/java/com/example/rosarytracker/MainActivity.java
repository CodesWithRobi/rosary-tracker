package com.example.rosarytracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.rosarytracker.data.MysterySet;
import com.example.rosarytracker.data.RosaryState;
import com.example.rosarytracker.service.RosaryPlaybackService;
import com.example.rosarytracker.viewmodel.RosaryViewModel;
import com.google.android.material.slider.Slider;
import java.util.Calendar;
import java.util.Locale;

/**
 * Main activity for the Rosary Tracker.
 */
@UnstableApi
public class MainActivity extends AppCompatActivity {

    private RosaryViewModel viewModel;

    private TextView textViewTodayHeader;
    private ImageView imageViewArtwork;
    private TextView textViewSetLabel;
    private TextView textViewMysteryName;
    private TextView textViewMysteryNumber;
    private TextView textViewCountdown;
    private TextView textViewCompletion;
    private TextView textViewSliderValue;
    private TextView textViewTimerValue;
    private ImageButton buttonPrevious;
    private ImageButton buttonPlayPause;
    private ImageButton buttonNext;
    private ImageButton buttonReset;
    private Slider sliderRosaryCount;
    private Slider sliderMysteryTimer;
    private Slider audioProgressBar;
    private LinearLayout timerContainer;
    private ConstraintLayout contentRoot;
    private ToggleButton toggleMode;
    private ToggleButton toggleAudio;
    private final View[] beadViews = new View[5];

    private boolean isUserSeeking = false;
    private boolean audioEnabledFromState = false;
    private int currentDurationMs = 0;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handled automatically by system
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge: draw behind the status and navigation bars (required on
        // Android 15+ where this is enforced). Content is kept inside the system
        // bars via the insets listener below, so nothing is clipped or overlapped.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // Make the navigation bar fully transparent on all API levels. On 29+
        // the theme handles this, but API 24-28 needs the programmatic call.
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_main);

        // Pad the root by the system bar insets: the status bar at the top, the
        // navigation bar at the bottom. This pushes the scroll content below the
        // status bar and keeps the toggles just above the gesture pill. On
        // API 24-29 insets are zero (decor still fits), so this is a no-op there.
        View rootLayout = findViewById(R.id.rootLayout);
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        checkNotificationPermission();

        textViewTodayHeader = findViewById(R.id.textViewTodayHeader);
        contentRoot = findViewById(R.id.contentRoot);
        imageViewArtwork = findViewById(R.id.imageViewArtwork);
        textViewSetLabel = findViewById(R.id.textViewSetLabel);
        textViewMysteryName = findViewById(R.id.textViewMysteryName);
        textViewMysteryNumber = findViewById(R.id.textViewMysteryNumber);
        textViewCountdown = findViewById(R.id.textViewCountdown);
        textViewCompletion = findViewById(R.id.textViewCompletion);
        textViewSliderValue = findViewById(R.id.textViewSliderValue);
        textViewTimerValue = findViewById(R.id.textViewTimerValue);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonNext = findViewById(R.id.buttonNext);
        buttonReset = findViewById(R.id.buttonReset);
        sliderRosaryCount = findViewById(R.id.sliderRosaryCount);
        sliderMysteryTimer = findViewById(R.id.sliderMysteryTimer);
        timerContainer = findViewById(R.id.timerContainer);
        audioProgressBar = findViewById(R.id.audioProgressBar);
        toggleMode = findViewById(R.id.toggleMode);
        toggleAudio = findViewById(R.id.toggleAudio);
        beadViews[0] = findViewById(R.id.bead0);
        beadViews[1] = findViewById(R.id.bead1);
        beadViews[2] = findViewById(R.id.bead2);
        beadViews[3] = findViewById(R.id.bead3);
        beadViews[4] = findViewById(R.id.bead4);

        viewModel = new ViewModelProvider(this).get(RosaryViewModel.class);

        buttonPlayPause.setOnClickListener(v -> onPlayPauseClicked());
        buttonNext.setOnClickListener(v -> onNextClicked());
        buttonPrevious.setOnClickListener(v -> onPreviousClicked());
        buttonReset.setOnClickListener(v -> onResetClicked());
        
        textViewMysteryName.setOnClickListener(v -> showReflection());
        textViewMysteryName.setOnLongClickListener(v -> {
            showMysterySelector();
            return true;
        });

        audioProgressBar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                long remaining = (long) (slider.getValueTo() - value);
                textViewCountdown.setText(formatTime(remaining));
            }
        });
        audioProgressBar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) { isUserSeeking = true; }
            @Override
            public void onStopTrackingTouch(Slider slider) {
                viewModel.seekTo((int) slider.getValue());
                isUserSeeking = false;
            }
        });

        sliderRosaryCount.addOnChangeListener((slider, value, fromUser) -> {
            int v = Math.round(value);
            textViewSliderValue.setText(v + "");
            if (fromUser && v > 0) viewModel.setTargetRosaries(v);
        });

        sliderMysteryTimer.addOnChangeListener((slider, value, fromUser) -> {
            int v = Math.round(value);
            textViewTimerValue.setText(v + " min");
            if (fromUser && v > 0) viewModel.setMysteryTimerMinutes(v);
        });

        toggleMode.setOnClickListener(v -> viewModel.toggleMode());
        toggleAudio.setOnClickListener(v -> viewModel.toggleAudio());

        viewModel.getState().observe(this, state -> updateUI());
        viewModel.isPlaying().observe(this, isPlaying -> {
            buttonPlayPause.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            // Playback-driven views must follow the LIVE controller state, not the
            // DB snapshot. The DB field only changes after a reload, which is why
            // pressing play in-app never showed the progress bar before.
            textViewCountdown.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
            audioProgressBar.setVisibility((isPlaying || audioEnabledFromState) ? View.VISIBLE : View.GONE);
        });

        viewModel.getCountdown().observe(this, countdown -> {
            if (!isUserSeeking) textViewCountdown.setText(countdown);
        });

        viewModel.getCurrentPosition().observe(this, position -> {
            if (!isUserSeeking && currentDurationMs > 0) {
                audioProgressBar.setValue(Math.min(position.intValue(), currentDurationMs));
            }
        });

        viewModel.getDuration().observe(this, duration -> {
            if (duration > 0) {
                currentDurationMs = duration.intValue();
                audioProgressBar.setValueTo(currentDurationMs);
            }
        });

        viewModel.loadState();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // On API 30+ the bars are transparent over the warm background, so the
            // status icons and gesture pill must be dark to stay visible. On API 24-29
            // the theme's deep-blue status bar is used and icons stay light.
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
            // Disable the system contrast scrim behind the nav bar (API 29+).
            // Without this, Android adds a translucent overlay behind the gesture pill.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getWindow().setNavigationBarContrastEnforced(false);
            }
        }
    }

    private void onPlayPauseClicked() {
        if (Boolean.TRUE.equals(viewModel.isPlaying().getValue())) {
            viewModel.stopPlayback();
        } else {
            viewModel.startPlayback();
        }
    }

    private void onNextClicked() {
        Intent intent = new Intent(this, RosaryPlaybackService.class);
        intent.setAction(RosaryPlaybackService.ACTION_NEXT);
        startService(intent);
        // Reload state after service updates DB
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> viewModel.loadState(), 300);
    }

    private void onPreviousClicked() {
        Intent intent = new Intent(this, RosaryPlaybackService.class);
        intent.setAction(RosaryPlaybackService.ACTION_PREVIOUS);
        startService(intent);
        // Reload state after service updates DB
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> viewModel.loadState(), 300);
    }

    private void onResetClicked() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Progress")
                .setMessage("Clear today's completed rosaries?")
                .setPositiveButton("Reset", (dialog, which) -> viewModel.resetProgress())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReflection() {
        RosaryState state = viewModel.getState().getValue();
        if (state != null) {
            MysterySet set = state.getCurrentSet();
            int localIndex = state.getCurrentLocalIndex();
            new AlertDialog.Builder(this)
                    .setTitle(set.getMystery(localIndex))
                    .setMessage(set.getDescription(localIndex))
                    .setPositiveButton("Close", null)
                    .show();
        }
    }

    private void showMysterySelector() {
        String[] allMysteries = new String[20];
        for (int i = 0; i < 20; i++) {
            MysterySet set = MysterySet.fromGlobalIndex(i);
            allMysteries[i] = (i + 1) + ". " + set.getMystery(MysterySet.toLocalIndex(i));
        }

        new AlertDialog.Builder(this)
                .setTitle("Jump to Mystery")
                .setItems(allMysteries, (dialog, which) -> {
                    RosaryState state = viewModel.getState().getValue();
                    if (state != null) {
                        state.currentMysteryIndex = which;
                        viewModel.saveState(state);
                        Intent intent = new Intent(this, RosaryPlaybackService.class);
                        intent.setAction(RosaryPlaybackService.ACTION_SETTINGS_CHANGED);
                        startService(intent);
                    }
                })
                .show();
    }

    private void updateUI() {
        RosaryState state = viewModel.getState().getValue();
        if (state == null) return;

        Calendar cal = Calendar.getInstance();
        String dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        textViewTodayHeader.setText(String.format("%s: The %s Mysteries", dayName, MysterySet.getLiturgicalSet().getDisplayName()));

        textViewSetLabel.setText(state.getCurrentSet().getDisplayName() + " Mysteries");
        textViewMysteryName.setText(state.getCurrentMysteryName());
        textViewMysteryNumber.setText((state.currentMysteryIndex + 1) + " of 20");
        textViewCompletion.setText(viewModel.getCompletionDisplay());

        Glide.with(this)
                .load(state.getCurrentSet().getArtworkResId(state.getCurrentLocalIndex()))
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.placeholder_artwork)
                .error(R.drawable.placeholder_artwork)
                .into(imageViewArtwork);

        textViewCountdown.setVisibility(state.isPlaying ? View.VISIBLE : View.GONE);
        audioProgressBar.setVisibility(state.audioEnabled || state.isPlaying ? View.VISIBLE : View.GONE);
        audioEnabledFromState = state.audioEnabled;

        sliderRosaryCount.setValue(Math.max(1, Math.min(4, state.targetRosaries)));
        textViewSliderValue.setText(state.targetRosaries + "");

        // The per-mystery timer only applies to silent (tracker) mode; in audio mode
        // each file plays to its end, so hide the whole section and let the artwork
        // absorb the freed space.
        boolean showTimer = !state.audioEnabled;
        timerContainer.setVisibility(showTimer ? View.VISIBLE : View.GONE);
        sliderMysteryTimer.setVisibility(showTimer ? View.VISIBLE : View.GONE);

        // The scroll area's vertical chain must end at whichever slider is the last
        // visible one, so the artwork can absorb the slack and the content fills
        // the screen. GONE views don't anchor chains, so the tail has to move.
        ConstraintSet constraints = new ConstraintSet();
        constraints.clone(contentRoot);
        if (showTimer) {
            constraints.clear(R.id.sliderRosaryCount, ConstraintSet.BOTTOM);
            constraints.connect(R.id.sliderMysteryTimer, ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        } else {
            constraints.connect(R.id.sliderRosaryCount, ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
            constraints.clear(R.id.sliderMysteryTimer, ConstraintSet.BOTTOM);
        }
        constraints.applyTo(contentRoot);

        sliderMysteryTimer.setValue(Math.max(1, Math.min(10, state.mysteryTimerMinutes)));
        textViewTimerValue.setText(state.mysteryTimerMinutes + " min");
        toggleMode.setChecked("LITURGICAL".equals(state.mode));
        toggleAudio.setChecked(state.audioEnabled);

        updateBeads(state.getCurrentLocalIndex());
    }

    private void updateBeads(int localIndex) {
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 5; i++) {
            View bead = beadViews[i];
            if (i < localIndex) {
                bead.setBackgroundResource(R.drawable.bead_done);
            } else if (i == localIndex) {
                bead.setBackgroundResource(R.drawable.bead_current);
            } else {
                bead.setBackgroundResource(R.drawable.bead_empty);
            }
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bead.getLayoutParams();
            int size = Math.round((i == localIndex ? 16 : 12) * density);
            lp.width = size;
            lp.height = size;
            bead.setLayoutParams(lp);
        }
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        return String.format(java.util.Locale.US, "%02d:%02d", mins, secs);
    }
}

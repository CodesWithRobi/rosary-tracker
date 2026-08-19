package com.example.rosarytracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.rosarytracker.data.RosaryState;
import com.example.rosarytracker.service.RosaryPlaybackService;
import com.example.rosarytracker.viewmodel.RosaryViewModel;

/**
 * Main activity for the Rosary Tracker.
 * Displays current mystery, playback controls, and settings.
 */
public class MainActivity extends AppCompatActivity {

    private RosaryViewModel viewModel;

    private TextView textViewSetLabel;
    private TextView textViewMysteryName;
    private TextView textViewMysteryNumber;
    private TextView textViewCompletion;
    private ImageButton buttonPrevious;
    private ImageButton buttonPlayPause;
    private ImageButton buttonNext;
    private SeekBar sliderRosaryCount;
    private TextView textViewSliderLabel;
    private ToggleButton toggleMode;
    private ToggleButton toggleAudio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find all views
        textViewSetLabel = findViewById(R.id.textViewSetLabel);
        textViewMysteryName = findViewById(R.id.textViewMysteryName);
        textViewMysteryNumber = findViewById(R.id.textViewMysteryNumber);
        textViewCompletion = findViewById(R.id.textViewCompletion);
        buttonPrevious = findViewById(R.id.buttonPrevious);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonNext = findViewById(R.id.buttonNext);
        sliderRosaryCount = findViewById(R.id.sliderRosaryCount);
        textViewSliderLabel = findViewById(R.id.textViewSliderLabel);
        toggleMode = findViewById(R.id.toggleMode);
        toggleAudio = findViewById(R.id.toggleAudio);

        // Get ViewModel
        viewModel = new ViewModelProvider(this).get(RosaryViewModel.class);

        // Set click listeners
        buttonPlayPause.setOnClickListener(v -> onPlayPauseClicked());
        buttonNext.setOnClickListener(v -> onNextClicked());
        buttonPrevious.setOnClickListener(v -> onPreviousClicked());

        // Set slider listener
        sliderRosaryCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    onRosaryCountChanged(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set toggle listeners
        toggleMode.setOnClickListener(v -> onModeToggled());
        toggleAudio.setOnClickListener(v -> onAudioToggled());

        // Observe state changes
        viewModel.getState().observe(this, state -> updateUI());

        // Observe playback state changes
        viewModel.isPlaying().observe(this, isPlaying -> {
            buttonPlayPause.setImageResource(
                    isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        });

        // Load state from database
        viewModel.loadState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Handles play/pause button click — starts or stops the fake music player service.
     */
    private void onPlayPauseClicked() {
        if (Boolean.TRUE.equals(viewModel.isPlaying().getValue())) {
            viewModel.stopPlayback();
        } else {
            viewModel.startPlayback();
        }
    }

    /**
     * Handles next mystery button click — sends intent to service to advance.
     */
    private void onNextClicked() {
        Intent intent = new Intent(this, RosaryPlaybackService.class);
        intent.setAction("ACTION_NEXT");
        startService(intent);
    }

    /**
     * Handles previous mystery button click — sends intent to service to go back.
     */
    private void onPreviousClicked() {
        Intent intent = new Intent(this, RosaryPlaybackService.class);
        intent.setAction("ACTION_PREVIOUS");
        startService(intent);
    }

    /**
     * Handles rosary count slider change (1-4).
     */
    private void onRosaryCountChanged(int count) {
        viewModel.setTargetRosaries(count);
    }

    /**
     * Handles mode toggle (Liturgical <-> Custom).
     */
    private void onModeToggled() {
        viewModel.toggleMode();
    }

    /**
     * Handles audio toggle (on/off).
     */
    private void onAudioToggled() {
        viewModel.toggleAudio();
    }

    /**
     * Updates the UI to reflect current state from ViewModel.
     */
    private void updateUI() {
        RosaryState state = viewModel.getState().getValue();
        if (state == null) return;

        textViewSetLabel.setText(state.getCurrentSet().getDisplayName() + " Mysteries");
        textViewMysteryName.setText(state.getCurrentMysteryName());
        textViewMysteryNumber.setText((state.currentMysteryIndex + 1) + " of 20");
        textViewCompletion.setText(viewModel.getCompletionDisplay());

        // Avoid triggering listener when programmatically setting progress
        sliderRosaryCount.setOnSeekBarChangeListener(null);
        sliderRosaryCount.setProgress(state.targetRosaries);
        sliderRosaryCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) onRosaryCountChanged(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        toggleMode.setChecked("CUSTOM".equals(state.mode));
        toggleAudio.setChecked(state.audioEnabled);
    }
}

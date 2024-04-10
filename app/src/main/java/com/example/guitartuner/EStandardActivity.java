package com.example.guitartuner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.nio.ByteOrder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class EStandardActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView pitchTextView, pitchTextView1;

    private AudioRecord audioRecord;
    private TarsosDSPAudioFormat audioFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_estandard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        pitchTextView = findViewById(R.id.pitchTextView);
        pitchTextView1 = findViewById(R.id.pitchTextView1);

        RadioGroup tuningRadioGroup = findViewById(R.id.tuningRadioGroup);
        tuningRadioGroup.setOnCheckedChangeListener((group, checkedId) -> handleSelectedTuningOption(checkedId));

        if (checkPermissions()) {
            setupAudioRecorder();
            startPitchDetection();
        }
    }
    public void onButtonClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    private boolean checkPermissions() {
        int recordPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (recordPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void setupAudioRecorder() {
        int sampleRate = 44100;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioFormat = new TarsosDSPAudioFormat(
                TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                2 * 8,
                1,
                2,
                sampleRate,
                ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder())
        );

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    private void startPitchDetection() {
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
            AudioDispatcher audioDispatcher = AudioDispatcherFactory.fromDefaultMicrophone((int) audioFormat.getSampleRate(), 2048, 0);

            PitchDetectionHandler pitchDetectionHandler = (res, e) -> handlePitch(res);
            AudioProcessor pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, audioFormat.getSampleRate(), 2048, pitchDetectionHandler);
            audioDispatcher.addAudioProcessor(pitchProcessor);
            Thread audioThread = new Thread(audioDispatcher, "Audio Thread");
            audioThread.start();
        }
    }
    @SuppressLint("SetTextI18n")
    private void handlePitch(PitchDetectionResult res) {
        final float pitchInHz = res.getPitch();
        runOnUiThread(() -> pitchTextView.setText("Pitch: " + pitchInHz));

        RadioButton[] radioButtons = {
                findViewById(R.id.radiobutton1),
                findViewById(R.id.radiobutton2),
                findViewById(R.id.radiobutton3),
                findViewById(R.id.radiobutton4),
                findViewById(R.id.radiobutton5),
                findViewById(R.id.radiobutton6)
        };

        for (int i = 0; i < radioButtons.length; i++) {
            RadioButton radioButton = radioButtons[i];
            if (radioButton.isChecked()) {
                handleRadioButton(i, pitchInHz);
                break;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void handleRadioButton(int index, float pitchInHz) {
        int[] lowerBounds = {81, 109, 145, 195, 245, 329};
        int[] upperBounds = {83, 111, 147, 197, 247, 331};
        String[] soundNames = {"E6", "A", "D", "G", "B", "E1"};

        if (isInRange(pitchInHz, lowerBounds[index], upperBounds[index])) {
            runOnUiThread(() -> pitchTextView1.setText("Sound: " + soundNames[index]));
        } else if (pitchInHz <= lowerBounds[index]) {
            runOnUiThread(() -> pitchTextView1.setText("Sound: Higher"));
        } else {
            runOnUiThread(() -> pitchTextView1.setText("Sound: Lower"));
        }
    }
    @SuppressLint("NonConstantResourceId")
    private void handleSelectedTuningOption(int checkedId) {
        if (checkedId == R.id.radiobutton1) {
        } else if (checkedId == R.id.radiobutton2) {
        } else if (checkedId == R.id.radiobutton3) {
        } else if (checkedId == R.id.radiobutton4) {
        } else if (checkedId == R.id.radiobutton5) {
        } else if (checkedId == R.id.radiobutton6) {
        }
    }

    private boolean isInRange(float value, int lowerBound, int upperBound) {
        return value > lowerBound && value < upperBound;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord != null) {
            audioRecord.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAudioRecorder();
                startPitchDetection();
            } else {
                Log.e("Permission", "Record audio permission denied");
            }
        }
    }
}

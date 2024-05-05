
package com.example.guitartuner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.nio.ByteOrder;
import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class DADGADActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private TextView pitchTextView, pitchTextView1, pitchTextView2;
    private AudioRecord audioRecord;
    private TarsosDSPAudioFormat audioFormat;
    private LineChart mChart;
    private ArrayList<Entry> entries;
    private Runnable chartUpdater;
    private Handler mHandler;
    private int dataCounter;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dadgadactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        pitchTextView = findViewById(R.id.pitchTextView);
        pitchTextView1 = findViewById(R.id.pitchTextView1);
        pitchTextView2 = findViewById(R.id.pitchTextView2);

        RadioGroup tuningRadioGroup = findViewById(R.id.tuningRadioGroup);
        tuningRadioGroup.setOnCheckedChangeListener((group, checkedId) -> handleSelectedTuningOption(checkedId));

        mChart = findViewById(R.id.chart);
        setupChart();

        entries = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
        dataCounter = 0;
        chartUpdater = new Runnable() {
            @Override
            public void run() {
                if (!entries.isEmpty()) {
                    entries.remove(0);
                    for (int i = 0; i < entries.size(); i++) {
                        entries.get(i).setX(i);
                    }
                    LineDataSet dataSet = new LineDataSet(entries, "Frequency");
                    dataSet.setColor(Color.GREEN);
                    dataSet.setDrawCircles(false);
                    dataSet.setDrawValues(false);
                    LineData lineData = new LineData(dataSet);
                    mChart.setData(lineData);
                    mChart.moveViewToX(dataCounter);
                    mChart.setVisibleXRangeMaximum(100);
                    mChart.invalidate();
                }
                mHandler.postDelayed(this, 50);
            }
        };
        mHandler.postDelayed(chartUpdater, 50);

        if (checkPermissions()) {
            setupAudioRecorder();
            startPitchDetection();
        }
    }
    public void onButtonClick(View view) {
        finish();
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
    private void setupChart() {
        mChart.getDescription().setEnabled(false);
        mChart.setDrawGridBackground(false);
        mChart.getAxisLeft().setDrawGridLines(false);
        mChart.getAxisLeft().setAxisMinimum(50);
        mChart.getAxisLeft().setAxisMaximum(400);
        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setDrawAxisLine(true);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.getLegend().setEnabled(false);
        mChart.getXAxis().setDrawLabels(false);
        mChart.getAxisLeft().setDrawLabels(false);
        mChart.setTouchEnabled(false);
        mChart.setExtraOffsets(60, 90, 1, 450);
    }
    private void addEntry(float pitchInHz) {
        entries.add(new Entry(dataCounter++, pitchInHz));
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
        runOnUiThread(() -> {
            pitchTextView.setText("Pitch: " + pitchInHz);
            addEntry(pitchInHz);
        });
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
        int[] lowerBounds = {72, 109, 145, 195, 219, 292};
        int[] upperBounds = {74, 111, 147, 197, 221, 294};

        if (isInRange(pitchInHz, lowerBounds[index], upperBounds[index])) {
            runOnUiThread(() -> pitchTextView1.setText("Sound: Correct"));
        } else if (pitchInHz <= lowerBounds[index]) {
            runOnUiThread(() -> pitchTextView1.setText("Sound: Higher"));
        } else {
            runOnUiThread(() -> pitchTextView1.setText("Sound: Lower"));
        }
    }
    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    private void handleSelectedTuningOption(int checkedId) {
        String[] soundNames = {"D6", "A5", "D4", "G3", "A2", "D1"};
        float[] frequencies = {73, 110, 146, 196, 220, 293};

        mChart.getAxisLeft().removeAllLimitLines();

        int index = -1;
        if (checkedId == R.id.radiobutton1) {
            index = 0;
        } else if (checkedId == R.id.radiobutton2) {
            index = 1;
        } else if (checkedId == R.id.radiobutton3) {
            index = 2;
        } else if (checkedId == R.id.radiobutton4) {
            index = 3;
        } else if (checkedId == R.id.radiobutton5) {
            index = 4;
        } else if (checkedId == R.id.radiobutton6) {
            index = 5;
        }
        if (index != -1) {
            pitchTextView2.setText(soundNames[index]);
            LimitLine[] existingLimitLines = mChart.getAxisLeft().getLimitLines().toArray(new LimitLine[0]);
            boolean limitLineExists = false;
            for (LimitLine limitLine : existingLimitLines) {
                if (limitLine.getLabel().equals(String.valueOf((int) frequencies[index]))) {
                    limitLineExists = true;
                    break;
                }
            }
            if (!limitLineExists) {
                LimitLine limitLine = new LimitLine(frequencies[index], String.valueOf((int) frequencies[index]));
                limitLine.setLineColor(Color.RED);
                limitLine.setLineWidth(1f);
                limitLine.setTextColor(Color.WHITE);
                limitLine.setTextSize(12f);
                mChart.getAxisLeft().addLimitLine(limitLine);
            }
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
        mHandler.removeCallbacks(chartUpdater);
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


package com.example.guitartuner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @SuppressLint("NonConstantResourceId")
    public void onButtonClick(View view) {
        Class<?>[] activityClasses = {
                EStandardActivity.class,
                DDropActivity.class,
                DADGADActivity.class,
                OpenEActivity.class,
                BassTuningActivity.class,
                UkuleleTuningActivity.class
        };
        int[] buttonIds = {
                R.id.button1,
                R.id.button2,
                R.id.button3,
                R.id.button4,
                R.id.button5,
                R.id.button6
        };
        int viewId = view.getId();
        for (int i = 0; i < buttonIds.length; i++) {
            if (viewId == buttonIds[i]) {
                startActivity(new Intent(this, activityClasses[i]));
                return;
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
    }
}

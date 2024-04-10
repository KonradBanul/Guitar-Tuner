
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
        Intent intent;
        int viewId = view.getId();

        if (viewId == R.id.button1) {
            intent = new Intent(this, EStandardActivity.class);
        } else if (viewId == R.id.button2) {
            intent = new Intent(this, DDropActivity.class);
        } else if (viewId == R.id.button3) {
            intent = new Intent(this, DADGADActivity.class);
        } else if (viewId == R.id.button4) {
            intent = new Intent(this, OpenEActivity.class);
        } else if (viewId == R.id.button5) {
            intent = new Intent(this, BassTuningActivity.class);
        } else if (viewId == R.id.button6) {
            intent = new Intent(this, UkuleleTuningActivity.class);
        } else {
            return;
        }
        startActivity(intent);
    }
}

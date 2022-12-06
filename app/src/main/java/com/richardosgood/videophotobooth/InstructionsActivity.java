package com.richardosgood.videophotobooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class InstructionsActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private TextView tvInstructions;
    private String personName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        tvInstructions = findViewById(R.id.tvInstructions);
        personName = getIntent().getStringExtra("personName");

        loadPreferences();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    // Start recorder activity
    public void onButtonPress(View view) {
        Intent intent = new Intent(this, VideoRecorder.class);
        intent.putExtra("personName", personName);
        startActivity(intent);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("enterNameText")){
            tvInstructions.setText(sharedPreferences.getString(key, null));
        }
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String text = sharedPreferences.getString("recordInstructionsText", null);
        tvInstructions.setText(text);
    }
}
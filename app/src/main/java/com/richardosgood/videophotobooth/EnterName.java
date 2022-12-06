package com.richardosgood.videophotobooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EnterName extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private EditText etName;
    private TextView tvEnterNameInstructions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_name);

        etName = findViewById(R.id.etPersonName);
        tvEnterNameInstructions = findViewById(R.id.tvEnterNameInstructions);

        loadPreferences();
    }

    // Called when user submits their name in the activity
    public void submitName(View view) {
        String name = etName.getText().toString();

        // If user doesn't enter a name
        if (name.equals("")){
            returnToRecorder(name);
            return;
        }

        // Make sure the entered name only has alphanumeric characters and spaces
        if (name.matches("[A-Za-z0-9 ]+")) {
            returnToRecorder(name);
        } else {
            Toast.makeText(this, getString(R.string.name_alpha_numeric), Toast.LENGTH_SHORT).show();
        }
    }

    public void skipName(View view) {
        // Return with no name
        returnToRecorder("");
    }

    private void returnToRecorder(String name) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean instructionsEnabled = sharedPreferences.getBoolean("swEnableInstructions", true);

        Intent intent;

        if (instructionsEnabled) {
            intent = new Intent(this, InstructionsActivity.class);
        } else {
            intent = new Intent(this, VideoRecorder.class);
        }

        intent.putExtra("personName", name);
        startActivity(intent);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("enterNameText")){
            tvEnterNameInstructions.setText(sharedPreferences.getString(key, null));
        }
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String text = sharedPreferences.getString("enterNameText", null);
        tvEnterNameInstructions.setText(text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
}
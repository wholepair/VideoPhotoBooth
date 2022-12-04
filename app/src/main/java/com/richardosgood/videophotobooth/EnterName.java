package com.richardosgood.videophotobooth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class EnterName extends AppCompatActivity {
    private EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_name);

        etName = findViewById(R.id.etPersonName);
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
        Intent intent = new Intent(this, VideoRecorder.class);
        intent.putExtra("personName", name);
        startActivity(intent);
        setResult(RESULT_OK, intent);
        finish();
    }

}
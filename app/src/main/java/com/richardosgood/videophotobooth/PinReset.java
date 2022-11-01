package com.richardosgood.videophotobooth;

import static com.richardosgood.videophotobooth.MainActivity.savePin;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PinReset extends Activity {
    EditText etPin1;
    EditText etPin2;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pin_reset);
        etPin1 = findViewById(R.id.editTextPin1);
        etPin2 = findViewById(R.id.editTextPin2);


    }

    public void resetPin(View view) {
        String pin1 = etPin1.getText().toString();
        String pin2 = etPin2.getText().toString();

        if (pin2.equals(pin1)) {
            if (savePin(pin1)) {
                Toast.makeText(PinReset.this, getString(R.string.pin_updated), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(PinReset.this, getString(R.string.error_pin_validate), Toast.LENGTH_SHORT).show();
            }
            finish();
        } else {
            Toast.makeText(PinReset.this, getString(R.string.pin_nomatch), Toast.LENGTH_SHORT).show();
            pin1 = null;
            pin2 = null;
            etPin1.setText("");
            etPin2.setText("");
        }
    }
}

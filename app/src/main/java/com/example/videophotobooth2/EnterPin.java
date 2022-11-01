package com.example.videophotobooth2;

import static com.example.videophotobooth2.MainActivity.checkPin;
import static com.example.videophotobooth2.MainActivity.savePin;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class EnterPin extends Activity {
    private static final String TAG = "VideoPhotoBooth";
    private String mode;
    private EditText editTextPin;
    private String pin1 = null;
    private String pin2 = null;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mode = extras.getString("mode");
        } else {
            finish();
        }

        editTextPin = (EditText) findViewById(R.id.editTextPin);
        editTextPin.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    if (mode.equals("setPin")) {
                        try {
                            setPin();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(EnterPin.this, "Error! Unable to set PIN!", Toast.LENGTH_SHORT).show();
                        }
                    } else if (mode.equals("checkPin")) {
                        String enteredPin = editTextPin.getText().toString();
                        try {
                            checkPin(enteredPin);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(EnterPin.this, "Error! Unable to validate PIN!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void setPin() throws Exception {
        // pin1 was already set, so we must check that pin2 matches
        if (pin1 != null) {
            pin2 = editTextPin.getText().toString();

            if (pin2.equals(pin1)) {
                savePin(pin1);
            } else {
                Toast.makeText(EnterPin.this, "PINs do not match!", Toast.LENGTH_SHORT).show();
                pin1 = null;
                pin2 = null;
            }
        } else {
            pin1 = editTextPin.getText().toString();
            Toast.makeText(EnterPin.this, "Re-enter pin.", Toast.LENGTH_SHORT).show();
            editTextPin.setText("");
        }
    }
}

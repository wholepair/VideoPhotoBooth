package com.richardosgood.videophotobooth2;

import static com.richardosgood.videophotobooth2.MainActivity.checkPin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PinCheck extends Activity {
    private EditText editTextPin;
    private String pin1 = null;
    private String pin2 = null;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_unlock);

        Bundle extras = getIntent().getExtras();

        editTextPin = (EditText) findViewById(R.id.editTextPin);
        editTextPin.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    // Perform action on key press
                    String enteredPin = editTextPin.getText().toString();
                    boolean result = false;

                    try {
                        result = checkPin(enteredPin);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(PinCheck.this, getString(R.string.error_pin_validate), Toast.LENGTH_SHORT).show();
                        returnFalse();
                        finish();
                    }

                    if (result == false) {
                        Toast.makeText(PinCheck.this, getString(R.string.pin_incorrect), Toast.LENGTH_SHORT).show();
                        editTextPin.setText("");
                    } else {
                        returnTrue();
                        finish();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void returnTrue() {
        Intent intent = new Intent();
        intent.putExtra("result", true);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void returnFalse() {
        Intent intent = new Intent();
        intent.putExtra("result", false);
        setResult(RESULT_OK, intent);
        finish();
    }
}

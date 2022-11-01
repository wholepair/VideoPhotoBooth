package com.example.videophotobooth2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String KEY_ALIAS = "VideoBoothKey";
    private KeyStore keyStore;
    private static SharedPreferences prefs;
    public static DataEncryptor dataEncryptor;

    private static final int CAMERA_REQUEST = 1888;
    ImageView imageView;
    MainActivity ThisActivity = this;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataEncryptor = new DataEncryptor();

        // Try to open the keystore for storing PIN
        if (!dataEncryptor.initialize()) {
            Toast.makeText(MainActivity.this, "Error! Unable to initialize encryption", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Try to open preferences to store encrypted PIN
        try {
            prefs = getSharedPreferences("VideoBooth", MODE_PRIVATE);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Error! Unable to load preferences!!", Toast.LENGTH_SHORT).show();
            finish();
        }

        String pin = prefs.getString("PIN", null);
        if (pin == null) {
            // No PIN is set, so require a PIN to be set
            setPin();
        }

    }

    private void setPin() {
        Intent pinIntent = new Intent(this, EnterPin.class);
        pinIntent.putExtra("mode", "setPin");
        startActivity(pinIntent);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(photo);
        }
    }

    public void screenTapped(View view) {
        Intent cameraIntent = new Intent(view.getContext(), VideoRecorder.class);
        startActivity(cameraIntent);
    }

    public static void savePin(String pin) throws Exception {
        String encryptedPin = dataEncryptor.encryptData(pin.getBytes());
        prefs.edit().putString("PIN", encryptedPin);
    }

    public static boolean checkPin(String enteredPin) throws Exception {
        String storedPin_encrypted = prefs.getString("PIN", null);
        String storedPin = dataEncryptor.decryptData(storedPin_encrypted);

        if (storedPin.equals(enteredPin)) {
            return true;
        }

        return false;
    }
}

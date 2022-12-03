package com.richardosgood.videophotobooth;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity {
    public static final String TAG = "VideoPhotoBooth";
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String KEY_ALIAS = "VideoBoothKey";
    private KeyStore keyStore;
    private static SharedPreferences prefs;
    public static DataEncryptor dataEncryptor;
    private static String iv = null;

    private static final int CAMERA_REQUEST = 1888;
    ImageView imageView;
    MainActivity ThisActivity = this;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        dataEncryptor = new DataEncryptor();

        // Try to open the keystore for storing PIN
        if (!dataEncryptor.initialize()) {
            Toast.makeText(MainActivity.this, getString(R.string.error_enc_init), Toast.LENGTH_SHORT).show();
            finish();
        }

        // Try to open preferences to store encrypted PIN
        try {
            prefs = getSharedPreferences("VideoBooth", MODE_PRIVATE);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, getString(R.string.error_enc_pref), Toast.LENGTH_SHORT).show();
            finish();
        }

        // retrieve the random IV from app preferences. It's used along with a key to encrypt/decrypt the video export PIN
        iv = prefs.getString("IV", null);
        if (iv == null) {
            // IV doesn't exist, so randomly generate one
            iv = generateIv();
        }


        // Retrieve the encrypted PIN from app preferences
        String pin = prefs.getString("PIN", null);
        if (pin == null) {
            // No PIN is set, so require a PIN to be set
            Toast.makeText(MainActivity.this, getString(R.string.main_pin_choose), Toast.LENGTH_SHORT).show();
            setPin(iv);
        }

    }

    private void setPin(String iv) {
        Intent pinIntent = new Intent(this, PinReset.class);
        startActivity(pinIntent);
    }

    public static boolean savePin(String pin) {
        String encryptedPin = dataEncryptor.encryptData(pin.getBytes(), iv);
        if (encryptedPin.equals(null)){
            return false;
        }
        prefs.edit().putString("PIN", encryptedPin).commit();
        return true;
    }

    private String generateIv() {
        int leftLimit = 33; // letter 'a'
        int rightLimit = 126; // letter 'z'
        int targetStringLength = 12;

        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);

        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        String iv = buffer.toString();

        prefs.edit().putString("IV", iv).commit();
        return iv;
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

    public static boolean checkPin(String enteredPin) throws Exception {
        String storedPin_encrypted = prefs.getString("PIN", null);
        String storedPin = dataEncryptor.decryptData(storedPin_encrypted, iv);
        if (storedPin.equals(enteredPin)) {
            return true;
        }

        return false;
    }

    private void checkPermissions() {
        ArrayList<String> perms = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (perms.size() > 0) {
            Log.d(TAG, "Requesting perms: " + perms.toString());
            String[] permArray = new String[perms.size()];
            perms.toArray(permArray);
            ActivityCompat.requestPermissions(this, permArray, 100);
        }
    }
}

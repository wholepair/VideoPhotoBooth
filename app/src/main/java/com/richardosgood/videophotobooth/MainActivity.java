package com.richardosgood.videophotobooth;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "VideoPhotoBooth";
    private static final String AndroidKeyStore = "AndroidKeyStore";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final String KEY_ALIAS = "VideoBoothKey";
    private KeyStore keyStore;
    private static SharedPreferences prefs;
    public static DataEncryptor dataEncryptor;
    private static String iv = null;
    private final String BACKGROUND_PATH = android.os.Environment.DIRECTORY_PICTURES;

    private static final int CAMERA_REQUEST = 1888;
    private ImageView backgroundImage;
    private TextView tvHomeScreenInstructions;
    MainActivity ThisActivity = this;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHomeScreenInstructions = findViewById(R.id.homeScreenInstructions);
        backgroundImage = findViewById(R.id.ivBackground);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        sharedPreferences.edit().putBoolean("backgroundChanged", false).commit();
        loadPreferences();
        refreshBackgroundImage();

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

    public void screenTapped(View view) {
        // For obtaining the user's first and last name later
        Intent enterNameIntent = new Intent(this, EnterName.class);
        startActivity(enterNameIntent);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        loadPreferences();

        if (sharedPreferences.getBoolean("backgroundChanged", true)) {
            sharedPreferences.edit().putBoolean("backgroundChanged", false).commit();
            refreshBackgroundImage();
        }
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        // Home screen text
        String homeScreenText = sharedPreferences.getString("homeScreenText", null);
        tvHomeScreenInstructions.setText(homeScreenText);

        // Home image
        if (sharedPreferences.getBoolean("swHomeScreenBackground", false)) {
            backgroundImage.setVisibility(View.GONE);
        } else {
            backgroundImage.setVisibility(View.VISIBLE);
        }
    }

    public void refreshBackgroundImage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("swHomeScreenBackground", false)) {
            File backgroundFileDir = getExternalFilesDir(BACKGROUND_PATH);
            File backgroundFile = new File(backgroundFileDir.getPath() + "/background");
            if (backgroundFile.exists()) {
                try {
                    Log.v(TAG, "Setting background image");
                    backgroundImage.setImageURI(Uri.parse(backgroundFile.getPath()));
                    backgroundImage.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Unable to load background image!", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, e.getMessage());
                    backgroundImage.setImageURI(null);
                }
            }
        }
    }
}

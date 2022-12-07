package com.richardosgood.videophotobooth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsActivity extends AppCompatActivity {
    public static final String TAG = "VideoPhotoBooth";
    public static final int PICK_IMAGE = 1;

    private static final String BACKGROUND_PATH = android.os.Environment.DIRECTORY_PICTURES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private SharedPreferences sharedPreferences;
        private Preference filePicker;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            filePicker = (Preference) findPreference("filePicker");
            filePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    sharedPreferences.edit().putBoolean("backgroundChanged", true).commit();
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == PICK_IMAGE) {
                Uri content_describer = data.getData();
                InputStream in = null;
                OutputStream out = null;

                try {
                    // open the user-picked file for reading:
                    in = getActivity().getContentResolver().openInputStream(content_describer);
                    // open the output-file:
                    File outputFileDir = getActivity().getExternalFilesDir(BACKGROUND_PATH);
                    File outputFile = new File(outputFileDir.getPath() + "/background");
                    Log.v(TAG, "background_path: " + BACKGROUND_PATH);
                    //outputFile.createNewFile();
                    out = new FileOutputStream(outputFile);
                    // copy the content:
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    // Contents are copied!
                } catch (FileNotFoundException e) {
                    Toast.makeText(getActivity(), "Error updating background image", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error copying file: FileNotFoundException: " + e.getMessage());
                } catch (IOException e) {
                    Toast.makeText(getActivity(), "Error updating background image", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error copying file: IOException: " + e.getMessage());
                } finally {
                    Log.v(TAG, "File copied");
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), "Error updating background image", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error copying file on input: " + e.getMessage());
                        }
                    }
                    if (out != null){
                        try {
                            out.close();
                        } catch (IOException e) {
                            Toast.makeText(getActivity(), "Error updating background image", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error copying file on output: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
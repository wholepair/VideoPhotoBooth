package com.richardosgood.videophotobooth;

import static android.os.Environment.DIRECTORY_MOVIES;

import static androidx.camera.view.PreviewView.ScaleType.FIT_CENTER;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class VideoRecorder extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "VideoPhotoBooth";

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;

    private Button bRecording;
    private Button bPlay;
    private Button bSave;
    private Toolbar toolbar;

    boolean recorded;

    private androidx.camera.core.VideoCapture videoCapture;
    private ImageCapture imageCapture;
    private String nextAction;
    private String tempVideoName;
    private final String SAVED_PATH = android.os.Environment.DIRECTORY_MOVIES + "/saved";
    private final String TEMP_PATH = android.os.Environment.DIRECTORY_MOVIES + "/temp";
    private ActivityResultLauncher<Intent> launchCheckPin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recorded = false;

        tempVideoName = getIntent().getStringExtra("personName");

        setContentView(R.layout.activity_recorder);
        emptyTempVideos();

        previewView = findViewById(R.id.previewView);
        bRecording = findViewById(R.id.bRecord);
        bRecording.setText("Record");
        bRecording.setOnClickListener(this);
        bPlay = findViewById(R.id.bPlay);
        bSave = findViewById(R.id.bSave);
        toolbar = (Toolbar)findViewById(R.id.toolbar);

        // Hide these buttons until a video has been recorded
        bPlay.setVisibility(View.GONE);
        bSave.setVisibility(View.GONE);

        //setSupportActionBar(toolbar);
        toolbar.setSubtitle("Video Booth");
        toolbar.inflateMenu(R.menu.options_menu);

        // For checking the user's PIN later
        launchCheckPin = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            boolean pinResult = data.getBooleanExtra("result", false);

                            if (pinResult == true) {
                                if (nextAction == "export") {
                                    exportVideos();
                                } else if (nextAction == "resetPin") {
                                    setPin();
                                }
                            }

                        }
                    }
                });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.export) {
                    nextAction = "export";
                    launchCheckPin();
                } else if (item.getItemId() == R.id.setPin) {
                    nextAction = "resetPin";
                    launchCheckPin();
                } else {
                    // do something
                }

                return false;
            }
        });

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, getExecutor());

    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {

        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        previewView.setScaleType(FIT_CENTER);
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture);
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bRecord: {
                if (bRecording.getText() == "Record") {
                    bRecording.setText("Stop");
                    bPlay.setVisibility(View.GONE);
                    bSave.setVisibility(View.GONE);
                    recordVideo();
                } else if (bRecording.getText() == "Discard") {
                    discardVideo();
                } else {
                    bRecording.setText("Discard");
                    bPlay.setVisibility(View.VISIBLE);
                    bSave.setVisibility(View.VISIBLE);
                    videoCapture.stopRecording();
                }
                break;
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private void recordVideo() {
        if (videoCapture != null) {
            // If the person didn't enter a name
            if (tempVideoName.equals("")) {
                tempVideoName = System.currentTimeMillis() + ".mp4";
            } else {
                // Add timestamp to prevent duplicate filenames
                tempVideoName += "_" + System.currentTimeMillis() + ".mp4";
            }

            // Delete previous temp file if it exists
            File videoDir = getExternalFilesDir(TEMP_PATH);
            File fdelete = new File(videoDir.getPath() + "/" + tempVideoName);
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    Log.v(TAG, tempVideoName + " deleted");
                } else {
                    Log.e(TAG, "Error deleting " + tempVideoName);
                }
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, tempVideoName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");



            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            File videoDirOut = getExternalFilesDir(TEMP_PATH);
            videoDirOut.mkdirs();
            File tempVideoOut = new File(videoDirOut.getPath() + "/" + tempVideoName);
            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(
                            tempVideoOut
                    ).build(),
                    getExecutor(),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            videoRecorded();
                            //Toast.makeText(VideoRecorder.this,"Saving " + outputFileResults.getSavedUri(),Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            Toast.makeText(VideoRecorder.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }

            );

        }
    }

    private void moveFile(File fileIn, File fileOut) {
        Log.d(TAG, "Moving " + fileIn.toString() + " to " + fileOut.toString());

        // Delete video if it already exists
        if (fileOut.exists()) {
            if (fileOut.delete()) {
                Log.v(TAG, tempVideoName + " deleted");
            } else {
                Log.e(TAG, "Error deleting " + tempVideoName);
            }
        }

        InputStream in = null;
        OutputStream out = null;
        try {

            in = new FileInputStream(fileIn);
            out = new FileOutputStream(fileOut);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            fileIn.delete();

        }

        catch (FileNotFoundException fnfe1) {
            Log.e(TAG, fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void videoRecorded() {
        this.recorded = true;
        bPlay.setVisibility(View.VISIBLE);
        bSave.setVisibility(View.VISIBLE);
    }

    public void playVideo(View view) {
        Intent playbackIntent = new Intent(view.getContext(), VideoPlayer.class);
        playbackIntent.putExtra("tempVideoName", tempVideoName);
        startActivity(playbackIntent);
    }

    private void setPin() {
        Intent pinIntent = new Intent(this, PinReset.class);
        startActivity(pinIntent);
    }

    public void saveVideo(View view) {
        File videoDirIn = getExternalFilesDir(TEMP_PATH);
        File tempVideoIn = new File(videoDirIn.getPath() + "/" + tempVideoName);

        File videoDirOut = getExternalFilesDir(SAVED_PATH);
        videoDirOut.mkdirs();
        File tempVideoOut = new File(videoDirOut.getPath() + "/" + tempVideoName);

        moveFile(tempVideoIn, tempVideoOut);

        tempVideoName = "";

        Toast.makeText(VideoRecorder.this, "Video saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateVideoFilename(String personName) {}

    //Move to public storage to be accessible in gallery
    private void exportVideos() {
        File directory = getExternalFilesDir(SAVED_PATH);
        File[] files = directory.listFiles();
        Log.d(TAG, "Files: "+ files.length);

        int fileCount = 0;

        for (int i = 0; i < files.length; i++)
        {
            String fileName = files[i].getName();

            // Don't want to export temporary files, only saved files
            if (fileName.equals(tempVideoName)) {
                continue;
            }

            File videoIn = new File(directory.getPath() + "/" + fileName);

            File videoDirOut = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
            File videoOut = new File(videoDirOut.getPath() + "/" + fileName);

            moveFile(videoIn, videoOut);
            scanFile(this, videoOut, "video/mp4");
            fileCount++;
        }
        Toast.makeText(VideoRecorder.this, "Exported " + fileCount + " videos to gallery", Toast.LENGTH_SHORT).show();
    }

    // Launch activity to check pin
    private void launchCheckPin() {
        Intent pinIntent = new Intent(this, PinCheck.class);
        launchCheckPin.launch(pinIntent);
    }

    // Must tell MediaStore about the file or it won't show up in gallery until it is eventually scanned
    private void scanFile(Context c, File f, String mimeType) {
        MediaScannerConnection.scanFile(c, new String[] {f.getAbsolutePath()}, new String[] {mimeType}, null);
    }

    private void discardVideo() {
        File videoDir = getExternalFilesDir(TEMP_PATH);
        File tempVideo = new File(videoDir.getPath() + "/" + tempVideoName);
        tempVideo.delete();

        Toast.makeText(VideoRecorder.this, "Video discarded", Toast.LENGTH_SHORT).show();
        bRecording.setText("Record");
        bPlay.setVisibility(View.GONE);
        bSave.setVisibility(View.GONE);
    }

    private void emptyTempVideos() {
        File directory = getExternalFilesDir(TEMP_PATH);
        File[] files = directory.listFiles();

        for (int i = 0; i < files.length; i++) {
            // Only delete if it's not a folder
            if (files[i].isFile()) {
                files[i].delete();
            }
        }
    }
}

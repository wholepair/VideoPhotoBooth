package com.example.videophotobooth2;

import static android.os.Environment.DIRECTORY_MOVIES;

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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
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
    PreviewView previewView;

    Button bRecording;
    Button bPlay;
    Button bSave;

    Toolbar toolbar;

    boolean recorded;

    private androidx.camera.core.VideoCapture videoCapture;
    private ImageCapture imageCapture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recorded = false;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        setContentView(R.layout.activity_recorder);

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

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // TODO: Authenticate with PIN


                if (item.getItemId() == R.id.export) {
                    exportVideos();
                } else if (item.getItemId() == R.id.setPin) {
                    setPin();
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
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

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
                    bRecording.setText("Record");
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
            // Delete previous temp file if it exists
            //File videoDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
            File videoDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
            File fdelete = new File(videoDir.getPath() + "/tempVideo.mp4");
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    Log.v(TAG, "tempVideo.mp4 deleted");
                } else {
                    Log.e(TAG, "Error deleting tempVideo.mp4");
                }
            }

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "tempVideo");
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
            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(
                            getContentResolver(),
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            //MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                            //Uri.fromFile(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)),
                            contentValues
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
                Log.v(TAG, "tempVideo.mp4 deleted");
            } else {
                Log.e(TAG, "Error deleting tempVideo.mp4");
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
        moveTempVideoToPrivateStorage();
    }

    // Move temp video to private app storage
    // For some reason I couldn't save it there directly
    private void moveTempVideoToPrivateStorage() {
        File videoDirIn = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
        File tempVideoIn = new File(videoDirIn.getPath() + "/tempVideo.mp4");

        File videoDirOut = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        File tempVideoOut = new File(videoDirOut.getPath() + "/tempVideo.mp4");

        moveFile(tempVideoIn, tempVideoOut);
    }

    public void playVideo(View view) {
        Intent playbackIntent = new Intent(view.getContext(), VideoPlayer.class);
        startActivity(playbackIntent);
    }

    private void setPin() {
        Intent pinIntent = new Intent(this, EnterPin.class);
        pinIntent.putExtra("mode", "checkPin");
        startActivity(pinIntent);
    }

    public void saveVideo(View view) {
        File videoDirIn = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        File tempVideoIn = new File(videoDirIn.getPath() + "/tempVideo.mp4");

        File videoDirOut = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        File tempVideoOut = new File(videoDirOut.getPath() + "/" + System.currentTimeMillis() + ".mp4");

        moveFile(tempVideoIn, tempVideoOut);

        Toast.makeText(VideoRecorder.this, "Video saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    //Move to public storage to be accessible in gallery
    private void exportVideos() {
        File directory = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        File[] files = directory.listFiles();
        Log.d(TAG, "Files: "+ files.length);

        for (int i = 0; i < files.length; i++)
        {
            String fileName = files[i].getName();

            // Don't want to export temporary files, only saved files
            if (fileName.equals("tempVideo.mp4")) {
                continue;
            }

            File videoIn = new File(directory.getPath() + "/" + fileName);

            File videoDirOut = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
            File videoOut = new File(videoDirOut.getPath() + "/" + fileName);

            moveFile(videoIn, videoOut);
            scanFile(this, videoOut, "video/mp4");
        }
    }

    // Must tell MediaStore about the file or it won't show up in gallery until it is eventually scanned
    private void scanFile(Context c, File f, String mimeType) {
        MediaScannerConnection.scanFile(c, new String[] {f.getAbsolutePath()}, new String[] {mimeType}, null);
    }

    private void discardVideo() {
        File videoDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        File tempVideo = new File(videoDir.getPath() + "/tempVideo.mp4");
        tempVideo.delete();

        Toast.makeText(VideoRecorder.this, "Video discarded.", Toast.LENGTH_SHORT).show();
        bRecording.setText("Record");
        bPlay.setVisibility(View.GONE);
        bSave.setVisibility(View.GONE);
    }
}

package com.example.videophotobooth2;

import static android.os.Environment.DIRECTORY_MOVIES;

import android.os.Bundle;
import android.os.Environment;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class VideoPlayer extends AppCompatActivity {
    VideoView videoView;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        videoView = (VideoView)findViewById(R.id.videoView);

    }

    private void playVideo() {
        File videoDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES);
        File fdelete = new File(videoDir.getPath() + "/tempVideo.mp4");

        videoView.setVideoURI(Uri.parse(“android.resource://” + getPackageName() +”/”+R.raw.video));
        videoView.setMediaController(new MediaController(this));
        videoView.requestFocus();
        videoView.start();
    }
}

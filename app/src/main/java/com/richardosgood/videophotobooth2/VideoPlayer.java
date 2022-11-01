package com.richardosgood.videophotobooth2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class VideoPlayer extends AppCompatActivity {
    VideoView videoView;
    Button bStop;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        videoView = (VideoView)findViewById(R.id.videoView);
        bStop = (Button)findViewById(R.id.bStop);

        playVideo();

    }

    private void playVideo() {
        File videoDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);

        videoView.setVideoPath(videoDir.getPath() + "/tempVideo.mp4");
        //videoView.setMediaController(new MediaController(this, ));
        videoView.requestFocus();
        videoView.start();
    }

    public void buttonStop(View view) {
        videoView.stopPlayback();
        finish();
    }
}

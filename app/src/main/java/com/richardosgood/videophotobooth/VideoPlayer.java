package com.richardosgood.videophotobooth;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class VideoPlayer extends AppCompatActivity {
    private VideoView videoView;
    private Button bStop;
    private String tempVideoName;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        tempVideoName = getIntent().getStringExtra("tempVideoName");

        videoView = (VideoView)findViewById(R.id.videoView);
        bStop = (Button)findViewById(R.id.bStop);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override public void onPrepared(MediaPlayer mp) {
                // so it fits on the screen
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                float videoProportion = (float) videoWidth / (float) videoHeight;


                DisplayMetrics mDisplayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

                int screenWidth = videoView.getWidth();
                int screenHeight = videoView.getHeight();

                float screenProportion = (float) screenWidth / (float) screenHeight;
                android.view.ViewGroup.LayoutParams lp = videoView.getLayoutParams();

                if (videoProportion > screenProportion) {
                    lp.width = screenWidth;
                    lp.height = (int) ((float) screenWidth / videoProportion);
                } else {
                    lp.width = (int) (videoProportion * (float) screenHeight);
                    lp.height = screenHeight;
                }

                videoView.setLayoutParams(lp);

            }
        });

        playVideo();

    }

    private void playVideo() {
        File videoDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);

        videoView.setVideoPath(videoDir.getPath() + "/temp/" + tempVideoName);
        //videoView.setMediaController(new MediaController(this, ));
        videoView.requestFocus();
        videoView.start();
    }

    public void buttonStop(View view) {
        videoView.stopPlayback();
        finish();
    }
}

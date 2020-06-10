package com.vikas.myst;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

public class VideoPlayer extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        MediaController mediaControls= new MediaController(this);
        Intent myIntent=getIntent();
        Uri uri=Uri.parse(myIntent.getStringExtra("filePath"));
        VideoView videoView=findViewById(R.id.videoPlayer);
        videoView.setVideoURI(uri);
        videoView.start();
        videoView.setMediaController(mediaControls);
        mediaControls.setAnchorView(videoView);
    }
}

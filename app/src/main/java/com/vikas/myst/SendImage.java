package com.vikas.myst;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;


import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

public class SendImage extends AppCompatActivity {
    ImageView imv;
    VideoView vv;
    Uri filePath;
    Button imageCancel;
    Button imageSendButton;
    String type;
    Context ctx;
    private MediaController mediaControls;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_image);
        ctx=this;
        Intent myIntent=getIntent();
        type=myIntent.getStringExtra("type");
        imv=findViewById(R.id.chatImage);
        imageCancel=findViewById(R.id.imageCancel);
        imageSendButton=findViewById(R.id.imageSendButton);
        vv=findViewById(R.id.pickVideo);
        // set the media controller buttons
            mediaControls = new MediaController(this);
        startImageLoad();
        initiative();
    }

    private void initiative() {
        imageCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void startImageLoad() {
        Intent intent = new Intent();

        intent.setAction(Intent.ACTION_GET_CONTENT);
        if(type.equalsIgnoreCase("image")) {
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Image from here..."), 22);
        }else if(type.equalsIgnoreCase("video")){
            intent.setType("video/*");
            startActivityForResult(Intent.createChooser(intent, "Select video from here..."), 44);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == 22&& resultCode == RESULT_OK && data != null && data.getData() != null) {
            vv.setVisibility(View.GONE);
            imv.setVisibility(View.VISIBLE);
            filePath = data.getData();
            imv.setImageURI(filePath);
            imageSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View vr) {
                   /* View v= LayoutInflater.from(ctx).inflate(R.layout.image_preview,null);
                    ImageView imagePreview=v.findViewById(R.id.imagePreview);
                    imagePreview.setImageURI(filePath);

                    AlertDialog.Builder builder=new AlertDialog.Builder(ctx);
                    AlertDialog ad=builder.create();
                    ad.setView(v);
                    ad.show();*/
                    setResult(Activity.RESULT_OK,new Intent(getApplicationContext(),ChatBox.class).setData(filePath));
                    finish();
                }
            });
        }else if (requestCode == 44&& resultCode == RESULT_OK && data != null && data.getData() != null) {
            vv.setVisibility(View.VISIBLE);
            imv.setVisibility(View.GONE);
        filePath = data.getData();

            vv.setMediaController(mediaControls);
            vv.setVideoURI(filePath);
            vv.start();
            imageSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(Activity.RESULT_OK,new Intent(getApplicationContext(),ChatBox.class).setData(filePath));
                    finish();
                }
            });
        }else
            finish();;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

package com.vikas.myst;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity  {
static {
    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
}
Intent loginProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginProcess = new Intent(getBaseContext(), LoginVerification.class);
        setContentView(R.layout.activity_main);
        final Intent chatBox = new Intent(getBaseContext(), ChatList.class);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                chatBox.putExtra("myNumber", "7008529481");
                startActivity(chatBox);
                finish();
            }
        },150);

    }
    }


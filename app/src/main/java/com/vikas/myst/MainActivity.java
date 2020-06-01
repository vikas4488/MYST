package com.vikas.myst;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.FirebaseDatabase;


public class MainActivity extends AppCompatActivity  {
static {
    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
}
Intent loginProcess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginProcess=new Intent(getBaseContext(),LoginVerification.class);
        setContentView(R.layout.activity_main);
        Intent chatBox=new Intent(getBaseContext(),ChatList.class);
        chatBox.putExtra("myNumber","7008529481");
        startActivity(chatBox);

        //owsom();
    }
}

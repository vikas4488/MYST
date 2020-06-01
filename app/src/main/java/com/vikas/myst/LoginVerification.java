package com.vikas.myst;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.vikas.myst.sql.LoginTable;

public class LoginVerification extends AppCompatActivity {
String android_id;
    Intent homeIntent;
    Intent loginIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_verification);
        android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        loginIntent = new Intent(getBaseContext(),LoginProcess.class);
        homeIntent=new Intent(getBaseContext(),ChatList.class);
        startSecurityProcess(android_id);
    }

    public void startSecurityProcess(String android_id){
        String loginCheck= LoginTable.checkLoginStatus(android_id,getApplicationContext());
        if(loginCheck.equalsIgnoreCase("device_not_verified")){
            startActivity(loginIntent);
            finish();
        }else if(loginCheck.equalsIgnoreCase("logged_out")){
            finish();
        }else if(loginCheck.equalsIgnoreCase("unknown_login_check_fail")){

        }else{

            homeIntent.putExtra("myNumber",loginCheck);
            startActivity(homeIntent);
            finish();
        }
    }
}

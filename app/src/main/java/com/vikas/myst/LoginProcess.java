package com.vikas.myst;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.vikas.myst.custom.CustomLoading;
import com.vikas.myst.sql.LoginTable;
import com.vikas.myst.util.ChatUtil;

import java.util.concurrent.TimeUnit;

public class LoginProcess extends AppCompatActivity {

    EditText phNumber;
    EditText otp;
    Button sendOtpButton;
    Button btnSubmitOtp;
    String codeSent;
    FirebaseAuth mAuth;
    Intent homeIntent;
    Intent loginIntent;
    String android_id;
    ConstraintLayout loginOtp;
    CustomLoading lodingCircle;
    Context ctx;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_process);
        phNumber =  findViewById(R.id.editText);
        sendOtpButton = findViewById(R.id.sendOtpButton);
        mAuth = FirebaseAuth.getInstance();
        android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        loginIntent = new Intent(getBaseContext(),LoginProcess.class);
        homeIntent = new Intent(getBaseContext(),ChatList.class);
        lodingCircle=findViewById(R.id.lodingCircle);
        ctx=this;
        sendOtp();
    }

    public void sendOtp(){
        sendOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOtpButton.setVisibility(View.INVISIBLE);
                sendOtpButton.post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
                if(ChatUtil.isConnected()) {
                    String number=phNumber.getText().toString();
                    if(number.trim().equalsIgnoreCase("")||number.trim().length()<10)
                    {
                        android.app.AlertDialog s=ChatUtil.getCustomAlert("Invalid number","Please enter 10 digit number",ctx);
                        s.show();
                        s.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                sendOtpButton.setVisibility(View.VISIBLE);
                            }
                        });

                    }else {
                        number = "+91"+number;
                        startPhoneNumberVerification(number);
                        lodingCircle.setVisibility(View.VISIBLE);
                    }
                }else{
                    android.app.AlertDialog s=ChatUtil.getCustomAlert("No Internet"," please connect to Internet",ctx);
                    s.show();
                    s.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            sendOtpButton.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            LoginTable.saveLoginDetails(android_id,phNumber.getText().toString(),getApplicationContext());
                            homeIntent.putExtra("myNumber",phNumber.getText().toString());
                            startActivity(homeIntent);
                            finish();
                        } else {
                            ChatUtil.getCustomAlert("Failed","Otp Did not match",ctx).show();
                            sendOtpButton.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }
    private void startPhoneNumberVerification(String phoneNumber) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]
    }
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks=new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onCodeAutoRetrievalTimeOut(@NonNull String s) {
            super.onCodeAutoRetrievalTimeOut(s);
        }

        @Override
        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
            signInWithPhoneAuthCredential(phoneAuthCredential);
            //loadingCs.setVisibility(View.INVISIBLE);
        }
        @Override
        public void onVerificationFailed(@NonNull FirebaseException e) {
        }
        @Override
        public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            mResendToken=forceResendingToken;
            codeSent=s;
            lodingCircle.setVisibility(View.INVISIBLE);
            AlertDialog.Builder builder=new AlertDialog.Builder(ctx).setCancelable(false);
            View otpView= LayoutInflater.from(getApplicationContext()).inflate(R.layout.otp_pop,null);
            final EditText otpText=otpView.findViewById(R.id.otpTextView);
            Button otpButton=otpView.findViewById(R.id.otpButtonView);
            builder.setView(otpView);
            final AlertDialog alertDialog=builder.create();
            alertDialog.show();
            otpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String code=otpText.getText().toString();
                    if(!code.trim().equalsIgnoreCase("")) {
                        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codeSent, code);
                        //alertDialog.dismiss();
                        signInWithPhoneAuthCredential(credential);
                    }
                }
            });
        }
    };
}

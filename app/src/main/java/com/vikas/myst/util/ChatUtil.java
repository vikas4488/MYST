package com.vikas.myst.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.vikas.myst.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatUtil extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 22;
    static SimpleDateFormat simpleTimeDateFormat=new SimpleDateFormat("yyyyMMddHHmmssSSSS", Locale.ENGLISH);
    static SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    private static Uri filePath;
    public static String getABUnit(String number1,String number2){
        Long n1=Long.parseLong(number1);
        Long n2=Long.parseLong(number2);
        if(n1>n2)
            return "A"+number1+"B"+number2;
        else
            return "A"+number2+"B"+number1;
    }
    public  static String getMyType(String myNumber, String friendNumber){
        Long n1=Long.parseLong(myNumber);
        Long n2=Long.parseLong(friendNumber);
        if(n1>n2)
            return "A";
        else
            return "B";
    }
    public static String getFriendType(String myNumber, String friendNumber){
        Long n1=Long.parseLong(myNumber);
        Long n2=Long.parseLong(friendNumber);
        if(n1>n2)
            return "B";
        else
            return "A";
    }
    public static String getFrom(String myNumber, String friendNumber) {
        return getMyType(myNumber,friendNumber)+getFriendType(myNumber,friendNumber);
    }

    public static AlertDialog getCustomAlert(String title, String message, Context context){
        View v= LayoutInflater.from(context).inflate(R.layout.alert_custom,null);
        TextView t1=v.findViewById(R.id.customAlertTitle);
        TextView t2=v.findViewById(R.id.customAlertContent);
        Button b=v.findViewById(R.id.customAlertOkButton);
        t1.setText(title);
        t2.setText(message);

        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        final AlertDialog ad=builder.create();
        ad.setView(v);
        ad.show();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });

        return ad;
    }
    public static AlertDialog getCustomAlertBoolean(String title, String message,String booleanMessage, Context context){
        View v= LayoutInflater.from(context).inflate(R.layout.delete_message_alert,null);
        TextView t1=v.findViewById(R.id.customAlertTitle);
        TextView t2=v.findViewById(R.id.customAlertContent);
        CheckBox c=v.findViewById(R.id.check);
        c.setText(booleanMessage);
        Button b=v.findViewById(R.id.customAlertOkButton);
        t1.setText(title);
        t2.setText(message);

        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        final AlertDialog ad=builder.create();
        ad.setView(v);
        ad.show();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ad.dismiss();
            }
        });

        return ad;
    }

    public static String processOnline(String time, Context ctx, String friendNumber) {
        String status=time;
        if(!status.equalsIgnoreCase("unknown")) {
            String crTime = simpleTimeDateFormat.format(new Date());
            Long diff = Long.parseLong(crTime) - Long.parseLong(status);
            Date wasDate = null;
            try {
                wasDate = simpleTimeDateFormat.parse(status);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String wasTime=simpleDateFormat.format(wasDate);
            Long mins = (diff / 60);
            Long hrs = mins / 60;
            if (diff < 5)
                status = "online";
            else if (diff < 59)
                status = diff+" secs ago";
            else if (mins < 59)
                status = mins + " mins ago";
            else if (hrs < 24)
                status = hrs + " hours ago";
            else
                status =wasTime;
        }
        return status;
    }
    public static boolean isConnected() {
        final String command = "ping -c 1 google.com";
        try {
            return Runtime.getRuntime().exec(command).waitFor() == 0;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static AlertDialog getImagePreview(Uri filepath, Context ctx){

    View v= LayoutInflater.from(ctx).inflate(R.layout.image_preview,null);
    ImageView imagePreview=v.findViewById(R.id.imagePreview);
    imagePreview.setImageURI(filepath);

    AlertDialog.Builder builder=new AlertDialog.Builder(ctx);
    AlertDialog ad=builder.create();
    ad.setView(v);
    ad.show();
    return ad;
}
public  static int getIntegerId(String id){
return Integer.parseInt(id.substring(id.length()-9));
}
    public  static int getIntegerId(Long id){
        String idl=String.valueOf(id);
        return Integer.parseInt(idl.substring(idl.length()-10));
    }
    public  static int getIntegerId(int id){
        String idl=String.valueOf(id);
        return Integer.parseInt(idl.substring(idl.length()-10));
    }
}

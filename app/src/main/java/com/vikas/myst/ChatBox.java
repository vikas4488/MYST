package com.vikas.myst;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vikas.myst.bean.Chat;
import com.vikas.myst.cloud.Sync;
import com.vikas.myst.sql.ChatTable;
import com.vikas.myst.sql.ContactTable;
import com.vikas.myst.sql.NewChatTable;
import com.vikas.myst.util.ChatUtil;
import com.vikas.myst.util.ParallelProcess;
import com.vikas.myst.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ChatBox extends AppCompatActivity {

    TextView msgView;
    ImageView sendMsg;
    Context ctx;
    String myNumber;
    String friendNumber;
    String friendName;
    TextView friendNameView;
    ScrollView chatScroll;
    TextView onlineDot;
    TextView lastSeen;
    Timer timer;
    Button imageUploadButton;
    Intent sendImage;
    LinearLayout chatLayout;
    StorageReference storageReference;
    LinearLayout imageVideoLayout;
    LinearLayout chatPrent;
    private MediaController mediaControls;
    private static final int REQUEST_EXTERNAL_STORAGE = 111;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    Button deleteChats;
    SimpleDateFormat simpleTimeDateFormat=new SimpleDateFormat("yyyyMMddHHmmssSSSS", Locale.ENGLISH);
    static SimpleDateFormat simpleTimeFormat=new SimpleDateFormat("hh mm a", Locale.ENGLISH);
    public static String friendStatus="unknown";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_box);
        ctx=this;
        msgView=findViewById(R.id.msgView);
        sendMsg=findViewById(R.id.sendMsg);
        chatPrent=findViewById(R.id.chatPrent);
        deleteChats=findViewById(R.id.deleteChats);
        Sync.deleteChats=deleteChats;
        Intent myIntent=getIntent();
        mediaControls = new MediaController(this);
        myNumber=myIntent.getStringExtra("myNumber");
        friendNumber=myIntent.getStringExtra("friendNumber");
        friendNameView=findViewById(R.id.friendNameView);
        onlineDot=findViewById(R.id.onlineDot);
        lastSeen=findViewById(R.id.lastSeen);


        chatLayout=findViewById(R.id.scrollLayout);
        imageUploadButton=findViewById(R.id.imageUploadButton);
        sendImage=new Intent(getBaseContext(),SendImage.class);
        storageReference = FirebaseStorage.getInstance().getReference();
        friendName= ContactTable.getContactName(friendNumber,ctx);
        friendNameView.setText(friendName);
        imageVideoLayout=findViewById(R.id.imageVideoLayout);
        Sync.chatBoxContext=ctx;
        Sync.chatNumber=friendNumber;
        chatScroll = findViewById(R.id.chatScroll);
        initiateIt();
        checkOnline();
        timer=new Timer();
        getOnline();
    }

    private void checkOnline() {
        FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
        DatabaseReference onlineReference=firebaseDatabase.getReference("myst-online").child(friendNumber);
        onlineReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                friendStatus=dataSnapshot.getValue().toString();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                friendStatus=dataSnapshot.getValue().toString();

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        Sync.chatBoxContext=null;
        Sync.chatNumber=null;
        Sync.markerList.clear();
        timer.cancel();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(imageVideoLayout.getVisibility()==View.VISIBLE)
            imageVideoLayout.setVisibility(View.INVISIBLE);
        else
            finish();
    }

    private void initiateIt() {
        deleteChats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog ad=ChatUtil.getCustomAlertBoolean("DELETE","DELETE CHATS FOR ME","delete media from phone too?",ctx);
                ad.setCancelable(false);
                ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        CheckBox checkBox =  ((AlertDialog) dialog).findViewById(R.id.check);
                        Boolean deleteMedia=checkBox.isChecked();
                        List<String> messagesIds=new ArrayList<>();
                        List<Chat> imagesIds=new ArrayList<>();
                        for(Chat chat:Sync.markerList){
                            LinearLayout layout= findViewById(ChatUtil.getIntegerId(chat.getId())-1);
                            layout.setVisibility(View.GONE);
                            messagesIds.add(chat.getId());
                            if(chat.getMsgType().equalsIgnoreCase("image")||chat.getMsgType().equalsIgnoreCase("video"))
                                imagesIds.add(chat);
                        }
                        if(deleteMedia) {
                            ParallelProcess.DeleteChatsParams deleteChatsParams = new ParallelProcess.DeleteChatsParams(imagesIds, ctx);
                            ParallelProcess.DeleteChatFromDbInBackground deleteChatFromDbInBackground = new ParallelProcess.DeleteChatFromDbInBackground();
                            deleteChatFromDbInBackground.execute(deleteChatsParams);
                        }
                        ChatTable.deleteSelectedChats(messagesIds,ctx);
                        Sync.markerList.clear();
                        Sync.marker=false;
                        deleteChats.setVisibility(View.GONE);
                    }
                });
            }
        });
        chatPrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageVideoLayout.setVisibility(View.INVISIBLE);
            }
        });
        NewChatTable.resetNewChatToZeroCount(friendNumber,ctx);
        Sync.setChatsFromDb(myNumber,friendNumber,ctx);
        msgView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    chatScroll.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    },150);
                }
            }
        });
        msgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                chatScroll.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                },150);
            }
        });
        sendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg=msgView.getText().toString();
                msg=msg.trim();
                if(!msg.equalsIgnoreCase("")){
                    String timeId=simpleTimeDateFormat.format(new Date());
                    String time=simpleTimeFormat.format(new Date());
                    Chat chat=new Chat();
                    chat.setMsg(msg);
                    chat.setMsgType("text");
                    chat.setFriend(friendNumber);
                    chat.setId(timeId);
                    chat.setTime(time);
                    Sync.saveInFireDbAndSetInViewChat(myNumber,ctx,chat);
                    msgView.setText("");
                }
            }
        });
        imageUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyStoragePermissions((Activity) ctx);
            }
        });
    }
    private void mediaPermissionBypass(){
        if(imageVideoLayout.getVisibility()==View.INVISIBLE)
            imageVideoLayout.setVisibility(View.VISIBLE);
        else
            imageVideoLayout.setVisibility(View.INVISIBLE);
        Button pickImage=findViewById(R.id.pickImage);
        Button pickVideo=findViewById(R.id.pickVideo);
        final Intent filePickerIntent = new Intent();
        pickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filePickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                filePickerIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(filePickerIntent, "Select Image from here..."), 100);
            }
        });
        pickVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filePickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                filePickerIntent.setType("video/*");
                startActivityForResult(Intent.createChooser(filePickerIntent, "Select Video from here..."), 101);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        imageVideoLayout.setVisibility(View.INVISIBLE);
        if ((requestCode == 100||requestCode == 101)&& resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri filePath = data.getData();
            View pickerView=LayoutInflater.from(ctx).inflate(R.layout.send_image,null);

            String type="";
            if(requestCode == 100) {
                ImageView pickImageView = pickerView.findViewById(R.id.pickImage);
                pickImageView.setVisibility(View.VISIBLE);
                pickImageView.setImageURI(filePath);
                type="image";
            }else {
                VideoView pickVideo=pickerView.findViewById(R.id.pickVideo);
                pickVideo.setVisibility(View.VISIBLE);
                pickVideo.setVideoURI(filePath);
                pickVideo.setMediaController(mediaControls);
                pickVideo.start();
                type="video";
            }
            Button imageOkButton=pickerView.findViewById(R.id.imageSendButton);
            Button cancelButton=pickerView.findViewById(R.id.imageCancel);
            android.app.AlertDialog.Builder builder=new android.app.AlertDialog.Builder(ctx);
            final AlertDialog ad=builder.create();
            ad.setView(pickerView);
            ad.show();
            final String finalType = type;
            final Chat chat =new Chat();
            String expPath = null;
            try {
                expPath=PathUtil.getPath(ctx,filePath);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            chat.setMsg(expPath);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                }
            });
            imageOkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ad.dismiss();
                    View imageView=LayoutInflater.from(ctx).inflate(R.layout.sent_image_unit_chat,null);
                    TextView msgTime=imageView.findViewById(R.id.msgTime);
                    final String time=simpleTimeFormat.format(new Date());
                    msgTime.setText(time);
                    ImageView imv;
                    chat.setMsgType(finalType);
                    if(finalType.equalsIgnoreCase("image"))
                    {
                        imv=imageView.findViewById(R.id.chatImage);
                        imv.setVisibility(View.VISIBLE);
                        imv.setImageURI(filePath);
                        imv.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View vr) {
                                ChatUtil.getImagePreview(filePath,ctx);
                            }
                        });
                    }else{
                        imv=imageView.findViewById(R.id.chatVideo);
                        imv.setVisibility(View.VISIBLE);
                        try {
                            String absPath=PathUtil.getPath(ctx,filePath);
                            Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(absPath, MediaStore.Images.Thumbnails.MINI_KIND);
                            imv.setImageBitmap(bmThumbnail);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }

                    }
                    LinearLayout layout= (LinearLayout) imv.getParent().getParent().getParent();
                    String timeId=simpleTimeDateFormat.format(new Date());
                    layout.setId(ChatUtil.getIntegerId(timeId)-1);

                    chat.setId(timeId);
                    layout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            if(Sync.marker){
                                if(Sync.markerList.contains(chat))
                                {
                                    v.setBackgroundColor(0x00000000);
                                    Sync.markerList.remove(chat);
                                    if(Sync.markerList.size()==0)
                                    {
                                        Sync.marker=false;
                                        deleteChats.setVisibility(View.GONE);
                                    }
                                }
                                else
                                {
                                    deleteChats.setVisibility(View.VISIBLE);
                                    v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                                    {
                                        Sync.markerList.add(chat);
                                        deleteChats.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        }
                    });
                    layout.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Sync.marker=true;
                            v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                            Sync.markerList.add(chat);
                            deleteChats.setVisibility(View.VISIBLE);
                            return true;
                        }
                    });
                    ProgressBar imageLoading=imageView.findViewById(R.id.imageLoading);
                    TextView imageWait=imageView.findViewById(R.id.imageWait);
                    ImageView iconError=imageView.findViewById(R.id.iconError);
                    TextView uploadPercentage=imageView.findViewById(R.id.uploadPercentage);

                    imageWait.setId(ChatUtil.getIntegerId(chat.getId())-3);

                    try {
                        MyTaskParams myTaskParams=new MyTaskParams(ctx,imageLoading,imageWait,iconError,myNumber,friendNumber,timeId,storageReference,filePath,uploadPercentage, finalType,time);
                        LongOperation longOperation=new LongOperation();
                        longOperation.execute(myTaskParams);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    chatLayout.addView(imageView);
                    chatScroll.post(new Runnable() {
                        @Override
                        public void run() {
                            chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            });

        }
    }
    private void getOnline(){
        TimerTask task=new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String status=ChatUtil.processOnline(friendStatus,ctx,friendNumber);
                        TextView onlineDot=findViewById(R.id.onlineDot);
                        if(status.equalsIgnoreCase("online")){
                            lastSeen.setVisibility(View.GONE);
                            onlineDot.setText("⬤ online");
                            onlineDot.setTextColor(Color.parseColor("#4CAF50"));
                        }else{
                            if(status.equalsIgnoreCase("unknown")) {
                                lastSeen.setVisibility(View.GONE);
                                onlineDot.setText("");
                            }else {
                                lastSeen.setVisibility(View.VISIBLE);
                                onlineDot.setText(status);
                            }
                            onlineDot.setTextColor(Color.parseColor("#000000"));
                        }
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(task,0,5000);
    }
    private static class MyTaskParams {
        String time;
        Context context;
        ProgressBar imageLoading;
        TextView imageWait;
        ImageView iconError;
        String status;
        String myNumber;
        String friendNumber;
        String timeId;
        StorageReference storageReference;
        Uri filePath;
        TextView uploadPercentage;
        String type;

        MyTaskParams(Context context,
                     ProgressBar imageLoading,
                     TextView imageWait, ImageView iconError,
                     String myNumber, String friendNumber,
                     String timeId, StorageReference storageReference,
                     Uri filePath, TextView uploadPercentage, String type,String time) {
            this.context = context;
            this.imageLoading=imageLoading;
            this.imageWait=imageWait;
            this.iconError=iconError;
            this.myNumber=myNumber;
            this.friendNumber=friendNumber;
            this.timeId=timeId;
            this.storageReference=storageReference;
            this.filePath=filePath;
            this.uploadPercentage=uploadPercentage;
            this.type=type;
            this.time=time;
        }
    }
    private static final class LongOperation extends AsyncTask<MyTaskParams, MyTaskParams, MyTaskParams[]> {

        @Override
        protected MyTaskParams[] doInBackground(MyTaskParams... myTaskParams) {
            try {
                saveFileToFirebase(myTaskParams);
                return myTaskParams;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return myTaskParams;
        }
        @Override
        protected void onPostExecute(MyTaskParams[] myTaskParams) {
            super.onPostExecute(myTaskParams);
            myTaskParams[0].imageLoading.setVisibility(View.GONE);
                myTaskParams[0].uploadPercentage.setVisibility(View.VISIBLE);
        }
        private void saveFileToFirebase(final MyTaskParams[] myTaskParams){
            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            StorageReference ref= storageReference.child("MystImages/"+ myTaskParams[0].timeId);
            ref.putFile(myTaskParams[0].filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    myTaskParams[0].uploadPercentage.setVisibility(View.GONE);
                    myTaskParams[0].imageLoading.setVisibility(View.GONE);
                    myTaskParams[0].imageWait.setVisibility(View.VISIBLE);

                    String exPath= null;
                    try {
                        exPath = PathUtil.getPath(myTaskParams[0].context, Uri.parse(myTaskParams[0].filePath.toString()));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    Chat chat=new Chat();
                    chat.setId(myTaskParams[0].timeId);
                    chat.setFriend(myTaskParams[0].friendNumber);
                    chat.setMsgType(myTaskParams[0].type);
                    chat.setMsg(exPath);
                    chat.setTime(myTaskParams[0].time);
                    Sync.saveInFireDbAndSetInViewChat(myTaskParams[0].myNumber,myTaskParams[0].context,chat);
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    int progress = (int) (100* taskSnapshot.getBytesTransferred()/ taskSnapshot.getTotalByteCount());
                    myTaskParams[0].uploadPercentage.setText(String.valueOf(progress)+"%");
                }
            });

        }
    }



    public void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(activity,PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        else
            mediaPermissionBypass();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mediaPermissionBypass();
            }
            else {
                ChatUtil.getCustomAlert("Permission Denied","please allow permission to send video and images",ctx);
            }
        }


    }
}

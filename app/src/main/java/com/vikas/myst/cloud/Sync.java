package com.vikas.myst.cloud;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.vikas.myst.ChatBox;
import com.vikas.myst.R;
import com.vikas.myst.bean.Chat;
import com.vikas.myst.bean.NewChat;
import com.vikas.myst.sql.ChatStructure;
import com.vikas.myst.sql.ChatTable;
import com.vikas.myst.sql.ContactTable;
import com.vikas.myst.sql.NewChatTable;
import com.vikas.myst.util.ChatUtil;
import com.vikas.myst.util.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Sync {
    public static Context chatListContext =null;
    static SimpleDateFormat simpleTimeDateFormat=new SimpleDateFormat("ddMMyyyyHHmmss", Locale.ENGLISH);
    static SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    static SimpleDateFormat simpleTimeFormat=new SimpleDateFormat("hh mm ss a", Locale.ENGLISH);
public static Context chatBoxContext=null;
public static String chatNumber=null;
public static String myNumber=null;
        public static void saveInFireDbAndSetInViewChat(final String myNumber, final String friendNumber, String msg, final Context ctx,final String msgType,final String timeId){
            String myType=ChatUtil.getMyType(myNumber,friendNumber);
            String crDateAndTime=simpleTimeDateFormat.format(new Date());
            String crDate=simpleDateFormat.format(new Date());
            String crTime=simpleTimeFormat.format(new Date());
            FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
            DatabaseReference databaseReference=firebaseDatabase.getReference(ChatStructure.TABLE).child(friendNumber).child(myNumber);
            DatabaseReference referenceUnit= databaseReference.child(crDateAndTime);
            referenceUnit.keepSynced(true);
            final Chat chat=new Chat();

            chat.setTime(crTime);
            chat.setStatus("unread");
            chat.setFrom(myType);
            chat.setDate(crDate);
            chat.setFriend(friendNumber);
            chat.setMsgType(msgType);
            chat.setMsg(msg);
            if(msgType.equalsIgnoreCase("image")||msgType.equalsIgnoreCase("video"))
            {
                chat.setId(timeId);
            }else{
                chat.setId(crDateAndTime);
                setSendChatView(ctx,chat);
            }
            ChatTable.saveChat(ctx,chat);
            if(msgType.equalsIgnoreCase("image")||msgType.equalsIgnoreCase("video"))//setting blank message for firebase to frnd as he will receive
                chat.setMsg("");
            final NewChat newChat=new NewChat();
            if(msgType.equalsIgnoreCase("image")||msgType.equalsIgnoreCase("video"))
                newChat.setMsg(msgType);
            else
            newChat.setMsg(chat.getMsg());
            newChat.setStatus(chat.getStatus());
            newChat.setFriend(chat.getFriend());
            newChat.setDate(chat.getDate());
            newChat.setTime(chat.getTime());
            newChat.setMsgCount("0");
            NewChatTable.updateMsgNewChat(myNumber,ctx, newChat);
            chat.setFriend(myNumber);
            //setting in firebase chat
            referenceUnit.setValue(chat).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    if(chatBoxContext!=null) {
                        TextView msgStatus = ((Activity) chatBoxContext).findViewById((int) Long.parseLong(chat.getId()));
                        msgStatus.setBackgroundResource(R.drawable.icon_sent);
                    }
                    newChat.setStatus("sent");
                    NewChatTable.updateMsgNewChat(myNumber,ctx, newChat);
                    chat.setStatus("sent");
                    ChatTable.updateMsgStatus(chat,ctx);
                }
            });
        }

    private static void setSentImageView(Context ctx, Chat chat) {
        View imageView=LayoutInflater.from(ctx).inflate(R.layout.sent_image_unit_chat,null);
        ImageView imv = null;
        ImageView viv=null;
        if(chat.getMsgType().equalsIgnoreCase("image"))
        {
            imv=imageView.findViewById(R.id.chatImage);
            imv.setVisibility(View.VISIBLE);
        }
        else
        {
            viv=imageView.findViewById(R.id.chatVideo);
            viv.setVisibility(View.VISIBLE);
        }
        ProgressBar imageLoading=imageView.findViewById(R.id.imageLoading);
        TextView imageWait=imageView.findViewById(R.id.imageWait);

        MyTaskParams myTaskParams=new MyTaskParams(chat,imv,null,ctx,viv);
        LongOperation longOperation=new LongOperation();
        longOperation.execute(myTaskParams);

        imageWait.setId((int) Long.parseLong(chat.getId()));

        imageLoading.setVisibility(View.GONE);
        if(chat.getStatus().equalsIgnoreCase("sent"))
            imageWait.setBackgroundResource(R.drawable.icon_sent);
        else if(chat.getStatus().equalsIgnoreCase("delivered"))
            imageWait.setBackgroundResource(R.drawable.icon_delivered);
        else if(chat.getStatus().equalsIgnoreCase("read"))
            imageWait.setBackgroundResource(R.drawable.icon_read);
        imageWait.setVisibility(View.VISIBLE);
        LinearLayout scrollLayout=((Activity)ctx).findViewById(R.id.scrollLayout);
        final ScrollView chatScroll=((Activity)ctx).findViewById(R.id.chatScroll);
        scrollLayout.addView(imageView);
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    private static void setSendChatView(Context ctx, Chat chat) {
        View sendView=LayoutInflater.from(ctx).inflate(R.layout.chat_send_unit,null);
        TextView sendText=sendView.findViewById(R.id.sendText);
        TextView msgStatus=sendView.findViewById(R.id.msgStatus);
        sendText.setText(chat.getMsg());
        msgStatus.setId((int) Long.parseLong(chat.getId()));
        if(chat.getStatus().equalsIgnoreCase("sent"))
            msgStatus.setBackgroundResource(R.drawable.icon_sent);
        else if(chat.getStatus().equalsIgnoreCase("delivered"))
            msgStatus.setBackgroundResource(R.drawable.icon_delivered);
        else if(chat.getStatus().equalsIgnoreCase("read"))
            msgStatus.setBackgroundResource(R.drawable.icon_read);
        LinearLayout scrollLayout=((Activity)ctx).findViewById(R.id.scrollLayout);
        final ScrollView chatScroll=((Activity)ctx).findViewById(R.id.chatScroll);
        scrollLayout.addView(sendView);
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    private static void setReceiveChatView(Context ctx, Chat chat) {
        View receiveView=LayoutInflater.from(ctx).inflate(R.layout.chat_receive_unit,null);
        TextView receiveText=receiveView.findViewById(R.id.receiveText);
        receiveText.setText(chat.getMsg());
        LinearLayout scrollLayout=((Activity)ctx).findViewById(R.id.scrollLayout);
        final ScrollView chatScroll=((Activity)ctx).findViewById(R.id.chatScroll);
        scrollLayout.addView(receiveView);
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
    private static void setReceiveImageViewFromDb(Context ctx, Chat chat) {
        View receiveView=LayoutInflater.from(ctx).inflate(R.layout.received_image_unit,null);
        ImageView imv = null;
        ImageView viv = null;
        if(chat.getMsgType().equalsIgnoreCase("image"))
        {
            imv=receiveView.findViewById(R.id.chatImage);
            imv.setVisibility(View.VISIBLE);
        }
        else
        {
            viv=receiveView.findViewById(R.id.chatVideo);
            viv.setVisibility(View.VISIBLE);
        }
        final ImageView downloadImage=receiveView.findViewById(R.id.downloadImage);
        downloadImage.setVisibility(View.GONE);
        MyTaskParams myTaskParams=new MyTaskParams(chat,imv,null,ctx,viv);
        LongOperation longOperation=new LongOperation();
        longOperation.execute(myTaskParams);

        LinearLayout scrollLayout=((Activity)ctx).findViewById(R.id.scrollLayout);
        final ScrollView chatScroll=((Activity)ctx).findViewById(R.id.chatScroll);
        scrollLayout.addView(receiveView);
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }
    private static void setReceiveImageView(final Context ctx, final Chat chat) {
        View receiveView=LayoutInflater.from(ctx).inflate(R.layout.received_image_unit,null);
        ImageView imvr = null;
        ImageView viv = null;
        imvr=receiveView.findViewById(R.id.chatImage);
        viv=receiveView.findViewById(R.id.chatVideo);
        if(chat.getMsgType().equalsIgnoreCase("image"))
        {
            imvr.setVisibility(View.VISIBLE);
        }
        else
        {

            viv.setVisibility(View.VISIBLE);
        }
        final ImageView downloadImage=receiveView.findViewById(R.id.downloadImage);
        final TextView downloadPercentage=receiveView.findViewById(R.id.downloadPercentage);
        final ImageView finalImvr = imvr;
        final ImageView finalViv = viv;
        downloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadPercentage.setVisibility(View.VISIBLE);
                downloadImage.setVisibility(View.GONE);
                DownloadImageInBackground downloadImageInBackground=new DownloadImageInBackground();
                MyTaskParams myTaskParams=new MyTaskParams(chat, finalImvr,downloadPercentage,ctx, finalViv);
                downloadImageInBackground.execute(myTaskParams);
            }
        });
        LinearLayout scrollLayout=((Activity)ctx).findViewById(R.id.scrollLayout);
        final ScrollView chatScroll=((Activity)ctx).findViewById(R.id.chatScroll);
        scrollLayout.addView(receiveView);
        chatScroll.post(new Runnable() {
            @Override
            public void run() {
                chatScroll.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }
    public static void setChatsFromDb(String myNumber, String friendNumber, Context ctx) {
        final FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
            List<Chat> chats= ChatTable.getChatListByFilterArgs(ChatStructure.FRIEND,friendNumber,ctx);
            String myType=ChatUtil.getMyType(myNumber,friendNumber);
            for(Chat chat:chats){
                if(chat.getFrom().equalsIgnoreCase(myType))
                    if(chat.getMsgType().equalsIgnoreCase("image")||chat.getMsgType().equalsIgnoreCase("video"))
                        setSentImageView(ctx,chat);
                    else
                    setSendChatView(ctx,chat);
                else
                {
                    if((chat.getStatus().equalsIgnoreCase("unread")||chat.getStatus().equalsIgnoreCase("delivered"))&&!chat.getMsgType().equalsIgnoreCase("image")) {
                        DatabaseReference fireRefs = firebaseDatabase.getReference("readSync").child(friendNumber).child(myNumber).child(String.valueOf(chat.getId()));
                        fireRefs.setValue("read");
                    }

                    System.out.println("status------------------->"+chat.getStatus());
                    if(chat.getMsgType().equalsIgnoreCase("image")||chat.getMsgType().equalsIgnoreCase("video"))
                    {
                        if(chat.getStatus().equalsIgnoreCase("read"))
                        setReceiveImageViewFromDb(ctx,chat);
                        else
                            setReceiveImageView(chatBoxContext,chat);
                    }
                    else
                    {
                        chat.setStatus("read");
                        ChatTable.updateMsgStatus(chat,ctx);
                        setReceiveChatView(ctx,chat);
                    }
                }
            }
    }


    public static void syncNewChat(final String myNumber, final Context ctx, final boolean updateView) {
            if(updateView) {
                List<NewChat> newChats = NewChatTable.getAllNewChatList(ctx);
                for (NewChat newChat : newChats) {
                    setNewChatView(myNumber, newChat);
                }
            }
        final FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
        final DatabaseReference newChatReference=firebaseDatabase.getReference(ChatStructure.TABLE).child(myNumber);
        newChatReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                String friendNumber=dataSnapshot.getKey();
                for(DataSnapshot ds:dataSnapshot.getChildren()){

                    Chat chat=ds.getValue(Chat.class);
                    DatabaseReference fireRefs=firebaseDatabase.getReference("readSync").child(friendNumber).child(myNumber).child(String.valueOf(chat.getId()));

                    NewChat newChat=new NewChat();
                    if(chatBoxContext!=null&&chat.getFriend().equalsIgnoreCase(chatNumber)){
                        if(chat.getMsgType().equalsIgnoreCase("image"))
                            setReceiveImageView(chatBoxContext,chat);
                        else
                        setReceiveChatView(chatBoxContext,chat);
                        newChat.setMsgCount("0");
                        if(chat.getMsgType().equalsIgnoreCase("image")) {
                            fireRefs.setValue("delivered");
                            chat.setStatus("delivered");
                        }
                        else{
                            fireRefs.setValue("read");
                            chat.setStatus("read");
                        }

                    }else
                    {
                        chat.setStatus("delivered");
                        fireRefs.setValue("delivered");
                        newChat.setMsgCount("1");
                    }
                    ChatTable.saveChat(ctx,chat);
                    if(chat.getMsgType().equalsIgnoreCase("image"))
                        newChat.setMsg(chat.getMsgType());
                    else
                    newChat.setMsg(chat.getMsg());
                    newChat.setStatus("receivedChatNoStatus");
                    newChat.setFriend(chat.getFriend());
                    newChat.setDate(chat.getDate());
                    newChat.setTime(chat.getTime());

                    NewChatTable.updateMsgNewChat(myNumber,ctx, newChat);
                }
                newChatReference.child(friendNumber).removeValue();
            // DatabaseReference reference=fireRef.child(friendNumber).child();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

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


        //sent chat delivered status
        final DatabaseReference fireRef=firebaseDatabase.getReference("readSync").child(myNumber);
        fireRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    Chat chat=new Chat();
                String friendNumber=dataSnapshot.getKey();
                    for(DataSnapshot ds:dataSnapshot.getChildren()){
                        long id=Long.parseLong(ds.getKey());
                        chat.setId(String.valueOf(id));
                        chat.setStatus(ds.getValue().toString());
                        ChatTable.updateMsgStatus(chat,ctx);
                        if(chatBoxContext!=null) {
                            TextView msgStatus = ((Activity) chatBoxContext).findViewById((int) id);
                            if(ds.getValue().toString().equalsIgnoreCase("delivered"))
                                msgStatus.setBackgroundResource(R.drawable.icon_delivered);
                            else
                                msgStatus.setBackgroundResource(R.drawable.icon_read);
                        }
                        fireRef.child(dataSnapshot.getKey()).child(ds.getKey()).removeValue();
                    }
                    NewChat newChat=new NewChat();
                newChat.setStatus(chat.getStatus());
                newChat.setMsgCount("0");
                newChat.setFriend(friendNumber);
                NewChatTable.updateMsgNewChat(myNumber,ctx, newChat);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                Chat chat=new Chat();
                String friendNumber=dataSnapshot.getKey();
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    long id=Long.parseLong(ds.getKey());
                    chat.setId(String.valueOf(id));
                    chat.setStatus(ds.getValue().toString());
                    ChatTable.updateMsgStatus(chat,ctx);
                    if(chatBoxContext!=null) {
                        TextView msgStatus = ((Activity) chatBoxContext).findViewById((int) id);
                        if(ds.getValue().toString().equalsIgnoreCase("delivered"))
                            msgStatus.setBackgroundResource(R.drawable.icon_delivered);
                        else
                            msgStatus.setBackgroundResource(R.drawable.icon_read);
                    }
                    fireRef.child(dataSnapshot.getKey()).child(ds.getKey()).removeValue();
                }
                NewChat newChat=new NewChat();
                newChat.setStatus(chat.getStatus());
                newChat.setMsgCount("0");
                newChat.setFriend(friendNumber);
                NewChatTable.updateMsgNewChat(myNumber,ctx, newChat);

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



    public static void setNewChatView(final String myNumber,final NewChat newChat) {
        View newChatView=LayoutInflater.from(chatListContext).inflate(R.layout.chat_list_unit,null);
        TextView newChatContactView=newChatView.findViewById(R.id.newChatContactView);
        TextView newChatMsgView=newChatView.findViewById(R.id.newChatMsgView);
        TextView msgCount=newChatView.findViewById(R.id.msgCount);
        TextView newMsgStatus=newChatView.findViewById(R.id.newMsgStatus);
        String friendName= ContactTable.getContactName(newChat.getFriend(),chatListContext);
        newChatContactView.setText(friendName);
        newChatMsgView.setText(newChat.getMsg());
        if(Integer.parseInt(newChat.getMsgCount())>0)
        msgCount.setText(newChat.getMsgCount());
        else
            msgCount.setVisibility(View.INVISIBLE);
        newChatMsgView.setId((int) Long.parseLong(newChat.getFriend()));
        msgCount.setId((int) Long.parseLong(newChat.getFriend()+"0"));
        newMsgStatus.setId((int) Long.parseLong(newChat.getFriend()+"1"));
        if(!newChat.getStatus().equalsIgnoreCase("receivedChatNoStatus")) {
            newMsgStatus.setVisibility(View.VISIBLE);
            if (newChat.getStatus().equalsIgnoreCase("sent"))
                newMsgStatus.setBackgroundResource(R.drawable.icon_sent);
            else if (newChat.getStatus().equalsIgnoreCase("delivered"))
                newMsgStatus.setBackgroundResource(R.drawable.icon_delivered);
            else if (newChat.getStatus().equalsIgnoreCase("read"))
                newMsgStatus.setBackgroundResource(R.drawable.icon_read);
            else
                newMsgStatus.setBackgroundResource(R.drawable.icon_wait);
        }else
            newMsgStatus.setVisibility(View.GONE);
        newChatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent chatBox=new Intent(chatListContext, ChatBox.class);
                chatBox.putExtra("myNumber",myNumber);
                chatBox.putExtra("friendNumber",newChat.getFriend());
                chatListContext.startActivity(chatBox);
            }
        });
        LinearLayout scrollLayout=((Activity)chatListContext).findViewById(R.id.newChatListView);
        TextView noNewChatMsg=scrollLayout.findViewById(R.id.noNewChatMsg);
        noNewChatMsg.setVisibility(View.GONE);
        scrollLayout.addView(newChatView);
    }

    public static void keepMeOnline(String myNumber, Timer timer){
        FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
        final DatabaseReference onlineReference=firebaseDatabase.getReference("myst-online").child(myNumber).child("status");
        TimerTask timerTask=new TimerTask() {
            @Override
            public void run() {
                String now=simpleTimeDateFormat.format(new Date());
                onlineReference.setValue(now);
            }
        };
        timer.scheduleAtFixedRate(timerTask,0,5000);

    }
    private static class MyTaskParams {
        Chat chat;
        ImageView imv;
        Uri uri;
        TextView downloadPercentage;
        Context context;
        ImageView viv;

        public MyTaskParams(Chat chat, ImageView imv,TextView downloadPercentage,Context context,ImageView viv) {
            this.chat=chat;
            this.imv=imv;
            this.downloadPercentage=downloadPercentage;
            this.context=context;
            this.viv=viv;
        }
    }
    private static final class LongOperation extends AsyncTask<MyTaskParams, MyTaskParams, MyTaskParams[]> {
        @Override
        protected MyTaskParams[] doInBackground(MyTaskParams... myTaskParams) {
            System.out.println("real path------->"+myTaskParams[0].chat.getMsg());
            String exPath= myTaskParams[0].chat.getMsg();
            myTaskParams[0].uri=Uri.parse(exPath);
            return myTaskParams;
        }
        @Override
        protected void onPostExecute(final MyTaskParams[] myTaskParams) {

            if(myTaskParams[0].uri==null)
                myTaskParams[0].imv.setImageResource(R.drawable.image_error);
            else
            {
                if(myTaskParams[0].chat.getMsgType().equalsIgnoreCase("image")) {
                    myTaskParams[0].imv.setVisibility(View.VISIBLE);
                    myTaskParams[0].imv.setImageURI(myTaskParams[0].uri);
                    myTaskParams[0].imv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ChatUtil.getImagePreview(myTaskParams[0].uri, myTaskParams[0].context);
                        }
                    });
                }else{
                    myTaskParams[0].viv.setVisibility(View.VISIBLE);
                    Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(myTaskParams[0].chat.getMsg(), MediaStore.Images.Thumbnails.MINI_KIND);
                    myTaskParams[0].viv.setImageBitmap(bmThumbnail);
                }
            }



        }
    }
    private static final class DownloadImageInBackground extends AsyncTask<MyTaskParams, MyTaskParams, MyTaskParams[]> {
        @Override
        protected MyTaskParams[] doInBackground(final MyTaskParams... myTaskParams) {
            System.out.println("chat id------------? "+myTaskParams[0].chat.getId());
            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            StorageReference ref= storageReference.child("MystImages/"+myTaskParams[0].chat.getId());
            final TextView downloadPercentage=myTaskParams[0].downloadPercentage;
            File localFile = null;
            if(myTaskParams[0].chat.getMsgType().equalsIgnoreCase("image"))
            localFile = new File("/storage/emulated/0/MYST/", myTaskParams[0].chat.getId()+".jpeg");
            else
            localFile = new File("/storage/emulated/0/MYST/", myTaskParams[0].chat.getId() + ".mp4");
            final File finalLocalFile = localFile;

            ref.getFile(finalLocalFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    final Uri uri=Uri.parse(finalLocalFile.getPath());
                    try {

                        //String absPath=ChatUtil.createDirectoryAndSaveFile(uri,String.valueOf(myTaskParams[0].chat.getId()),myTaskParams[0].chat.getMsgType(),finalLocalFile);
                        myTaskParams[0].chat.setStatus("read");
                        myTaskParams[0].chat.setMsg(finalLocalFile.getPath());
                        System.out.println("abs path--------------------> "+finalLocalFile.getPath());
                        ChatTable.updateMsgStatusAndMsg(myTaskParams[0].chat,chatBoxContext);
                        FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
                        DatabaseReference fireRefs=firebaseDatabase.getReference("readSync").child(chatNumber).child(myNumber).child(String.valueOf(myTaskParams[0].chat.getId()));
                        fireRefs.setValue("read");
                        downloadPercentage.setVisibility(View.GONE);
                        if(myTaskParams[0].chat.getMsgType().equalsIgnoreCase("image")) {
                            myTaskParams[0].imv.setImageURI(uri);
                            myTaskParams[0].imv.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (uri != null)
                                        ChatUtil.getImagePreview(uri, myTaskParams[0].context);
                                }
                            });
                        }else{
                            Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(finalLocalFile.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
                            myTaskParams[0].viv.setImageBitmap(bmThumbnail);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    if(downloadPercentage!=null) {
                        int progress = (int) (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                        downloadPercentage.setText(progress+"%");
                    }
                }
            });

            return myTaskParams;
        }

        @Override
        protected void onProgressUpdate(MyTaskParams... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(final MyTaskParams[] myTaskParams) {
            if(myTaskParams[0].chat.getMsgType().equalsIgnoreCase("image")) {
                if (myTaskParams[0].uri != null) {
                    myTaskParams[0].imv.setImageURI(myTaskParams[0].uri);
                    myTaskParams[0].imv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ChatUtil.getImagePreview(myTaskParams[0].uri, myTaskParams[0].context);
                        }
                    });
                } else {
                    myTaskParams[0].imv.setVisibility(View.VISIBLE);
                    myTaskParams[0].imv.setImageResource(R.drawable.image_error);
                }
            }else{
                Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(myTaskParams[0].chat.getMsg(), MediaStore.Images.Thumbnails.MINI_KIND);
                myTaskParams[0].viv.setImageBitmap(bmThumbnail);
            }
        }
    }
}

package com.vikas.myst.cloud;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
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
import com.vikas.myst.VideoPlayer;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Sync {
    public static Context chatListContext =null;
    static SimpleDateFormat simpleTimeDateFormat=new SimpleDateFormat("yyyyMMddHHmmssSSSS", Locale.ENGLISH);
    static SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
    static SimpleDateFormat simpleTimeFormat=new SimpleDateFormat("hh mm a", Locale.ENGLISH);
public static Context chatBoxContext=null;
public static String chatNumber=null;
public static String myNumber=null;
public static boolean marker=false;
public static Button deleteChats=null;
public static List<Chat> markerList=new ArrayList<>();
        public static void saveInFireDbAndSetInViewChat(final String myNumber, final Context ctx, final Chat chat){
            String myType=ChatUtil.getMyType(myNumber,chat.getFriend());
            String crDateAndTime=simpleTimeDateFormat.format(new Date());
            String crDate=simpleDateFormat.format(new Date());
            String crTime=simpleTimeFormat.format(new Date());
            FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
            DatabaseReference databaseReference=firebaseDatabase.getReference(ChatStructure.TABLE).child(chat.getFriend()).child(myNumber);
            DatabaseReference referenceUnit= databaseReference.child(crDateAndTime);
            referenceUnit.keepSynced(true);
            chat.setFrom(myType);
            chat.setStatus("unread");
            chat.setDate(crDate);
            String msgType=chat.getMsgType();
            if(msgType.equalsIgnoreCase("text"))
                setSendChatView(ctx,chat);

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
                        TextView msgStatus = ((Activity) chatBoxContext).findViewById(ChatUtil.getIntegerId(chat.getId())-3);
                        msgStatus.setBackgroundResource(R.drawable.icon_sent);
                    }
                    newChat.setStatus("sent");
                    NewChatTable.updateMsgNewChat(myNumber,ctx, newChat);
                    chat.setStatus("sent");
                    ChatTable.updateMsgStatus(chat,ctx);
                }
            });
        }

    private static void setSentImageView(Context ctx, final Chat chat) {
        View imageView=LayoutInflater.from(ctx).inflate(R.layout.sent_image_unit_chat,null);
        ImageView imv;
        if(chat.getMsgType().equalsIgnoreCase("image"))
            imv=imageView.findViewById(R.id.chatImage);
        else
            imv=imageView.findViewById(R.id.chatVideo);
        TextView msgTime=imageView.findViewById(R.id.msgTime);
        imv.setId(ChatUtil.getIntegerId(chat.getId())-2);
        imv.setVisibility(View.VISIBLE);
        msgTime.setText(chat.getTime());
        ProgressBar imageLoading=imageView.findViewById(R.id.imageLoading);
        TextView imageWait=imageView.findViewById(R.id.imageWait);
        LinearLayout layout= (LinearLayout) imv.getParent().getParent().getParent();
        layout.setId(ChatUtil.getIntegerId(chat.getId())-1);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(marker){
                    if(markerList.contains(chat))
                    {
                        v.setBackgroundColor(0x00000000);
                        markerList.remove(chat);
                        if(markerList.size()==0)
                        {
                            marker=false;
                            deleteChats.setVisibility(View.GONE);
                        }
                    }
                    else
                    {
                        deleteChats.setVisibility(View.VISIBLE);
                        v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                        {
                            markerList.add(chat);
                            deleteChats.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                marker=true;
                v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                markerList.add(chat);
                deleteChats.setVisibility(View.VISIBLE);
                return true;
            }
        });
        /*MyTaskParams myTaskParams=new MyTaskParams(chat,imv,null,ctx,viv);
        LongOperation longOperation=new LongOperation();
        longOperation.execute(myTaskParams);*/

        imageWait.setId(ChatUtil.getIntegerId(chat.getId())-3);

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
    private static void setSendChatView(Context ctx, final Chat chat) {

        View sendView=LayoutInflater.from(ctx).inflate(R.layout.chat_send_unit,null);
        final TextView sendText=sendView.findViewById(R.id.sendText);
        TextView msgStatus=sendView.findViewById(R.id.msgStatus);
        sendText.setText(chat.getMsg());
        TextView msgTime=sendView.findViewById(R.id.msgTime);

        msgTime.setText(chat.getTime());
        msgStatus.setId(ChatUtil.getIntegerId(chat.getId())-3);
        LinearLayout layout= (LinearLayout) sendText.getParent().getParent().getParent();
        layout.setId(ChatUtil.getIntegerId(chat.getId())-1);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(marker){
                    if(markerList.contains(chat))
                    {
                        v.setBackgroundColor(0x00000000);
                        markerList.remove(chat);
                        if(markerList.size()==0)
                        {
                            marker=false;
                            deleteChats.setVisibility(View.GONE);
                        }
                    }
                    else
                    {
                        deleteChats.setVisibility(View.VISIBLE);
                        v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                        {
                            markerList.add(chat);
                            deleteChats.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                marker=true;
                    v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                    markerList.add(chat);
                deleteChats.setVisibility(View.VISIBLE);
                return true;
            }
        });
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
    private static void setReceiveChatView(Context ctx, final Chat chat) {
        View receiveView=LayoutInflater.from(ctx).inflate(R.layout.chat_receive_unit,null);
        TextView receiveText=receiveView.findViewById(R.id.receiveText);
        receiveText.setText(chat.getMsg());
        TextView msgTime=receiveView.findViewById(R.id.msgTime);
        msgTime.setText(chat.getTime());
        LinearLayout layout= (LinearLayout) receiveText.getParent().getParent().getParent();
        layout.setId(ChatUtil.getIntegerId(chat.getId())-1);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(marker){

                    if(markerList.contains(chat))
                    {
                        v.setBackgroundColor(0x00000000);
                        markerList.remove(chat);
                        if(markerList.size()==0)
                        {
                            marker=false;
                            deleteChats.setVisibility(View.GONE);
                        }
                    }
                    else
                    {
                        deleteChats.setVisibility(View.VISIBLE);
                        v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                        {
                            markerList.add(chat);
                            deleteChats.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                marker=true;

                v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                markerList.add(chat);
                deleteChats.setVisibility(View.VISIBLE);
                return true;
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
    private static void setReceiveImageViewFromDb(Context ctx, final Chat chat) {
        View receiveView=LayoutInflater.from(ctx).inflate(R.layout.received_image_unit,null);
        ImageView imv;
        if(chat.getMsgType().equalsIgnoreCase("image"))
            imv=receiveView.findViewById(R.id.chatImage);
        else
            imv=receiveView.findViewById(R.id.chatVideo);
            imv.setVisibility(View.VISIBLE);
            imv.setId(ChatUtil.getIntegerId(chat.getId())-2);
        System.out.println("imags iddd"+(ChatUtil.getIntegerId(chat.getId())-2));
        TextView msgTime=receiveView.findViewById(R.id.msgTime);
        msgTime.setText(chat.getTime());
        LinearLayout layout= (LinearLayout) imv.getParent().getParent().getParent();
        layout.setId(ChatUtil.getIntegerId(chat.getId())-1);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(marker){
                    if(markerList.contains(chat))
                    {
                        v.setBackgroundColor(0x00000000);
                        markerList.remove(chat);
                        if(markerList.size()==0)
                        {
                            marker=false;
                            deleteChats.setVisibility(View.GONE);
                        }
                    }
                    else
                    {
                        deleteChats.setVisibility(View.VISIBLE);
                        v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                        {
                            markerList.add(chat);
                            deleteChats.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                marker=true;
                v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                markerList.add(chat);
                deleteChats.setVisibility(View.VISIBLE);
                return true;
            }
        });
        final ImageView downloadImage=receiveView.findViewById(R.id.downloadImage);
        downloadImage.setVisibility(View.GONE);

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
        ImageView imv;

        if(chat.getMsgType().equalsIgnoreCase("image"))
            imv=receiveView.findViewById(R.id.chatImage);
        else
            imv=receiveView.findViewById(R.id.chatVideo);
        imv.setVisibility(View.VISIBLE);
        TextView msgTime=receiveView.findViewById(R.id.msgTime);
        msgTime.setText(chat.getTime());
        imv.setVisibility(View.VISIBLE);
        final ImageView downloadImage=receiveView.findViewById(R.id.downloadImage);
        final TextView downloadPercentage=receiveView.findViewById(R.id.downloadPercentage);
        final ImageView finalImv = imv;
        LinearLayout layout= (LinearLayout) imv.getParent().getParent().getParent();
        layout.setId(ChatUtil.getIntegerId(chat.getId())-1);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(marker){
                    if(markerList.contains(chat))
                    {
                        v.setBackgroundColor(0x00000000);
                        markerList.remove(chat);
                        if(markerList.size()==0)
                        {
                            marker=false;
                            deleteChats.setVisibility(View.GONE);
                        }
                    }
                    else
                    {
                        deleteChats.setVisibility(View.VISIBLE);
                        v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                        {
                            markerList.add(chat);
                            deleteChats.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                marker=true;

                v.setBackgroundColor(Color.parseColor("#FBC3C3"));
                markerList.add(chat);
                deleteChats.setVisibility(View.VISIBLE);
                return true;
            }
        });
        downloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadPercentage.setVisibility(View.VISIBLE);
                downloadImage.setVisibility(View.GONE);
                DownloadImageInBackground downloadImageInBackground=new DownloadImageInBackground();
                MyTaskParams myTaskParams=new MyTaskParams(chat, finalImv,downloadPercentage,ctx, finalImv);
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
    public static void setChatsFromDb(String myNumber, String friendNumber, final Context ctx) {
        final FirebaseDatabase firebaseDatabase =FirebaseDatabase.getInstance();
            List<Chat> chats= ChatTable.getChatListByFilterArgs(ChatStructure.FRIEND,friendNumber,ctx);
            String myType=ChatUtil.getMyType(myNumber,friendNumber);
            final List<Chat> backgroundChats = new ArrayList<>();

            for(Chat chat:chats){

                if(chat.getFrom().equalsIgnoreCase(myType)) {
                    if (chat.getMsgType().equalsIgnoreCase("image") || chat.getMsgType().equalsIgnoreCase("video")) {
                        setSentImageView(ctx, chat);
                        backgroundChats.add(chat);
                    } else
                        setSendChatView(ctx, chat);
                } else
                {
                    System.out.println("chat11 type "+chat.getMsgType());
                    System.out.println("chat22 msg "+chat.getMsg());
                    System.out.println("chat33 idd "+chat.getId());
                    if((chat.getStatus().equalsIgnoreCase("unread")||chat.getStatus().equalsIgnoreCase("delivered"))&&!chat.getMsgType().equalsIgnoreCase("image")) {
                        DatabaseReference fireRefs = firebaseDatabase.getReference("readSync").child(friendNumber).child(myNumber).child(String.valueOf(chat.getId()));
                        fireRefs.setValue("read");
                    }

                    if(chat.getMsgType().equalsIgnoreCase("image")||chat.getMsgType().equalsIgnoreCase("video"))
                    {
                        if(chat.getStatus().equalsIgnoreCase("read"))
                        {
                            setReceiveImageViewFromDb(ctx,chat);
                            backgroundChats.add(chat);
                        }
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
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ChatsParams chatsParams=new ChatsParams(backgroundChats,ctx);
                    SetChatFromDbInBackground setChatFromDbInBackground=new SetChatFromDbInBackground();
                    setChatFromDbInBackground.execute(chatsParams);
                }
            },200);

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
                        if(chat.getMsgType().equalsIgnoreCase("image")||chat.getMsgType().equalsIgnoreCase("video"))
                            setReceiveImageView(chatBoxContext,chat);
                        else
                        setReceiveChatView(chatBoxContext,chat);
                        newChat.setMsgCount("0");
                        if(chat.getMsgType().equalsIgnoreCase("image")||chat.getMsgType().equalsIgnoreCase("video")) {
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
                    if(chat.getMsgType().equalsIgnoreCase("image")||chat.getMsgType().equalsIgnoreCase("image"))
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
                        chat.setId(ds.getKey());
                        chat.setStatus(ds.getValue().toString());
                        ChatTable.updateMsgStatus(chat,ctx);
                        if(chatBoxContext!=null) {
                            TextView msgStatus = ((Activity) chatBoxContext).findViewById(ChatUtil.getIntegerId(chat.getId())-3);
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

                    chat.setId(ds.getKey());
                    chat.setStatus(ds.getValue().toString());
                    ChatTable.updateMsgStatus(chat,ctx);
                    if(chatBoxContext!=null) {
                        TextView msgStatus = ((Activity) chatBoxContext).findViewById(ChatUtil.getIntegerId(chat.getId())-3);
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
        if(Long.parseLong(newChat.getMsgCount())>0)
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
    private static class ChatsParams{
            List<Chat> chats;
            Context context;

        public ChatsParams(List<Chat> chats, Context ctx) {
            this.chats = chats;
            this.context=ctx;
        }
    }
    private static final class SetChatFromDbInBackground extends AsyncTask<ChatsParams, ChatsParams, ChatsParams[]> {
        @Override
        protected ChatsParams[] doInBackground(ChatsParams... chatsParams) {
            return chatsParams;
        }
        @Override
        protected void onPostExecute(final ChatsParams[] chatsParams) {

            for(Chat chat:chatsParams[0].chats){
                final String exPath= chat.getMsg();
                final Uri uri=Uri.parse(exPath);
                ImageView imv=((Activity) chatsParams[0].context).findViewById(ChatUtil.getIntegerId(chat.getId())-2);
                if(uri==null)
                    imv.setImageResource(R.drawable.image_error);
                else
                {
                    if(chat.getMsgType().equalsIgnoreCase("image")) {
                        imv.setVisibility(View.VISIBLE);
                        imv.setImageURI(uri);
                        imv.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ChatUtil.getImagePreview(uri, chatsParams[0].context);
                            }
                        });
                    }else{
                        imv.setVisibility(View.VISIBLE);
                       if(new File(chat.getMsg()).exists()){
                            Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(chat.getMsg(), MediaStore.Images.Thumbnails.MINI_KIND);
                            imv.setImageBitmap(bmThumbnail);
                            imv.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent videoPlayerIntent=new Intent(chatsParams[0].context, VideoPlayer.class);
                                    videoPlayerIntent.putExtra("filePath",exPath);
                                    (chatsParams[0].context).startActivity(videoPlayerIntent);
                                }
                            });
                        }else {
                            imv.setForeground(null);
                            imv.setBackgroundResource(R.drawable.file_not_found);
                        }

                    }
                }

            }




        }
    }
    private static final class DownloadImageInBackground extends AsyncTask<MyTaskParams, MyTaskParams, MyTaskParams[]> {
        @Override
        protected MyTaskParams[] doInBackground(final MyTaskParams... myTaskParams) {
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
                            myTaskParams[0].viv.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent videoPlayerIntent=new Intent(myTaskParams[0].context, VideoPlayer.class);

                                    videoPlayerIntent.putExtra("filePath",finalLocalFile.getPath());
                                    (myTaskParams[0].context).startActivity(videoPlayerIntent);
                                }
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    int progress = (int) (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    if(downloadPercentage!=null) {

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

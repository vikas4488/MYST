package com.vikas.myst.sql;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.vikas.myst.R;
import com.vikas.myst.bean.NewChat;
import com.vikas.myst.cloud.Sync;

import java.util.ArrayList;
import java.util.List;

public class NewChatTable extends SQLiteOpenHelper {
    private final static String CREATE_TABLE="create table "+ NewChatStructure.TABLE+"(ID INTEGER PRIMARY KEY AUTOINCREMENT,"
            + NewChatStructure.FRIEND+" TEXT,"
            + NewChatStructure.DATE+" TEXT,"
            + NewChatStructure.MSG_COUNT +" TEXT,"
            + NewChatStructure.MSG+" TEXT,"
            + NewChatStructure.STATUS+" TEXT,"
            + NewChatStructure.TIME+" TEXT )";
    private final static String DROP_TABLE="DROP TABLE IF EXISTS "+ NewChatStructure.TABLE;
    public static Context chatListContext=null;

    public NewChatTable(@Nullable Context context) {
        super(context, NewChatStructure.DB_NAME, null, NewChatStructure.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }
    private static SQLiteDatabase getReadableDatabase(Context ctx){
        NewChatTable newChatTable=new NewChatTable(ctx);
        return newChatTable.getReadableDatabase();
    }
    private static SQLiteDatabase getWritableDatabase(Context ctx){
        NewChatTable newChatTable=new NewChatTable(ctx);
        return newChatTable.getWritableDatabase();
    }
    private static ContentValues getNewChatContentValues(NewChat newChat){
        ContentValues contentValues=new ContentValues();
        contentValues.put(NewChatStructure.DATE,newChat.getDate());
        contentValues.put(NewChatStructure.FRIEND,newChat.getFriend());
        contentValues.put(NewChatStructure.MSG_COUNT,newChat.getMsgCount());
        contentValues.put(NewChatStructure.MSG,newChat.getMsg());
        contentValues.put(NewChatStructure.STATUS,newChat.getStatus());
        contentValues.put(NewChatStructure.TIME,newChat.getTime());
        return contentValues;
    }
    private static String[] getAllProjection(){
        String projection[]={
                "ID",
                NewChatStructure.TIME,
                NewChatStructure.FRIEND,
                NewChatStructure.STATUS,
                NewChatStructure.MSG,
                NewChatStructure.MSG_COUNT,
                NewChatStructure.DATE,
        };
        return projection;
    }
    public static int saveNewChat(Context ctx, NewChat newChat){
        SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
        ContentValues contentValues=getNewChatContentValues(newChat);
        return (int) liteDatabase.insert(NewChatStructure.TABLE,null,contentValues);
    }
    public static void resetNewChatToZeroCount(String friendNumber,Context ctx){
        List<NewChat> newChats=getNewChatListByFilterArgs(NewChatStructure.FRIEND,friendNumber,ctx);
        if(newChats.size()>0) {
            String selections = NewChatStructure.FRIEND + " =? ";
            String selectionArgs[] = {friendNumber};
            SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
            ContentValues contentValues=new ContentValues();
            contentValues.put(NewChatStructure.MSG_COUNT,"0");
            liteDatabase.update(NewChatStructure.TABLE,
                    contentValues,
                    selections,
                    selectionArgs);
            LinearLayout scrollLayout = ((Activity) chatListContext).findViewById(R.id.newChatListView);
            TextView msgCount = scrollLayout.findViewById((int) Long.parseLong(friendNumber+"0"));
                msgCount.setVisibility(View.INVISIBLE);
        }
    }
    public static void updateMsgNewChat(String myNumber,Context ctx, NewChat newChat) {

        List<NewChat> newChats=getNewChatListByFilterArgs(NewChatStructure.FRIEND,newChat.getFriend(),ctx);
        if(newChats.size()>0){
            String selections = NewChatStructure.FRIEND + " =? ";
            String selectionArgs[] = {newChat.getFriend()};
            int msgc=0;
            if(newChat.getMsgCount().equalsIgnoreCase("1")) {
                NewChat newC = newChats.get(0);
                msgc = Integer.parseInt(newC.getMsgCount());
                msgc++;
            }
            newChat.setMsgCount(String.valueOf(msgc));

            SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
            ContentValues contentValues=new ContentValues();
            if(!(newChat.getMsg()==null||newChat.getMsg().equalsIgnoreCase("")))
            contentValues.put(NewChatStructure.MSG,newChat.getMsg());
            if(!(newChat.getMsgCount()==null||newChat.getMsgCount().equalsIgnoreCase("")))
            contentValues.put(NewChatStructure.MSG_COUNT,newChat.getMsgCount());
            if(!(newChat.getDate()==null||newChat.getDate().equalsIgnoreCase("")))
            contentValues.put(NewChatStructure.DATE,newChat.getDate());
            if(!(newChat.getTime()==null||newChat.getTime().equalsIgnoreCase("")))
            contentValues.put(NewChatStructure.TIME,newChat.getTime());
            if(!(newChat.getStatus()==null||newChat.getStatus().equalsIgnoreCase("")))
            contentValues.put(NewChatStructure.STATUS,newChat.getStatus());
            liteDatabase.update(NewChatStructure.TABLE,
                    contentValues,
                    selections,
                    selectionArgs);
                LinearLayout scrollLayout = ((Activity) chatListContext).findViewById(R.id.newChatListView);
                TextView newChatMsg = scrollLayout.findViewById((int) Long.parseLong(newChat.getFriend()));
                TextView msgCount = scrollLayout.findViewById((int) Long.parseLong(newChat.getFriend()+"0"));
                TextView newMsgStatus = scrollLayout.findViewById((int) Long.parseLong(newChat.getFriend()+"1"));
                if (newChatMsg != null) {
                    if(!(newChat.getMsg()==null||newChat.getMsg().equalsIgnoreCase("")))
                    newChatMsg.setText(newChat.getMsg());
                    if(msgc>0)
                    {
                        msgCount.setVisibility(View.VISIBLE);
                        msgCount.setText(String.valueOf(msgc));
                    }
                    else
                        msgCount.setVisibility(View.INVISIBLE);
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
            }
        }else{
            saveNewChat(ctx,newChat);
            Sync.setNewChatView(myNumber,newChat);
        }
    }
    public  static List<NewChat> getAllNewChatList(Context ctx){
        return getNewChatListByFilterArgs(null,null,ctx);
    }
    public static List<NewChat> getNewChatListByFilterArgs(String argName,String argValue,Context ctx){
        SQLiteDatabase liteDatabase=getReadableDatabase(ctx);
        String[] projections=getAllProjection();
        String selections=null;
        String selectionArgs[]=null;
        if(!(argName==null&&argValue==null)) {
            selections = argName + " =? ";
            selectionArgs = new String[]{argValue};
        }
        Cursor cr=liteDatabase.query(NewChatStructure.TABLE,
                projections,
                selections,
                selectionArgs,
                null,
                null,
                null);
        return cursorToNewChatList(cr);
    }

    private static List<NewChat> cursorToNewChatList(Cursor cr) {
        List<NewChat> newChats=new ArrayList<>();
        while (cr.moveToNext()){
            NewChat chat=new NewChat();
            chat.setDate(cr.getString(cr.getColumnIndex(NewChatStructure.DATE)));
            chat.setMsgCount(cr.getString(cr.getColumnIndex(NewChatStructure.MSG_COUNT)));
            chat.setMsg(cr.getString(cr.getColumnIndex(NewChatStructure.MSG)));
            chat.setStatus(cr.getString(cr.getColumnIndex(NewChatStructure.STATUS)));
            chat.setTime(cr.getString(cr.getColumnIndex(NewChatStructure.TIME)));
            chat.setFriend(cr.getString(cr.getColumnIndex(NewChatStructure.FRIEND)));
            newChats.add(chat);
        }
        return newChats;
    }
}

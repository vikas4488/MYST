package com.vikas.myst.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.vikas.myst.bean.Chat;

import java.util.ArrayList;
import java.util.List;

public class ChatTable extends SQLiteOpenHelper {
private final static String CREATE_TABLE="create table "+ChatStructure.TABLE
        +"("+ChatStructure.ID+" TEXT,"
        +ChatStructure.FRIEND+" TEXT,"
        +ChatStructure.DATE+" TEXT,"
        +ChatStructure.FROM+" TEXT,"
        +ChatStructure.MSG+" TEXT,"
        +ChatStructure.MSG_TYPE+" TEXT,"
        +ChatStructure.STATUS+" TEXT,"
        +ChatStructure.TIME+" TEXT )";
private final static String DROP_TABLE="DROP TABLE IF EXISTS "+ChatStructure.TABLE;
    public ChatTable(@Nullable Context context) {
        super(context, ChatStructure.DB_NAME, null, ChatStructure.DB_VERSION);
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
        ChatTable chatTable=new ChatTable(ctx);
        return chatTable.getReadableDatabase();
    }
    private static SQLiteDatabase getWritableDatabase(Context ctx){
        ChatTable chatTable=new ChatTable(ctx);
        return chatTable.getWritableDatabase();
    }
    private static ContentValues getChatContentValues(Chat chat){
        ContentValues contentValues=new ContentValues();
        contentValues.put(ChatStructure.ID,chat.getId());
        contentValues.put(ChatStructure.DATE,chat.getDate());
        contentValues.put(ChatStructure.FRIEND,chat.getFriend());
        contentValues.put(ChatStructure.FROM,chat.getFrom());
        contentValues.put(ChatStructure.MSG,chat.getMsg());
        contentValues.put(ChatStructure.MSG_TYPE,chat.getMsgType());
        contentValues.put(ChatStructure.STATUS,chat.getStatus());
        contentValues.put(ChatStructure.TIME,chat.getTime());
        return contentValues;
    }
    private static String[] getAllProjection(){
        String projection[]={
            ChatStructure.ID,
            ChatStructure.TIME,
            ChatStructure.FRIEND,
            ChatStructure.STATUS,
            ChatStructure.MSG,
            ChatStructure.MSG_TYPE,
            ChatStructure.FROM,
            ChatStructure.DATE,
        };
        return projection;
    }
    public static int saveChat(Context ctx, Chat chat){
        SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
        ContentValues contentValues=getChatContentValues(chat);
        return (int) liteDatabase.insert(ChatStructure.TABLE,null,contentValues);
    }
    public  static List<Chat> getAllChatList(Context ctx){
        return getChatListByFilterArgs(null,null,ctx);
    }
    public static List<Chat> getChatListByFilterArgs(String argName,String argValue,Context ctx){
        SQLiteDatabase liteDatabase=getReadableDatabase(ctx);
        String[] projections=getAllProjection();
        String selections=null;
        String selectionArgs[]=null;
        if(!(argName==null&&argValue==null)) {
           selections = argName + " =? ";
            selectionArgs = new String[]{argValue};
        }
        Cursor cr=liteDatabase.query(ChatStructure.TABLE,
                projections,
                selections,
                selectionArgs,
                null,
                null,
                null);
        return cursorToChatList(cr);
    }

    private static List<Chat> cursorToChatList(Cursor cr) {
        List<Chat> chats=new ArrayList<>();
        while (cr.moveToNext()){
            Chat chat=new Chat();
            chat.setId(cr.getString(cr.getColumnIndex(ChatStructure.ID)));
            chat.setDate(cr.getString(cr.getColumnIndex(ChatStructure.DATE)));
            chat.setFrom(cr.getString(cr.getColumnIndex(ChatStructure.FROM)));
            chat.setMsg(cr.getString(cr.getColumnIndex(ChatStructure.MSG)));
            chat.setStatus(cr.getString(cr.getColumnIndex(ChatStructure.STATUS)));
            chat.setMsgType(cr.getString(cr.getColumnIndex(ChatStructure.MSG_TYPE)));
            chat.setTime(cr.getString(cr.getColumnIndex(ChatStructure.TIME)));
            chat.setFriend(cr.getString(cr.getColumnIndex(ChatStructure.FRIEND)));
            chats.add(chat);
        }
        return chats;
    }
    public static void updateMsgStatus(Chat chat, Context ctx) {
        SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
        ContentValues contentValues=new ContentValues();
        contentValues.put(ChatStructure.STATUS,chat.getStatus());
        String selection=ChatStructure.ID+" =?";
        String[] selectionArgs={String.valueOf(chat.getId())};
        liteDatabase.update(ChatStructure.TABLE,
                contentValues,
                selection,
                selectionArgs);
    }
    public static void updateMsgStatusAndMsg(Chat chat, Context ctx) {
        SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
        ContentValues contentValues=new ContentValues();
        contentValues.put(ChatStructure.STATUS,chat.getStatus());
        contentValues.put(ChatStructure.MSG,chat.getMsg());
        String selection=ChatStructure.ID+" =?";
        String[] selectionArgs={String.valueOf(chat.getId())};
        liteDatabase.update(ChatStructure.TABLE,
                contentValues,
                selection,
                selectionArgs);
    }
    public static void deleteSelectedChats(List<String> markerList, Context ctx) {

        SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
        String idlist= TextUtils.join("\",\"",markerList);
        idlist="\""+idlist+"\"";
        String query = "DELETE FROM "+ChatStructure.TABLE
                + " WHERE "+ChatStructure.ID+" IN ("+idlist+")";

        Cursor cursor = liteDatabase.rawQuery(query, null);
        cursor.moveToNext();
    }
}

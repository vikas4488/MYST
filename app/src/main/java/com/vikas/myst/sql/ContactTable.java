package com.vikas.myst.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.vikas.myst.bean.Chat;
import com.vikas.myst.bean.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactTable extends SQLiteOpenHelper {
    private final static String CREATE_TABLE="create table "+ContactStructure.TABLE+" (ID INTEGER PRIMARY KEY AUTOINCREMENT,"
            +ContactStructure.NAME+" TEXT, "
            +ContactStructure.NUMBER+" TEXT, "
            +ContactStructure.ONLINE_STATUS+" TEXT, "
            +ContactStructure.STATUS+" TEXT)";
    private final static String DROP_TABLE="drop table if exists "+ContactStructure.TABLE;
    public ContactTable(@Nullable Context context) {
        super(context, ContactStructure.DB_NAME, null, ContactStructure.DB_VERSION);
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
        ContactTable contactTable=new ContactTable(ctx);
        return contactTable.getReadableDatabase();
    }
    private static SQLiteDatabase getWritableDatabase(Context ctx){
        ContactTable contactTable=new ContactTable(ctx);
        return contactTable.getWritableDatabase();
    }
    private static ContentValues getContentValues(Contact contact){
        ContentValues contentValues=new ContentValues();
        contentValues.put(ContactStructure.NAME,contact.getName());
        contentValues.put(ContactStructure.NUMBER,contact.getNumber());
        contentValues.put(ContactStructure.STATUS,contact.getStatus());
        return contentValues;
    }
    private static String[] getAllProjection(){
        String projection[]={
                "ID",
                ContactStructure.NAME,
                ContactStructure.NUMBER,
                ContactStructure.STATUS,
                ContactStructure.ONLINE_STATUS
        };
        return projection;
    }
    public static void saveContactList(List<Contact> contacts,Context ctx){
        ContactTable contactTable=new ContactTable(ctx);
        SQLiteDatabase liteDatabase=contactTable.getWritableDatabase();
        liteDatabase.execSQL(DROP_TABLE);
        liteDatabase.execSQL(CREATE_TABLE);
        for(Contact contact:contacts){
           int ss= saveContact(contact,ctx);
        }
    }
    public static int saveContact(Contact contact,Context ctx){
        SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
        ContentValues contentValues=getContentValues(contact);
        return (int) liteDatabase.insert(ContactStructure.TABLE,null,contentValues);
    }
    public static String getContactName(String friendNumber, Context ctx) {
        String name=friendNumber;
        SQLiteDatabase liteDatabase=getReadableDatabase(ctx);
        String projections[]=getAllProjection();
        String selection=ContactStructure.NUMBER+" =?";
        String selectionArgs[]={friendNumber};
        Cursor cr=liteDatabase.query(ContactStructure.TABLE,
                getAllProjection(),
                selection,
                selectionArgs,
                null,
                null,
                null);
        if(cr.moveToNext())
            name=cr.getString(cr.getColumnIndex(ContactStructure.NAME));

        return name;
    }
    public static List<Contact> getAllContact(Context ctx){
        String[] projections=getAllProjection();
        SQLiteDatabase liteDatabase=getReadableDatabase(ctx);
        String orderBy=ContactStructure.STATUS+" DESC,"+ContactStructure.NAME+" COLLATE NOCASE ASC";
        Cursor cr=liteDatabase.query(ContactStructure.TABLE,
                projections,
                null,
                null,
                null,
                null,
                orderBy);
        return cursorToContactList(cr);
    }

    private static List<Contact> cursorToContactList(Cursor cr) {
        List<Contact> contacts=new ArrayList<>();
        while (cr.moveToNext()){
            Contact contact=new Contact();
            contact.setName(cr.getString(cr.getColumnIndex(ContactStructure.NAME)));
            contact.setNumber(cr.getString(cr.getColumnIndex(ContactStructure.NUMBER)));
            contact.setStatus(cr.getString(cr.getColumnIndex(ContactStructure.STATUS)));
            contacts.add(contact);
        }
        return contacts;
    }


    private static void updateContact(String contentValueArg, String contentValue,Context ctx,String arg,String argVal) {
        SQLiteDatabase liteDatabase=getWritableDatabase(ctx);
        ContentValues contentValues=new ContentValues();
        contentValues.put(contentValueArg,contentValue);
        String selection=arg+" =? ";
        String selectionArg[]={argVal};
        liteDatabase.update(ContactStructure.TABLE,
                contentValues,
                selection,
                selectionArg);
    }
}

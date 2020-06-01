package com.vikas.myst.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.vikas.myst.bean.Contact;
import com.vikas.myst.bean.Image;

import java.util.List;

public class ImageTable extends SQLiteOpenHelper {
    private final static String CREATE_TABLE="create table "+ImageStructure.TABLE+" ("+ImageStructure.ID+" INTEGER PRIMARY KEY,"
            +ImageStructure.NAME+" TEXT, "
            +ImageStructure.IMAGE_DATA+" BLOB, "
            +ImageStructure.STATUS+" TEXT)";
    private final static String DROP_TABLE="drop table if exists "+ImageStructure.TABLE;
    public ImageTable(@Nullable Context context) {
        super(context, ImageStructure.DB_NAME, null, ImageStructure.DB_VERSION);
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
        contentValues.put(ImageStructure.NAME,contact.getName());
        contentValues.put(ImageStructure.IMAGE_DATA,contact.getNumber());
        contentValues.put(ImageStructure.STATUS,contact.getStatus());
        return contentValues;
    }
    private static String[] getAllProjection(){
        String projection[]={
                ImageStructure.NAME,
                ImageStructure.IMAGE_DATA,
                ImageStructure.STATUS,
                ImageStructure.ID
        };
        return projection;
    }

}

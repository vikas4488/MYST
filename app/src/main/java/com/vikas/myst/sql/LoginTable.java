package com.vikas.myst.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginTable extends SQLiteOpenHelper {
    private final static String CREATE_TABLE="create table "+LoginStructure.TABLE_NAME+"(ID INTEGER PRIMARY KEY AUTOINCREMENT ,"+
    LoginStructure.DEVICE_UNIQUE_ID + " TEXT," +
    LoginStructure.USER_TYPE + " TEXT," +
    LoginStructure.PLACE + " TEXT," +
    LoginStructure.MOBILE_NO + " TEXT," +
    LoginStructure.LOGIN_STATUS + " TEXT)";
    private static final String DROP_TABLE=" DROP TABLE IF EXISTS "+LoginStructure.TABLE_NAME;

    public LoginTable(@Nullable Context context) {
        super(context, LoginStructure.DB_Name, null, LoginStructure.DB_VERSION);
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



    public static String checkLoginStatus(String android_id, Context ct) {
        String myNumber="unknown_login_check_fail";
        LoginTable loginTable=new LoginTable(ct);
        SQLiteDatabase db = loginTable.getReadableDatabase();

        String[] projection = {
                "ID",
                LoginStructure.DEVICE_UNIQUE_ID,
                LoginStructure.MOBILE_NO,
                LoginStructure.LOGIN_STATUS,
                LoginStructure.USER_TYPE,
                LoginStructure.PLACE
        };
        String selection = LoginStructure.DEVICE_UNIQUE_ID + "=?";
        String[] selectionArgs = {android_id};
        Cursor cursor = db.query(
                LoginStructure.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null               // The sort order
        );
        try {
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.getString(cursor.getColumnIndex(LoginStructure.LOGIN_STATUS)).equalsIgnoreCase("loggedIn"))
                {
                    myNumber = cursor.getString(cursor.getColumnIndex(LoginStructure.MOBILE_NO));
                    loginCheckUpFromCloud(myNumber,ct);
                }
                else
                    myNumber = "logged_out";
            }
        }catch(Exception e){
            myNumber="device_not_verified";
        }
        cursor.close();
        return myNumber;
    }
    public static void saveLoginDetails(String android_id,String mobileNo, Context ct){
        LoginTable loginTable=new LoginTable(ct);
        SQLiteDatabase db = loginTable.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LoginStructure.DEVICE_UNIQUE_ID, android_id);
        values.put(LoginStructure.LOGIN_STATUS, "loggedIn");
        values.put(LoginStructure.MOBILE_NO, mobileNo);
        values.put(LoginStructure.USER_TYPE, "view");
        values.put(LoginStructure.PLACE, "place");
        db.insert(LoginStructure.TABLE_NAME, null, values);
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference h2oReference=firebaseDatabase.getReference("Myst-customer");
        DatabaseReference customerTableFire = h2oReference.child(mobileNo);
        customerTableFire.child(LoginStructure.DEVICE_UNIQUE_ID).setValue(android_id);
        customerTableFire.child(LoginStructure.LOGIN_STATUS).setValue("loggedIn");
        customerTableFire.child(LoginStructure.DEVICE_UNIQUE_ID).setValue(android_id);
        customerTableFire.child(LoginStructure.USER_TYPE).setValue("view");
        loginCheckUpFromCloud(mobileNo,ct);
    }
    public static String getAccessType(Context ctx){
        String view="none";
        LoginTable loginTable=new LoginTable(ctx);
        SQLiteDatabase db = loginTable.getWritableDatabase();
        String[] projection = {
                LoginStructure.USER_TYPE,
        };
        Cursor cursor = db.query(
                LoginStructure.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
        if(cursor.moveToNext())
            view=cursor.getString(cursor.getColumnIndex(LoginStructure.USER_TYPE));
        return view;
    }
    public static int updateView(String mobileNo, ContentValues contentValues, Context ctx){
        LoginTable loginTable=new LoginTable(ctx);
        SQLiteDatabase db = loginTable.getWritableDatabase();
        String selections = LoginStructure.MOBILE_NO+" =?";
        String[] SelectionsArgs = {String.valueOf(mobileNo)};
        return db.update(LoginStructure.TABLE_NAME,
                contentValues,
                selections,
                SelectionsArgs
        );
    }
    public static void loginCheckUpFromCloud(final String mobileNo, final Context ctx){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference customerTableFire = firebaseDatabase.getReference("H2O-customer").child(mobileNo);
        customerTableFire.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(dataSnapshot.getKey(),dataSnapshot.getValue().toString());
                updateView(mobileNo,contentValues,ctx);
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
}

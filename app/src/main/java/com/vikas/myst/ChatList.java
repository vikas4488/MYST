package com.vikas.myst;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vikas.myst.bean.Contact;
import com.vikas.myst.cloud.Sync;
import com.vikas.myst.custom.ChatRecyclerViewAdapter;
import com.vikas.myst.custom.CustomLoading;
import com.vikas.myst.sql.ChatStructure;
import com.vikas.myst.sql.ContactTable;
import com.vikas.myst.sql.NewChatTable;
import com.vikas.myst.util.ChatUtil;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class ChatList extends AppCompatActivity implements ChatRecyclerViewAdapter.ItemClickListener {
    private static final int CONTACT_PERMISSION_CODE = 100;
    Context ctx;
    RecyclerView contactListView;
    LinearLayout newChatListView;
    Intent chatBox;
    String myNumber;
    ImageView openContact;
    LinearLayout contactLayout;
    Timer timer;
    ChatRecyclerViewAdapter recyclerViewAdapter;
    Button refreshContact;
    CustomLoading customLoading;
    private static final int REQUEST_EXTERNAL_STORAGE = 111;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    static SimpleDateFormat simpleTimeDateFormat=new SimpleDateFormat("ddMMyyyyHHmmss", Locale.ENGLISH);
    @SuppressLint("ClickableViewAccessibility")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        refreshContact=findViewById(R.id.refreshContact);
        customLoading=findViewById(R.id.customLoading);
        ctx=this;
        chatBox=new Intent(getBaseContext(),ChatBox.class);
        contactListView=findViewById(R.id.contactListView);
        newChatListView=findViewById(R.id.newChatListView);
        Intent myIntent=getIntent();
        myNumber=myIntent.getStringExtra("myNumber");
        openContact=findViewById(R.id.openContact);
        contactLayout=findViewById(R.id.contactLayout);
        Sync.chatListContext =ctx;
        Sync.myNumber=myNumber;
        checkPermission();
        NewChatTable.chatListContext =ctx;
        timer=new Timer();

        Sync.keepMeOnline(myNumber,timer);
    }



    @Override
    public void onBackPressed() {
        if(contactLayout.getVisibility()==View.VISIBLE) {
            TranslateAnimation animate = new TranslateAnimation(0,contactLayout.getWidth(),0,0);
            animate.setDuration(300);
            animate.setFillAfter(true);
            contactLayout.startAnimation(animate);
            contactLayout.setVisibility(View.GONE);
            openContact.setVisibility(View.VISIBLE);
        }else{
            finish();
            timer.cancel();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void initiateIt() {
        refreshContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customLoading.setVisibility(View.VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fetchContactFromPhone();
                    }
                },100);

            }
        });
        openContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TranslateAnimation animate = new TranslateAnimation(contactLayout.getWidth(),0,0,0);
                animate.setDuration(300);
                animate.setFillAfter(true);
                contactLayout.startAnimation(animate);
                contactLayout.setVisibility(View.VISIBLE);
                openContact.setVisibility(View.GONE);
            }
        });

        Sync.syncNewChat(myNumber,ctx,true);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startContactList();
            }
        },100);
    }
    private void startContactList(){
        customLoading.setVisibility(View.GONE);

        if(contactLayout.getVisibility()==View.VISIBLE)
            openContact.setVisibility(View.GONE);
        else
            openContact.setVisibility(View.VISIBLE);
        List<Contact> contacts=ContactTable.getAllContact(ctx);
        if(!contacts.isEmpty()){
            contactListView.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewAdapter = new ChatRecyclerViewAdapter(this, contacts);
            recyclerViewAdapter.setClickListener(this);
            contactListView.setAdapter(recyclerViewAdapter);
        }else{
            fetchContactFromPhone();
        }
    }
    @Override
    public void onItemClick(View view, int position) {
           Contact contact=recyclerViewAdapter.getItem(position);
            if(contact.getStatus().equalsIgnoreCase("myst")) {
                chatBox.putExtra("myNumber", myNumber);
                chatBox.putExtra("friendNumber", contact.getNumber());
                startActivity(chatBox);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        openContact.setVisibility(View.VISIBLE);
                        contactLayout.clearAnimation();
                        contactLayout.setVisibility(View.GONE);
                    }
                }, 200);
            }
    }
    private void fetchContactFromPhone(){
        final List<Contact> contactList=new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((cur != null ? cur.getCount() : 0) > 0) {

            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));
                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phoneNo=phoneNo.replace(" ","").replace("-","");
                        if(phoneNo.length()>10){
                            int trimLength=phoneNo.length()-10;
                            phoneNo=phoneNo.substring(trimLength);
                        }
                        Contact contact=new Contact(name,phoneNo,"active");
                        if(!contactList.contains(contact)&& !(phoneNo.length() <10) && !phoneNo.equalsIgnoreCase(myNumber))
                        {
                            contactList.add(contact);
                        }
                    }

                }
            }
        }
        FirebaseDatabase firebaseDatabase=FirebaseDatabase.getInstance();
        DatabaseReference databaseReference=firebaseDatabase.getReference("Myst-customer");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (Contact ct:contactList) {
                    if (dataSnapshot.hasChild(ct.getNumber()))
                    {
                        int index=contactList.indexOf(ct);
                        ct.setStatus("myst");
                        contactList.set(index,ct);
                    }

                }
                Contact contact=new Contact("Developer","7008529481","myst");
                if(contactList.contains(contact))
                {
                    int index=contactList.indexOf(contact);
                    contactList.set(index,contact);
                }else
                contactList.add(contact);
                ContactTable.saveContactList(contactList,ctx);
                startContactList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }























    @RequiresApi(api = Build.VERSION_CODES.P)
    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if ((checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED )) {
                initiateIt();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.READ_CONTACTS,}, CONTACT_PERMISSION_CODE);
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        if (requestCode == CONTACT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verifyStoragePermissions((Activity) ctx);
            }
            else {
                AlertDialog ad=ChatUtil.getCustomAlert("CONTACT PERMISSION","PERMISSION DENIED CAN'T PROCEED FURTHER",ctx);
                ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
            }
        }

        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initiateIt();
            }
            else {
                AlertDialog ad=ChatUtil.getCustomAlert("Permission Denied","please allow permission to send video and images",ctx);
                ad.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                });
            }
        }


    }
    public void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(activity,PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);

    }


}

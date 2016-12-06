package com.example.messagingapp;

import android.app.Activity;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * Created by Matthew on 11/16/2016.
 */
public class MessageActivity extends ListActivity {
    ArrayList<Message> messages;
    private String sender;
    MessageAdapter adapter;
    EditText text;
    static Random rand = new Random();

    TextView nameText;

    private BroadcastReceiver receiver;
    NewMessageService mService;
    Binder mBinder;
    boolean isBound = false;
    private String user;
    private String groupName;



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_activity);

        Button send = (Button)findViewById(R.id.sendButton);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get user text
                EditText e = (EditText) findViewById(R.id.msgText);
                Log.i("message_activity", e.getText().toString());
                sendMessage(new Message(
                        e.getText().toString(), true,
                        new SimpleDateFormat("HH:mm:ss").format(new Date())
                ));
                e.setText("");
            }
        });

                //final String user = getSharedPreferences("name",0).getString("name","");


        groupName = this.getIntent().getStringExtra("name");
        user = this.getIntent().getStringExtra("client");
        Log.i("message_activity", user);
        String FILENAME = groupName;
        String string = "hello world!";

        nameText = (TextView)findViewById(R.id.nameText);
        nameText.setText(groupName);

        Intent intent = new Intent(this, NewMessageService.class);

        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);




        this.setTitle("SPORTS");
        messages = new ArrayList<Message>();

        messages.add(new Message("Hello", false, new SimpleDateFormat("HH:mm:ss").format(new Date())));
        messages.add(new Message("Hi!", true, new SimpleDateFormat("HH:mm:ss").format(new Date())));

        messages.add(new Message("Wassup??", false, new SimpleDateFormat("HH:mm:ss").format(new Date())));


        adapter = new MessageAdapter(this, messages);
        setListAdapter(adapter);

        // register receiver
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra("message");
                Log.i("message_activity", msg);

                if(msg.startsWith("+MSG,")){
                    msg = msg.substring("+MSG,".length());

                    // "+OK,MSG,{sender},{group_name},{msg}
                    String[] m = msg.split(",");
                    String sender = m[0];
                    String group = m[1];
                    msg = m[3];

                    if(!sender.equals(user)){ // will handle own message when sending it
                        Log.i("message_activity", "adding message");
                        addNewMessage(new Message(msg, false,
                                new SimpleDateFormat("HH:mm:ss").format(new Date())));
                    }

                }
            }
        };

        registerReceiver(receiver, new IntentFilter(user));


        // internal storage of converations
        /*File file = new File(FILENAME);
        if(!file.exists())
        {
            try {
                file.createNewFile();
            }catch(IOException e){
                // can't create file
            }
            // write code for saving data to the file
        }

        try {
            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(string.getBytes());
            fos.close();
        }catch(FileNotFoundException e){
            // should not throw

        }catch(IOException e){

        }*/
    }


    // will handle sending messages and be set for 'Enter' button
    private void sendMessage(Message m){
        Log.i("message_activity", "sending message to server");
        // handle networking
        String msg = m.getMessage();
        mService.sendMessage(msg,user,groupName);


        // update UI
        addNewMessage(m);
    }

    private void addNewMessage(Message m)
    {
        messages.add(m);
        adapter.notifyDataSetChanged();
        getListView().setSelection(messages.size()-1);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName _name, IBinder service) {
            NewMessageService.MyBinder myBinder = (NewMessageService.MyBinder) service;
            mService = myBinder.getService();
            isBound = true;
            Log.i("main", "Service connected");

        }
    };
}

package com.example.messagingapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;


import java.util.ArrayList;

import com.example.messagingapp.NewMessageService.MyBinder;


public class MainActivity extends Activity {
    ArrayList<String> conversations = new ArrayList<String>();
    private BroadcastReceiver receiver;
    NewMessageService mService;
    Binder mBinder;
    private boolean isBound = false;
    private String mClientName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("main Activity", "testing");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClientName = getIntent().getStringExtra("name");
        SharedPreferences settings = getSharedPreferences("name", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("name", mClientName);

        // Commit the edits!
        editor.commit();

        final ListView listView = (ListView) findViewById(R.id.listview);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , conversations);

        // start networking service, add intent filter
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra("message");
                Log.i("main_activity", msg);

                if(msg.startsWith("+OK,LIST,MYGROUPS:")) {
                    conversations.clear();
                    msg = msg.substring("+OK,LIST,MYGROUPS:".length());
                    String[] names = msg.split(",");
                    for (String name : names) {
                        //name=name.substring(1);
                        //name=name.split("\\(")[0];
                        conversations.add(name);
                    }
                }else if(msg.startsWith("+OK,JOIN,")){
                    msg = msg.substring("+OK,JOIN,".length());
                    msg=msg.substring(1);
                    msg=msg.split("\\(")[0];
                    conversations.add(msg);
                }
                adapter.notifyDataSetChanged();
            }
        };

        registerReceiver(receiver, new IntentFilter("main"));

        // starts the service, binds main activity to it
        Intent intent = new Intent(this, NewMessageService.class);

        startService(intent);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);




        //conversations.add("hello");
        //conversations.add("matt");
        //conversations.add("sports");

        final EditText text = (EditText)findViewById(R.id.editText1);
        Button start = (Button)findViewById(R.id.button1);

        start.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String groupName = text.getText().toString();


                Log.i("main", mClientName);

                // client will attempt to join group.
                // if it doesn't exist, it will be created by server
                mService.joinGroup(groupName);

            }
        });

        //conversations.add("JOHN");

        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // will spawn activity for conversation

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                String name = (String) listView.getItemAtPosition(position);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :" + itemPosition + "  ListItem : " + name, Toast.LENGTH_SHORT)
                        .show();

                Intent intent = new Intent(getApplicationContext(), MessageActivity.class);


                intent.putExtra("name", name);
                intent.putExtra("client", mClientName);
                startActivity(intent);

            }

        });


    }

    // handler for messages from the service
    public static class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;

        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            //mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyBinder myBinder = (MyBinder) service;
            mService = myBinder.getService();
            isBound = true;
            Log.i("main", "Service connected");

            // add client name to server
            try {
                Thread.sleep(1500);
            }catch(InterruptedException e){}
            mService.addName(mClientName);
        }
    };

}
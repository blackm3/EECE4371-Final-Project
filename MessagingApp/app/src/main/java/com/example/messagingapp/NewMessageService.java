package com.example.messagingapp;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class NewMessageService extends Service {
    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();

    // server to connect to
    protected static final int GROUPCAST_PORT = 20000;
    protected static final String GROUPCAST_SERVER = "10.32.143.216"; // public IP address

    // networking
    Socket socket = null;
    BufferedReader in = null;
    PrintWriter out = null;
    boolean connected = false;
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "in onBind");
        return mBinder;
    }

    public void onCreate() {
        // only called once - starting a thread that will act as 'receiver' from server

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "In onStartCommand");
        if(!connected) {
            new Thread(new Runnable() {
                public void run() {

                    try {
                        try {
                            connected = false;
                            socket = new Socket(GROUPCAST_SERVER, GROUPCAST_PORT);
                            Log.i(LOG_TAG, "Socket created");
                            in = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream()));
                            out = new PrintWriter(socket.getOutputStream());

                            connected = true;
                            Log.i(LOG_TAG, "Input and output streams ready");

                        } catch (UnknownHostException e1) {

                        } catch (IOException e1) {

                            try {
                                if (out != null) {
                                    out.close();
                                }
                                if (socket != null) {
                                    socket.close();
                                }
                            } catch (IOException ignored) {
                            }
                        }



                        while (true) {

                            String msg = in.readLine();
                            Log.i(LOG_TAG,msg);
                            if (msg == null) { // other side closed the
                                // connection
                                break;
                            }else{
                                // will broadcast the received message
                                String name = msg.split(",")[1];
                                Intent broadcastIntent = new Intent("main");
                                broadcastIntent.putExtra("message", msg);
                                sendBroadcast(broadcastIntent);
                            }

                        }
                    } catch (UnknownHostException e1) {
                        Log.i(LOG_TAG, "UnknownHostException in receive task");
                    } catch (IOException e1) {
                        Log.i(LOG_TAG, "IOException in receive task");
                    } finally {
                        connected = false;
                        try {
                            if (out != null)
                                out.close();
                            if (socket != null)
                                socket.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }).start();
        }
        return START_REDELIVER_INTENT;
    }


    /** public method for clients **/
    public void sendMessage(String message, String sender, String groupName){
        // called using Binder
        String msg = "MSG,@" + groupName + "," + sender + "," + message;
        Log.i(LOG_TAG, msg);
        send(msg);
    }

    public void addName(String name){
        send("NAME,"+name);
    }

    public void getGroups(){
        send("LIST,MYGROUPS");
    }

    public void joinGroup(String groupName){
        // groupname must start with @
        send("JOIN,@" + groupName);
    }



    // method that sends messages to server
    private void send(String msg){
        if (!connected) {
            Log.i(LOG_TAG, "can't send: not connected");

        }else {
            new AsyncTask<String, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(String... msg) {
                    Log.i(LOG_TAG, "sending: " + msg[0]);
                    out.println(msg[0]);
                    return out.checkError();
                }

                @Override
                protected void onPostExecute(Boolean error) {
                    if (!error) {
                        Toast.makeText(getApplicationContext(),
                                "Message sent to server", Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Error sending message to server",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg);
        }
    }
    public class MyBinder extends Binder {
        NewMessageService getService() {
            return NewMessageService.this;
        }
    }
}

package com.example.messagingapp;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Matthew on 11/18/2016.
 */
public class MessageService extends IntentService {
    // Messenger for sending messages to activity
    private Messenger messageHandler;

    // TAG for logging
    private static final String TAG = "MessageService";

    // server to connect to
    protected static final int GROUPCAST_PORT = 20000;
    protected static final String GROUPCAST_SERVER = "35.162.91.229"; // public IP address

    // networking
    Socket socket = null;
    BufferedReader in = null;
    PrintWriter out = null;
    boolean connected = false;

    public MessageService() {
        super(MessageService.class.getName());
    }
    @Override
    protected void onHandleIntent(Intent workIntent) {
       // networking here
        Log.i("Service", "Service started!");

        Bundle extras = workIntent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");

        if(connect()){
            receive();
        }

    }

    public void sendMessage(String msg) {
        Message message = Message.obtain();
        switch (msg) {


        }
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void receive(){

        // always looking for messages from the server
        Log.i(TAG, "Receive task started");
        try {
            while (true) {

                String msg = in.readLine();

                if (msg == null) { // other side closed the
                    // connection
                    break;
                }else{
                    sendMessage(msg);
                }

            }

        } catch (UnknownHostException e1) {
            Log.i(TAG, "UnknownHostException in receive task");
        } catch (IOException e1) {
            Log.i(TAG, "IOException in receive task");
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
        Log.i(TAG, "Receive task finished");
        return;
    }

    private boolean connect(){
        Log.i(TAG, "Connect task started");
        try {
            connected = false;
            socket = new Socket(GROUPCAST_SERVER, GROUPCAST_PORT);
            Log.i(TAG, "Socket created");
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());

            connected = true;
            Log.i(TAG, "Input and output streams ready");

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
        Log.i(TAG, "Connect task finished");

        return connected;
    }

}

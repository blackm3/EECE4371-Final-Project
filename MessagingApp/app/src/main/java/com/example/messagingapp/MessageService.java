package com.example.messagingapp;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by Matthew on 11/18/2016.
 */
public class MessageService extends IntentService {

    public MessageService() {
        super(MessageService.class.getName());
    }
    @Override
    protected void onHandleIntent(Intent workIntent) {
       // networking here
    }
}

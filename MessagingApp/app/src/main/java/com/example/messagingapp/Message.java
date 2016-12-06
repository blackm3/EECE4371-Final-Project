package com.example.messagingapp;

import java.util.Date;

/**
 * Created by Matthew on 12/3/2016.
 */
public class Message {
    private String message;
    private boolean isMine;
    private String time;

    public Message(String message, boolean isMine, String time){
        this.message = message;
        this.isMine = isMine;
        this.time = time;
    }

    public String getMessage(){
        return this.message;
    }

    public boolean getOwner(){
        return this.isMine;
    }

    public String getTime(){
        return this.time;
    }
}

package com.example.messagingapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by Matthew on 12/5/2016.
 */
public class LoginActivity extends Activity{

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        final EditText text = (EditText)findViewById(R.id.usernameText);
        Button login = (Button)findViewById(R.id.loginButton);

        login.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String name = text.getText().toString();
                Log.i("main", name);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);


                intent.putExtra("name", name);
                startActivity(intent);

            }
        });
    }

}

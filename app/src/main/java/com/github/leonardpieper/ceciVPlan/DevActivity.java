package com.github.leonardpieper.ceciVPlan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.messaging.FirebaseMessaging;

public class DevActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dev);

        final EditText versionET = (EditText)findViewById(R.id.devVersionET);

        Button versionBtn  =(Button)findViewById(R.id.devVersionBtn);
        versionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String PREFS_NAME = "MyPrefsFile";
                final String PREF_VERSION_CODE_KEY = "version_code";
                final int DOESNT_EXIST = -1;

                // Get saved version code
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);


                prefs.edit().putInt(PREF_VERSION_CODE_KEY, Integer.parseInt(versionET.getText().toString())).commit();
            }
        });

        Button webViewBtn = (Button)findViewById(R.id.launchWebMode);
        webViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(DevActivity.this, DevWebViewActivity.class);
                startActivity(i);
            }
        });

        final Button displayNameBtn = (Button)findViewById(R.id.devDisplayNameBtn);
        displayNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText displayEt = (EditText)findViewById(R.id.devDisplayNameEt);
                String name = displayEt.getText().toString();

//                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
//                        .setDisplayName(name)
//                        .build();
//
//                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//                user.updateProfile(profileUpdates);
                name = name.replace(" ", "%20");
                name = name.toLowerCase();
                FirebaseMessaging.getInstance().subscribeToTopic(name);
            }
        });
    }
}

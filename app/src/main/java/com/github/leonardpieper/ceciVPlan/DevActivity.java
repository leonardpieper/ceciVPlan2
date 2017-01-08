package com.github.leonardpieper.ceciVPlan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
    }
}

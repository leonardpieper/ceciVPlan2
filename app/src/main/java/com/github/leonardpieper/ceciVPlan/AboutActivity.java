package com.github.leonardpieper.ceciVPlan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.github.leonardpieper.ceciVPlan.tools.EasterEgg;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.FirebaseApp;

import org.json.JSONException;
import org.w3c.dom.Text;

public class AboutActivity extends AppCompatActivity {

    private TextView tvLicenses;
    private int easterEggCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvLicenses = (TextView)findViewById(R.id.tv_licenses);
        showLicenses();
    }

    private void showLicenses(){
        String licenses = GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(AboutActivity.this);
        tvLicenses.setText(licenses);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void aboutEasterEgg(View view) {
        easterEggCounter++;
        if(easterEggCounter>=10){
            EasterEgg easterEgg = new EasterEgg(AboutActivity.this);
            try {
                easterEgg.addEmoji("\uD83D\uDD25 ", "das Feuer");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            easterEggCounter = 0;
        }
    }
}

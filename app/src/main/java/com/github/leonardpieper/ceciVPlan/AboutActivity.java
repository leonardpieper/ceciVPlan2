package com.github.leonardpieper.ceciVPlan;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.firebase.FirebaseApp;

import org.w3c.dom.Text;

public class AboutActivity extends AppCompatActivity {

    private TextView tvLicenses;

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
}

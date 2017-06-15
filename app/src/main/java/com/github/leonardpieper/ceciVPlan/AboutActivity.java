package com.github.leonardpieper.ceciVPlan;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
        String licenses = new StringBuilder()
                .append("Clans Floating ActionButton: <br />")
                .append("Copyright 2015 Dmytro Tarianyk<br />")
                .append("<br />")
                .append("Licensed under the Apache License, Version 2.0 (the \"License\");<br />")
                .append("you may not use this file except in compliance with the License.<br />")
                .append("You may obtain a copy of the License at<br />")
                .append("<br />")
                .append("   http://www.apache.org/licenses/LICENSE-2.0<br />")
                .append("<br />")
                .append("Unless required by applicable law or agreed to in writing, software<br />")
                .append("distributed under the License is distributed on an \"AS IS\" BASIS,<br />")
                .append("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br />")
                .append("See the License for the specific language governing permissions and<br />")
                .append("limitations under the License. <br />")
                .append("<a href='https://github.com/Clans/FloatingActionButton/blob/master/LICENSE'>Ganze Lizenz</a>")
                .append("<br /><br /><br />")
                .append("CountryCodePickerProject: <br />")
                .append("Copyright (C) 2016 Harsh Bhakta<br />")
                .append("<br />")
                .append("Licensed under the Apache License, Version 2.0 (the \"License\");<br />")
                .append("you may not use this file except in compliance with the License.<br />")
                .append("You may obtain a copy of the License at<br />")
                .append("<br />")
                .append("   http://www.apache.org/licenses/LICENSE-2.0<br />")
                .append("<br />")
                .append("Unless required by applicable law or agreed to in writing, software<br />")
                .append("distributed under the License is distributed on an \"AS IS\" BASIS,<br />")
                .append("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br />")
                .append("See the License for the specific language governing permissions and<br />")
                .append("limitations under the License. <br />")
                .append("<a href='https://github.com/hbb20/CountryCodePickerProject/blob/master/License.txt'>Ganze Lizenz</a>")
                .append("<br /><br /><br />")
                .append("Google API License:<br />")
                .append("<a href='https://developers.google.com/terms/'>https://developers.google.com/terms/</a>")
                .toString();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvLicenses.setText(Html.fromHtml(licenses, Html.FROM_HTML_MODE_COMPACT));
            tvLicenses.setMovementMethod(LinkMovementMethod.getInstance());
        }

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

package com.github.leonardpieper.ceciVPlan;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class KlausurenActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "KlausurenActivity";

    private FirebaseAuth mAuth;

    private TableLayout tableKEF;
    private TableLayout tableKQ1;
    private TableLayout tableKQ2;

    private String oldDatum = "99.99";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_klausuren);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        tableKEF = (TableLayout)findViewById(R.id.vPlanTableLayoutKEF);
        tableKQ1 = (TableLayout)findViewById(R.id.vPlanTableLayoutKQ1);
        tableKQ2 = (TableLayout)findViewById(R.id.vPlanTableLayoutKQ2);

        mAuth = FirebaseAuth.getInstance();

        final KlausurenCrawler crawler = new KlausurenCrawler();
        crawler.addEventListener(new CrawlerFinishListener() {
            @Override
            public void handleCrawlFinishEvent(EventObject e) {
                List<String> htmls = crawler.allHtmls;
                String[] stufen = new String[]{"KlausurQ2", "KlausurQ1", "KlausurEF"};

                if(!htmls.isEmpty()) {
                    for (int j = 0; j < htmls.size(); j++) {
                        String html = htmls.get(j);
                        String stufe = stufen[j];

                        String[] lines = html.split("\\r?\\n");
                        List data = new ArrayList();
                        for (int i = 0; i < lines.length; i++) {
                            if (i > 0) {
                                if (lines[i - 1].contains("<TD align=center>") && lines[i + 1].contains("</TD>")) {
//                            System.out.println(lines[i]);
                                    if (lines[i].contains("<B>") || lines[i].contains("</B>")) {
                                        lines[i] = lines[i].replace("<B>", "");
                                        lines[i] = lines[i].replace("</B>", "");
                                    }
                                    data.add(lines[i]);
                                } else if (lines[i].contains("<TD align=center>") && lines[i].contains("</TD>")) {
                                    lines[i] = lines[i].replace("<TD align=center>", "");
                                    lines[i] = lines[i].replace("</TD>", "");
                                    lines[i] = lines[i].replace("&nbsp;", "");
                                    data.add(lines[i]);
                                }
                            }
                        }
                        JSONArray jaStufe = dataToJSON(data);
                        List l;
                        try {
                            l = toList(jaStufe);
                            uploadFirebase(stufe, l);
                            System.out.println(l.toString());
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }

                        System.out.println(data.toString());
                    }
                }
            }
        });
        crawler.execute("");
        if(mAuth.getCurrentUser()!=null){
            getFBData();
        }else{
            mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if(firebaseAuth.getCurrentUser()!=null) {
                        getFBData();
                    }
                }
            });
        }
    }

    public JSONArray dataToJSON(List data){
        JSONArray root = new JSONArray();
        try {
            for (int i = 0; i < data.size(); i=i+8) {
                JSONObject jsonAdd = new JSONObject();
                for(int j = 0; j<=7; j++) {
                    switch (j) {
                        case 0:
                            jsonAdd.put("Tag", data.get(i+j));
                            break;
                        case 1:
                            jsonAdd.put("Datum", data.get(i+j));
                            break;
                        case 2:
                            jsonAdd.put("Klasse(n)", data.get(i+j));
                            break;
                        case 3:
                            jsonAdd.put("Stunde", data.get(i+j));
                            break;
                        case 4:
                            jsonAdd.put("Fach", data.get(i+j));
                            break;
                        case 5:
                            jsonAdd.put("Vertreter", data.get(i+j));
                            break;
                        case 6:
                            jsonAdd.put("Raum", data.get(i+j));
                            break;
                        case 7:
                            jsonAdd.put("Vertretungs-Text", data.get(i+j));
                            break;
                    }
                }
                root.put(jsonAdd);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return root;
    }

    public static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap();
        Iterator keys = object.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, fromJson(object.get(key)));
        }
        return map;
    }

    public static List toList(JSONArray array) throws JSONException {
        List list = new ArrayList();
        for (int i = 0; i < array.length(); i++) {
            list.add(fromJson(array.get(i)));
        }
        return list;
    }

    private static Object fromJson(Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return toMap((JSONObject) json);
        } else if (json instanceof JSONArray) {
            return toList((JSONArray) json);
        } else {
            return json;
        }
    }

    public void uploadFirebase(String jahrgang, List data){
        FirebaseDatabase mData = FirebaseDatabase.getInstance();

        if(mAuth.getCurrentUser()!=null){
            DatabaseReference mRef = mData.getReference("vPlan/" + jahrgang);
            mRef.setValue(data);
        }
    }

    public void getFBData(){
        FirebaseDatabase mData = FirebaseDatabase.getInstance();
        DatabaseReference mRef = mData.getReference("vPlan");
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                tableKEF.removeAllViews();
                tableKQ1.removeAllViews();
                tableKQ2.removeAllViews();
                for(DataSnapshot stufenSnapshot: dataSnapshot.getChildren()) {
                    String stufe = stufenSnapshot.getKey();
                    for (DataSnapshot vPlanSnapshot : stufenSnapshot.getChildren()) {
                        String datum = vPlanSnapshot.child("Datum").getValue(String.class);

                        if (!oldDatum.equals(datum) && !(datum == null || datum.contains("Datum"))) {
                            String tag = vPlanSnapshot.child("Tag").getValue(String.class);
                            addDateTableRow(stufe, tag, datum);
                        }

                        if (!(datum == null || datum.contains("Datum"))) {
                            String fach = vPlanSnapshot.child("Fach").getValue(String.class);
                            String stunde = vPlanSnapshot.child("Stunde").getValue(String.class);
                            String vertreter = vPlanSnapshot.child("Vertreter").getValue(String.class);
                            String raum = vPlanSnapshot.child("Raum").getValue(String.class);
                            String text = vPlanSnapshot.child("Vertretungs-Text").getValue(String.class);

                            addTableRow(stufe, fach, stunde, vertreter, raum, text);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void addTableRow(String stufe, String fach, String stunde, String lehrer, String raum, String text) {
        TableRow row = new TableRow(this);

        TextView lesson = new TextView(this);
        TextView time = new TextView(this);
        TextView tutor = new TextView(this);
        TextView room = new TextView(this);
        TextView extra = new TextView(this);

        lesson.setPadding(5,15,15,15);
        time.setPadding(5,15,15,15);
        tutor.setPadding(5,15,15,15);
        room.setPadding(5,15,15,15);
        extra.setPadding(5,15,15,15);

        lesson.setBackgroundResource(R.drawable.cell_shape);
        time.setBackgroundResource(R.drawable.cell_shape);
        tutor.setBackgroundResource(R.drawable.cell_shape);
        room.setBackgroundResource(R.drawable.cell_shape);
        extra.setBackgroundResource(R.drawable.cell_shape);

        TableRow.LayoutParams trparams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);

        lesson.setLayoutParams(trparams);
        time.setLayoutParams(trparams);
        tutor.setLayoutParams(trparams);
        room.setLayoutParams(trparams);
        extra.setLayoutParams(trparams);

        lesson.setText(fach);
        time.setText(stunde);
        tutor.setText(lehrer);
        room.setText(raum);
        extra.setText(text);

        row.addView(lesson);
        row.addView(time);
        row.addView(tutor);
        row.addView(room);
        row.addView(extra);

        switch(stufe){
            case "KlausurEF":
                tableKEF.addView(row);
                break;
            case "KlausurQ1":
                tableKQ1.addView(row);
                break;
            case "KlausurQ2":
                tableKQ2.addView(row);
                break;
            default:
                Log.d(TAG, "Stufe ist nicht erkannt!");
        }
    }

    public void addDateTableRow(String stufe, String tag, String datum){
        TableRow row = new TableRow(this);

        TextView day = new TextView(this);
        TextView date = new TextView(this);

        day.setPadding(15,15,5,15);
        date.setPadding(15,15,5,15);

        day.setTextSize(20);
        date.setTextSize(20);

        TableRow.LayoutParams twoColParam = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                android.widget.TableRow.LayoutParams.WRAP_CONTENT);
        twoColParam.span = 2;

        TableRow.LayoutParams threeColParam = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                android.widget.TableRow.LayoutParams.WRAP_CONTENT);
        threeColParam.span = 3;

        day.setLayoutParams(twoColParam);
        date.setLayoutParams(threeColParam);

        day.setText(tag);
        date.setText(datum);

        row.addView(day);
        row.addView(date);

        switch(stufe){
            case "KlausurEF":
                tableKEF.addView(row);
                break;
            case "KlausurQ1":
                tableKQ1.addView(row);
                break;
            case "KlausurQ2":
                tableKQ2.addView(row);
                break;
            default:
                Log.d(TAG, "Stufe ist nicht erkannt!");
        }

        oldDatum=datum;
    }



    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch(id){
            case R.id.nav_dashboard:
                break;
            case R.id.nav_vplan:
                Intent vIntent = new Intent(this, VPlanActivity.class);
                startActivity(vIntent);
                break;
            case R.id.nav_kurse:
                Intent kuIntent = new Intent(this, VPlanActivity.class);
                startActivity(kuIntent);
                break;
            case R.id.nav_klausuren:
                Intent kIntent = new Intent(this, KlausurenActivity.class);
                startActivity(kIntent);
                break;
            case R.id.nav_settings:
                Intent sIntent = new Intent(this, SettingsActivity.class);
                startActivity(sIntent);
                break;
        }
//        if(id == R.id.nav_dashboard){
//
//        }
//        else if (id == R.id.nav_vplan) {
//            Intent intent = new Intent(this, VPlanActivity.class);
//            startActivity(intent);
//        } else if (id == R.id.nav_settings) {
//            Intent intent = new Intent(this, SettingsActivity.class);
//            startActivity(intent);
//        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.nav_dashboard){

        }
        else if (id == R.id.nav_vplan) {
            Intent intent = new Intent(this, VPlanActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

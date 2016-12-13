package com.github.leonardpieper.ceciVPlan;

import android.content.Intent;
import android.net.Uri;
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
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

//import com.google.android.gms.appindexing.Action;
//import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VPlanActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

//    /**
//     * ATTENTION: This was auto-generated to implement the App Indexing API.
//     * See https://g.co/AppIndexing/AndroidStudio for more information.
//     */
//    private GoogleApiClient client;

    private static final String TAG = "VPlanActivity";

    private TableLayout tableLayout;

    private com.github.clans.fab.FloatingActionMenu fabyear;
    private com.github.clans.fab.FloatingActionButton fabEF;
    private com.github.clans.fab.FloatingActionButton fabQ1;
    private com.github.clans.fab.FloatingActionButton fabQ2;

    private LinearLayout tablePlaceholder;
    private TableLayout tableEF;
    private TableLayout tableQ1;
    private TableLayout tableQ2;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference conditionRef;

    private String oldDatum = "99.99";
    private String currentStufe = "EF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vplan);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Vertretungsplan");

        mAuth = FirebaseAuth.getInstance();

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        fabyear = (com.github.clans.fab.FloatingActionMenu)findViewById(R.id.fab_year);
        fabEF = (com.github.clans.fab.FloatingActionButton)findViewById(R.id.fabEF);
        fabQ1 = (com.github.clans.fab.FloatingActionButton)findViewById(R.id.fabQ1);
        fabQ2 = (com.github.clans.fab.FloatingActionButton)findViewById(R.id.fabQ2);





        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
//        // ATTENTION: This was auto-generated to implement the App Indexing API.
//        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

//        tableLayout = (TableLayout)findViewById(R.id.vPlanTableLayout);
        tableEF = (TableLayout)findViewById(R.id.vPlanTableLayoutEF);
        tableQ1 = (TableLayout)findViewById(R.id.vPlanTableLayoutQ1);
        tableQ2 = (TableLayout)findViewById(R.id.vPlanTableLayoutQ2);

//        String s = mAuth.getCurrentUser().getUid();
//        Log.d("", s);

        final VPlanCrawler crawler = new VPlanCrawler();
        crawler.addEventListener(new CrawlerFinishListener() {
            @Override
            public void handleCrawlFinishEvent(EventObject e) {
                List<String> htmls = crawler.allHtmls;
//                html = html.replaceAll("\r\n", " ");

//                final Pattern pattern = Pattern.compile("<TD.*>.*\\n(.+?)\\n.*</TD>");
//                final Matcher matcher = pattern.matcher(html);
//                while (matcher.find()){
//                    System.out.println(matcher.group(1));
//                }
                String[] stufen = new String[]{"Q2", "Q1", "EF"};
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
            conditionRef = mRootRef.child("vPlan");
            getFBData();
        }else{
            mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if(firebaseAuth.getCurrentUser()!=null) {
                        conditionRef = mRootRef.child("vPlan");
                        getFBData();
                    }
                }
            });
        }


//        mAuthListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                FirebaseUser user = firebaseAuth.getCurrentUser();
//                if(user != null){

//                }
//            }
//        };

//        mAuth.addAuthStateListener(mAuthListener);
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

    public void getFBData(){
        conditionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                tableEF.removeAllViews();
                tableQ1.removeAllViews();
                tableQ2.removeAllViews();
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

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.nav_dashboard){
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        else if (id == R.id.nav_vplan) {
//            Intent intent = new Intent(this, VPlanActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
            case "EF":
                tableEF.addView(row);
                break;
            case "Q1":
                tableQ1.addView(row);
                break;
            case "Q2":
                tableQ2.addView(row);
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
            case "EF":
                tableEF.addView(row);
                break;
            case "Q1":
                tableQ1.addView(row);
                break;
            case "Q2":
                tableQ2.addView(row);
                break;
            default:
                Log.d(TAG, "Stufe ist nicht erkannt!");
        }

        oldDatum=datum;
    }

    public void changeStufe(View v){
        switch(v.getId()){
            case(R.id.fabEF):
                tableEF.setVisibility(View.VISIBLE);
                tableQ1.setVisibility(View.GONE);
                tableQ2.setVisibility(View.GONE);
                break;
            case(R.id.fabQ1):
                tableEF.setVisibility(View.GONE);
                tableQ1.setVisibility(View.VISIBLE);
                tableQ2.setVisibility(View.GONE);
                break;
            case(R.id.fabQ2):
                tableEF.setVisibility(View.GONE);
                tableQ1.setVisibility(View.GONE);
                tableQ2.setVisibility(View.VISIBLE);
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

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client.connect();
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "VPlan Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://com.github.leonardpieper.ceciVPlan/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        Action viewAction = Action.newAction(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "VPlan Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://com.github.leonardpieper.ceciVPlan/http/host/path")
//        );
//        AppIndex.AppIndexApi.end(client, viewAction);
//        client.disconnect();
    }
}

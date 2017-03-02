package com.github.leonardpieper.ceciVPlan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private CardView cvKurse;

    private TableLayout tlToday;
    private TableLayout tlTomorrow;
    private TextView tvErr;


    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
//    DatabaseReference conditionRef = mRootRef.child("vPlan");

    TextView mTextview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Dashboard");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

//        final TextView vPlanToday = (TextView)findViewById(R.id.vPlanToday);
//        final TextView vPlanTomorrow = (TextView)findViewById(R.id.vPlanTomorrow);
        tlToday = (TableLayout)findViewById(R.id.vPlanToday);
        tlTomorrow = (TableLayout)findViewById(R.id.vPlanTomorrow);
        tvErr = (TextView)findViewById(R.id.tvErr_Main);

        cvKurse = (CardView)findViewById(R.id.cv_Kurse);

        mAuth = FirebaseAuth.getInstance();


        checkFirstRun();

        setDailyAlarm();
        displayKurse();

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();


        System.out.println(FirebaseInstanceId.getInstance().getToken());
        FirebaseMessaging.getInstance().subscribeToTopic("D__EFa");
//        FirebaseCrash.report(new Exception("My first Android non-fatal error"));




        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    if(mFirebaseRemoteConfig.getBoolean("vplan_enabled")) {
                        DatabaseReference conditionRef = mRootRef.child("vPlan");

                        String stufe = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("jahrgang", "EF");
                        final DatabaseReference stufenRef = conditionRef.child(stufe);

                        Log.d("FirebaseAuth", "onAuthStateChanged:signed_in:" + user.getUid());

                        stufenRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                tlToday.removeAllViews();
                                tlTomorrow.removeAllViews();


                                Vertretungsplan vPlan = new Vertretungsplan();
                                String sToday = "";
                                String sTomorrow = "";
                                for (DataSnapshot vPlanSnapshot : dataSnapshot.getChildren()) {
                                    String datum = vPlanSnapshot.child("Datum").getValue(String.class);
                                    if (!(datum == null || datum.contains("Datum"))) {

                                        String fach = vPlanSnapshot.child("Fach").getValue(String.class);
                                        String stunde = vPlanSnapshot.child("Stunde").getValue(String.class);
                                        String vertreter = vPlanSnapshot.child("Vertreter").getValue(String.class);
                                        String raum = vPlanSnapshot.child("Raum").getValue(String.class);
                                        String text = vPlanSnapshot.child("Vertretungs-Text").getValue(String.class);

                                        boolean bToday = vPlan.isToday(datum);
                                        boolean bTomorrow = vPlan.isTomorrow(datum);

                                        if (bToday) {
                                            addTableRow("today", fach, stunde, vertreter, raum, text);
//                                        sToday = sToday + "\n" + fach + " " + stunde + " " + vertreter + " " + raum + " " + text;
//                                        vPlanToday.setText(sToday);
                                        } else if (bTomorrow) {
                                            addTableRow("tomorrow", fach, stunde, vertreter, raum, text);
//                                        sTomorrow = sTomorrow + "\n" + fach + " " + stunde + " " + vertreter + " " + raum + " " + text;
//                                        vPlanTomorrow.setText(sTomorrow);
                                        }
                                    }

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.d("MainActivity", databaseError.getMessage());
                            }
                        });
                    }

                }else{
                    Log.d("FirebaseAuth", "onAuthStateChanged:signed_out");
                    tvErr.setText("Du bist nicht angemeldet!");
                    tvErr.setVisibility(View.VISIBLE);
                }
            }
        };

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();

        mFirebaseRemoteConfig.activateFetched();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetch(21600)//21600 = 6 Hours in seconds
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                        } else {
                            Toast.makeText(MainActivity.this, "Fetch Failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

//        mAuth.signInWithEmailAndPassword("g@g.co", "123456");
    }


    public void addTableRow(String tag, String fach, String stunde, String lehrer, String raum, String text) {
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

        switch(tag){
            case "today":
                tlToday.addView(row);
                break;
            case "tomorrow":
                tlTomorrow.addView(row);
                break;
            default:
                Log.d(TAG, "Stufe ist nicht erkannt!");
        }
    }

    private void displayKurse(){
        final KursCache kursCache = new KursCache(MainActivity.this);
        final LinearLayout ll = (LinearLayout) findViewById(R.id.main_kurse_display);

        long cachedTime = kursCache.getCacheTime();
        long currMill = System.currentTimeMillis();

        if(cachedTime == -1 || cachedTime + 604800000 < currMill) {

            if (mAuth.getCurrentUser() != null) {
                mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        kursCache.newCache();

                        if(dataSnapshot.getValue()!=null) {
                            cvKurse.setVisibility(View.VISIBLE);
                        }

                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            final String kurs = childSnapshot.child("name").getValue(String.class);

                            kursCache.addCache(kurs);

                            LinearLayout column = makeKursIcon(kurs);
                            ll.addView(column);
                            ll.setPadding(0, 0, 0, 0);

//                        mRootRef.child("Kurse").child(kurs).addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(DataSnapshot dataSnapshot) {

                        }

//                            @Override
//                            public void onCancelled(DatabaseError databaseError) {
//
//                            }
//                        });
//                    }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        }else {

            JSONObject root = kursCache.getCache();
            JSONArray kurse = null;

            try {
                kurse = root.getJSONArray("kurse");
                if(kurse!=null) {
                    cvKurse.setVisibility(View.VISIBLE);
                }
                for(int i = 0; i<kurse.length(); i++){
                    String title = kurse.getString(i);

                    LinearLayout column = makeKursIcon(title);
                    ll.addView(column);
                    ll.setPadding(0, 0, 0, 0);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }



        }
    }

    private LinearLayout makeKursIcon(final String title){


        final float scale = MainActivity.this.getResources().getDisplayMetrics().density;
        int width = (int) (50 * scale + 0.5f);
        int height = (int) (50 * scale + 0.5f);

        LinearLayout column = new LinearLayout(MainActivity.this);
        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                width,
                height
        );
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        column.setLayoutParams(columnParams);
        column.setPadding(32, 0, 32, 0);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, KursActivity.class);
                intent.putExtra("name", title);
                startActivity(intent);
            }
        });

        ImageView iv = new ImageView(MainActivity.this);
        iv.setBackgroundResource(getResourceIdByName(title));
        iv.setScaleType(ImageView.ScaleType.FIT_START);
        iv.setAdjustViewBounds(true);
        iv.setLayoutParams(ivParams);

        TextView tv = new TextView(MainActivity.this);
        tv.setText(title);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getResources().getColor(R.color.colorAccent));
        tv.setLayoutParams(tvParams);

        column.addView(iv);
        column.addView(tv);

        return column;

    }

    private int getResourceIdByName(String name){
        String arr[] = name.split(" ", 2);
        String fach = arr[0];
        fach=fach.toLowerCase();
        switch (fach){
            case "bi":
                return R.drawable.ic_biologie_bug;
            case "ch":
                return  R.drawable.ic_chemie_poppet;
            case "d":
                return R.drawable.ic_deutsch;
            case "e":
                return R.drawable.ic_englisch;
            case "ek":
                return R.drawable.ic_erdkunde_landscape;
            case "el":
                return R.drawable.ic_ernahrungslehre_dining;
            case "ew":
                return R.drawable.ic_erziehungswissenschaften_child;
            case "f":
                return R.drawable.ic_franzosisch;
            case "ge":
                return R.drawable.ic_geschichte_buste;
            case "if":
                return R.drawable.ic_informatik_computer;
            case "ku":
                return R.drawable.ic_kunst_art;
            case "m":
                return R.drawable.ic_mathe_calc;
            case "mu":
                return R.drawable.ic_musik_note;
            case "pl":
                return R.drawable.ic_philosophie_scroll;
            case "ph":
                return R.drawable.ic_physik_lightbulb;
            case "sw":
                return R.drawable.ic_sozialwissenschaften_group;
            case "s":
                return R.drawable.ic_spanisch;
            case "sp":
                return R.drawable.ic_sport_run;
            default:
                return R.drawable.ic_school_black_24dp;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
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
//        }else if( id == R.id.action_vPlan){
//            Intent intent = new Intent(this, VPlanActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//            startActivity(intent);
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
                Intent kuIntent = new Intent(this, KurseActivity.class);
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
            case R.id.nav_devSettings:
                Intent devIntent = new Intent(this, DevActivity.class);
                startActivity(devIntent);
                break;
            case R.id.nav_signup:
                Intent signIntent = new Intent(this, SignUpActivity.class);
                startActivity(signIntent);
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setDailyAlarm(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND, 0);
//        to avoid firing the alarm immediately
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Intent intent1 = new Intent(MainActivity.this, AlarmReceiver.class);
        intent1.putExtra("dailyAlarm", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0,intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) MainActivity.this.getSystemService(MainActivity.this.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private void checkFirstRun() {

        final String PREFS_NAME = "MyPrefsFile";
        final String PREF_VERSION_CODE_KEY = "version_code";
        final int DOESNT_EXIST = -1;

        // Get current version code
        int currentVersionCode = 0;
        try {
            currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            // handle exception
            e.printStackTrace();
            return;
        }
        // Get saved version code
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);

        // Check for first run or upgrade
        if (currentVersionCode == savedVersionCode) {
            // This is just a normal run
            final int[] currVersion = {0};
            final String[] verInfo = {""};
            final int finalCurrentVersionCode = currentVersionCode;
            mRootRef.child("data").child("info").child("currVers")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currVersion[0] = dataSnapshot.getValue(Integer.class);

                    if(currVersion[0] > finalCurrentVersionCode){
                        mRootRef.child("data").child("info").child("verInfo")
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        verInfo[0] = dataSnapshot.getValue(String.class);
                                        if(!verInfo[0].equals("null")) {
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            builder.setMessage(verInfo[0])
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            //NOTHING
                                                        }
                                                    });
                                            builder.show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                                    }
                                });
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                }
            });


            return;
        } else if (savedVersionCode == DOESNT_EXIST) {
            Intent signIntent = new Intent(this, SignUpActivity.class);
            signIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(signIntent);

        } else if (currentVersionCode > savedVersionCode) {
            // TODO This is an upgrade
            if(mAuth.getCurrentUser()!=null){
                if(!mAuth.getCurrentUser().isAnonymous()){
                    if(mAuth.getCurrentUser().getEmail().contains("ceci@example.com")){
                        Intent signAIntent = new Intent(this, SignUpAnonymActivity.class);
                        startActivity(signAIntent);
                    }
                }
            }
        }
        // Update the shared preferences with the current version code
        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).commit();
    }
}

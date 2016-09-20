package com.github.leonardpieper.ceciVPlan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private TableLayout tlToday;
    private TableLayout tlTomorrow;


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

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });

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

        mAuth = FirebaseAuth.getInstance();

        checkFirstRun();

        setDailyAlarm();

//        FirebaseCrash.report(new Exception("My first Android non-fatal error"));




        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    DatabaseReference conditionRef = mRootRef.child("users").child(user.getUid());

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
                            for(DataSnapshot vPlanSnapshot: dataSnapshot.getChildren()){
                                String datum = vPlanSnapshot.child("Datum").getValue(String.class);
                                if(!(datum == null || datum.contains("Datum"))) {

                                    String fach = vPlanSnapshot.child("Fach").getValue(String.class);
                                    String stunde = vPlanSnapshot.child("Stunde").getValue(String.class);
                                    String vertreter = vPlanSnapshot.child("Vertreter").getValue(String.class);
                                    String raum = vPlanSnapshot.child("Raum").getValue(String.class);
                                    String text = vPlanSnapshot.child("Vertretungs-Text").getValue(String.class);

                                    boolean bToday = vPlan.isToday(datum);
                                    boolean bTomorrow = vPlan.isTomorrow(datum);

                                    if(bToday){
                                        addTableRow("today", fach, stunde, vertreter, raum, text);
//                                        sToday = sToday + "\n" + fach + " " + stunde + " " + vertreter + " " + raum + " " + text;
//                                        vPlanToday.setText(sToday);
                                    }else if(bTomorrow){
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

                }else{
                    Log.d("FirebaseAuth", "onAuthStateChanged:signed_out");
                }
            }
        };

//        mTextview = (TextView)findViewById(R.id.mTextView);
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

        if(id == R.id.nav_dashboard){

        }
        else if (id == R.id.nav_vplan) {
            Intent intent = new Intent(this, VPlanActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_signup){
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
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
            // TODO This is a new install

        } else if (currentVersionCode > savedVersionCode) {
            // TODO This is an upgrade
        }
        // Update the shared preferences with the current version code
        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).commit();
    }
}

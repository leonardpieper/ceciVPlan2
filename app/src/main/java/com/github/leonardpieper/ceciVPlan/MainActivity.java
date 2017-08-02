package com.github.leonardpieper.ceciVPlan;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.fragments.KurseFragment;
import com.github.leonardpieper.ceciVPlan.fragments.MainFragment;
import com.github.leonardpieper.ceciVPlan.fragments.VPlanFragment;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    public static boolean isInForeground;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        mRootRef = MyDatabaseUtil.getDatabase().getReference();
        mAuth = FirebaseAuth.getInstance();


        if(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("pref_vplan_etpref_user", "unknowm").equals("unknowm")) {
            migrateVPlanToSharedPref();
        }

        MainFragment mainFragment = new MainFragment();
        this.getFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, mainFragment)
                .commit();

        if(mAuth.getCurrentUser()==null){
            Intent signIntent = new Intent(this, SignUpAnonActivity.class);
            startActivity(signIntent);
        }


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

        final float scale = getResources().getDisplayMetrics().density;
        int marginDPs = (int) (5 * scale + 0.5f);
        int paddingDPs = (int) (10 * scale + 0.5f);

        LinearLayout llNeu = (LinearLayout) navigationView.getMenu().findItem(R.id.nav_kurse).getActionView();
        TextView textViewNeu = new TextView(this);
        textViewNeu.setText("Neu");
        textViewNeu.setTextColor(Color.WHITE);
        textViewNeu.setAllCaps(true);
        textViewNeu.setGravity(Gravity.CENTER_VERTICAL);
        textViewNeu.setBackgroundResource(R.drawable.round_corner);
        textViewNeu.setPadding(paddingDPs,0,paddingDPs,0);

        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        llNeu.setLayoutParams(llp);
        llNeu.setGravity(Gravity.CENTER);

        llNeu.addView(textViewNeu);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();


    }

    private void migrateVPlanToSharedPref() {
        if (mAuth.getCurrentUser() != null) {
            DatabaseReference vplanRef = mRootRef.child("Users")
                    .child(mAuth.getCurrentUser().getUid())
                    .child("vPlan");
            vplanRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("pref_vplan_etpref_user", dataSnapshot.child("uname").getValue(String.class));
                    editor.putString("pref_vplan_etpref_pwd", dataSnapshot.child("pwd").getValue(String.class));
                    editor.commit();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    protected void onResume() {
        isInForeground = true;
        super.onResume();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
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
    protected void onStop() {
        super.onStop();
        isInForeground=false;
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch(id){
            case R.id.nav_dashboard:
                MainFragment mainFragment = new MainFragment();
                this.getFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, mainFragment)
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.nav_vplan:
                VPlanFragment vPlanFragment = new VPlanFragment();
                this.getFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, vPlanFragment)
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.nav_kurse:
                KurseFragment kurseFragment = new KurseFragment();
                this.getFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, kurseFragment)
                        .addToBackStack(null)
                        .commit();
                break;
            case R.id.nav_klausuren:
                Intent kIntent = new Intent(this, KlausurenActivity.class);
                startActivity(kIntent);
                break;
            case R.id.nav_settings:
                Intent sIntent = new Intent(this, SettingsActivity2.class);
                startActivity(sIntent);
                break;
            case R.id.nav_devSettings:
                Intent devIntent = new Intent(this, DevActivity.class);
                startActivity(devIntent);
                break;
            case R.id.nav_about:
                Intent aboutIntent = new Intent(this, AboutActivity.class);
                startActivity(aboutIntent);
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

//    private void checkFirstRun() {
//
//        final String PREFS_NAME = "MyPrefsFile";
//        final String PREF_VERSION_CODE_KEY = "version_code";
//        final int DOESNT_EXIST = -1;
//
//        // Get current version code
//        int currentVersionCode = 0;
//        try {
//            currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
//        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
//            // handle exception
//            e.printStackTrace();
//            return;
//        }
//        // Get saved version code
//        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        int savedVersionCode = prefs.getInt(PREF_VERSION_CODE_KEY, DOESNT_EXIST);
//
//        // Check for first run or upgrade
//        if (currentVersionCode == savedVersionCode) {
//            // This is just a normal run
//            final int[] currVersion = {0};
//            final String[] verInfo = {""};
//            final int finalCurrentVersionCode = currentVersionCode;
//            mRootRef.child("data").child("info").child("currVers")
//            .addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(DataSnapshot dataSnapshot) {
//                    currVersion[0] = dataSnapshot.getValue(Integer.class);
//
//                    if(currVersion[0] > finalCurrentVersionCode){
//                        mRootRef.child("data").child("info").child("verInfo")
//                                .addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(DataSnapshot dataSnapshot) {
//                                        verInfo[0] = dataSnapshot.getValue(String.class);
//                                        if(!verInfo[0].equals("null")) {
//                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                                            builder.setMessage(verInfo[0])
//                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                                                        @Override
//                                                        public void onClick(DialogInterface dialog, int which) {
//                                                            //NOTHING
//                                                        }
//                                                    });
//                                            builder.show();
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onCancelled(DatabaseError databaseError) {
//                                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
//                                    }
//                                });
//                    }
//
//                }
//
//                @Override
//                public void onCancelled(DatabaseError databaseError) {
//                    Log.w(TAG, "getUser:onCancelled", databaseError.toException());
//                }
//            });
//
//
//            return;
//        } else if (savedVersionCode == DOESNT_EXIST) {
////            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
////            try {
////                builder1.setMessage("Es kann sein, dass deine Vertretungsplan Daten und dein Jahrgang gelöscht wurden\nUm diese zu aktualisieren gehe zu Einstellungen --> Vertretungsplan")
////                        .setTitle("Update " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
////                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
////                            @Override
////                            public void onClick(DialogInterface dialog, int which) {
////
////                            }
////                        });
////                builder1.show();
////            } catch (PackageManager.NameNotFoundException e) {
////                e.printStackTrace();
////            }
//
//        }else if(savedVersionCode==23){
//            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//            builder.setMessage("Du hast dieses Update erhalten, da du dich für die Beta-Version im Google Play Store angemeldet hast. " +
//                    "Bitte sei im Klaren, dass es während der Beta gehäuft zu Problemen und unerwünschten Verhalten kommen kann. " +
//                    "Um die stabile Version zu installieren verlasse einfach das Betaprogramm.")
//                    .setTitle("Info zum Betaprogramm")
//                    .setPositiveButton("Ich habe verstanden", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                        }
//                    });
//            builder.show();
//
//        }
//        else if (currentVersionCode > savedVersionCode && savedVersionCode!=23) {
//            // TODO This is an upgrade
//                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
//                    try {
//                        builder1.setMessage("Es kann sein, dass deine Vertretungsplan Daten und dein Jahrgang gelöscht wurden\nUm diese zu aktualisieren gehe zu Einstellungen --> Vertretungsplan")
//                                .setTitle("Update " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
//                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//
//                            }
//                        });
//                        builder1.show();
//                    } catch (PackageManager.NameNotFoundException e) {
//                        e.printStackTrace();
//                    }
//
////            if(mAuth.getCurrentUser()!=null){
////                if(!mAuth.getCurrentUser().isAnonymous()){
////                    if(mAuth.getCurrentUser().getEmail().contains("ceci@example.com")){
////                        Intent signAIntent = new Intent(this, SignUpAnonymActivity.class);
////                        startActivity(signAIntent);
////                    }
////                }
////            }
//        }
//        // Update the shared preferences with the current version code
//        prefs.edit().putInt(PREF_VERSION_CODE_KEY, currentVersionCode).commit();
//    }
}

package com.github.leonardpieper.ceciVPlan;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KurseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    private String[] kurseIcons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kurse);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();


        com.github.clans.fab.FloatingActionButton kursCreateFab = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.kurse_create_fab);
        kursCreateFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog(1);
            }
        });

        com.github.clans.fab.FloatingActionButton kursJoinFab = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.kurse_join_fab);
        kursJoinFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog(2);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        getKurse();
    }

    private void getKurse(){
        final LinearLayout llroot = (LinearLayout)findViewById(R.id.kurse_ll);

        if(mAuth.getCurrentUser()!=null){
            DatabaseReference kurseRef = mRootRef
                    .child("Users")
                    .child(mAuth.getCurrentUser().getUid())
                    .child("Kurse");

            kurseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    llroot.removeAllViews();
                    for(DataSnapshot childSnapshot: dataSnapshot.getChildren()){
                        TextView tv = new TextView(KurseActivity.this);
                        final String title = childSnapshot.child("name").getValue(String.class);
//                        tv.setText(childSnapshot.child("name").getValue(String.class));
//                        tv.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Intent intent = new Intent(KurseActivity.this, KursActivity.class);
//                                intent.putExtra("name", title);
//                                startActivity(intent);
//                            }
//                        });
                        CardView cv = createKursCard(title);
                        llroot.addView(cv);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    private CardView createKursCard(final String title){
        CardView cv = new CardView(this);
        cv.setRadius(1);
        cv.setContentPadding(15, 15, 15, 15);
        cv.setCardElevation(5);
        cv.setMinimumHeight(250);
        cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(KurseActivity.this, KursActivity.class);
                intent.putExtra("name", title);
                startActivity(intent);
            }
        });

        LinearLayout.LayoutParams cvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cvParams.setMargins(0,1,0,1);

        TextView tv = new TextView(this);
        tv.setText(title);

        cv.setLayoutParams(cvParams);

        kurseIcons = getResources().getStringArray(R.array.kursIcons);
        for(String kursIcon:kurseIcons){
            Matcher m = Pattern.compile("\\b"+kursIcon+"\\s").matcher(title);
            if(m.find()){
//                ImageView iv = new ImageView(this);
//                iv.setBackgroundResource(R.drawable.ic_deutsch_book);
//                iv.setScaleType(ImageView.ScaleType.FIT_START);
//                cv.addView(iv);
            }
        }



        cv.addView(tv);

        return cv;
    }

    private void createDialog(int type){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.dialog_kurse_add, null);


        final EditText kursName = (EditText)   textEntryView.findViewById(R.id.dialog_add_abk);
        final EditText kursSecret = (EditText) textEntryView.findViewById(R.id.dialog_add_pwd);
        builder.setView(textEntryView);
        switch (type){
            case 1:
                builder.setTitle("Neuen Kurs erstellen");
                builder.setPositiveButton("Hinzufügen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createKurs(kursName.getText().toString(), kursSecret.getText().toString());
                    }
                });
                break;
            case 2:
                builder.setTitle("Kurs beitreten");
                builder.setPositiveButton("Beitreten", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        joinKurs(kursName.getText().toString(), kursSecret.getText().toString());
                    }
                });
                break;
        }

        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    private void createKurs(final String name, final String secret){
        mRootRef.child("Data").child("lehrerRead").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mRootRef.child("Kurse").child(name).child("secret").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getValue()!=null){
                            Toast t = Toast.makeText(getApplicationContext(), "Kurs existiert bereits!", Toast.LENGTH_LONG);
                            t.show();
                        }else{
                            if(!name.isEmpty()&&!secret.isEmpty()){
                                mRootRef.child("Kurse").child(name).child("secret").setValue(secret);

                                HashMap<String, Object> user = new HashMap<String, Object>();
                                user.put("name", name);
                                user.put("secret", secret);
                                mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").child(name).setValue(user);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        System.out.println(databaseError);
                        if(!databaseError.getMessage().contains("Permission denied")){
                            Toast t = Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_LONG);
                            t.show();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast t = Toast.makeText(getApplicationContext(), "Sie dürfen keine Kurse hinzufügen. Sind Sie Lehrer?", Toast.LENGTH_LONG);
                t.show();
            }
        });
    }

    private void joinKurs(final String name, final String secret){
        HashMap<String, Object> user = new HashMap<String, Object>();
        user.put("name", name);
        user.put("secret", secret);
        mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").child(name).setValue(user);
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
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case R.id.nav_vplan:
                Intent vIntent = new Intent(this, VPlanActivity.class);
                startActivity(vIntent);
                break;
            case R.id.nav_kurse:
//                Intent kuIntent = new Intent(this, VPlanActivity.class);
//                startActivity(kuIntent);
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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

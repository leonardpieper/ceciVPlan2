package com.github.leonardpieper.ceciVPlan;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.Gravity;
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
import android.widget.RelativeLayout;
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
        setTitle("Kurse");

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

        com.github.clans.fab.FloatingActionButton kursLeaveFab = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.kurse_edit_fab);
        kursLeaveFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout kursell = (LinearLayout)findViewById(R.id.kurse_ll);
                for(int i = 0; i<kursell.getChildCount(); i++){
//                    View childView = kursell.getChildAt(i);
                    ViewGroup cvChildViews = (ViewGroup) kursell.getChildAt(i);
                    ViewGroup llChildViews = (ViewGroup) cvChildViews.getChildAt(0);
                    ViewGroup rlChildViews = (ViewGroup) llChildViews.getChildAt(2) ;
                    View leaveBtn = rlChildViews.getChildAt(0);
                    leaveBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        getPermissionStatus();
        getKurse();
    }

    private void getPermissionStatus(){
        mRootRef.child("Data").child("lehrerRead").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue(Boolean.class)==true){
                    com.github.clans.fab.FloatingActionButton kursCreateFab = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.kurse_create_fab);
                    kursCreateFab.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
        final float scale = KurseActivity.this.getResources().getDisplayMetrics().density;
        int ivWidth = (int) (50 * scale + 0.5f);
        int ivHeight = (int) (50 * scale + 0.5f);
        int exivWidth = (int) (35 * scale + 0.5f);
        int exivHeight = (int) (35 * scale + 0.5f);
        int height = (int) (75 * scale + 0.5f);


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
                height
        );
        cvParams.setMargins(0,1,0,1);

        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                ivWidth,
                ivHeight
        );
        ivParams.setMargins(0,0,35,0);

        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        RelativeLayout.LayoutParams editParams = new RelativeLayout.LayoutParams(
                exivWidth,
                exivHeight
        );
        editParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setLayoutParams(llParams);

        RelativeLayout relativeLayout = new RelativeLayout(this);
        relativeLayout.setLayoutParams(rlParams);
        relativeLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setGravity(Gravity.CENTER);

        cv.setLayoutParams(cvParams);

        ImageView exiv = new ImageView(KurseActivity.this);
        exiv.setBackgroundResource(R.drawable.ic_exit_to_app_red_24dp);
        exiv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        exiv.setAdjustViewBounds(true);
        exiv.setVisibility(View.GONE);
        exiv.setLayoutParams(editParams);
        exiv.setTag("leaveBtn");
        exiv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leaveKurs(title);
            }
        });

        ImageView iv = new ImageView(KurseActivity.this);
        iv.setBackgroundResource(getResourceIdByName(title));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
        iv.setLayoutParams(ivParams);

        relativeLayout.addView(exiv);

        linearLayout.addView(iv);
        linearLayout.addView(tv);
        linearLayout.addView(relativeLayout);
//        linearLayout.addView(exiv);

        cv.addView(linearLayout);

        return cv;
    }

    private void leaveKurs(String name){
        if(mAuth.getCurrentUser()!=null) {
            mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").child(name).removeValue();
        }else {
            Toast t = Toast.makeText(KurseActivity.this, "Kein Nutzer angemeldet", Toast.LENGTH_LONG);
            t.show();
        }
    }

    private int getResourceIdByName(String name){
        String arr[] = name.split(" ", 2);
        String fach = arr[0];
        fach=fach.toLowerCase();
        switch (fach){
            case "bi":
                return R.drawable.ic_biologie_tree;
            case "ch":
                return  R.drawable.ic_chemie_poppet;
            case "d":
                return R.drawable.ic_deutsch_book;
            case "e":
                return R.drawable.ic_englisch_book;
            case "ek":
                return R.drawable.ic_erdkunde_landscape;
            case "el":
                return R.drawable.ic_ernahrungslehre_dining;
            case "ew":
                return R.drawable.ic_erziehungswissenschaften_child;
            case "f":
                return R.drawable.ic_franzosisch_book;
            case "ge":
                return R.drawable.ic_geschichte_hourglass;
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
                return R.drawable.ic_spanisch_book;
            case "sp":
                return R.drawable.ic_sport_run;
            default:
                return R.drawable.ic_school_black_24dp;
        }
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

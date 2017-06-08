package com.github.leonardpieper.ceciVPlan.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.leonardpieper.ceciVPlan.R;
import com.github.leonardpieper.ceciVPlan.Vertretungsplan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.HashMap;
import java.util.Map;

public class MainFragment extends Fragment {
    private final String TAG = "MainFragment";

    private FirebaseAuth mAuth;

    private DatabaseReference mRootRef;
    private DatabaseReference stufenRef;
    private ValueEventListener valueEventListener;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();


        setFirebaseAuthListener();

        return inflater.inflate(R.layout.content_main, container, false);
    }

    /**
     * Setzt den Listener, ob ein Nutzer angemeldet ist
     */
    private void setFirebaseAuthListener(){
        FirebaseAuth.AuthStateListener mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                getVPlan(firebaseAuth);
            }
        };
        mAuth.addAuthStateListener(mAuthListener);
    }

    /**
     * Lädt den Vertretungsplan herunter
     * @param newFirebaseAuth Ist der Nutzer, der in setFirebaseAuthListener() bekommen wird
     */
    private void getVPlan(FirebaseAuth newFirebaseAuth){
        FirebaseUser user = newFirebaseAuth.getCurrentUser();

        /*
        Überprüft, ob der Nutzer angemeldet ist.
        Wenn er angemeldet ist ist user != null
         */
        if(user != null){
            /*
            Überprüft, ob der Vertretungsplan serverseitig aktiviert wurde.
            Wenn alles OK, dass ist vplan_enabled==true
             */
            if(mFirebaseRemoteConfig.getBoolean("vplan_enabled")) {
                DatabaseReference conditionRef = mRootRef.child("vPlan");

                String stufe = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("jahrgang", "EF");
                stufenRef = conditionRef.child(stufe);

                Log.d("FirebaseAuth", "onAuthStateChanged:signed_in:" + user.getUid());

                stufenRef.addValueEventListener(valueEventListener = new ValueEventListener(){
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(MainFragment.this.isVisible()) {
                            TableLayout tlToday = (TableLayout) getActivity().findViewById(R.id.main_tl_vPlanToday);
                            TableLayout tlTomorrow = (TableLayout) getActivity().findViewById(R.id.main_tl_vPlanTomorrow);

                            tlToday.removeAllViews();
                            tlTomorrow.removeAllViews();


                            Vertretungsplan vPlan = new Vertretungsplan();
                            for (DataSnapshot vPlanSnapshot : dataSnapshot.getChildren()) {
                                String datum = vPlanSnapshot.child("Datum").getValue(String.class);
                                if (!(datum == null || datum.contains("Datum"))) {

                                    String fach = vPlanSnapshot.child("Fach").getValue(String.class);
                                    String stunde = vPlanSnapshot.child("Stunde").getValue(String.class);
                                    String vertreter = vPlanSnapshot.child("Vertreter").getValue(String.class);
                                    String raum = vPlanSnapshot.child("Raum").getValue(String.class);
                                    String text = vPlanSnapshot.child("Vertretungs-Text").getValue(String.class);

                                    if (vPlan.isToday(datum)) {
                                        addTableRow("today", fach, stunde, vertreter, raum, text);
                                    } else if (vPlan.isTomorrow(datum)) {
                                        addTableRow("tomorrow", fach, stunde, vertreter, raum, text);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, databaseError.getMessage());
                    }
                });
            }

        }else{
            Log.d("FirebaseAuth", "onAuthStateChanged:signed_out");
            TextView tvErr = (TextView)getActivity().findViewById(R.id.main_tv_errMain);
            tvErr.setText("Du bist nicht angemeldet!");
            tvErr.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Fügt eine Zeile zur Tabelle hinzu
     * @param tag
     * @param fach
     * @param stunde
     * @param lehrer
     * @param raum
     * @param text
     */
    public void addTableRow(String tag, String fach, String stunde, String lehrer, String raum, String text) {
        TableRow row = new TableRow(getActivity());

        TextView lesson = new TextView(getActivity());
        TextView time = new TextView(getActivity());
        TextView tutor = new TextView(getActivity());
        TextView room = new TextView(getActivity());
        TextView extra = new TextView(getActivity());

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

        TableLayout tlToday = (TableLayout)getActivity().findViewById(R.id.main_tl_vPlanToday);
        TableLayout tlTomorrow = (TableLayout)getActivity().findViewById(R.id.main_tl_vPlanTomorrow);

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

//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        stufenRef.removeEventListener(valueEventListener);
//    }

    @Override
    public void onStop() {
        super.onStop();
//        stufenRef.removeEventListener(valueEventListener);
    }
}

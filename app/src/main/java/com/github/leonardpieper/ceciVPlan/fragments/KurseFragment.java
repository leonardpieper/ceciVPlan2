package com.github.leonardpieper.ceciVPlan.fragments;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.KursActivity;
import com.github.leonardpieper.ceciVPlan.SettingsActivity;
import com.github.leonardpieper.ceciVPlan.models.Kurs;
import com.github.leonardpieper.ceciVPlan.tools.KursCache;
import com.github.leonardpieper.ceciVPlan.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class KurseFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;

    private SwipeRefreshLayout swrReload;
    private LinearLayout llKurse;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.app_bar_kurse, container, false);

        getActivity().setTitle("Kurse");

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();

        llKurse = (LinearLayout)view.findViewById(R.id.kurse_ll);

        com.github.clans.fab.FloatingActionButton kursJoinFab = (com.github.clans.fab.FloatingActionButton) view.findViewById(R.id.kurse_join_fab);
        kursJoinFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog(2);
            }
        });

        com.github.clans.fab.FloatingActionButton kursLeaveFab = (com.github.clans.fab.FloatingActionButton) view.findViewById(R.id.kurse_edit_fab);
        kursLeaveFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout kursell = (LinearLayout)view.findViewById(R.id.kurse_ll);
                for(int i = 0; i<kursell.getChildCount(); i++){
                    ViewGroup cvChildViews = (ViewGroup) kursell.getChildAt(i);
                    ViewGroup llChildViews = (ViewGroup) cvChildViews.getChildAt(0);
                    ViewGroup rlChildViews = (ViewGroup) llChildViews.getChildAt(2) ;
                    View leaveBtn = rlChildViews.getChildAt(0);
                    leaveBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        swrReload = (SwipeRefreshLayout) view.findViewById(R.id.kurse_srl_reloadKurse);
        swrReload.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reloadKurse();
            }
        });

        getKurse();
        return view;
    }

    /**
     * Überprüft, ob der KursCache älter als eine Woche ist.
     * -->Wenn Ja: Der Cache wird aus der Firebase Database neugeladen und gespeichert
     * -->Wenn Nein: Die Kurse werden aus dem Cache abgerufen
     */
    private void getKurse(){
        final KursCache kursCache = new KursCache(getActivity());

        long cachedTime = kursCache.getCacheTime();
        long currMill = System.currentTimeMillis();

        if(cachedTime == -1 || cachedTime + 604800000 < currMill ) {


            if (mAuth.getCurrentUser() != null) {
                reloadKurse();
            }
        }else{
            JSONObject root = kursCache.getCache();
            JSONArray kurse = null;

            try {
                kurse = root.getJSONArray("kurse");

                for(int i = 0; i<kurse.length(); i++){
                    String title = (!kurse.getJSONObject(i).isNull("name")) ? kurse.getJSONObject(i).getString("name") : null;
                    String type = (!kurse.getJSONObject(i).isNull("type")) ? kurse.getJSONObject(i).getString("type") : null;

                    CardView cv = createKursCard(title, type);
                    llKurse.addView(cv);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    /**
     * Lädt die Kurse aus der Firebase Database in den Cache
     */
    private void reloadKurse(){
        final KursCache kursCache = new KursCache(getActivity());
        DatabaseReference kurseRef = mRootRef
                .child("Users")
                .child(mAuth.getCurrentUser().getUid())
                .child("Kurse");

        kurseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                swrReload.setRefreshing(false);
                llKurse.removeAllViews();
                kursCache.newCache();

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    Kurs kurs = childSnapshot.getValue(Kurs.class);

                    String displayName = kurs.name.replace("%2E", ".");
                    CardView cardView = createKursCard(displayName, kurs.type);
                    llKurse.addView(cardView);
                    kursCache.addCache(kurs.name, kurs.type);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                swrReload.setRefreshing(false);
            }
        });
    }

    /**
     * Erstellt ein CardView mit Bild und Title des Kurses
     * @param title Ist der Name des Kurses
     * @param kursType Ist der Kurstyp: Entweder "online", oder "offline"
     * @return Gibt einen fertigen CardView zurück
     */
    private CardView createKursCard(final String title, String kursType){
        final float scale = getActivity().getResources().getDisplayMetrics().density;
        int ivWidth = (int) (50 * scale + 0.5f);
        int ivHeight = (int) (50 * scale + 0.5f);
        int ivMarginTop = (int) (7 * scale + 0.5f);
        int exivWidth = (int) (35 * scale + 0.5f);
        int exivHeight = (int) (35 * scale + 0.5f);
        int height = (int) (75 * scale + 0.5f);


        final CardView cv = new CardView(getActivity());
        cv.setRadius(1);
        cv.setContentPadding(15, 15, 15, 15);
        cv.setCardElevation(5);

        if(kursType!=null&&!kursType.equals("offline")) {
            cv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), KursActivity.class);
                    intent.putExtra("name", title);
                    startActivity(intent);
                }
            });
        }


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
        ivParams.setMargins(0,ivMarginTop,35,0);
//        ivParams.gravity=Gravity.END;

        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        RelativeLayout.LayoutParams tvParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        RelativeLayout.LayoutParams editParams = new RelativeLayout.LayoutParams(
                exivWidth,
                exivHeight
        );
        editParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setLayoutParams(llParams);

        RelativeLayout relativeLayout = new RelativeLayout(getActivity());
        relativeLayout.setLayoutParams(rlParams);
        relativeLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(getActivity());
        tv.setLayoutParams(tvParams);
        tv.setText(title);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        if(kursType!=null&&kursType.equals("online")){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tv.setTextColor(getResources().getColor(R.color.colorAccent, getActivity().getTheme()));
            }else {
                tv.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        }

        cv.setLayoutParams(cvParams);


        ImageView exiv = new ImageView(getActivity());
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
//                cv.removeAllViews();
                cv.setVisibility(View.GONE);
            }
        });

        ImageView iv = new ImageView(getActivity());
        iv.setBackgroundResource(getResourceIdByName(title));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
//        iv.setPadding(0,12,0,0);
        iv.setLayoutParams(ivParams);

        relativeLayout.addView(exiv);

        linearLayout.addView(iv);
        linearLayout.addView(tv);
        linearLayout.addView(relativeLayout);
//        linearLayout.addView(exiv);

        cv.addView(linearLayout);

        return cv;
    }

    /**
     * Löscht den Kurs sowohl aus dem Cache, als auch aus der Firebase Database
     * @param name Der Name des zu löschenden Kurses
     */
    private void leaveKurs(String name) {
        if (mAuth.getCurrentUser() != null) {
            String refName = name.replace(".", "%2E").toLowerCase();
            mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").child(refName).removeValue();

            KursCache kursCache = new KursCache(getActivity());
            kursCache.removeFromCache(name);
        } else {
            Toast t = Toast.makeText(getActivity(), "Kein Nutzer angemeldet", Toast.LENGTH_LONG);
            t.show();
        }
    }

    /**
     * Gibt das zum Kurs gehörige Icon aus
     * @param name Der Kursname zu dem das Icon gehören soll
     * @return Gibt eine RessourceID zurück, unter des das Icon gefunden werden kann
     */
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

    /**
     * Erstellt einen Dialog um einen neuen Kurs zu erstellen, oder einem beizutreten.
     * @param type
     */
    private void createDialog(int type){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View textEntryView = factory.inflate(R.layout.dialog_kurse_add, null);


        final EditText kursName = (EditText)   textEntryView.findViewById(R.id.dialog_add_abk);
        final EditText kursSecret = (EditText) textEntryView.findViewById(R.id.dialog_add_pwd);
        final CheckBox checkBox = (CheckBox) textEntryView.findViewById(R.id.dialog_kurse_add_chkbx_offline);
        builder.setView(textEntryView);
        switch (type){
            case 1:
                builder.setTitle("Neuen Kurs erstellen");
                builder.setPositiveButton("Hinzufügen", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        createKurs(kursName.getText().toString(), kursSecret.getText().toString());
                    }
                });
                break;
            case 2:
                builder.setTitle("Kurs beitreten");
                builder.setPositiveButton("Beitreten", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(checkBox.isChecked()) {
                            joinKurs(kursName.getText().toString(), kursSecret.getText().toString(), "offline");
                        }else{
                            joinKurs(kursName.getText().toString(), kursSecret.getText().toString(), "online");
                        }
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

    /**
     * Lässt einen einem Kurs beitreten.
     * Hier wird unterschieden zwischen "online" und "offline" Kursen.
     * Offline Kurse beinhalten nur die Benachrichtigungsfunktion
     * @param name Der Kursname
     * @param secret Das Kurspasswort. Bei offline Kursen ist dies nicht nötig
     * @param type Der Kurstyp: "online" oder "offline"
     */
    private void joinKurs(String name, String secret, String type){
        if(mAuth.getCurrentUser()!=null) {
            HashMap<String, Object> user = new HashMap<String, Object>();

            if (type.equals("offline")) {
                user.put("name", name);
                user.put("type", type);
            } else {
                user.put("name", name);
                user.put("secret", secret);
                user.put("type", type);
            }

            String refName = name.replace(".", "%2E");
            refName = refName.toLowerCase();
            mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").child(refName).setValue(user);


            String fcmTopic = refName.replace(" ", "%20");
            FirebaseMessaging.getInstance().subscribeToTopic(fcmTopic);

            KursCache kursCache = new KursCache(getActivity());
            kursCache.addCache(name, type);
        }else{
            Toast t = Toast.makeText(getActivity(), "Für diese Aktion musst du angemeldet sein", Toast.LENGTH_LONG);
            t.show();
        }
    }
}

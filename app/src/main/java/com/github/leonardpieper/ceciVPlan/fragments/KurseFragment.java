package com.github.leonardpieper.ceciVPlan.fragments;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.KursActivity;
import com.github.leonardpieper.ceciVPlan.R;
import com.github.leonardpieper.ceciVPlan.SignUpActivity;
import com.github.leonardpieper.ceciVPlan.SignUpAnonActivity;
import com.github.leonardpieper.ceciVPlan.models.Kurs;
import com.github.leonardpieper.ceciVPlan.tools.KursCache;
import com.github.leonardpieper.ceciVPlan.tools.KursIcon;
import com.github.leonardpieper.ceciVPlan.tools.Kurse;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class KurseFragment extends Fragment {
    private final String TAG = "KurseFragment";
    private static boolean isInForeground;

    private View view;

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;

    private SwipeRefreshLayout swrReload;
    private LinearLayout llKurse;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.app_bar_kurse, container, false);

        getActivity().setTitle("Kurse");

        mAuth = FirebaseAuth.getInstance();
        mRootRef = MyDatabaseUtil.getDatabase().getReference();

        llKurse = (LinearLayout) view.findViewById(R.id.kurse_ll);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.kurse_fab_add);
        fab.setClickable(true);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDialog(2);
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

    @Override
    public void onStart() {
        super.onStart();
        isInForeground = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        isInForeground = false;
    }

    /**
     * Überprüft, ob der KursCache älter als eine Woche ist.
     * -->Wenn Ja: Der Cache wird aus der Firebase Database neugeladen und gespeichert
     * -->Wenn Nein: Die Kurse werden aus dem Cache abgerufen
     */
    private void getKurse() {
        final Kurse kurse = new Kurse(getActivity());
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Kurs> kursListe = new ArrayList<>();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    kursListe.add(childSnapshot.getValue(Kurs.class));
                }
                java.util.Collections.reverse(kursListe);
                displayKurse(kursListe);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        List<Kurs> kurses = kurse.getKurse(valueEventListener);
        if (kurses != null) {
            displayKurse(kurses);
        } else {
            RelativeLayout rlNoKurse = (RelativeLayout) view.findViewById(R.id.kurse_rl_noKurse);
            rlNoKurse.setVisibility(View.VISIBLE);
        }

    }

    private void displayKurse(List<Kurs> kursListe) {
        for (Kurs kurs : kursListe) {
            String title = kurs.name;
            String type = kurs.type;

            CardView cv = createKursCard(title, type);
            llKurse.addView(cv);
        }
    }

    /**
     * Lädt die Kurse aus der Firebase Database in den Cache
     */
    private void reloadKurse() {
        final KursCache kursCache = new KursCache(getActivity());
        DatabaseReference kurseRef = mRootRef
                .child("Users")
                .child(mAuth.getCurrentUser().getUid())
                .child("Kurse");

        Query kurseQuery = kurseRef.orderByChild("type");

        kurseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (KurseFragment.isInForeground) {
                    swrReload.setRefreshing(false);
                    llKurse.removeAllViews();
                    kursCache.newCache();

                    if (dataSnapshot.getValue() == null) {
                        RelativeLayout rlNoKurse = (RelativeLayout) view.findViewById(R.id.kurse_rl_noKurse);
                        rlNoKurse.setVisibility(View.VISIBLE);
                    }

                    ArrayList<DataSnapshot> kurseList = new ArrayList<DataSnapshot>();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        kurseList.add(childSnapshot);
                    }
                    java.util.Collections.reverse(kurseList);

                    for (DataSnapshot childSnapshot : kurseList) {
                        Kurs kurs = childSnapshot.getValue(Kurs.class);


                        String displayName = kurs.name.replace("%2E", ".");
                        CardView cardView = createKursCard(displayName, kurs.type);
                        llKurse.addView(cardView);
                        kursCache.addCache(kurs.name, kurs.type);
                        try {
                            String fcmTopic = URLEncoder.encode(kurs.name, "UTF-8").replace("+", "%20").toLowerCase();
                            FirebaseMessaging.getInstance().subscribeToTopic(fcmTopic);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                if (KurseFragment.isInForeground) {
                    swrReload.setRefreshing(false);
                }
            }
        });
    }

    /**
     * Erstellt ein CardView mit Bild und Title des Kurses
     *
     * @param title    Ist der Name des Kurses
     * @param kursType Ist der Kurstyp: Entweder "online", oder "offline"
     * @return Gibt einen fertigen CardView zurück
     */
    private CardView createKursCard(final String title, String kursType) {
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

        if (kursType != null && !kursType.equals("offline")) {
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
        cvParams.setMargins(0, 1, 0, 1);

        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                ivWidth,
                ivHeight
        );
        ivParams.setMargins(0, ivMarginTop, 35, 0);
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
        if (kursType != null && kursType.equals("online")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tv.setTextColor(getResources().getColor(R.color.colorAccent, getActivity().getTheme()));
            } else {
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
        iv.setBackgroundResource(KursIcon.getResourceIdByName(title));
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
     *
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
     * Erstellt einen Dialog um einen neuen Kurs zu erstellen, oder einem beizutreten.
     *
     * @param type
     */
    private void createDialog(int type) {
        if(mAuth.getCurrentUser()!=null&&mAuth.getCurrentUser().isAnonymous()){
            AlertDialog.Builder signBuilder = new AlertDialog.Builder(getActivity());
            signBuilder.setTitle("Anmeldung erforderlich")
                    .setMessage("Um Kurse hinzuzufügen ist eine Anmeldung mittels E-Mail-Adresse oder Telefonnummer erforderlich.")
                    .setPositiveButton("Anmelden", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent signIntent = new Intent(getActivity(), SignUpActivity.class);
                            signIntent.putExtra("kursJoin", true);
                            startActivity(signIntent);
                        }
                    })
                    .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            android.support.v7.app.AlertDialog dialog = signBuilder.show();
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.secondary_text_light));


        }else {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            LayoutInflater factory = LayoutInflater.from(getActivity());
            final View textEntryView = factory.inflate(R.layout.dialog_kurse_add, null);


            final EditText kursName = (EditText) textEntryView.findViewById(R.id.dialog_add_abk);
            final EditText kursSecret = (EditText) textEntryView.findViewById(R.id.dialog_add_pwd);
            final CheckBox checkBox = (CheckBox) textEntryView.findViewById(R.id.dialog_kurse_add_chkbx_offline);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (checkBox.isChecked()) {
                        kursSecret.setVisibility(View.GONE);
                    } else {
                        kursSecret.setVisibility(View.VISIBLE);
                    }
                }
            });
            builder.setView(textEntryView);
            switch (type) {
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
                            Kurse kurse = new Kurse(getActivity());
                            if (checkBox.isChecked()) {
                                kurse.joinKurs(kursName.getText().toString(), kursSecret.getText().toString(), "offline");
                            } else {
                                kurse.joinKurs(kursName.getText().toString(), kursSecret.getText().toString(), "online");
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
    }
}

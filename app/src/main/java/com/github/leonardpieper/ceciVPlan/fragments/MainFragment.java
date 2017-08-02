package com.github.leonardpieper.ceciVPlan.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.leonardpieper.ceciVPlan.KursActivity;
import com.github.leonardpieper.ceciVPlan.MainActivity;
import com.github.leonardpieper.ceciVPlan.R;
import com.github.leonardpieper.ceciVPlan.SignUpActivity;
import com.github.leonardpieper.ceciVPlan.Vertretungsplan;
import com.github.leonardpieper.ceciVPlan.models.Kurs;
import com.github.leonardpieper.ceciVPlan.tools.KursCache;
import com.github.leonardpieper.ceciVPlan.tools.KursIcon;
import com.github.leonardpieper.ceciVPlan.tools.Kurse;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainFragment extends Fragment {
    private final String TAG = "MainFragment";
    private static boolean isInForeground;

    private View view;

    private TextView notLoggedIn;

    private FirebaseAuth mAuth;

    private DatabaseReference mRootRef;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Dashboard");
        view = inflater.inflate(R.layout.app_bar_main, container, false);

        mAuth = FirebaseAuth.getInstance();
        mRootRef = MyDatabaseUtil.getDatabase().getReference();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        notLoggedIn = (TextView) view.findViewById(R.id.main_tv_errMain);

        isConnectedToFirebaseDatabase();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        isInForeground = true;
        setFirebaseAuthListener();
    }


    /**
     * Setzt den Listener, ob ein Nutzer angemeldet ist
     */
    private void setFirebaseAuthListener() {
        FirebaseAuth.AuthStateListener mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (MainFragment.isInForeground) {
                    getVPlan(firebaseAuth);
                    displayKurse();
                }
            }
        };
        mAuth.addAuthStateListener(mAuthListener);
    }

    private void isConnectedToFirebaseDatabase() {
        DatabaseReference connectedRef = MyDatabaseUtil.getDatabase().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (MainFragment.isInForeground) {
                    ProgressBar offlineProg = (ProgressBar) view.findViewById(R.id.main_progBar_offline);
                    if (connected) {
                        offlineProg.setVisibility(View.GONE);
                    } else {
                        offlineProg.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Listener was cancelled");
            }
        });
    }

    /**
     * Lädt den Vertretungsplan herunter
     *
     * @param newFirebaseAuth Ist der Nutzer, der in setFirebaseAuthListener() bekommen wird
     */
    private void getVPlan(FirebaseAuth newFirebaseAuth) {
        FirebaseUser user = newFirebaseAuth.getCurrentUser();

        /*
        Überprüft, ob der Nutzer angemeldet ist.
        Wenn er angemeldet ist ist user != null
         */
        if (user != null) {
            /*
            Überprüft, ob der Vertretungsplan serverseitig aktiviert wurde.
            Wenn alles OK, dann ist vplan_enabled==true
             */
            if (mFirebaseRemoteConfig.getBoolean("vplan_enabled")) {
                DatabaseReference conditionRef = mRootRef.child("vPlan");

                String stufe = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("jahrgang", "EF");
                DatabaseReference stufenRef = conditionRef.child(stufe);

                Log.d("FirebaseAuth", "onAuthStateChanged:signed_in:" + user.getUid());


                stufenRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (MainFragment.this.isVisible()) {
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
                            CardView cvToday = (CardView) view.findViewById(R.id.main_cv_today);
                            CardView cvTomorrow = (CardView) view.findViewById(R.id.main_cv_tomorrow);
                            if (tlToday.getChildCount() < 1) {
                                cvToday.setVisibility(View.GONE);
                            }
                            if (tlTomorrow.getChildCount() < 1) {
                                cvTomorrow.setVisibility(View.GONE);
                            }
                            if (tlToday.getChildCount() < 1 && tlTomorrow.getChildCount() < 1) {
                                RelativeLayout rlNoVertretung = (RelativeLayout) view.findViewById(R.id.main_rl_noVertretung);
                                rlNoVertretung.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, databaseError.getMessage());
                    }
                });
            }

        } else {
            Log.d("FirebaseAuth", "onAuthStateChanged:signed_out");

            if (MainActivity.isInForeground) {
                notLoggedIn.setText("Du bist nicht angemeldet!");
                notLoggedIn.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Fügt eine Zeile zur Tabelle hinzu
     *
     * @param tag
     * @param fach
     * @param stunde
     * @param lehrer
     * @param raum
     * @param text
     */
    public void addTableRow(String tag, final String fach, String stunde, String lehrer, String raum, String text) {
        TableRow row = new TableRow(getActivity());
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showContextMenu(fach);
                return true;
            }
        });

        TextView lesson = new TextView(getActivity());
        TextView time = new TextView(getActivity());
        TextView tutor = new TextView(getActivity());
        TextView room = new TextView(getActivity());
        TextView extra = new TextView(getActivity());

        lesson.setPadding(5, 15, 15, 15);
        time.setPadding(5, 15, 15, 15);
        tutor.setPadding(5, 15, 15, 15);
        room.setPadding(5, 15, 15, 15);
        extra.setPadding(5, 15, 15, 15);

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

        TableLayout tlToday = (TableLayout) getActivity().findViewById(R.id.main_tl_vPlanToday);
        TableLayout tlTomorrow = (TableLayout) getActivity().findViewById(R.id.main_tl_vPlanTomorrow);

        switch (tag) {
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

    private void showContextMenu(final String kursName) {
        if(mAuth.getCurrentUser()!=null&&mAuth.getCurrentUser().isAnonymous()){
            android.support.v7.app.AlertDialog.Builder signBuilder = new android.support.v7.app.AlertDialog.Builder(getActivity());
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
            builder.setTitle(kursName)
                    .setItems(R.array.context_kursAdd, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Kurse kurse = new Kurse(getActivity());
                            kurse.joinKurs(kursName, "", "offline");
                        }
                    });
            builder.show();
        }
    }

    private void displayKurse() {
        final KursCache kursCache = new KursCache(getActivity());
        final LinearLayout ll = (LinearLayout) view.findViewById(R.id.main_kurse_display);

        /*
        Überprüft, ob die Kurse aus dem Cache geladen werden sollen, oder aus der Firebase Database
        Wenn kursCache.isCacheUpToDate(7)==false, dann wurde der Cache seit 7 Tagen nicht mehr aktualisiert
        und wird neu aus der Firebase Database heruntergeladen
         */
        if (!kursCache.isCacheUpToDate(7)) {
            if (mAuth.getCurrentUser() != null) {
                mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (MainFragment.isInForeground) {
                            kursCache.newCache();
                            ll.removeAllViews();

                            if (dataSnapshot.getValue() == null) {
                                TextView tvNoKurse = (TextView) view.findViewById(R.id.noKurse);
                                tvNoKurse.setVisibility(View.VISIBLE);
                            }
                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                Kurs kurs = childSnapshot.getValue(Kurs.class);
                                String displayName = kurs.name.replace("%2E", ".");

                                kursCache.addCache(kurs.name, kurs.type);

                                LinearLayout column = makeKursIcon(displayName, kurs.type);
                                ll.addView(column);
                                ll.setPadding(0, 0, 0, 0);

                            }
                            CardView cvKurse = (CardView) view.findViewById(R.id.main_cv_Kurse);
                            cvKurse.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        } else {
            JSONObject root = kursCache.getCache();
            JSONArray kurse = null;

            try {
                kurse = root.getJSONArray("kurse");

                if (kurse == null || kurse.length() == 0) {
                    TextView tvNoKurse = (TextView) view.findViewById(R.id.noKurse);
                    tvNoKurse.setVisibility(View.VISIBLE);
                } else {
                    ll.removeAllViews();
                    for (int i = 0; i < kurse.length(); i++) {
                        String title = (!kurse.getJSONObject(i).isNull("name")) ? kurse.getJSONObject(i).getString("name") : null;
                        String type = (!kurse.getJSONObject(i).isNull("type")) ? kurse.getJSONObject(i).getString("type") : null;

                        LinearLayout column = makeKursIcon(title, type);
                        ll.addView(column);
                        ll.setPadding(0, 0, 0, 0);
                    }
                    CardView cvKurse = (CardView) view.findViewById(R.id.main_cv_Kurse);
                    cvKurse.setVisibility(View.VISIBLE);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private LinearLayout makeKursIcon(final String title, final String kursType) {
        final float scale = getActivity().getResources().getDisplayMetrics().density;
        int widthIv = (int) (45 * scale + 0.5f);
        int height = (int) (45 * scale + 0.5f);
        int width = (int) (50 * scale + 0.5f);


        LinearLayout column = new LinearLayout(getActivity());
        LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                widthIv,
                height
        );
        ivParams.gravity = Gravity.CENTER_HORIZONTAL;
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                width,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        column.setLayoutParams(columnParams);
        column.setPadding(32, 0, 32, 0);
        column.setOrientation(LinearLayout.VERTICAL);

        if (kursType != null && !kursType.equals("offline")) {
            column.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), KursActivity.class);
                    intent.putExtra("name", title);
                    startActivity(intent);
                }
            });
        }


        ImageView iv = new ImageView(getActivity());
        iv.setBackgroundResource(KursIcon.getResourceIdByName(title));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);
        iv.setLayoutParams(ivParams);

        TextView tv = new TextView(getActivity());
        tv.setText(title);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(tvParams);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        if (kursType != null && kursType.equals("online")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tv.setTextColor(getResources().getColor(R.color.colorAccent, getActivity().getTheme()));
            } else {
                tv.setTextColor(getResources().getColor(R.color.colorAccent));
            }
        }

        column.addView(iv);
        column.addView(tv);

        return column;

    }

    @Override
    public void onStop() {
        super.onStop();
        isInForeground = false;
    }
}

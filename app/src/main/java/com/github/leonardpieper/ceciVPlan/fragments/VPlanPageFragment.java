package com.github.leonardpieper.ceciVPlan.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.leonardpieper.ceciVPlan.R;
import com.github.leonardpieper.ceciVPlan.tools.Kurse;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class VPlanPageFragment extends Fragment {
    private final String TAG = "VPlanPageFragment";

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private boolean isInForeground;

    private View view;

    private TableLayout tableStufe;

    private String oldDatum;
    private boolean isTutorialNeedful;

    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String ARG_SECTION_NAME = "section_name";

    public VPlanPageFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static VPlanPageFragment newInstance(int sectionNumber) {
        VPlanPageFragment fragment = new VPlanPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putString(ARG_SECTION_NAME, convertNumberToName(sectionNumber));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.content_vplan, container, false);
//        TextView textView = (TextView) rootView.findViewById(R.id.item_kurs_iv_title);
//        textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));

        mAuth = FirebaseAuth.getInstance();
        mRootRef = MyDatabaseUtil.getDatabase().getReference();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        tableStufe = (TableLayout) view.findViewById(R.id.vplan_tl_stufe);

        isConnectedToFirebaseDatabase();

        isTutorialNeedful = checkTutorialStatus();
        Button tutorialFinishBtn = (Button) view.findViewById(R.id.vplan_btn_tutorialFinish);
        tutorialFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("kursTutorialNeedful", false);
                editor.commit();

                CardView cvTut = (CardView) view.findViewById(R.id.vplan_cv_tutorial);
                cvTut.setVisibility(View.GONE);
                view.setBackgroundResource(android.R.color.transparent);
            }
        });

        if(isTutorialNeedful){
            CardView cvTut = (CardView) view.findViewById(R.id.vplan_cv_tutorial);
            cvTut.setVisibility(View.VISIBLE);
            view.setBackgroundResource(R.drawable.vplan_tutorial_background);
        }


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        isInForeground=true;

        if (mAuth.getCurrentUser() != null) {
            if (mFirebaseRemoteConfig.getBoolean("vplan_enabled")) {
                getFBData(getArguments().getString(ARG_SECTION_NAME));
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        isInForeground=false;
    }

    private void isConnectedToFirebaseDatabase() {
        DatabaseReference connectedRef = MyDatabaseUtil.getDatabase().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (isInForeground) {
                    ProgressBar offlineProg = (ProgressBar) view.findViewById(R.id.vplan_progBar_offline);
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

    private void getFBData(String stufe) {
        DatabaseReference conditionRef = mRootRef.child("vPlan");
        DatabaseReference stufenRef = conditionRef.child(stufe);

        stufenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (isInForeground) {
                    tableStufe.removeAllViews();

                    String stufe = dataSnapshot.getKey();
                    oldDatum = "99.99";
                    for (DataSnapshot vPlanSnapshot : dataSnapshot.getChildren()) {
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

                            TableRow row = addTableRow(stufe, fach, stunde, vertreter, raum, text);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "Cancelled");
                if(isInForeground){
                    tableStufe.removeAllViews();
                }
            }
        });
    }

    private TableRow addTableRow(String stufe, final String fach, String stunde, String lehrer, String raum, String text) {
        TableRow row = new TableRow(getActivity());
        row.setClickable(true);
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showContextMenu(fach);
                return true;
            }
        });

        TypedValue outValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.selectableItemBackground, outValue, true);
        row.setBackgroundResource(outValue.resourceId);

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


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            lesson.setText(Html.fromHtml(fach, Html.FROM_HTML_MODE_COMPACT));
            time.setText(Html.fromHtml(stunde, Html.FROM_HTML_MODE_COMPACT));
            tutor.setText(Html.fromHtml(lehrer, Html.FROM_HTML_MODE_COMPACT));
            room.setText(Html.fromHtml(raum, Html.FROM_HTML_MODE_COMPACT));
            extra.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        } else {
            lesson.setText(Html.fromHtml(fach));
            time.setText(Html.fromHtml(stunde));
            tutor.setText(Html.fromHtml(lehrer));
            room.setText(Html.fromHtml(raum));
            extra.setText(Html.fromHtml(text));
        }


        row.addView(lesson);
        row.addView(time);
        row.addView(tutor);
        row.addView(room);
        row.addView(extra);

        tableStufe.addView(row);

        return row;
    }

    private void addDateTableRow(String stufe, String tag, String datum) {
        TableRow row = new TableRow(getActivity());

        TextView day = new TextView(getActivity());
        TextView date = new TextView(getActivity());

        day.setPadding(15, 15, 5, 15);
        date.setPadding(15, 15, 5, 15);

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

        tableStufe.addView(row);

        oldDatum = datum;
    }

    private void showContextMenu(final String kursName) {
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

    private boolean checkTutorialStatus(){
        return PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("kursTutorialNeedful", true);
    }

    private static String convertNumberToName(int sectionNumer){
        switch (sectionNumer) {
            case 0:
                return "EF";
            case 1:
                return "Q1";
            case 2:
                return "Q2";
        }
        return null;
    }
}

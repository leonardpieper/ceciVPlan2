package com.github.leonardpieper.ceciVPlan.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.leonardpieper.ceciVPlan.CrawlerFinishListener;
import com.github.leonardpieper.ceciVPlan.R;
import com.github.leonardpieper.ceciVPlan.VPlanCrawler;
import com.github.leonardpieper.ceciVPlan.tools.EasterEgg;
import com.github.leonardpieper.ceciVPlan.tools.Kurse;
import com.github.leonardpieper.ceciVPlan.tools.MyDatabaseUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VPlanFragment extends Fragment {
    private final String TAG = "VPlanFragment";
    private static boolean isInForeground;

    private boolean isTutorialNeedful;

    private View view;

    private FirebaseAuth mAuth;

    private DatabaseReference mRootRef;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private TableLayout tableStufe;
    private String oldDatum;

    private int easterEggCounter = 0;


    private VPlanFragment.SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.app_bar_vplan, container, false);
        getActivity().setTitle("Vertretungsplan");

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new VPlanFragment.SectionsPagerAdapter(((AppCompatActivity) getActivity()).getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) view.findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);


//        mAuth = FirebaseAuth.getInstance();
//        mRootRef = MyDatabaseUtil.getDatabase().getReference();
//        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
//
//        tableStufe = (TableLayout) view.findViewById(R.id.vplan_tl_stufe);
//
//        FloatingActionButton fabEF = (FloatingActionButton) view.findViewById(R.id.vplan_fab_ef);
//        FloatingActionButton fabQ1 = (FloatingActionButton) view.findViewById(R.id.vplan_fab_q1);
//        FloatingActionButton fabQ2 = (FloatingActionButton) view.findViewById(R.id.vplan_fab_q2);
//
//        fabEF.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                changeStufe("EF");
//            }
//        });
//        fabQ1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                changeStufe("Q1");
//            }
//        });
//        fabQ2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                changeStufe("Q2");
//            }
//        });
//
//        isConnectedToFirebaseDatabase();
//
//        isTutorialNeedful = checkTutorialStatus();
//        Button tutorialFinishBtn = (Button) view.findViewById(R.id.vplan_btn_tutorialFinish);
//        tutorialFinishBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
//                SharedPreferences.Editor editor = preferences.edit();
//                editor.putBoolean("kursTutorialNeedful", false);
//                editor.commit();
//
//                CardView cvTut = (CardView) view.findViewById(R.id.vplan_cv_tutorial);
//                cvTut.setVisibility(View.GONE);
//                view.setBackgroundResource(android.R.color.transparent);
//            }
//        });
//
//        if(isTutorialNeedful){
//            CardView cvTut = (CardView) view.findViewById(R.id.vplan_cv_tutorial);
//            cvTut.setVisibility(View.VISIBLE);
//            view.setBackgroundResource(R.drawable.vplan_tutorial_background);
//        }
//
//        crawlVPlan();
//        if (mAuth.getCurrentUser() != null) {
//            if (mFirebaseRemoteConfig.getBoolean("vplan_enabled")) {
//                String stufe = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("jahrgang", "EF");
//                getFBData(stufe);
//            }
//        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        isInForeground=true;
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
                if (VPlanFragment.isInForeground) {
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

    private void crawlVPlan() {
        final VPlanCrawler vPlanCrawler = new VPlanCrawler();
        vPlanCrawler.addEventListener(new CrawlerFinishListener() {
            @Override
            public void handleCrawlFinishEvent(EventObject e) {
                List<String> htmls = vPlanCrawler.allHtmls;
                String[] stufen = new String[]{"Q2", "Q1", "EF"};

                if (!htmls.isEmpty()) {
                    for (int i = 0; i < htmls.size(); i++) {
                        String html = htmls.get(i);
                        String stufe = stufen[i];

                        String[] lines = html.split("\\r?\\n");
                        List data = new ArrayList();
                        for (int j = 0; j < lines.length; j++) {
                            if (j > 0) {
                                if (lines[j - 1].contains("<TD align=center>") && lines[j + 1].contains("</TD>")) {
                                    if (lines[j].contains("<B>") || lines[i].contains("</B>")) {
                                        lines[j] = lines[j].replace("<B>", "");
                                        lines[j] = lines[j].replace("</B>", "");
                                    }
                                    data.add(lines[j]);
                                } else if (lines[j].contains("<TD align=center>") && lines[j].contains("</TD>")) {
                                    lines[j] = lines[j].replace("<TD align=center>", "");
                                    lines[j] = lines[j].replace("</TD>", "");
                                    lines[j] = lines[j].replace("&nbsp;", "");
                                    data.add(lines[j]);
                                }
                            }
                        }
                        JSONArray jaStufe = dataToJSON(data);
                        try {
                            uploadFirebase(stufe, toList(jaStufe));
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });
        if (mFirebaseRemoteConfig.getBoolean("load_vplan_enabled")) {
            vPlanCrawler.execute("");
        }
    }

    private void getFBData(String stufe) {
        DatabaseReference conditionRef = mRootRef.child("vPlan");
        DatabaseReference stufenRef = conditionRef.child(stufe);

        stufenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (VPlanFragment.isInForeground) {
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
            }
        });
    }

    private TableRow addTableRow(String stufe, final String fach, String stunde, String lehrer, String raum, String text) {
        TableRow row = new TableRow(getActivity());
        row.setClickable(true);
        if(false) {
            row.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showContextMenu(fach);
                    return true;
                }
            });
        }

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

    private JSONArray dataToJSON(List data) {
        JSONArray root = new JSONArray();
        try {
            for (int i = 0; i < data.size(); i = i + 8) {
                JSONObject jsonAdd = new JSONObject();
                for (int j = 0; j <= 7; j++) {
                    switch (j) {
                        case 0:
                            jsonAdd.put("Tag", data.get(i + j));
                            break;
                        case 1:
                            jsonAdd.put("Datum", data.get(i + j));
                            break;
                        case 2:
                            jsonAdd.put("Klasse(n)", data.get(i + j));
                            break;
                        case 3:
                            jsonAdd.put("Stunde", data.get(i + j));
                            break;
                        case 4:
                            jsonAdd.put("Fach", data.get(i + j));
                            break;
                        case 5:
                            jsonAdd.put("Vertreter", data.get(i + j));
                            break;
                        case 6:
                            jsonAdd.put("Raum", data.get(i + j));
                            break;
                        case 7:
                            jsonAdd.put("Vertretungs-Text", data.get(i + j));
                            break;
                    }
                }
                root.put(jsonAdd);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return root;
    }

    public void changeStufe(String stufe) {
        getFBData(stufe);
        if (stufe.equals("Q1")) {
            getFBData("Q1");

            easterEggCounter++;
            if (easterEggCounter >= 10) {
                EasterEgg easterEgg = new EasterEgg(getActivity());
                try {
                    easterEgg.addEmoji("\uD83D\uDC36", "den Hund");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                easterEggCounter = 0;
            }
        }
//            case (R.id.fabMe):
//                tableEF.setVisibility(View.GONE);
//                tableQ1.setVisibility(View.GONE);
//                tableQ2.setVisibility(View.GONE);
//                tableMe.setVisibility(View.VISIBLE);
    }

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        if(v.getId()==R.id.vplan_tl_stufe){
//            TableLayout tl = (TableLayout) v;
//
//            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
//            TableRow row = (TableRow) tl.getChildAt(acmi.position);
//
//            MenuInflater inflater = getActivity().getMenuInflater();
//            inflater.inflate(R.menu.kurs_hinzufugen_menu, menu);
//        }
//    }

    private List toList(JSONArray array) throws JSONException {
        List list = new ArrayList();
        for (int i = 0; i < array.length(); i++) {
            list.add(fromJson(array.get(i)));
        }
        return list;
    }

    private Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap();
        Iterator keys = object.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, fromJson(object.get(key)));
        }
        return map;
    }

    private Object fromJson(Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return toMap((JSONObject) json);
        } else if (json instanceof JSONArray) {
            return toList((JSONArray) json);
        } else {
            return json;
        }
    }

    public void uploadFirebase(String jahrgang, List data) {
        FirebaseDatabase mData = MyDatabaseUtil.getDatabase();
        if (mAuth.getCurrentUser() != null) {
            DatabaseReference mRef = mData.getReference("vPlan/" + jahrgang);
            mRef.setValue(data);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends android.support.v4.app.Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static VPlanFragment.PlaceholderFragment newInstance(int sectionNumber) {
            VPlanFragment.PlaceholderFragment fragment = new VPlanFragment.PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tabbed, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return VPlanFragment.PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }
}

package com.github.leonardpieper.ceciVPlan.fragments;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.leonardpieper.ceciVPlan.CrawlerFinishListener;
import com.github.leonardpieper.ceciVPlan.R;
import com.github.leonardpieper.ceciVPlan.VPlanActivity;
import com.github.leonardpieper.ceciVPlan.VPlanCrawler;
import com.github.leonardpieper.ceciVPlan.tools.EasterEgg;
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

    private FirebaseAuth mAuth;

    private DatabaseReference mRootRef;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private TableLayout tableStufe;
    private String oldDatum;

    private int easterEggCounter = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        View view = inflater.inflate(R.layout.app_bar_vplan, container, false);

        tableStufe = (TableLayout)view.findViewById(R.id.vplan_tl_stufe);

        FloatingActionButton fabEF = (FloatingActionButton)view.findViewById(R.id.vplan_fab_ef);
        FloatingActionButton fabQ1 = (FloatingActionButton)view.findViewById(R.id.vplan_fab_q1);
        FloatingActionButton fabQ2 = (FloatingActionButton)view.findViewById(R.id.vplan_fab_q2);

        fabEF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStufe("EF");
            }
        });
        fabQ1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStufe("Q1");
            }
        });
        fabQ2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeStufe("Q2");
            }
        });

        crawlVPlan();
        if(mAuth.getCurrentUser()!=null){
            if(mFirebaseRemoteConfig.getBoolean("vplan_enabled")) {
                String stufe = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("jahrgang", "EF");
                getFBData(stufe);
            }
        }

        return view;
    }

    private void crawlVPlan(){
        final VPlanCrawler vPlanCrawler = new VPlanCrawler();
        vPlanCrawler.addEventListener(new CrawlerFinishListener() {
            @Override
            public void handleCrawlFinishEvent(EventObject e) {
                List<String> htmls = vPlanCrawler.allHtmls;
                String[] stufen = new String[]{"Q2", "Q1", "EF"};

                if(!htmls.isEmpty()){
                    for(int i=0; i<htmls.size(); i++){
                        String html = htmls.get(i);
                        String stufe = stufen[i];

                        String[] lines = html.split("\\r?\\n");
                        List data = new ArrayList();
                        for(int j = 0; j<lines.length; j++){
                            if(j>0){
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
        if(mFirebaseRemoteConfig.getBoolean("load_vplan_enabled")) {
            vPlanCrawler.execute("");
        }
    }

    private void getFBData(String stufe){
        DatabaseReference conditionRef = mRootRef.child("vPlan");
        DatabaseReference stufenRef = conditionRef.child(stufe);

        stufenRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(VPlanFragment.this.isVisible()) {
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

                            addTableRow(stufe, fach, stunde, vertreter, raum, text);
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

    private void addTableRow(String stufe, String fach, String stunde, String lehrer, String raum, String text) {
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


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            lesson.setText(Html.fromHtml(fach, Html.FROM_HTML_MODE_COMPACT));
            time.setText(Html.fromHtml(stunde, Html.FROM_HTML_MODE_COMPACT));
            tutor.setText(Html.fromHtml(lehrer, Html.FROM_HTML_MODE_COMPACT));
            room.setText(Html.fromHtml(raum, Html.FROM_HTML_MODE_COMPACT));
            extra.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));
        }else {
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

//        switch(stufe){
//            case "EF":
//                tableEF.addView(row);
//                break;
//            case "Q1":
//                tableQ1.addView(row);
//                break;
//            case "Q2":
//                tableQ2.addView(row);
//                break;
//            case "me":
//                tableMe.addView(row);
//                break;
//            default:
//                Log.d(TAG, "Stufe ist nicht erkannt!");
//        }
    }

    private void addDateTableRow(String stufe, String tag, String datum){
        TableRow row = new TableRow(getActivity());

        TextView day = new TextView(getActivity());
        TextView date = new TextView(getActivity());

        day.setPadding(15,15,5,15);
        date.setPadding(15,15,5,15);

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
//        switch(stufe){
//            case "EF":
//                tableEF.addView(row);
//                break;
//            case "Q1":
//                tableQ1.addView(row);
//                break;
//            case "Q2":
//                tableQ2.addView(row);
//                break;
//            case "me":
//                tableMe.addView(row);
//            default:
//                Log.d(TAG, "Stufe ist nicht erkannt!");
//        }

        oldDatum=datum;
    }

    private JSONArray dataToJSON(List data){
        JSONArray root = new JSONArray();
        try {
            for (int i = 0; i < data.size(); i=i+8) {
                JSONObject jsonAdd = new JSONObject();
                for(int j = 0; j<=7; j++) {
                    switch (j) {
                        case 0:
                            jsonAdd.put("Tag", data.get(i+j));
                            break;
                        case 1:
                            jsonAdd.put("Datum", data.get(i+j));
                            break;
                        case 2:
                            jsonAdd.put("Klasse(n)", data.get(i+j));
                            break;
                        case 3:
                            jsonAdd.put("Stunde", data.get(i+j));
                            break;
                        case 4:
                            jsonAdd.put("Fach", data.get(i+j));
                            break;
                        case 5:
                            jsonAdd.put("Vertreter", data.get(i+j));
                            break;
                        case 6:
                            jsonAdd.put("Raum", data.get(i+j));
                            break;
                        case 7:
                            jsonAdd.put("Vertretungs-Text", data.get(i+j));
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

    public void changeStufe(String stufe){
        getFBData(stufe);
            if(stufe.equals("Q1")) {
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

    public void uploadFirebase(String jahrgang, List data){
        FirebaseDatabase mData = FirebaseDatabase.getInstance();
        if(mAuth.getCurrentUser()!=null){
            DatabaseReference mRef = mData.getReference("vPlan/" + jahrgang);
            mRef.setValue(data);
        }
    }
}
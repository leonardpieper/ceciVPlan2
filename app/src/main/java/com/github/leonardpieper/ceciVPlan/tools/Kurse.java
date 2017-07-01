package com.github.leonardpieper.ceciVPlan.tools;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.leonardpieper.ceciVPlan.R;
import com.github.leonardpieper.ceciVPlan.fragments.KurseFragment;
import com.github.leonardpieper.ceciVPlan.models.Kurs;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Kurse {
    private Context context;

    private FirebaseAuth mAuth;
    private DatabaseReference mRootRef;

    public Kurse(Context context) {
        this.context = context;

        mAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Lässt einen einem Kurs beitreten.
     * Hier wird unterschieden zwischen "online" und "offline" Kursen.
     * Offline Kurse beinhalten nur die Benachrichtigungsfunktion
     *
     * @param name   Der Kursname
     * @param secret Das Kurspasswort. Bei offline Kursen ist dies nicht nötig
     * @param type   Der Kurstyp: "online" oder "offline"
     */
    public void joinKurs(String name, String secret, String type) {
        if (mAuth.getCurrentUser() != null) {
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

            KursCache kursCache = new KursCache(context);
            kursCache.addCache(name, type);
        } else {
            Toast t = Toast.makeText(context, "Für diese Aktion musst du angemeldet sein", Toast.LENGTH_LONG);
            t.show();
        }
    }

//    /**
//     *
//     * @param name
//     * @param secret
//     * @param type
//     */
//    public void editKurs(String name, String secret, String type){
//        if(mAuth.getCurrentUser()!=null){
//            HashMap<String, Object> user = new HashMap<String, Object>();
//
//            if (type.equals("offline")) {
//                user.put("name", name);
//                user.put("type", type);
//            } else {
//                user.put("name", name);
//                user.put("secret", secret);
//                user.put("type", type);
//            }
//
//            String refName = name.replace(".", "%2E");
//            refName = refName.toLowerCase();
//            mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").child(refName).setValue(user);
//
//
//            String fcmTopic = refName.replace(" ", "%20");
//            FirebaseMessaging.getInstance().subscribeToTopic(fcmTopic);
//
//            KursCache kursCache = new KursCache(context);
//            kursCache.editCache(name, type);
//        }
//    }

    /**
     * Lässt den Nutzer einen Kurs verlassen
     * @param name Name des zu verlassenden Kurses
     */
    public void leaveKurs(String name){
        String refName = name.replace(".", "%2E");
        refName = refName.toLowerCase();
        mRootRef.child("Users").child(mAuth.getCurrentUser().getUid()).child("Kurse").child(refName).removeValue();

        KursCache cache = new KursCache(context);
        cache.removeFromCache(name);
    }

    /**
     * Gibt alle aktuellen Kurse zurück.
     * Entweder von der Firebase Database oder aus dem Cache
     * @param valueEventListener Der Listener, der gecalled wird, wenn die die Kurse
     *                           aus der Firebase Database kommen
     * @return Gibt eine Liste an Kursen zurück
     */
    public List<Kurs> getKurse(ValueEventListener valueEventListener) {
        final KursCache kursCache = new KursCache(context);

        List<Kurs> kurses = new ArrayList<>();

        long cachedTime = kursCache.getCacheTime();
        long currMill = System.currentTimeMillis();

        if (cachedTime == -1 || cachedTime + 604800000 < currMill) {
            if (mAuth.getCurrentUser() != null) {
                getKurseOnline(valueEventListener);
            }
        } else {
            JSONObject root = kursCache.getCache();
            JSONArray kurse = null;

            try {
                kurse = root.getJSONArray("kurse");

                if (kurse == null || kurse.length() == 0) {
                    //Keine Kurse gefunden
                    return null;
                }

                for (int i = 0; i < kurse.length(); i++) {
                    String title = (!kurse.getJSONObject(i).isNull("name")) ? kurse.getJSONObject(i).getString("name") : null;
                    String secret = (!kurse.getJSONObject(i).isNull("secret")) ? kurse.getJSONObject(i).getString("secret") : null;
                    String type = (!kurse.getJSONObject(i).isNull("type")) ? kurse.getJSONObject(i).getString("type") : null;

                    Kurs kurs = new Kurs(title, secret, type);
                    kurses.add(kurs);
                }
                return kurses;

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Lädt die die Kurse aus der Firebase Database
     * @param valueEventListener Der Listener, der gecalled wird, wenn die die Kurse
     *                           aus der Firebase Database kommen
     * @return Gibt die Requestquery zurück
     */
    private Query getKurseOnline(ValueEventListener valueEventListener) {
        DatabaseReference kurseRef = mRootRef
                .child("Users")
                .child(mAuth.getCurrentUser().getUid())
                .child("Kurse");

        //Sortiert die Kurse nach online oder offline Typ
        Query kurseQuery = kurseRef.orderByChild("type");
        kurseQuery.addListenerForSingleValueEvent(valueEventListener);

        return kurseQuery;
    }
}

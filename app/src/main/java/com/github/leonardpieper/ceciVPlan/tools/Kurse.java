package com.github.leonardpieper.ceciVPlan.tools;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

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
}

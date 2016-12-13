package com.github.leonardpieper.ceciVPlan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Created by Leonard on 18.09.2016.
 */
public class NotifyService extends Service {

    private DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    @Override
    public void onCreate() {
        super.onCreate();
        //whatever else you have to to here...
        //android.os.Debug.waitForDebugger();  // this line is key

//        notificationsToday();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            notificationsToday();
        }

        return START_NOT_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void notificationBuilder(int changedHours, ArrayList<String> data){
        int id = (int) System.currentTimeMillis();

        NotificationManager notificationManager = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if(data.size()!=0){
            Intent notifyIntent = new Intent(getApplicationContext(), MainActivity.class);
            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, id, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext());

            if(data.size()<=1){
                //Eine Vertretung wird als Nachricht angezeigt
                notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.mipmap.ic_launcher_alpha)
                        .setContentTitle("Heutige Vertretungen")
                        .setContentText(data.get(0))
                        .setColor(getResources().getColor(R.color.colorAccent));
            }else{
                //Wenn mehr als eine Vertretung ist wird sie in Gruppen angezeigt
                notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.mipmap.ic_launcher_alpha)
                        .setContentText(String.valueOf(changedHours) + " geänderte Stunden heute")
                        .setContentTitle("Heutige Vertretungen")
                        .setColor(getResources().getColor(R.color.colorAccent));

                inboxStyle.setBigContentTitle("Heutige Vertretungen");

                for(String s : data){
                    inboxStyle.addLine(s);
                }

                notification.setStyle(inboxStyle);
            }


            notification.setAutoCancel(true);

            notificationManager.notify(id, notification.build());
        }


    }

    private void notificationsToday(){
        String output = new String();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
            DatabaseReference conditionRef = mRootRef.child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            String stufe = PreferenceManager.getDefaultSharedPreferences(NotifyService.this).getString("jahrgang", "EF");
            final DatabaseReference stufenRef = conditionRef.child(stufe);

            stufenRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    int changedHours = 0;
                    ArrayList data = new ArrayList();

                    Vertretungsplan vPlan = new Vertretungsplan();
                    String sToday = "";
                    String sTomorrow = "";
                    for(DataSnapshot vPlanSnapshot: dataSnapshot.getChildren()){
                        String datum = vPlanSnapshot.child("Datum").getValue(String.class);
                        if(!(datum == null || datum.contains("Datum"))) {

                            String fach = vPlanSnapshot.child("Fach").getValue(String.class);
                            String stunde = vPlanSnapshot.child("Stunde").getValue(String.class);
                            String vertreter = vPlanSnapshot.child("Vertreter").getValue(String.class);
                            String raum = vPlanSnapshot.child("Raum").getValue(String.class);
                            String text = vPlanSnapshot.child("Vertretungs-Text").getValue(String.class);

                            boolean bToday = vPlan.isToday(datum);

                            if(bToday){
                                changedHours = changedHours + 1;
                                if(vertreter.equals("+")){
                                    data.add(fach + " entfällt");
                                }else{
                                    data.add(fach + " heute in " + raum + " mit " + vertreter);
                                }

                            }
                        }
                    }
                    notificationBuilder(changedHours, data);

                    stopSelf();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }

    }
}

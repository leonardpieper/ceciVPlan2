package com.github.leonardpieper.ceciVPlan.tools;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import com.github.leonardpieper.ceciVPlan.MainActivity;
import com.github.leonardpieper.ceciVPlan.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Leonard on 02.03.2017.
 */

public class EasterEgg {
    private Context mContext;

    public EasterEgg(Context context){
        mContext = context;
    }

    public void createEasterEgg(String contentTitle){
        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.mipmap.ic_launcher_alpha)
                        .setContentTitle(contentTitle)
                        .setContentText(getEmojiCoount() + "/3 gefundene Emojis");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(mContext, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(9000, mBuilder.build());
    }

    public void addEmoji(String emojiPic, String emojiName) throws JSONException {
        if(!hasEmojiBeenDiscovered(emojiName)){
            addEmojiCount();
            createEasterEgg(emojiPic + " Du hast " + emojiName + " gefunden");

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            String emojis = preferences.getString("emojis", "{emojis:[]}");

            JSONObject joRoot = new JSONObject(emojis);
            JSONArray jaRoot = joRoot.getJSONArray("emojis");
            jaRoot.put(emojiName);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("emojiNames", joRoot.toString());
            editor.commit();
        }
        else{
            createEasterEgg(emojiPic + " Du hast den " + emojiName + " wiedergefunden");

        }
    }

    private void setEmojiCount(int emojiCount){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("emojiCount", emojiCount);
        editor.commit();
    }

    private void addEmojiCount(){
        setEmojiCount(getEmojiCoount() + 1);
    }

    private int getEmojiCoount(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return preferences.getInt("emojiCount", 0);
    }

    private boolean hasEmojiBeenDiscovered(String emojiName){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String emojiNames = preferences.getString("emojiNames", null);
        if(emojiNames!=null){
            if(emojiNames.contains(emojiName)){
                return true;
            }
        }
        return false;
    }

//    public Bitmap makIcon(){
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setTextSize(12);
//        paint.setColor(12);
//        paint.setTextAlign(Paint.Align.LEFT);
//        float baseline = -paint.ascent();
//
//        int width = (int) (paint.measureText("\uD83D\uDC36") + 0.5f);
//        int height = (int) (baseline + paint.descent() + 0.5f);
//
//        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(image);
//        canvas.drawText("\uD83D\uDC36", 0, baseline, paint);
//        return image;
//    }
//
//    private Bitmap getCircleBitmap(Bitmap bitmap) {
//        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
//                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        final Canvas canvas = new Canvas(output);
//
//        final int color = Color.RED;
//        final Paint paint = new Paint();
//        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
//        final RectF rectF = new RectF(rect);
//
//        paint.setAntiAlias(true);
//        canvas.drawARGB(0, 0, 0, 0);
//        paint.setColor(color);
//        canvas.drawOval(rectF, paint);
//
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
//        canvas.drawBitmap(bitmap, rect, rect, paint);
//
//        bitmap.recycle();
//
//        return output;
//    }
}

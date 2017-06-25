package com.github.leonardpieper.ceciVPlan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Leonard on 17.09.2016.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getBooleanExtra("dailyAlarm", false)){
            context.startService(new Intent(context, NotifyService.class));
//            Log.d("HI", "Bye");
        }

    }

}

package com.github.leonardpieper.ceciVPlan;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by Leonard on 30.08.2016.
 */
public class Vertretungsplan {

    public Vertretungsplan(){

    }

    public void getVPlanForToday(){
        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1;
    }

    public Boolean isToday(String datum){
        String[] data = datum.split(Pattern.quote("."));
        int dayOfMonth = Integer.parseInt(data[0]);
        int month = Integer.parseInt(data[1]);

        Calendar cal = Calendar.getInstance();
        int calDayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int calMonth = cal.get(Calendar.MONTH) + 1;

        if(dayOfMonth == calDayOfMonth && month == calMonth){
            return true;
        }
        return false;
    }
    public Boolean isTomorrow(String datum){
        String[] data = datum.split(Pattern.quote("."));
        int dayOfMonth = Integer.parseInt(data[0]);
        int month = Integer.parseInt(data[1]);

        Calendar cal = Calendar.getInstance();
        int calDayOfMonth = cal.get(Calendar.DAY_OF_MONTH) + 1;
        int calMonth = cal.get(Calendar.MONTH) + 1;

        if(dayOfMonth == calDayOfMonth && month == calMonth){
            return true;
        }
        return false;
    }

}

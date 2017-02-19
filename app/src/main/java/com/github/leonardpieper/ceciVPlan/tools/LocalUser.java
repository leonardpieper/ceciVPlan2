package com.github.leonardpieper.ceciVPlan.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.github.leonardpieper.ceciVPlan.SettingsActivity;

/**
 * Created by Leonard on 18.02.2017.
 */

public class LocalUser {
    private Context mContext;

    private int jahrgang;

    private boolean isTeacher;
    private String teacherName;

    public LocalUser(Context context){
        this.mContext = context;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        isTeacher = preferences.getBoolean("isTeacher", false);
        teacherName = preferences.getString("lehrer-abk", "");
        jahrgang = preferences.getInt("jahrgangNumber", 99);
    }

    public int getJahrgang(){return jahrgang;}

    public void setTeacherStatus(boolean isTeacher){
        this.isTeacher = isTeacher;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isTeacher", isTeacher);
        editor.commit();
    }
    public boolean getTeacherStatus(){
        return isTeacher;
    }

    public String getTeacherName(){
        return teacherName;
    }
    public void setTeacherName(String teacherName){
        this.teacherName = teacherName;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("lehrer-abk", teacherName);
        editor.commit();
    }

    public void resetAll(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("isTeacher");
        editor.remove("lehrer-abk");
        editor.remove("jahrgangNumber");
        editor.commit();
    }


}

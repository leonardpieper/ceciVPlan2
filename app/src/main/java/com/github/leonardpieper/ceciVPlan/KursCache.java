package com.github.leonardpieper.ceciVPlan;

import android.app.Application;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class KursCache extends Application{
    private Context context;
    private File cacheFile;

    public KursCache(Context context){
        this.context = context;
        cacheFile = new File(context.getFilesDir(), "kurseCache");
    }

    public void newCache(){
        FileOutputStream outputStream;
        JSONObject root = new JSONObject();

        try {
            cacheFile.createNewFile();
            outputStream = new FileOutputStream(cacheFile);
            outputStream.write(root.toString().getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addCache(String kurs){
        FileInputStream inputStream;
        FileOutputStream outputStream;
        StringBuffer strContent = new StringBuffer("");

        try {
            inputStream = new FileInputStream(cacheFile);

            int content;
            while ((content = inputStream.read())!=-1){
                strContent.append((char)content);
            }

            JSONObject root = new JSONObject(strContent.toString());

            long time= System.currentTimeMillis();
            root.put("mill", time);
            if(!root.has("kurse")){
                JSONArray kurse = new JSONArray();
                kurse.put(kurs);
                root.put("kurse", kurse);
            }else {
                JSONArray kurse = root.getJSONArray("kurse");
                kurse.put(kurs);
                root.put("kurse", kurse);
            }

            outputStream = new FileOutputStream(cacheFile);
            outputStream.write(root.toString().getBytes());
            outputStream.close();


        }  catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getCache(){
        FileInputStream inputStream;
        StringBuffer strContent = new StringBuffer("");

        try {
            inputStream = new FileInputStream(cacheFile);

            int content;
            while ((content = inputStream.read())!=-1){
                strContent.append((char)content);
            }

            JSONObject root = new JSONObject(strContent.toString());
            return  root;


        }  catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long getCacheTime(){
        FileInputStream inputStream;
        StringBuffer strContent = new StringBuffer("");

        try {
            inputStream = new FileInputStream(cacheFile);

            int content;
            while ((content = inputStream.read())!=-1){
                strContent.append((char)content);
            }

            JSONObject root = new JSONObject(strContent.toString());
            long time = root.getLong("mill");
            return  time;


        }  catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return -1;
    }

}

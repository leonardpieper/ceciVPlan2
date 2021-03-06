package com.github.leonardpieper.ceciVPlan.tools;

import android.app.Application;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
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

    public void addCache(String kursName, String kursType){
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
            JSONObject kursContent = new JSONObject();
            kursContent.put("name", kursName);
            kursContent.put("type", kursType);

            long time= System.currentTimeMillis();
            root.put("mill", time);
            if(!root.has("kurse")){
                JSONArray kurse = new JSONArray();
                kurse.put(kursContent);
                root.put("kurse", kurse);
            }else {
                JSONArray kurse = root.getJSONArray("kurse");
                kurse.put(kursContent);
                root.put("kurse", kurse);
            }

            outputStream = new FileOutputStream(cacheFile);
            outputStream.write(root.toString().getBytes());
            outputStream.close();


        }  catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public void editCache(String name, String type){
        removeFromCache(name);
        addCache(name, type);
    }

    public void removeFromCache(String kursName){
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


            if(root.has("kurse")){
                JSONArray kurse = root.getJSONArray("kurse");
                for(int i = 0; i<kurse.length(); i++){
//                    System.out.println(kurse.getJSONObject(i).getString("name"));
                    if(kurse.getJSONObject(i).getString("name").equals(kursName)) {
                        kurse = removeJsonObjectAtJsonArrayIndex(kurse, i);
                    }
                }
                root.put("kurse", kurse);
            }

            outputStream = new FileOutputStream(cacheFile);
            outputStream.write(root.toString().getBytes());
            outputStream.close();


        }  catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static JSONArray removeJsonObjectAtJsonArrayIndex(JSONArray source, int index) throws JSONException {
        if (index < 0 || index > source.length() - 1) {
            throw new IndexOutOfBoundsException();
        }

        final JSONArray copy = new JSONArray();
        for (int i = 0, count = source.length(); i < count; i++) {
            if (i != index) copy.put(source.get(i));
        }
        return copy;
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

    public  JSONObject getCacheKurs(String kursName) throws JSONException {
        JSONObject root = getCache();
        if(root!=null && root.has("kurse")){
            JSONArray kurse = root.getJSONArray("kurse");
            for(int i = 0; i<kurse.length(); i++){
                if(kurse.getJSONObject(i).getString("name").equals(kursName)) {
                    return kurse.getJSONObject(i);
                }
            }
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

    public boolean isCacheUpToDate(){
        long cachedTime = getCacheTime();
        long currMill = System.currentTimeMillis();
        if(cachedTime == -1 || cachedTime + 604800000 < currMill){
            return true;
        }
        return false;
    }

}

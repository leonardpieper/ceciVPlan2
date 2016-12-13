package com.github.leonardpieper.ceciVPlan;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Leonard on 26.09.2016.
 */
public class KlausurenCrawler extends AsyncTask<String, Void, String>{
    public List<String> allHtmls = new ArrayList<>();
    private String[] stufen;
    private List _listeners = new ArrayList();

    public synchronized void addEventListener(CrawlerFinishListener listener) {
        _listeners.add(listener);
    }

    public synchronized void removeEventListener(CrawlerFinishListener listener) {
        _listeners.remove(listener);
    }

    public KlausurenCrawler(){
        stufen = new String[]{"KlausurQ2", "KlausurQ1", "KlausurEF"};
    }

    @Override
    protected String doInBackground(String... params) {
        for(String stufe:stufen){
            try{
                URL u = new URL("http://vplan.ceci-bielefeld.de/Sdc_Internet_"+ stufe +".htm");
                HttpURLConnection ucon = (HttpURLConnection) u.openConnection();
                if(ucon.getResponseCode() > -1){
                    Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
                    Matcher m = p.matcher(ucon.getContentType());

                    String charset = m.matches() ? m.group(1) : "UTF-8";
                    Reader r = new InputStreamReader(ucon.getInputStream(), charset);
                    StringBuilder buf = new StringBuilder();
                    while (true){
                        int ch = r.read();
                        if (ch<0){
                            break;
                        }
                        buf.append((char) ch);
                    }

                    this.allHtmls.add(buf.toString());
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




        loadFinishEvent();
        if(allHtmls.isEmpty()){
            return "Keine Datenverbindung!";
        }else{
            return allHtmls.get(0);
        }

    }

    private synchronized void loadFinishEvent(){
        CrawlerFinishEvent event = new CrawlerFinishEvent(this);
        Iterator i = _listeners.iterator();
        while (i.hasNext()){
            ((CrawlerFinishListener) i.next()).handleCrawlFinishEvent(event);
        }
    }
}

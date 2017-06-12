package com.github.leonardpieper.ceciVPlan.models;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Kurs {
    public String name;
    public String secret;
    public String type;

    public Kurs(){

    }

    public Kurs(String name, String secret, String type){
        this.name = name;
        this.secret = secret;
        this.type = type;
    }

    @Exclude
    public Map<String, Object> toMap(){
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("secret", secret);
        result.put("type", type);
        return result;
    }

}

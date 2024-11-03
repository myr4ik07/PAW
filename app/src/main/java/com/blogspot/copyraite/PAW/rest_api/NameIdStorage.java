package com.blogspot.copyraite.PAW.rest_api;

import java.util.HashMap;

public class NameIdStorage {

    private HashMap<String, String> nameToIdMap;

    public NameIdStorage() {
        nameToIdMap = new HashMap<>();
    }

    public void addEntry(String name, String id) {
        nameToIdMap.put(name, id);
    }

    public String getIdByName(String name) {
        return nameToIdMap.get(name);
    }

}

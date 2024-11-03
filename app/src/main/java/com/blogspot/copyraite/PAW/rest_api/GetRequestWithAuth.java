package com.blogspot.copyraite.PAW.rest_api;

import android.os.AsyncTask;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.blogspot.copyraite.PAW.Other.ConfigLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class GetRequestWithAuth extends AsyncTask<Void, Void, ArrayList<JSONObject>> {

    private Spinner spinner;
    private NameIdStorage storageUser = new NameIdStorage();

    // Конструктор для передачі Spinner
    public GetRequestWithAuth(Spinner spinner, NameIdStorage storageUser) {
        this.spinner = spinner;
        this.storageUser = storageUser;
    }

    @Override
    protected ArrayList<JSONObject> doInBackground(Void... voids) {
        ArrayList<JSONObject> itemList = new ArrayList<>();

        JSONObject config = ConfigLoader.loadConfig(spinner.getContext());
        String api_url = config.optString("api_url");
        String api_login_default = config.optString("api_login_default");
        String api_pass_default = config.optString("api_pass_default");

        try {
            String login = api_login_default;
            String password = api_pass_default;
            String credentials = login + ":" + password;
            String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            // URL вашого API
            URL url = new URL("" + api_url + "/listUser"); // Вставте ваш API URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.forName("windows-1251")));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Парсимо JSON масив
                JSONArray jsonArray = new JSONArray(response.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    itemList.add(jsonObject);
                }
            } else {
                System.out.println("Error: " + connection.getResponseCode());
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemList;
    }

    @Override
    protected void onPostExecute(ArrayList<JSONObject> itemList) {

        ArrayList<String> names = new ArrayList<>();
        for (JSONObject jsonObject : itemList) {
            try {
                String name = jsonObject.getString("name");
                String uid = jsonObject.getString("uid");
                storageUser.addEntry(name, uid);
                names.add(name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Створюємо адаптер для Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(spinner.getContext(),
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Встановлюємо адаптер у Spinner
        spinner.setAdapter(adapter);

    }

}
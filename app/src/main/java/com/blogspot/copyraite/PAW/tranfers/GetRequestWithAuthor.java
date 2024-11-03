package com.blogspot.copyraite.PAW.tranfers;

import android.os.AsyncTask;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.blogspot.copyraite.PAW.Other.AuthPasswordSave;
import com.blogspot.copyraite.PAW.rest_api.NameIdStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class GetRequestWithAuthor extends AsyncTask<Void, Void, ArrayList<JSONObject>> {

    private Spinner spinner;
    private NameIdStorage storageUser;
    private AuthPasswordSave authPasswordSave;
    private JSONObject config;

    // Конструктор для передачі Spinner, контексту та інших необхідних даних
    public GetRequestWithAuthor(Spinner spinner, NameIdStorage storageUser, AuthPasswordSave authPasswordSave, JSONObject config) {
        this.spinner = spinner;
        this.storageUser = storageUser;
        this.authPasswordSave = authPasswordSave;
        this.config = config;
    }

    @Override
    protected ArrayList<JSONObject> doInBackground(Void... voids) {
        ArrayList<JSONObject> itemList = new ArrayList<>();

        try {
            // Отримання збережених даних авторизації
            String login = authPasswordSave.getEnterLogin();
            String password = authPasswordSave.getEnterPassword();
            String credentials = login + ":" + password;
            String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            String api_url = config.optString("api_url");

            URL url = new URL(api_url + "/listUser");

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

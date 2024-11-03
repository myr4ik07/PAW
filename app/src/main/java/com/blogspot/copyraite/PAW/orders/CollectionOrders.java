package com.blogspot.copyraite.PAW.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspot.copyraite.PAW.Other.AuthPasswordSave;
import com.blogspot.copyraite.PAW.Other.ConfigLoader;
import com.blogspot.copyraite.PAW.ModeSelection;
import com.blogspot.copyraite.PAW.R;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.util.Base64;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

public class CollectionOrders extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_orders);

        // Запускаємо асинхронний GET запит при відкритті Activity
        new GetRequestWithAuth().execute();

        // Дозволяємо контенту заходити під статус-бар і навігаційну панель
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION); // Додаємо для навігаційної панелі

    }

    private class GetRequestWithAuth extends AsyncTask<Void, Void, List<JSONObject>> {

        @Override
        protected List<JSONObject> doInBackground(Void... voids) {
            List<JSONObject> resultList = new ArrayList<>();
            try {

                // Отримання збереженого даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();

                String login = authPasswordSave.getEnterLogin();
                String password = authPasswordSave.getEnterPassword();
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplicationContext());
                String api_url = config.optString("api_url");

                URL url = new URL(api_url + "/listOrders");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", basicAuth);
                connection.setRequestProperty("Accept", "application/json");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
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
                        resultList.add(jsonObject);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Error: " + connection.getResponseCode(), Toast.LENGTH_SHORT).show();
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultList;
        }

        @Override
        protected void onPostExecute(List<JSONObject> resultList) {
            if (resultList != null && !resultList.isEmpty()) {

                // Список замовлень
                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
                List<MyAdapter.Item> items = new ArrayList<>();

                // Обробка отриманого списку об'єктів
                for (JSONObject jsonObject : resultList) {
                    try {
                        items.add(new MyAdapter.Item(jsonObject.getString("ref"), jsonObject.getString("recipient"), jsonObject.getString("uid")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                MyAdapter adapter = new MyAdapter(items);
                recyclerView.setAdapter(adapter);

            } else {
                Toast.makeText(getApplicationContext(), "Немає даних для вдіображення", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Кнопка "Назад"
    public void ButtonBack(View view) {
        Intent ModeSelectionPage = new Intent(this, ModeSelection.class);
        startActivity(ModeSelectionPage);
    }

}
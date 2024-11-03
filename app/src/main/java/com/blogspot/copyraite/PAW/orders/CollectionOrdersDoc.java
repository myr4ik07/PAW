package com.blogspot.copyraite.PAW.orders;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspot.copyraite.PAW.Other.AuthPasswordSave;
import com.blogspot.copyraite.PAW.Other.ConfigLoader;
import com.blogspot.copyraite.PAW.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CollectionOrdersDoc extends AppCompatActivity {

    String ref_id_doc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_collection_orders_doc);

        Intent intent = getIntent();
        ref_id_doc = intent.getStringExtra("REF_ID");
        if (ref_id_doc == null) {
            Toast.makeText(this, "REF_ID is missing", Toast.LENGTH_SHORT).show();
            finish(); // Закриваємо Activity, якщо REF_ID відсутній
            return;
        }

        // Запускаємо асинхронний GET запит при відкритті Activity
        new CollectionOrdersDoc.GetRequestDoc().execute();

    }

    private class GetRequestDoc extends AsyncTask<Void, Void, List<JSONObject>> {

        @Override
        protected List<JSONObject> doInBackground(Void... voids) {
            List<JSONObject> resultList = new ArrayList<>();
            try {

                // Отримання збереженого даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();

                String login = authPasswordSave.getEnterLogin();;
                String password = authPasswordSave.getEnterPassword();;
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplicationContext());
                String api_url = config.optString("api_url");

                URL url = new URL(api_url + "/listOrdersDoc?ref_id=" + ref_id_doc);
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

                RecyclerView recyclerView = findViewById(R.id.recyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

                TextView manager = findViewById(R.id.textView1);
                TextView responsible = findViewById(R.id.textView2);
                TextView warehouse = findViewById(R.id.textView3);
                TextView sum = findViewById(R.id.textView4);

                List<CollectionOrdersDocAdapter.Item> items = new ArrayList<>();

                // Обробка отриманого списку об'єктів
                for (JSONObject jsonObject : resultList) {
                    try {

                        if (!jsonObject.optString("manager").isEmpty()) {
                            manager.setText("Менеджер: " + jsonObject.getString("manager"));
                        }

                        if (!jsonObject.optString("responsible").isEmpty()) {
                            responsible.setText("Відповідальний: " + jsonObject.getString("responsible"));
                        }

                        if (!jsonObject.optString("warehouse").isEmpty()) {
                            warehouse.setText("Склад: " + jsonObject.getString("warehouse"));
                        }

                        if (!jsonObject.optString("sum").isEmpty()) {
                            sum.setText("Сума: " + jsonObject.getString("sum"));
                        }

                        items.add(new CollectionOrdersDocAdapter.Item(
                                jsonObject.getString("product"),
                                jsonObject.getString("quantity"),
                                jsonObject.getString("product_id")
                                )
                        );

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                CollectionOrdersDocAdapter adapter = new CollectionOrdersDocAdapter(items);
                recyclerView.setAdapter(adapter);

            } else {
                Toast.makeText(getApplicationContext(), "Error retrieving data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void ButtonBack(View view){
        Intent CollectionOrdersPage = new Intent(this, CollectionOrders.class);
        startActivity(CollectionOrdersPage);
    }

}
package com.blogspot.copyraite.PAW.orders;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import java.util.HashMap;
import java.util.List;

import android.os.AsyncTask;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

public class CollectionOrders extends AppCompatActivity {

    private HashMap<String, String> itemsMap = new HashMap<>(); // Зберігає ID та назви
    private List<String> itemNames = new ArrayList<>(); // Список лише для назв
    private HashMapSpinnerAdapter warehouseAdapter;
    String selectedWarehouseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_orders);

        // Дозволяємо контенту заходити під статус-бар і навігаційну панель
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION); // Додаємо для навігаційної панелі

        // Заповнюємо список складів із API
        new FetchWarehouseFromAPI().execute();

        // Встановлюємо обробник подій для Spinner
        Spinner spinner = findViewById(R.id.warehouseSpinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Отримуємо вибраний елемент
                selectedWarehouseId = (String) parent.getItemAtPosition(position);
                // Заповнюємо таблицю документів із API
                new FetchOrdersFromAPI().execute(selectedWarehouseId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Цей метод викликається, коли жоден елемент не вибрано
                recyclerViewClear();
            }

        });

    }

    private void recyclerViewClear() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        if (recyclerView.getAdapter() != null) {
            MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
            adapter.clearData();
            adapter.notifyDataSetChanged();
        }
    }

    private class FetchOrdersFromAPI extends AsyncTask<String, Void, List<JSONObject>> {

        @Override
        protected List<JSONObject> doInBackground(String... params) {
            List<JSONObject> resultList = new ArrayList<>();
            try {
                String warehouseId = params[0];

                recyclerViewClear();

                // Отримання збереженого даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();

                String login = authPasswordSave.getEnterLogin();
                String password = authPasswordSave.getEnterPassword();
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplicationContext());
                String api_url = config.optString("api_url");

                URL url = new URL(api_url + "/listOrders?warehouse_id=" + warehouseId);
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
                    Toast.makeText(getApplicationContext(), "Помилка: " + connection.getResponseCode(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getApplicationContext(), "Немає даних для відображення", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class FetchWarehouseFromAPI extends AsyncTask<Void, Void, HashMap<String, String>> {

        @Override
        protected HashMap<String, String> doInBackground(Void... voids) {
            HashMap<String, String> result = new HashMap<>();
            try {
                // Отримання збереженого даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();

                String login = authPasswordSave.getEnterLogin();
                String password = authPasswordSave.getEnterPassword();
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplication());
                String api_url = config.optString("api_url");

                URL url = new URL(api_url + "/listWarehouse");

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
                        String id = jsonObject.getString("uid"); // отримуємо ID
                        String name = jsonObject.getString("name"); // отримуємо назву
                        result.put(id, name); // Додаємо в map назву та ID
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(HashMap<String, String> result) {
            if (!result.isEmpty()) {

                Spinner warehouseSpinner = findViewById(R.id.warehouseSpinner);
                warehouseAdapter = new HashMapSpinnerAdapter(getApplicationContext(), result);
                warehouseSpinner.setAdapter(warehouseAdapter);

                itemsMap.clear();
                itemsMap.putAll(result); // Оновлюємо itemsMap
                itemNames.clear();
                itemNames.addAll(result.keySet()); // Додаємо лише назви в itemNames

                warehouseAdapter.notifyDataSetChanged(); // Оновлюємо Spinner після завантаження даних

            }
        }
    }

    // Кнопка "Назад"
    public void ButtonBack(View view) {
        Intent ModeSelectionPage = new Intent(this, ModeSelection.class);
        startActivity(ModeSelectionPage);
    }

}

class HashMapSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

    private Context context;
    private HashMap<String, String> hashMap;

    public HashMapSpinnerAdapter(Context context, HashMap<String, String> hashMap) {
        this.context = context;
        this.hashMap = hashMap;
    }

    @Override
    public int getCount() {
        return hashMap.size();
    }

    @Override
    public Object getItem(int position) {
        return hashMap.keySet().toArray()[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.warehouse_spinner, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.spinner_item_text);
        textView.setText(hashMap.get(hashMap.keySet().toArray()[position]));
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.warehouse_spinner, parent, false);
        }
        TextView textView = convertView.findViewById(R.id.spinner_item_text);
        textView.setText(hashMap.get(hashMap.keySet().toArray()[position]));
        return convertView;
    }

}

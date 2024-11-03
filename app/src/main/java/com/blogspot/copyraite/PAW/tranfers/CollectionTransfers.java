package com.blogspot.copyraite.PAW.tranfers;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspot.copyraite.PAW.ModeSelection;
import com.blogspot.copyraite.PAW.Other.AuthPasswordSave;
import com.blogspot.copyraite.PAW.Other.ConfigLoader;
import com.blogspot.copyraite.PAW.R;
import com.blogspot.copyraite.PAW.rest_api.NameIdStorage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CollectionTransfers extends AppCompatActivity {

    private Spinner statusSpinner, authorSpinner;
    private TextView startDateTextView, endDateTextView;
    private Button applyFilterButton;
    private RecyclerView recyclerView;
    private MyAdapterTransfer adapter;

    String id_warehouse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_collection_transfers);

        // Отримуємо ID, передане з попереднього Activity
        id_warehouse = getIntent().getStringExtra("id_warehouse");

        // Ініціалізація RecyclerView
        recyclerView = findViewById(R.id.recyclerView2);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Ініціалізація елементів
        statusSpinner = findViewById(R.id.statusSpinner);
        authorSpinner = findViewById(R.id.authorSpinner);
        startDateTextView = findViewById(R.id.startDateTextView);
        endDateTextView = findViewById(R.id.endDateTextView);
        applyFilterButton = findViewById(R.id.applyFilterButton);

        // Заповнення Spinner даними
        setupSpinners();

        // Вибір дати
        startDateTextView.setOnClickListener(view -> showDatePicker(startDateTextView));
        endDateTextView.setOnClickListener(view -> showDatePicker(endDateTextView));

        // Обробка натискання кнопки фільтра
        applyFilterButton.setOnClickListener(v -> applyFilter());

        // Викликаємо запит для отримання Переміщень з урахуванням фільтрів
        executeQuery();

        // Отримання даних з API (вибір автора)
        NameIdStorage storageUser = new NameIdStorage();
        AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();
        JSONObject config = ConfigLoader.loadConfig(getApplication());
        Spinner spinner = findViewById(R.id.authorSpinner);

        new GetRequestWithAuthor(spinner, storageUser, authPasswordSave, config).execute();

    }

    private void setupSpinners() {
        String[] statuses = {"Всі", "Активний", "Неактивний"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        String[] authors = {"Всі", "Admin", "Автор2", "Автор3"};
        ArrayAdapter<String> authorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, authors);
        authorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        authorSpinner.setAdapter(authorAdapter);
    }

    private void showDatePicker(final TextView dateTextView) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
            String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            dateTextView.setText(date);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void executeQuery() {

//        String selectedStatus = statusSpinner.getSelectedItem().toString();
        String selectedStartDate = startDateTextView.getText().toString();
        String selectedEndDate = endDateTextView.getText().toString();
        String selectedAuthor = authorSpinner.getSelectedItem().toString();

        // Викликаємо запит для отримання даних
        new FetchItemsTask().execute(id_warehouse, selectedStartDate.toString(), selectedEndDate.toString(), selectedAuthor.toString());
    }

    // Логіка застосування фільтра тут
    private void applyFilter() {
        // Викликаємо запит для отримання Переміщень з урахуванням фільтрів
        executeQuery();
    }

    public void ButtonBack(View view){
        Intent ModeSelectionPage = new Intent(this, ModeSelection.class);
        startActivity(ModeSelectionPage);
    }

    private class FetchItemsTask extends AsyncTask<String, Void, List<MyAdapterTransfer.Item>> {

        @Override
        protected List<MyAdapterTransfer.Item> doInBackground(String... params) {
            String idWarehouse = params[0];
            String startDate = params[1];
            String endDate = params[2];
            String author = params[3];

            List<MyAdapterTransfer.Item> items = new ArrayList<>();

            try {
                // Отримання збереженого даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();

                String login = authPasswordSave.getEnterLogin();
                String password = authPasswordSave.getEnterPassword();
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + android.util.Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplicationContext());
                String api_url = config.optString("api_url");

                String par = "";
                Boolean isFilter = false;

                if (!startDate.isEmpty()) {
                    isFilter = true;
                    par += "startDate=" + startDate;
                }

                if (!endDate.isEmpty()) {
                    if (isFilter) {
                        par += "&";
                    } else {
                        isFilter = true;
                    }
                    par += "endDate=" + endDate;
                }

                if (!author.isEmpty()) {
                    if (isFilter) {
                        par += "&";
                    } else {
                        isFilter = true;
                    }
                    par += "author=" + author;
                }

                if (!idWarehouse.isEmpty()) {
                    if (isFilter) {
                        par += "&";
                    } else {
                        isFilter = true;
                    }
                    par += "id_warehouse=" + idWarehouse;
                }

                if (isFilter) {
                    par = "?" + par;
                }


                URL url = new URL(api_url + "/listTransfers" + par);
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

                    // Обробка JSON-відповіді
                    JSONArray jsonArray = new JSONArray(response.toString());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String reference = jsonObject.getString("reference");
                        String referenceUid = jsonObject.getString("reference_uid");
                        String recipient = jsonObject.optString("recipient", "Невідомий отримувач");
                        String authorName = jsonObject.optString("author_name", "Невідомий автор");

                        items.add(new MyAdapterTransfer.Item(reference, recipient, referenceUid));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return items;
        }

        @Override
        protected void onPostExecute(List<MyAdapterTransfer.Item> items) {
            if (items.isEmpty()) {
                Toast.makeText(CollectionTransfers.this, "Немає даних для вдіображення", Toast.LENGTH_SHORT).show();
            } else {
                adapter = new MyAdapterTransfer(items);
                recyclerView.setAdapter(adapter);
            }
        }
    }

}

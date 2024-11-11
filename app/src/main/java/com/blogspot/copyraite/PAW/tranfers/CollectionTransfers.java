package com.blogspot.copyraite.PAW.tranfers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspot.copyraite.PAW.Other.AuthPasswordSave;
import com.blogspot.copyraite.PAW.Other.ConfigLoader;
import com.blogspot.copyraite.PAW.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CollectionTransfers extends AppCompatActivity {

    private HashMap<String, String> itemsMap = new HashMap<>(); // Зберігає ID та назви
    private List<String> itemNames = new ArrayList<>(); // Список лише для назв
    private com.blogspot.copyraite.PAW.tranfers.HashMapSpinnerAdapterConterparty warehouseAdapter;
    private com.blogspot.copyraite.PAW.tranfers.HashMapSpinnerAdapterConterparty conterpartyAdapter; // CH
    private EditText barcodeEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_transfers);

        // Дозволяємо контенту заходити під статус-бар і навігаційну панель
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION); // Додаємо для навігаційної панелі

        // Заповнюємо список складів із API
        new com.blogspot.copyraite.PAW.tranfers.CollectionTransfers.FetchWarehouseFromAPI().execute();

        // Заповнюємо список контрагентів із API
        new com.blogspot.copyraite.PAW.tranfers.CollectionTransfers.FetchCounterpartiesFromAPI().execute();

        barcodeEditText = findViewById(R.id.barcodeInput);
        barcodeEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    onBarcodeEntered(v.getText().toString());
                    return true;
                }
                return false;
            }
        });

        // Малюємо таблицю
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        TableRow headerRow = new TableRow(getApplicationContext());
        headerRow.addView(createProductTextView("Товар"));
        headerRow.addView(createEditableTextView("Кількість"));
        tableLayout.addView(headerRow);

    }

    // Натиснення кнопки "Добавити надходження"
    public void ClockButtonAddDocIncome(View view) {
        // Отримуємо доступ до TableLayout
        TableLayout tableLayout = findViewById(R.id.tableLayout);

        // Перевіряємо, чи таблиця порожня
        int rowCount = tableLayout.getChildCount();
        if (rowCount == 0) {
            Toast.makeText(getApplicationContext(), "Таблиця порожня", Toast.LENGTH_SHORT).show();
            return;  // Виходимо з методу, якщо таблиця порожня
        }

        // Перевіряємо, чи всі поля "Кількість" заповнені
        for (int i = 0; i < rowCount; i++) {
            TableRow row = (TableRow) tableLayout.getChildAt(i);
            EditText quantityEditText = (EditText) row.getChildAt(1);
            String quantityText = quantityEditText.getText().toString().trim();

            // Якщо поле "Кількість" порожнє, показуємо повідомлення і зупиняємо метод
            if (quantityText.isEmpty() || quantityText.equals("0")) {
                Toast.makeText(getApplicationContext(), "Заповніть усі поля кількості", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Якщо таблиця не порожня і всі кількості заповнені, виконуємо AddDocIncome
        new AddDocIncome().execute();
    }

    // Метод для виклику API додавання документу
    private class AddDocIncome extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... voids) {
            JSONObject result = new JSONObject();
            try {
                // Отримання збережених даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();
                String login = authPasswordSave.getEnterLogin();
                String password = authPasswordSave.getEnterPassword();
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplicationContext());
                String api_url = config.optString("api_url");

                Spinner warehouseSpinner = findViewById(R.id.warehouseSpinner);
                Spinner counterpartySpinner = findViewById(R.id.counterpartySpinner);
                String warehouse_id = warehouseSpinner.getSelectedItem().toString();
                String counterparty_id = counterpartySpinner.getSelectedItem().toString();

                URL url = new URL(api_url + "/addIncome?products=" + productListGet()
                        + "&warehouse_id=" + warehouse_id + "&counterparty_id=" + counterparty_id);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", basicAuth);
                connection.setRequestProperty("Accept", "application/json");

                // Перевіряємо статус відповіді
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charset.forName("windows-1251")));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    result = new JSONObject(response.toString());
                } else {
                    result.put("result", "Помилка: " + connection.getResponseCode());
                }
                connection.disconnect();
            } catch (Exception e) {
                try {
                    result.put("result", "Помилка: " + e.getMessage());
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null && result.has("result")) {
                try {
                    String resultString = result.get("result").toString();
                    Toast.makeText(getApplicationContext(), resultString, Toast.LENGTH_LONG).show();
                    if (resultString.equals("Успішно добавлено")) {
                        barcodeEditText.setText("");
                        clearTable();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Помилка обробки відповіді", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Помилка отримання даних", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void clearTable() {
        TableLayout tableLayout = findViewById(R.id.tableLayout);

        // Перевіряємо, чи є в таблиці більше ніж один рядок
        int rowCount = tableLayout.getChildCount();
        if (rowCount > 1) {
            // Видаляємо всі рядки, окрім першого (заголовка)
            tableLayout.removeViews(1, rowCount - 1);
        }
    }

    // Метод для виклику API отримання складів
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
                warehouseAdapter = new com.blogspot.copyraite.PAW.tranfers.HashMapSpinnerAdapterConterparty(getApplicationContext(), result);
                warehouseSpinner.setAdapter(warehouseAdapter);

                itemsMap.clear();
                itemsMap.putAll(result); // Оновлюємо itemsMap
                itemNames.clear();
                itemNames.addAll(result.keySet()); // Додаємо лише назви в itemNames

                warehouseAdapter.notifyDataSetChanged(); // Оновлюємо Spinner після завантаження даних

            }
        }

    }

    // Метод для виклику API отримання контрагентів
    private class FetchCounterpartiesFromAPI extends AsyncTask<Void, Void, HashMap<String, String>> {

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

                URL url = new URL(api_url + "/getCounterparties");

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

                Spinner conterpartySpinner = findViewById(R.id.counterpartySpinner);
                conterpartyAdapter = new com.blogspot.copyraite.PAW.tranfers.HashMapSpinnerAdapterConterparty(getApplicationContext(), result);
                conterpartySpinner.setAdapter(conterpartyAdapter);

                itemsMap.clear();
                itemsMap.putAll(result); // Оновлюємо itemsMap
                itemNames.clear();
                itemNames.addAll(result.keySet()); // Додаємо лише назви в itemNames

                conterpartyAdapter.notifyDataSetChanged(); // Оновлюємо Spinner після завантаження даних

            }
        }

    }

    // Метод при зміні тексту в Штрих-коді
    private void onBarcodeEntered(String barcode) {
        new GetNomenclatureTask().execute(barcode);
    }

    // Клас для виклику API з отриманим штрих-кодом
    private class GetNomenclatureTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... barcodes) {
            String barcode = barcodes[0];
            String nomenclatureName = null;

            try {
                // Отримання збережених даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();
                String login = authPasswordSave.getEnterLogin();
                String password = authPasswordSave.getEnterPassword();
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplicationContext());
                String api_url = config.optString("api_url");

                URL url = new URL(api_url + "/getNomenclature?barcode=" + barcode);
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

                    // Припустимо, API повертає JSON-об'єкт з назвою номенклатури у полі "name"
                    JSONObject responseObject = new JSONObject(response.toString());
                    nomenclatureName = responseObject.optString("name");
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return nomenclatureName;
        }

        @Override
        protected void onPostExecute(String nomenclatureName) {
            if (nomenclatureName != null) {
                updateTableRow(nomenclatureName);
            } else {
                Toast.makeText(getApplicationContext(), "Номенклатуру з таким штрих-кодом не знайдено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Метод для оновлення рядка таблиці на основі назви номенклатури
    private void updateTableRow(String nomenclatureName) {

        // Якщо номенклатура знайдена, збільшуємо кількість на +1
        if (nomenclatureName.equals("Номенклатура не знайдена")) {
            Toast.makeText(getApplicationContext(), "Номенклатуру з таким штрих-кодом не знайдено", Toast.LENGTH_SHORT).show();
            return;
        }

        // Отримуємо доступ до TableLayout
        TableLayout tableLayout = findViewById(R.id.tableLayout);

        boolean found = false;
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow row = (TableRow) tableLayout.getChildAt(i);
            String rowNomenclature = ((TextView) row.getChildAt(0)).getText().toString();

            // Якщо номенклатура знайдена, збільшуємо кількість на +1
            if (nomenclatureName.equals(rowNomenclature)) {
                EditText quantityEditText = (EditText) row.getChildAt(1);
                int currentQuantity = Integer.parseInt(quantityEditText.getText().toString());
                quantityEditText.setText(String.valueOf(currentQuantity + 1));
                found = true;
                break;
            }
        }

        // Якщо номенклатуру не знайдено, створюємо новий рядок з кількістю 1
        if (!found) {
            // Створюємо новий рядок таблиці
            TableRow tableRow = new TableRow(this);

            // Налаштовуємо параметри для рядка
            TableRow.LayoutParams params = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT);
            tableRow.setLayoutParams(params);

            // Створюємо TextView для колонки "Номенклатура"
            TextView nomenclatureTextView = createTextView(nomenclatureName);

            // Створюємо EditText для колонки "Кількість" з початковим значенням 1
            EditText quantityEditText = createEditableTextView("1");

            // Додаємо TextView і EditText до рядка
            tableRow.addView(nomenclatureTextView);
            tableRow.addView(quantityEditText);

            // Додаємо рядок до TableLayout
            tableLayout.addView(tableRow);
        }
    }

    // Кнопка сканування штрих-коду
    public void ClickButtonBarcodeScan(View view) {
        // Обробка натискання кнопки сканування штрих-коду
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Скануйте штрих-код");
        integrator.setCameraId(0);  // вибір камери (0 - задня, 1 - передня)
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    // Метод для обробки результату сканування Штрих-коду
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Сканування скасовано", Toast.LENGTH_LONG).show();
            } else {
                String barcode = result.getContents();
                EditText barcodeEditText = findViewById(R.id.barcodeEditText);
                barcodeEditText.setText(barcode);
                new GetNomenclatureTask().execute(barcode);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Натиснення кнопки "Видалити рядок"
    public void ClickButtonDeleteRow(View view) {
        // Отримуємо доступ до TableLayout
        TableLayout tableLayout = findViewById(R.id.tableLayout);

        // Перевіряємо, чи таблиця має рядки
        int rowCount = tableLayout.getChildCount();
        if (rowCount > 1) {
            // Створюємо AlertDialog для підтвердження
            new AlertDialog.Builder(this)
                    .setTitle("Підтвердження видалення")
                    .setMessage("Ви впевнені, що хочете видалити останній рядок?")
                    .setPositiveButton("Так", (dialog, which) -> {
                        // Якщо користувач підтвердив, видаляємо останній рядок
                        tableLayout.removeViewAt(rowCount - 1);
                    })
                    .setNegativeButton("Ні", (dialog, which) -> {
                        // Якщо користувач відмінив, нічого не робимо
                        dialog.dismiss();
                    })
                    .show();
        } else {
            // Якщо таблиця порожня, показуємо повідомлення
            Toast.makeText(getApplicationContext(), "Таблиця порожня", Toast.LENGTH_SHORT).show();
        }
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(getApplicationContext());
        textView.setText(text);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextColor(Color.BLACK);
        return textView;
    }

    private EditText createEditableTextView(String text) {
        EditText editText = new EditText(getApplicationContext());
        editText.setText(text);
        editText.setPadding(16, 16, 16, 16);
        editText.setBackgroundColor(Color.parseColor("#FFCDD2")); // Світло-рожевий фон
        editText.setTextColor(Color.BLACK);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return editText;
    }

    private TextView createProductTextView(String text) {
        TextView textView = new TextView(getApplicationContext());
        textView.setText(text);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextColor(Color.BLACK);

        // Задаємо ширину колонки "Товар" як половину стандартної ширини
        TableRow.LayoutParams params = new TableRow.LayoutParams(700, TableRow.LayoutParams.WRAP_CONTENT, 0);
        textView.setLayoutParams(params);

        // Увімкнути автоматичне перенесення тексту на новий рядок
        textView.setSingleLine(false);  // Вимикаємо обмеження в один рядок
        textView.setMaxLines(Integer.MAX_VALUE);  // Дозволяємо довільну кількість рядків
        textView.setEllipsize(null);  // Вимикаємо обрізання тексту
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        return textView;
    }

    public String productListGet() {

        JSONArray jsonArray = new JSONArray();
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);

        // Обхід всіх рядків таблиці
        for (int i = 1; i < tableLayout.getChildCount(); i++) { // Починаємо з 1, щоб пропустити заголовок
            TableRow row = (TableRow) tableLayout.getChildAt(i);

            JSONObject jsonObject = new JSONObject();
            // Перевіряємо, що рядок не порожній
            if (row != null && row.getChildCount() == 2) {
                try {
                    // Отримуємо значення кожної колонки
                    String nomenclature = ((TextView) row.getChildAt(0)).getText().toString();
                    String quantity = ((TextView) row.getChildAt(1)).getText().toString();

                    // Додаємо дані до JSON об'єкта
                    jsonObject.put("Nomenclature_Name", nomenclature);
                    jsonObject.put("quantity", quantity);
                    jsonObject.put("mumber_line", i);

                    // Додаємо JSON об'єкт до JSON масиву
                    jsonArray.put(jsonObject);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        return jsonArray.toString();

    }

}

class HashMapSpinnerAdapterConterparty extends BaseAdapter implements SpinnerAdapter {

    private Context context;
    private HashMap<String, String> hashMap;

    public HashMapSpinnerAdapterConterparty(Context context, HashMap<String, String> hashMap) {
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

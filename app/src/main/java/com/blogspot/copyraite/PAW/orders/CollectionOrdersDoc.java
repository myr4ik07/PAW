package com.blogspot.copyraite.PAW.orders;

import static android.app.ProgressDialog.show;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import java.util.Collections;
import java.util.List;

public class CollectionOrdersDoc extends AppCompatActivity {

    private EditText barcodeEditText;

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

        barcodeEditText = findViewById(R.id.barcodeEditText);
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

        barcodeEditText = findViewById(R.id.barcodeEditText);
        barcodeEditText.addTextChangedListener(new TextWatcher() {

//            Підсумок:
//            beforeTextChanged: Викликається перед зміною тексту.
//            onTextChanged: Викликається під час зміни тексту.
//            afterTextChanged: Викликається після зміни тексту, де ви викликаєте onBarcodeEntered(s.toString()).

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String barcode = s.toString();
//                onBarcodeEntered(barcode);
            }

        });

        // Запускаємо асинхронний GET запит при відкритті Activity
        new GetRequestDoc().execute();
    }

    private class GetRequestDoc extends AsyncTask<Void, Void, List<JSONObject>> {

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
                TableLayout tableLayout = findViewById(R.id.tableLayout);
                tableLayout.removeAllViews();

                // Заголовок таблиці
                TableRow headerRow = new TableRow(getApplicationContext());
                headerRow.setBackgroundColor(Color.parseColor("#75AEDF"));

                String[] headers = {"Товар", "План", "Факт", "Різниця"};
                for (String header : headers) {
                    TextView headerText = new TextView(getApplicationContext());
                    headerText.setText(header);
                    headerText.setTextColor(Color.BLACK);
                    headerText.setPadding(16, 16, 16, 16);
                    headerText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    headerRow.addView(headerText);
                }
                tableLayout.addView(headerRow);

                // Рядки даних
                for (JSONObject jsonObject : resultList) {
                    TableRow dataRow = new TableRow(getApplicationContext());

                    // Створюємо комірки для кожного рядка
                    TextView productText = createProductTextView(jsonObject.optString("product"));
                    TextView planText = createTextView(jsonObject.optString("quantity"));
                    EditText factText = createEditableTextView(jsonObject.optString("fact", "0"));

                    int plan = jsonObject.optInt("quantity", 0);
                    int fact = jsonObject.optInt("fact", 0);
                    int difference = plan - fact;
                    TextView differenceText = createTextView(String.valueOf(difference));

                    // Додаємо слухача змін для колонки `Факт`
                    factText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            try {
                                int newFact = Integer.parseInt(s.toString());
                                int newDifference = plan - newFact;
                                differenceText.setText(String.valueOf(newDifference));
                            } catch (NumberFormatException e) {
                                differenceText.setText(String.valueOf(plan));
                            }
                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                        }
                    });

                    // Додаємо комірки до рядка
                    dataRow.addView(productText);
                    dataRow.addView(planText);
                    dataRow.addView(factText);
                    dataRow.addView(differenceText);

                    tableLayout.addView(dataRow);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Помилка отримання даних", Toast.LENGTH_SHORT).show();
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
            TableRow.LayoutParams params = new TableRow.LayoutParams(600, TableRow.LayoutParams.WRAP_CONTENT, 0);
            textView.setLayoutParams(params);

            // Увімкнути автоматичне перенесення тексту на новий рядок
            textView.setSingleLine(false);  // Вимикаємо обмеження в один рядок
            textView.setMaxLines(Integer.MAX_VALUE);  // Дозволяємо довільну кількість рядків
            textView.setEllipsize(null);  // Вимикаємо обрізання тексту
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            return textView;
        }

    }

    public void ClickButtonPrintBarcode(View view) {
        // Обробка натискання кнопки "Друк штрих-коду"
    }

    public void ClickButtonComplete(View view) {
        // Обробка натискання кнопки "Укомплектовано"
        if (checkCompleteness(getApplicationContext(), (TableLayout) findViewById(R.id.tableLayout))) {
            new SetCompleteDocRequest().execute();
        }
    }

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

    // Метод для виклику API з отриманим штрих-кодом
    private void onBarcodeEntered(String barcode) {
        new GetNomenclatureTask().execute(barcode);
    }

    private class SetCompleteDocRequest extends AsyncTask<Void, Void, JSONObject> {

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

                URL url = new URL(api_url + "/operationCompleted?ref_id=" + ref_id_doc + "&productsCompleted=" + productsCompletedGet());
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

                    result.put("result", response.toString());

                } else {
                    result.put("result", "Помилка: " + connection.getResponseCode());
                    Toast.makeText(getApplicationContext(), "Помилка: " + connection.getResponseCode(), Toast.LENGTH_SHORT).show();
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
                String resultString = "";
                try {
                    resultString = new JSONObject(result.optString("result")).optString("result");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                Toast toast = Toast.makeText(getApplicationContext(), resultString, Toast.LENGTH_LONG);
                toast.setText(resultString);
                toast.show();
//                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Помилка отримання даних", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public String productsCompletedGet() {

        JSONArray jsonArray = new JSONArray();
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);

        // Обхід всіх рядків таблиці
        for (int i = 1; i < tableLayout.getChildCount(); i++) { // Починаємо з 1, щоб пропустити заголовок
            TableRow row = (TableRow) tableLayout.getChildAt(i);

            JSONObject jsonObject = new JSONObject();
            // Перевіряємо, що рядок не порожній
            if (row != null && row.getChildCount() == 4) {
                try {
                    // Отримуємо значення кожної колонки
                    String nomenclature = ((TextView) row.getChildAt(0)).getText().toString();
                    String plan = ((TextView) row.getChildAt(1)).getText().toString();
                    String fact = ((TextView) row.getChildAt(2)).getText().toString();

                    // Додаємо дані до JSON об'єкта
                    jsonObject.put("Nomenclature_Name", nomenclature);
                    jsonObject.put("plan", plan);
                    jsonObject.put("fact", fact);
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

    public Boolean checkCompleteness(Context context, TableLayout tableLayout) {
        double totalPlan = 0;
        double totalFact = 0;

        // Обхід всіх рядків таблиці, щоб обчислити підсумки
        for (int i = 1; i < tableLayout.getChildCount(); i++) { // Починаємо з 1, щоб пропустити заголовок
            TableRow row = (TableRow) tableLayout.getChildAt(i);

            // Перевіряємо, що рядок не порожній
            if (row != null && row.getChildCount() >= 3) {
                try {
                    // Отримуємо значення з колонок "План" та "Факт"
                    double plan = Double.parseDouble(((TextView) row.getChildAt(1)).getText().toString());
                    double fact = Double.parseDouble(((TextView) row.getChildAt(2)).getText().toString());

                    // Додаємо значення до загальної суми
                    totalPlan += plan;
                    totalFact += fact;

                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Помилка обробки числового значення в рядку", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }

        // Перевіряємо, чи рівні підсумки "План" і "Факт"
        if (totalPlan != totalFact) {
            Toast.makeText(context, "Є розбіжність в укомплектуванні", Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }

    }

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
        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        boolean found = false;
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow row = (TableRow) tableLayout.getChildAt(i);
            String rowNomenclature = ((TextView) row.getChildAt(0)).getText().toString();

            if (nomenclatureName.equals(rowNomenclature)) {
                // Отримуємо поточні значення колонок "Plan" і "Fact"
                TextView planTextView = (TextView) row.getChildAt(2); // Припустимо, що "Plan" у другій колонці
                TextView factTextView = (TextView) row.getChildAt(3); // Припустимо, що "Fact" у третій колонці

                // Оновлюємо значення "Plan" на +1 та "Fact" на -1
                int planValue = Integer.parseInt(planTextView.getText().toString());
                int factValue = Integer.parseInt(factTextView.getText().toString());
                planTextView.setText(String.valueOf(planValue + 1));
                factTextView.setText(String.valueOf(factValue - 1));

                found = true;
                break;
            }
        }
        if (found) {
            Toast.makeText(getApplicationContext(), "Номенклатуру не знайдено", Toast.LENGTH_SHORT).show();
        }
    }

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

}



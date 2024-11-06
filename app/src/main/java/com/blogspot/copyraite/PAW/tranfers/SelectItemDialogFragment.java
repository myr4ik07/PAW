package com.blogspot.copyraite.PAW.tranfers;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

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
import java.util.HashMap;
import java.util.List;
import android.util.Base64;

public class SelectItemDialogFragment extends DialogFragment {

    public interface OnItemSelectedListener {
        void onItemSelected(String selectedItem, String selectedId);
    }

    private OnItemSelectedListener listener;
    private HashMap<String, String> itemsMap = new HashMap<>(); // Зберігає ID та назви
    private List<String> itemNames = new ArrayList<>(); // Список лише для назв
    private Spinner spinner;
    private ArrayAdapter<String> adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (OnItemSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnItemSelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_select_item);

        spinner = dialog.findViewById(R.id.spinner);
        Button confirmButton = dialog.findViewById(R.id.confirm_button);

        // Ініціалізація адаптера для Spinner
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, itemNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Завантаження даних з API
        new FetchWarehouseFromAPI().execute();

        confirmButton.setOnClickListener(v -> {
            String selectedItemName = spinner.getSelectedItem().toString();
            String selectedId = itemsMap.get(selectedItemName); // Отримуємо ID за назвою

            // Передаємо вибраний ID в нове Activity
            Intent intent = new Intent(getContext(), CollectionTransfers.class);
            intent.putExtra("id_warehouse", selectedId); // Передача ID
            startActivity(intent); // Відкриваємо нове Activity

            listener.onItemSelected(selectedItemName, selectedId); // Передача вибраного елемента і ID назад
            dismiss(); // Закриваємо діалог
        });

        return dialog;
    }

    private class FetchWarehouseFromAPI extends AsyncTask<Void, Void, HashMap<String, String>> {

        @Override
        protected HashMap<String, String> doInBackground(Void... voids) {
            HashMap<String, String> result = new HashMap<>();
            try {
                // Отримання збереженого даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getActivity().getApplication();

                String login = authPasswordSave.getEnterLogin();;
                String password = authPasswordSave.getEnterPassword();;
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getActivity().getApplication());
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
                        result.put(name, id); // Додаємо в map назву та ID
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
                itemsMap.clear();
                itemsMap.putAll(result); // Оновлюємо itemsMap
                itemNames.clear();
                itemNames.addAll(result.keySet()); // Додаємо лише назви в itemNames
                adapter.notifyDataSetChanged(); // Оновлюємо Spinner після завантаження даних
            }
        }
    }
}

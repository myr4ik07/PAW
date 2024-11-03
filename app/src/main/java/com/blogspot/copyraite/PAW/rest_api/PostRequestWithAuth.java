package com.blogspot.copyraite.PAW.rest_api;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Base64;
import android.widget.Toast;

import com.blogspot.copyraite.PAW.ModeSelection;
import com.blogspot.copyraite.PAW.Other.ConfigLoader;

import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostRequestWithAuth extends AsyncTask<Void, Void, Integer> {

    private Context context;
    private String userLogin;
    private String userPass;

    public PostRequestWithAuth(Context context, String userLogin, String userPass) {
        this.context = context;
        this.userLogin = userLogin;
        this.userPass = userPass;
     }

    @Override
    protected Integer doInBackground(Void... voids) {
        try {
            String login = userLogin;
            String password = userPass;
            String credentials = login + ":" + password;
            String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            // URL вашого API
            JSONObject config = ConfigLoader.loadConfig(this.context);
            String api_url = config.optString("api_url");
            URL url = new URL("" + api_url + "/auth"); // Вставте ваш API URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true); // Дозволяємо запис в запиті

            // Формуємо JSON об'єкт для відправки
            JSONObject jsonRequest = new JSONObject();

            OutputStream os = connection.getOutputStream();
            os.write(jsonRequest.toString().getBytes("UTF-8"));
            os.close();

            // Повертаємо код відповіді
            return connection.getResponseCode();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Integer responseCode) {
        if (responseCode != null && responseCode == HttpURLConnection.HTTP_OK) {
            Intent intent = new Intent(context, ModeSelection.class);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Пароль введений не вірно", Toast.LENGTH_SHORT).show();
        }
    }

}

package com.blogspot.copyraite.PAW.Other;

import android.content.Context;

import com.blogspot.copyraite.PAW.R;

import org.json.JSONObject;
import java.io.InputStream;

public class ConfigLoader {

    public static JSONObject loadConfig(Context context) {
        try {
            // Відкриваємо файл з ресурсів
            InputStream inputStream = context.getResources().openRawResource(R.raw.config);

            // Зчитуємо дані у вигляді байт
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            // Конвертуємо байти у рядок
            String json = new String(buffer, "UTF-8");

            // Повертаємо JSON-об'єкт
            return new JSONObject(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
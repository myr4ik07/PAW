package com.blogspot.copyraite.PAW.orders;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.blogspot.copyraite.PAW.Other.AuthPasswordSave;
import com.blogspot.copyraite.PAW.Other.ConfigLoader;
import com.blogspot.copyraite.PAW.R;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class CollectionOrdersPhoto extends AppCompatActivity {

    private String product_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_collection_orders_photo);

        Intent intent = getIntent();
        product_id = intent.getStringExtra("product_id");
        if (product_id == null) {
            Toast.makeText(this, "product_id is missing", Toast.LENGTH_SHORT).show();
            finish(); // Закриваємо Activity, якщо product_id відсутній
            return;
        }

        // Запускаємо асинхронний GET запит при відкритті Activity
        new GetRequestPhoto().execute();
    }

    private class GetRequestPhoto extends AsyncTask<Void, Void, List<byte[]>> {

        @Override
        protected List<byte[]> doInBackground(Void... voids) {
            List<byte[]> resultList = new ArrayList<>();
            try {
                // Отримання збереженого даних авторизації
                AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();

                String login = authPasswordSave.getEnterLogin();
                String password = authPasswordSave.getEnterPassword();
                String credentials = login + ":" + password;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

                JSONObject config = ConfigLoader.loadConfig(getApplicationContext());
                String api_url = config.optString("api_url");

                URL url = new URL(api_url + "/listPhotoProduct?product_id=" + product_id);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", basicAuth);
                connection.setRequestProperty("Accept", "application/octet-stream"); // Вказуємо, що очікуємо бінарні дані

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    byte[] data = new byte[16384];
                    int nRead;
                    List<Byte> byteList = new ArrayList<>();

                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        for (int i = 0; i < nRead; i++) {
                            byteList.add(data[i]);
                        }
                    }
                    byte[] imageBytes = new byte[byteList.size()];
                    for (int i = 0; i < byteList.size(); i++) {
                        imageBytes[i] = byteList.get(i);
                    }
                    resultList.add(imageBytes); // Додаємо бінарні дані до списку

                    inputStream.close();

                    // Логування для перевірки даних
                    Log.d("CollectionOrdersPhoto", "Received data size: " + imageBytes.length);
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
        protected void onPostExecute(List<byte[]> resultList) {
            if (resultList != null && !resultList.isEmpty()) {
                ViewPager2 viewPager = findViewById(R.id.viewPager);
                if (viewPager != null) {
                    viewPager.setAdapter(new ImagePagerAdapter(resultList));
                } else {
                    Log.e("CollectionOrdersPhoto", "ViewPager2 is null");
                }
            } else {
                Toast.makeText(getApplicationContext(), "Помилка отримання даних", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Адаптер для ViewPager2 для відображення списку зображень
    private class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ViewHolder> {
        private final List<byte[]> imageDataList;

        public ImagePagerAdapter(List<byte[]> imageDataList) {
            this.imageDataList = imageDataList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            byte[] imageData = imageDataList.get(position);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
            holder.imageView.setImageBitmap(bitmap);
        }

        @Override
        public int getItemCount() {
            return imageDataList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }

    public void ButtonBack(View view) {
        Intent AuthPage = new Intent(getApplicationContext(), CollectionOrdersDoc.class);
        startActivity(AuthPage);
    }

}

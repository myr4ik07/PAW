package com.blogspot.copyraite.PAW;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspot.copyraite.PAW.Other.AuthPasswordSave;
import com.blogspot.copyraite.PAW.rest_api.GetRequestWithAuth;
import com.blogspot.copyraite.PAW.rest_api.NameIdStorage;
import com.blogspot.copyraite.PAW.rest_api.PostRequestWithAuth;

public class AuthIn extends AppCompatActivity {

    NameIdStorage storageUser = new NameIdStorage();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_in);

        // Отримання даних з API (вибір користувача)
        new GetRequestWithAuth(findViewById(R.id.list_usr), storageUser).execute();
    }

    // Кнока "Авторизація" натиснута
    public void ClickButtonAuth(View view){

        Spinner spinner = findViewById(R.id.list_usr);
        EditText editTextPassword = findViewById(R.id.input_pass);

        String enteredPassword = editTextPassword.getText().toString();

        if (spinner.getSelectedItem() == null) {
            Toast.makeText(this, "Користувач не вибраний!", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedItem = spinner.getSelectedItem().toString();

        // Збереження пароля, введеного користувачем, та вибраного користувача
        AuthPasswordSave authPasswordSave = (AuthPasswordSave) getApplication();
        authPasswordSave.setEnterPassword(enteredPassword);
        authPasswordSave.setEnterLogin(selectedItem);

        // Передача паролю вибраного користувача для перевірки в API
        new PostRequestWithAuth(this, selectedItem, enteredPassword ).execute();

    }

}
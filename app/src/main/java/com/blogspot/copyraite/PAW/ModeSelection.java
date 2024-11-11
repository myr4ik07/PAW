package com.blogspot.copyraite.PAW;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspot.copyraite.PAW.orders.CollectionOrders;
import com.blogspot.copyraite.PAW.tranfers.CollectionTransfers;
import com.blogspot.copyraite.PAW.tranfers.CollectionTransfersOld;
import com.blogspot.copyraite.PAW.tranfers.SelectItemDialogFragment;

public class ModeSelection extends AppCompatActivity implements SelectItemDialogFragment.OnItemSelectedListener {

    Boolean newPage = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mode_selection);
    }

    // Відкриваємо список Замовлень
    public void ClickButtonModeSelectionCollectionOrders(View view) {
        Intent CollectionOrdersPage = new Intent(this, CollectionOrders.class);
        startActivity(CollectionOrdersPage);
    }

    // Відкриваємо список Переміщень
    public void ClickButtonModeSelectionCollectionTransfers(View view) {
        Button openDialogButton = findViewById(R.id.button2);

        if (newPage) {
            Intent CollectionTransfersPage = new Intent(this, CollectionTransfers.class);
            startActivity(CollectionTransfersPage);
            newPage = false;
        } else {
            openDialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Відкриваємо діалогове вікно
                    SelectItemDialogFragment dialog = new SelectItemDialogFragment();
                    dialog.show(getSupportFragmentManager(), "SelectItemDialog");
                }
            });
        }
    }

    @Override
    public void onItemSelected(String selectedItem, String selectedId) {

        // Обробка вибору елемента
        // selectedItem - це назва вибраного елемента
        // selectedId - це його ідентифікатор

        // Отримуємо результат із діалогу і відображаємо
        Toast.makeText(this, "Вибрано: " + selectedItem, Toast.LENGTH_SHORT).show();

        if (selectedItem == null || selectedItem.isEmpty()) {
            Toast.makeText(this, "Склад не вибрано!", Toast.LENGTH_SHORT).show();
        } else {
            Intent CollectionTransfersPage = new Intent(this, CollectionTransfersOld.class);
            CollectionTransfersPage.putExtra("id_warehouse", selectedId); // Передаємо id_warehouse
            startActivity(CollectionTransfersPage);
        }

    }

    // Кнопка "Назад"
    public void ButtonBack(View view) {
        Intent AuthPage = new Intent(this, AuthIn.class);
        startActivity(AuthPage);
    }

}
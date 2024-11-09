package com.blogspot.copyraite.PAW.orders;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.print.PrintHelper;

import com.blogspot.copyraite.PAW.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

public class BarcodeView extends AppCompatActivity {

    private ImageView barcodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_barcode_view);

        // Ініціалізуємо ImageView після виклику setContentView
        barcodeImage = findViewById(R.id.barcodeImage);

        Intent intent = getIntent();
        String barcodeText = intent.getStringExtra("barcode");
        if (barcodeText == null) {
            Toast.makeText(this, "штрих-код відсутній", Toast.LENGTH_SHORT).show();
            finish(); // Закриваємо Activity, якщо barcode відсутній
            return;
        }

        // Генеруємо штрих-код
        Bitmap barcodeBitmap = generateBarcode(barcodeText);

        // Відображаємо штрих-код в ImageView
        if (barcodeBitmap != null) {
            barcodeImage.setImageBitmap(barcodeBitmap);
        } else {
            Toast.makeText(this, "Помилка при генерації штрих-коду", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap generateBarcode(String barcodeText) {
        Code128Writer writer = new Code128Writer();
        try {
            BitMatrix bitMatrix = writer.encode(barcodeText, BarcodeFormat.CODE_128, 600, 300);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Неочікувана помилка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public void ClickBarcodePrintToPrinter(View view) {
        printImageView(this, barcodeImage);
    }

    public static void printImageView(Context context, ImageView imageView) {
        // Перевіряємо, чи є зображення в ImageView
        if (imageView.getDrawable() == null) {
            return;
        }

        // Отримуємо Bitmap з ImageView
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        // Використовуємо PrintHelper для друку
        PrintHelper printHelper = new PrintHelper(context);
        printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);

        // Запускаємо процес друку
        printHelper.printBitmap("image_print", bitmap);
    }
}

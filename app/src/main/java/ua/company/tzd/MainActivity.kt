package ua.company.tzd

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Головна активність застосунку.
 * Тут розміщені кнопки для переходу до інших розділів.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Підключаємо файл розмітки activity_main.xml як вигляд для цієї активності
        setContentView(R.layout.activity_main)

        // Знаходимо кнопку за її ідентифікатором і додаємо обробник натиску
        findViewById<Button>(R.id.btnOrders).setOnClickListener {
            // TODO: коли буде створено екран замовлень, тут відкрити його через Intent
            // startActivity(Intent(this, OrdersActivity::class.java))
        }

        // Обробник для переходу до списку відправлених документів
        findViewById<Button>(R.id.btnSent).setOnClickListener {
            // TODO: реалізувати відкриття відповідної активності
            // startActivity(Intent(this, SentActivity::class.java))
        }

        // Кнопка налаштувань відкриватиме екран налаштувань програми
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            // TODO: реалізувати перехід на SettingsActivity
            // startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}

package ua.company.tzd

import android.os.Bundle
import android.widget.Button
// AppCompatActivity міститься у бібліотеці appcompat, яка додає підтримку
// сучасних можливостей на старіших версіях Android
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

        // За допомогою findViewById отримуємо кнопку з розмітки за її ID
        // і додаємо обробник натискання
        findViewById<Button>(R.id.btnOrders).setOnClickListener {
            // TODO: коли буде створено екран замовлень, тут відкрити його через Intent
            // startActivity(Intent(this, OrdersActivity::class.java))
        }

        // Аналогічно отримуємо кнопку відправлених документів
        // та навішуємо на неї обробник натискання
        findViewById<Button>(R.id.btnSent).setOnClickListener {
            // TODO: реалізувати відкриття відповідної активності
            // startActivity(Intent(this, SentActivity::class.java))
        }

        // Кнопка "Налаштування" відкриватиме екран з налаштуваннями програми
        // Обробник натискання поки що містить лише TODO
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            // TODO: реалізувати перехід на SettingsActivity
            // startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}

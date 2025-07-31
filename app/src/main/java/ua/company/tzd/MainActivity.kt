package ua.company.tzd

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.preference.PreferenceManager
import ua.company.tzd.utils.TextScaleHelper
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

        // Зчитуємо масштаб шрифту з налаштувань користувача через PreferenceManager
        // Значення зберігається у відсотках, тому ділимо на 100
        val zoom = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt("font_zoom", 100)
        val scale = zoom / 100f

        // Застосовуємо масштабування до усіх елементів інтерфейсу за один виклик
        TextScaleHelper.applyTextScale(this, findViewById(android.R.id.content), scale)

        // Отримуємо кнопки з розмітки
        val btnOrders = findViewById<Button>(R.id.btnOrders)
        val btnSent = findViewById<Button>(R.id.btnSent)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnProducts = findViewById<Button>(R.id.btnProducts)

        // Додаємо обробники натискань
        btnOrders.setOnClickListener {
            // TODO: коли буде створено екран замовлень, тут відкрити його через Intent
            // startActivity(Intent(this, OrdersActivity::class.java))
        }

        // Аналогічно отримуємо кнопку відправлених документів
        // та навішуємо на неї обробник натискання
        btnSent.setOnClickListener {
            // TODO: реалізувати відкриття відповідної активності
            // startActivity(Intent(this, SentActivity::class.java))
        }

        // Кнопка "Налаштування" відкриває екран SettingsActivity
        btnSettings.setOnClickListener {
            // Запускаємо активність налаштувань через Intent
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Кнопка "Товари" відкриває новий екран зі списком товарів
        btnProducts.setOnClickListener {
            // Створюємо Intent для запуску ProductListActivity
            val intent = Intent(this, ProductListActivity::class.java)
            startActivity(intent)
        }
    }
}

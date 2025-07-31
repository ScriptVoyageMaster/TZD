package ua.company.tzd.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import ua.company.tzd.R
import ua.company.tzd.utils.TextScaleHelper
import java.io.File

/**
 * Екран зі списком усіх водіїв, знайдених у локальних файлах замовлень.
 */
class DriversActivity : AppCompatActivity() {

    /** Список, що відображаємо у RecyclerView */
    private val drivers = mutableListOf<String>()

    /** Адаптер для RecyclerView */
    private lateinit var adapter: DriversAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drivers)

        // Масштабуємо інтерфейс згідно з налаштуваннями користувача
        TextScaleHelper.applyTextScale(this, findViewById(android.R.id.content))

        // Налаштовуємо список водіїв
        val recycler = findViewById<RecyclerView>(R.id.recyclerViewDrivers)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = DriversAdapter(drivers) { driver ->
            // При виборі водія відкриваємо нову активність зі списком його замовлень
            val intent = Intent(this, OrdersActivity::class.java)
            intent.putExtra("driver", driver)
            startActivity(intent)
        }
        recycler.adapter = adapter

        // Обробка жесту "потягни, щоб оновити"
        val swipeLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeLayout.setOnRefreshListener {
            loadDrivers()
            swipeLayout.isRefreshing = false
        }

        // Кнопка повернення на попередній екран
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        // Кнопка примусового перезавантаження списку
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadDrivers() }

        // Вперше завантажуємо дані при створенні активності
        loadDrivers()
    }

    /**
     * Скануємо локальну папку "orders" та формуємо унікальний список водіїв.
     */
    private fun loadDrivers() {
        val ordersDir = File(filesDir, "orders")
        val foundDrivers = mutableSetOf<String>()

        if (ordersDir.exists()) {
            ordersDir.listFiles()?.forEach { file ->
                if (file.extension == "xml") {
                    // Зчитуємо тег <водій> з поточного файлу
                    parseDriverTag(file)?.let { foundDrivers.add(it) }
                }
            }
        }

        // Оновлюємо дані адаптера
        drivers.clear()
        drivers.addAll(foundDrivers.sorted())
        adapter.notifyDataSetChanged()
    }

    /**
     * Повертає значення тегу <водій> з переданого XML-файлу, якщо такий існує.
     */
    private fun parseDriverTag(file: File): String? {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(file.inputStream(), null)

            var event = parser.eventType
            var driver: String? = null
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "водій") {
                    driver = parser.nextText()
                    break
                }
                event = parser.next()
            }
            driver
        } catch (e: Exception) {
            // У випадку помилки просто виводимо її у консоль
            e.printStackTrace()
            null
        }
    }

    /**
     * Простий адаптер, який показує список рядків та передає натискання назовні.
     */
    private class DriversAdapter(
        private val items: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<DriversAdapter.ViewHolder>() {

        /** ViewHolder містить посилання на елемент списку */
        class ViewHolder(view: View, val onClick: (String) -> Unit) : RecyclerView.ViewHolder(view) {
            private val text: TextView = view.findViewById(android.R.id.text1)
            private var current: String? = null

            init {
                view.setOnClickListener {
                    current?.let { onClick(it) }
                }
            }

            fun bind(name: String) {
                current = name
                text.text = name
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }
}

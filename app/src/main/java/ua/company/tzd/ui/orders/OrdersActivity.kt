package ua.company.tzd.ui.orders

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import ua.company.tzd.R
import ua.company.tzd.utils.TextScaleHelper
import ua.company.tzd.ui.orders.ParsedOrderInfo
import java.io.File

/**
 * Показує список замовлень, що належать обраному водію.
 */
class OrdersActivity : AppCompatActivity() {

    /** Список замовлень з короткою інформацією */
    private val orders = mutableListOf<ParsedOrderInfo>()
    private lateinit var adapter: OrdersAdapter
    private lateinit var driverName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        // Масштабуємо усі елементи інтерфейсу
        TextScaleHelper.applyTextScale(this, findViewById(android.R.id.content))

        driverName = intent.getStringExtra("driver") ?: ""

        val recycler = findViewById<RecyclerView>(R.id.recyclerViewOrders)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = OrdersAdapter(orders) { info ->
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("orderFilePath", info.file.absolutePath)
            startActivity(intent)
        }
        recycler.adapter = adapter

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        loadOrders()
    }

    /** Завантажуємо всі замовлення для переданого водія */
    private fun loadOrders() {
        val ordersDir = File(filesDir, "orders")
        val list = mutableListOf<ParsedOrderInfo>()

        if (ordersDir.exists()) {
            ordersDir.listFiles()?.forEach { file ->
                if (file.extension == "xml" && parseDriverTag(file) == driverName) {
                    // Отримуємо коротку інформацію з файлу
                    parseOrderInfo(file)?.let { info -> list.add(info) }
                }
            }
        }

        orders.clear()
        orders.addAll(list)
        adapter.notifyDataSetChanged()
    }

    /** Пошук значення тегу <водій> у файлі замовлення */
    private fun parseDriverTag(file: File): String? {
        return try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
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
            e.printStackTrace()
            null
        }
    }

    /** Перевіряємо чи містить файл тег <блокування> */
    private fun hasLockTag(file: File): Boolean {
        return try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(file.inputStream(), null)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "блокування") {
                    return true
                }
                event = parser.next()
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Парсить файл замовлення та повертає стислий опис.
     */
    private fun parseOrderInfo(file: File): ParsedOrderInfo? {
        return try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(file.inputStream(), null)

            var number = ""
            var date = ""
            var client = ""
            var totalWeight = 0.0
            var locked = false

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "номер" -> number = parser.nextText()
                        "дата" -> date = parser.nextText()
                        "клієнт" -> client = parser.nextText()
                        "вага" -> totalWeight += parser.nextText().toDoubleOrNull() ?: 0.0
                        "блокування" -> locked = true
                    }
                }
                event = parser.next()
            }
            ParsedOrderInfo(number, date, client, totalWeight, locked, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}

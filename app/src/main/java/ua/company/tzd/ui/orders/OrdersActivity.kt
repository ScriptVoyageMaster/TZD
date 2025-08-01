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
import java.io.File

/**
 * Показує список замовлень, що належать обраному водію.
 */
class OrdersActivity : AppCompatActivity() {

    /** Дані про файл замовлення та його стан блокування */
    data class OrderInfo(val file: File, val isLocked: Boolean)

    /** Список замовлень з ознакою заблокованого */
    private val orders = mutableListOf<OrderInfo>()
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
        val list = mutableListOf<OrderInfo>()

        if (ordersDir.exists()) {
            ordersDir.listFiles()?.forEach { file ->
                if (file.extension == "xml" && parseDriverTag(file) == driverName) {
                    val locked = hasLockTag(file)
                    list.add(OrderInfo(file, locked))
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

}

package ua.company.tzd.ui.orders

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.TextView
import org.apache.commons.net.ftp.FTPClient
import java.net.InetAddress
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import ua.company.tzd.R
import java.io.File
import android.view.View
import ua.company.tzd.ui.orders.DisplayOrderItem

/**
 * Екран деталізації замовлення.
 * Тут показуємо список позицій та одразу додаємо результати сканування.
 */
class OrderDetailActivity : AppCompatActivity() {

    /**
     * Список позицій для відображення.
     * Використовуємо [DisplayOrderItem], щоб показати дані на екрані
     * та уникнути конфлікту з логічним класом [OrderItem] із `Order.kt`.
     */
    private val items = mutableListOf<DisplayOrderItem>()

    /** Мапа код -> назва товару, зчитана з файлу products.xml */
    private val productNames = mutableMapOf<String, String>()

    /** Адаптер для RecyclerView */
    private lateinit var adapter: OrderItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        // Налаштовуємо список позицій
        val recycler = findViewById<RecyclerView>(R.id.recyclerOrderItems)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = OrderItemAdapter(items)
        recycler.adapter = adapter

        // Спершу завантажуємо файл products.xml, щоб мати назви товарів
        loadProducts()

        // Отримуємо шлях до файлу замовлення з Intent
        val path = intent.getStringExtra("orderFilePath") ?: return
        val orderFile = File(path)

        // Зчитуємо файл та заповнюємо список позицій
        loadOrder(orderFile)

        // Кнопка "Назад" просто закриває поточний екран
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        // При натисканні починаємо обробку замовлення та блокуємо його
        findViewById<Button>(R.id.btnStartScan).setOnClickListener {
            // Показуємо індикатор активного сканування
            findViewById<TextView>(R.id.scanStatus).visibility = View.VISIBLE
            // Отримуємо налаштування FTP та ім'я терміналу
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val host = prefs.getString("ftpHost", "") ?: ""
            val port = prefs.getInt("ftpPort", 21)
            val user = prefs.getString("ftpUser", "") ?: ""
            val pass = prefs.getString("ftpPass", "") ?: ""
            val importDir = prefs.getString("ftp_import_dir", "") ?: ""
            val processingDir = prefs.getString("ftp_processing_dir", "") ?: ""
            val deviceName = prefs.getString("device_name", "device") ?: "device"

            // Формуємо мітку блокування з поточною датою та часом
            val time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // Додаємо тег <блокування> всередину файлу замовлення
            val original = orderFile.readText()
            val idx = original.lastIndexOf("</")
            val lockedText = if (idx != -1) {
                original.substring(0, idx) + "<блокування>$deviceName,$time</блокування>" + original.substring(idx)
            } else {
                original + "\n<блокування>$deviceName,$time</блокування>"
            }
            orderFile.writeText(lockedText)

            // В окремому потоці передаємо файл на FTP
            Thread {
                val ftp = FTPClient()
                try {
                    ftp.connect(InetAddress.getByName(host), port)
                    if (ftp.login(user, pass)) {
                        ftp.enterLocalPassiveMode()
                        ftp.setFileType(FTPClient.BINARY_FILE_TYPE)

                        // Завантажуємо файл у каталог processing
                        orderFile.inputStream().use { inp ->
                            ftp.storeFile("$processingDir/${orderFile.name}", inp)
                        }
                        // Видаляємо з каталогу import, якщо такий файл існує
                        ftp.deleteFile("$importDir/${orderFile.name}")
                        ftp.logout()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try { ftp.disconnect() } catch (_: Exception) {}
                }
            }.start()

            Toast.makeText(this, "Замовлення заблоковано", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Читає XML-файл замовлення та формує список позицій.
     */
    private fun loadOrder(file: File) {
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(file.inputStream(), null)

            var event = parser.eventType
            var code = ""
            var name = ""
            var weight = 0.0

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        // Початок позиції
                        "позиція" -> {
                            code = ""
                            name = ""
                            weight = 0.0
                        }
                        // Код товару
                        "код" -> code = parser.nextText()
                        // Назва товару може бути у файлі, але беремо її з мапи продуктів
                        "назва" -> name = parser.nextText()
                        // Замовлена вага
                        "вага" -> weight = parser.nextText().toDoubleOrNull() ?: 0.0
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "позиція") {
                        // Кінець позиції – формуємо об'єкт для відображення і додаємо у список
                        val finalName = productNames[code] ?: if (name.isNotEmpty()) name else "???"
                        items.add(DisplayOrderItem(code, finalName, weight))
                    }
                }
                event = parser.next()
            }
            // Оновлюємо список на екрані
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Обробляє штрихкод та оновлює позицію у списку, якщо код знайдено.
     */
    private fun parseBarcode(code: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val posProduct = prefs.getInt("scanner_pos_product", 0)
        val lenProduct = prefs.getInt("scanner_len_product", 3)
        val posKg = prefs.getInt("scanner_pos_kg", 3)
        val lenKg = prefs.getInt("scanner_len_kg", 3)
        val posGr = prefs.getInt("scanner_pos_gr", 6)
        val lenGr = prefs.getInt("scanner_len_gr", 1)
        val posPack = prefs.getInt("scanner_pos_pack", 7)
        val lenPack = prefs.getInt("scanner_len_pack", 2)

        try {
            val productCode = code.substring(posProduct, posProduct + lenProduct)
            val kg = code.substring(posKg, posKg + lenKg).toInt()
            val gr = code.substring(posGr, posGr + lenGr).toInt()
            val packs = code.substring(posPack, posPack + lenPack).toInt()
            val weight = kg + gr / 10.0

            val item = items.find { it.code == productCode }
            if (item != null) {
                item.actualWeight += weight
                item.actualPacks += packs
                adapter.notifyDataSetChanged()
            }
            // Сховати індикатор, якщо обробка завершена
            findViewById<TextView>(R.id.scanStatus).visibility = View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Неможливо розпізнати штрихкод", Toast.LENGTH_SHORT).show()
            findViewById<TextView>(R.id.scanStatus).visibility = View.GONE
        }
    }

    /**
     * Зчитує локальний файл products.xml і заповнює мапу productNames.
     */
    private fun loadProducts() {
        val file = File(filesDir, "products.xml")
        if (!file.exists()) return
        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(file.inputStream(), null)

            var event = parser.eventType
            var code = ""
            var name = ""
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "product", "товар" -> { code = ""; name = "" }
                        "code", "код" -> code = parser.nextText()
                        "name", "назва" -> name = parser.nextText()
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "product" || parser.name == "товар") {
                        productNames[code] = name
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

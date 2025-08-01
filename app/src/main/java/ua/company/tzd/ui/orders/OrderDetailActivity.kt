package ua.company.tzd.ui.orders

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
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

/**
 * Екран деталізації замовлення.
 * Тут показуємо список позицій та одразу додаємо результати сканування.
 */
class OrderDetailActivity : AppCompatActivity() {

    /** Список позицій, зчитаних з файлу замовлення */
    private val items = mutableListOf<OrderItem>()

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

        // Отримуємо шлях до файлу замовлення з Intent
        val path = intent.getStringExtra("orderFilePath") ?: return
        val orderFile = File(path)

        // Зчитуємо файл та заповнюємо список позицій
        loadOrder(orderFile)

        // При натисканні починаємо обробку замовлення та блокуємо його
        findViewById<Button>(R.id.btnStartScan).setOnClickListener {
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
                        // Назва товару
                        "назва" -> name = parser.nextText()
                        // Замовлена вага
                        "вага" -> weight = parser.nextText().toDoubleOrNull() ?: 0.0
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "позиція") {
                        // Кінець позиції – додаємо її у список
                        items.add(OrderItem(code, name, weight))
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
        } catch (e: Exception) {
            Toast.makeText(this, "Неможливо розпізнати штрихкод", Toast.LENGTH_SHORT).show()
        }
    }
}

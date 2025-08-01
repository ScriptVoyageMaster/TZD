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
import androidx.preference.PreferenceManager
import org.apache.commons.net.ftp.FTPClient
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
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
            // Завантажуємо файли замовлень з FTP і після цього
            // оновлюємо список водіїв та зупиняємо анімацію оновлення
            downloadOrdersFromFtp {
                loadDrivers()
                swipeLayout.isRefreshing = false
            }
        }

        // Кнопка повернення на попередній екран
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        // Кнопка примусового перезавантаження списку
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            // Спочатку завантажуємо останні файли з FTP, а
            // коли операція завершиться, оновлюємо список
            downloadOrdersFromFtp {
                loadDrivers()
            }
        }

        // Вперше завантажуємо дані при створенні активності
        loadDrivers()
    }

    /**
     * Завантажуємо всі файли замовлень з FTP-сервера у локальну папку.
     *
     * Ця функція запускає окремий потік, у якому відбувається
     * підключення до FTP, копіювання файлів у директорію filesDir/orders
     * та по завершенню викликає передану функцію onComplete() на UI-потоці.
     */
    private fun downloadOrdersFromFtp(onComplete: () -> Unit) {
        Thread {
            // Зчитуємо налаштування FTP із спільних налаштувань програми
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val host = prefs.getString("ftpHost", "") ?: ""
            val port = prefs.getInt("ftpPort", 21)
            val user = prefs.getString("ftpUser", "") ?: ""
            val pass = prefs.getString("ftpPass", "") ?: ""
            val importDir = prefs.getString("ftp_import_dir", "") ?: ""
            val processingDir = prefs.getString("ftp_processing_dir", "") ?: ""

            val ftpClient = FTPClient()
            try {
                // Підключаємося до вказаного FTP-сервера
                ftpClient.connect(InetAddress.getByName(host), port)
                if (ftpClient.login(user, pass)) {
                    // Встановлюємо пасивний режим та двійковий тип передачі
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

                    // Папка, куди будемо зберігати завантажені файли
                    val ordersDir = File(filesDir, "orders")
                    if (!ordersDir.exists()) ordersDir.mkdirs()

                    // Спершу завантажуємо файли з папки імпорту
                    val importFiles = ftpClient.listFiles(importDir)
                    for (file in importFiles) {
                        if (file.name.endsWith(".xml")) {
                            val localFile = File(ordersDir, file.name)
                            FileOutputStream(localFile).use { out ->
                                ftpClient.retrieveFile("$importDir/${file.name}", out)
                            }
                        }
                    }

                    // Далі опрацьовуємо файли з каталогу processing
                    val procFiles = ftpClient.listFiles(processingDir)
                    for (file in procFiles) {
                        if (file.name.endsWith(".xml")) {
                            val baos = ByteArrayOutputStream()
                            ftpClient.retrieveFile("$processingDir/${file.name}", baos)
                            val text = baos.toString("UTF-8")
                            baos.close()
                            // Якщо файл вже містить тег блокування - пропускаємо його
                            if (text.contains("<блокування>")) continue

                            // Додаємо локальний прапор <блокування/>
                            val insertIndex = text.lastIndexOf("</")
                            val updated = if (insertIndex != -1) {
                                text.substring(0, insertIndex) + "<блокування/>" + text.substring(insertIndex)
                            } else {
                                text + "\n<блокування/>"
                            }

                            val localFile = File(ordersDir, file.name)
                            localFile.writeText(updated)
                        }
                    }

                    ftpClient.logout()
                }
            } catch (e: Exception) {
                // У разі виникнення помилки просто виводимо її у консоль
                e.printStackTrace()
            } finally {
                try {
                    ftpClient.disconnect()
                } catch (_: Exception) {
                }
                // Повертаємося на головний потік та викликаємо колбек
                runOnUiThread { onComplete() }
            }
        }.start()
    }

    /**
     * Скануємо локальну папку "orders" та формуємо унікальний список водіїв.
     */
    private fun loadDrivers() {
        // Папка, де зберігаються файли замовлень
        val ordersDir = File(filesDir, "orders")
        // Множина для збирання унікальних імен водіїв
        val foundDrivers = mutableSetOf<String>()

        // Перевіряємо, що папка існує і це саме каталог
        if (ordersDir.exists() && ordersDir.isDirectory) {
            // Отримуємо список лише xml-файлів або порожній список, якщо файлів немає
            val xmlFiles = ordersDir.listFiles()?.filter { it.extension == "xml" } ?: emptyList()
            // Проходимося по кожному файлу та шукаємо тег <водій>
            xmlFiles.forEach { file ->
                parseDriverTag(file)?.let { foundDrivers.add(it) }
            }
        }

        // Оновлюємо список, який використовується адаптером
        // Спочатку очищаємо попередні дані
        drivers.clear()
        // Додаємо знайдених водіїв у відсортованому порядку
        drivers.addAll(foundDrivers.sorted())
        // Повідомляємо адаптер, що набір даних змінився,
        // щоб RecyclerView перемалював список
        adapter.notifyDataSetChanged()

        // Показуємо підказку, якщо водіїв не знайдено
        val emptyView = findViewById<TextView>(R.id.emptyTextView)
        // Якщо список порожній, робимо повідомлення видимим,
        // інакше приховуємо його
        emptyView.visibility = if (drivers.isEmpty()) View.VISIBLE else View.GONE
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

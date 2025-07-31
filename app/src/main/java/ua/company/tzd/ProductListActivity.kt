package ua.company.tzd

import android.os.Bundle
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.net.ftp.FTPClient
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.InetAddress

/**
 * Екран для відображення списку товарів, отриманого з FTP-сервера.
 *
 * Додаємо багато детальних коментарів, щоб будь-хто міг зрозуміти,
 * що саме відбувається у коді.
 */
class ProductListActivity : AppCompatActivity() {

    /** Таблиця у якій будемо показувати товари */
    private lateinit var tableLayout: TableLayout

    /** Клієнт для роботи з FTP */
    private val ftpClient = FTPClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Підключаємо розмітку activity_product_list.xml
        setContentView(R.layout.activity_product_list)

        // Знаходимо таблицю у розмітці за її ID
        tableLayout = findViewById(R.id.tableLayoutProducts)

        // Зчитуємо масштаб шрифтів з налаштувань
        val prefs = getSharedPreferences("tzd_settings", MODE_PRIVATE)
        val uiScale = prefs.getInt("ui_font_scale", 100) / 100.0f

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        btnBack.textSize = btnBack.textSize * uiScale
        btnRefresh.textSize = btnRefresh.textSize * uiScale

        // Кнопка "Назад" просто закриває поточну активність
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Кнопка "Оновити" завантажує файл з FTP і відображає результат
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            downloadAndShowProducts()
        }

        // Відразу при відкритті екрану пробуємо завантажити дані
        downloadAndShowProducts()
    }

    /**
     * Завантажити файл з FTP-сервера та заповнити таблицю отриманими даними.
     *
     * Операція мережі виконується у окремому потоці, адже Android не дозволяє
     * робити це на головному UI-потоці.
     */
    private fun downloadAndShowProducts() {
        // Запускаємо новий потік для завантаження файлу
        Thread {
            try {
                // Отримуємо налаштування FTP із SharedPreferences,
                // які ми зберігали у SettingsActivity
                val prefs = getSharedPreferences("tzd_settings", MODE_PRIVATE)
                val host = prefs.getString("ftpHost", "") ?: ""
                val port = prefs.getInt("ftpPort", 21)
                val user = prefs.getString("ftpUser", "") ?: ""
                val pass = prefs.getString("ftpPass", "") ?: ""
                // Каталог імпорту на FTP, може бути порожнім
                val importDir = prefs.getString("ftpImportDir", "") ?: ""

                // Підключаємося до FTP-сервера за отриманими реквізитами
                ftpClient.connect(InetAddress.getByName(host), port)

                // Логін з вказаним логіном та паролем
                if (ftpClient.login(user, pass)) {
                    // Переходимо в пасивний режим та обираємо двійковий тип файлів
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

                    // Витягаємо файл products.xml з налаштованої папки імпорту
                    val inputStream: InputStream = ftpClient.retrieveFileStream(importDir + "/products.xml")

                    // Створюємо XML парсер для читання файлу
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(inputStream, null)

                    // Список пар "код" - "назва" для кожного товару
                    val products = mutableListOf<Pair<String, String>>()

                    var event = parser.eventType
                    var code = ""
                    var name = ""

                    // Читаємо XML доки не дійдемо до його кінця
                    while (event != XmlPullParser.END_DOCUMENT) {
                        val tag = parser.name
                        when (event) {
                            XmlPullParser.START_TAG -> {
                                when (tag) {
                                    // Початок елемента <product>
                                    "product" -> {
                                        code = ""
                                        name = ""
                                    }
                                    // Елемент <code>
                                    "code" -> code = parser.nextText()
                                    // Елемент <name>
                                    "name" -> name = parser.nextText()
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                // Кінець елемента <product> - додаємо товар у список
                                if (tag == "product") {
                                    products.add(Pair(code, name))
                                }
                            }
                        }
                        event = parser.next()
                    }

                    // Закриваємо потік та розриваємо з'єднання з сервером
                    inputStream.close()
                    ftpClient.logout()
                    ftpClient.disconnect()

                    // Повертаємося на головний потік, щоб оновити інтерфейс
                    runOnUiThread {
                        // Масштаб шрифтів, заданий користувачем у налаштуваннях
                        val scale = prefs.getInt("ui_font_scale", 100) / 100.0f
                        // Спочатку очищаємо таблицю від попередніх рядків
                        tableLayout.removeAllViews()
                        // Для кожного товару створюємо рядок і додаємо у таблицю
                        for ((c, n) in products) {
                            val row = TableRow(this)

                            val tvCode = TextView(this)
                            val tvName = TextView(this)

                            tvCode.text = c
                            tvName.text = n
                            // Довгі назви переносимо на декілька рядків
                            tvName.setSingleLine(false)
                            tvName.setMaxLines(3)
                            tvName.ellipsize = null

                            // Невеликий відступ для кращого вигляду
                            tvCode.setPadding(16, 16, 16, 16)
                            tvName.setPadding(16, 16, 16, 16)
                            // Змінюємо розмір шрифту відповідно до зуму
                            tvCode.textSize = tvCode.textSize * scale
                            tvName.textSize = tvName.textSize * scale

                            row.addView(tvCode)
                            row.addView(tvName)
                            tableLayout.addView(row)
                        }
                    }
                }
            } catch (e: Exception) {
                // У разі помилки просто виводимо стек трасації у лог
                e.printStackTrace()
            }
        }.start()
    }
}

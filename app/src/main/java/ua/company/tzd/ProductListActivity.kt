package ua.company.tzd

import android.os.Bundle
import android.widget.Button
import android.widget.TableLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import android.util.TypedValue
import ua.company.tzd.utils.TextScaleHelper
import org.apache.commons.net.ftp.FTPClient
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
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

        // Масштабуємо усі елементи згідно з налаштуваннями користувача
        TextScaleHelper.applyTextScale(this, findViewById(android.R.id.content))

        // Знаходимо таблицю у розмітці за її ID
        tableLayout = findViewById(R.id.tableLayoutProducts)

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
                // Отримуємо налаштування, збережені у SettingsActivity
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                val host = prefs.getString("ftpHost", "") ?: ""
                val port = prefs.getInt("ftpPort", 21)
                val user = prefs.getString("ftpUser", "") ?: ""
                val pass = prefs.getString("ftpPass", "") ?: ""
                // Каталог, у якому розміщено файл products.xml на сервері
                val importDir = prefs.getString("ftp_import_dir", "") ?: ""
                // Масштаб шрифту для відображення тексту
                val scale = TextScaleHelper.getTextScale(this)
                
                // Підключаємося до FTP-сервера за отриманими реквізитами
                ftpClient.connect(InetAddress.getByName(host), port)

                // Логін з вказаним логіном та паролем
                if (ftpClient.login(user, pass)) {
                    // Переходимо в пасивний режим та обираємо двійковий тип файлів
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)

                    // Завантажуємо файл products.xml у локальну папку
                    val localFile = File(filesDir, "products.xml")
                    FileOutputStream(localFile).use { out ->
                        ftpClient.retrieveFile("$importDir/products.xml", out)
                    }

                    // Створюємо XML парсер для читання файлу
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(localFile.inputStream(), null)

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
                        // Спочатку очищаємо таблицю від попередніх рядків
                        tableLayout.removeAllViews()
                        // Для кожного товару створюємо рядок з розмітки item_product.xml
                        for ((c, n) in products) {
                            // Інфлейтимо (створюємо з XML) горизонтальний LinearLayout
                            val row = layoutInflater.inflate(
                                R.layout.item_product, tableLayout, false
                            ) as LinearLayout

                            val tvCode = row.findViewById<TextView>(R.id.codeTextView)
                            val tvName = row.findViewById<TextView>(R.id.nameTextView)

                            // Заповнюємо поля текстом з файлу
                            tvCode.text = c
                            tvName.text = n

                            // Дозволяємо перенесення довгих назв на декілька рядків
                            tvName.setSingleLine(false)
                            tvName.maxLines = 5
                            tvName.ellipsize = null

                            // Масштабуємо текст згідно з поточними налаштуваннями
                            val baseCode = tvCode.textSize / resources.configuration.fontScale
                            val baseName = tvName.textSize / resources.configuration.fontScale
                            tvCode.setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                baseCode * TextScaleHelper.getTextScale(this@ProductListActivity)
                            )
                            tvName.setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                baseName * TextScaleHelper.getTextScale(this@ProductListActivity)
                            )

                            // Додаємо відступи для кращого вигляду таблиці
                            tvCode.setPadding(16, 16, 16, 16)
                            tvName.setPadding(16, 16, 16, 16)

                            // Першу колонку робимо фіксованої ширини, другу розтягуємо
                            val layoutParamsCode = tvCode.layoutParams as LinearLayout.LayoutParams
                            layoutParamsCode.width = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics
                            ).toInt()
                            tvCode.layoutParams = layoutParamsCode

                            val layoutParamsName = tvName.layoutParams as LinearLayout.LayoutParams
                            layoutParamsName.width = 0
                            layoutParamsName.weight = 1f
                            tvName.layoutParams = layoutParamsName

                            // Додаємо готовий рядок у таблицю
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

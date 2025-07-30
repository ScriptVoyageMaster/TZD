package ua.company.tzd

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.*
import org.apache.commons.net.ftp.FTPClient

/**
 * Екран налаштувань програми.
 * Дає змогу вказати параметри розбору штрихкоду
 * та реквізити підключення до FTP-сервера.
 */
class SettingsActivity : AppCompatActivity() {

    /** SharedPreferences, де зберігаємо налаштування */
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Підключаємо розмітку activity_settings.xml
        setContentView(R.layout.activity_settings)

        // Отримуємо сховище налаштувань під ім'ям "tzd_settings"
        prefs = getSharedPreferences("tzd_settings", MODE_PRIVATE)

        // Поля для параметрів штрихкоду
        val codeStart = findViewById<EditText>(R.id.inputCodeStart)
        val codeLength = findViewById<EditText>(R.id.inputCodeLength)
        val weightKgStart = findViewById<EditText>(R.id.inputWeightKgStart)
        val weightKgLength = findViewById<EditText>(R.id.inputWeightKgLength)
        val weightGrStart = findViewById<EditText>(R.id.inputWeightGrStart)
        val weightGrLength = findViewById<EditText>(R.id.inputWeightGrLength)
        val countStart = findViewById<EditText>(R.id.inputCountStart)
        val countLength = findViewById<EditText>(R.id.inputCountLength)
        val delayMs = findViewById<EditText>(R.id.inputDelay)

        // Поля для налаштування FTP
        val ftpHost = findViewById<EditText>(R.id.inputFtpHost)
        val ftpPort = findViewById<EditText>(R.id.inputFtpPort)
        val ftpUser = findViewById<EditText>(R.id.inputFtpUser)
        val ftpPass = findViewById<EditText>(R.id.inputFtpPass)
        // Нові поля з папками імпорту та експорту на FTP-сервері
        val ftpImportDir = findViewById<EditText>(R.id.inputFtpImportDir)
        val ftpExportDir = findViewById<EditText>(R.id.inputFtpExportDir)

        val btnSave = findViewById<Button>(R.id.btnSaveSettings)
        val btnTestFtp = findViewById<Button>(R.id.btnTestFtp)

        // Завантажуємо збережені значення або підставляємо типові
        codeStart.setText(prefs.getInt("codeStart", 0).toString())
        codeLength.setText(prefs.getInt("codeLength", 3).toString())
        weightKgStart.setText(prefs.getInt("weightKgStart", 3).toString())
        weightKgLength.setText(prefs.getInt("weightKgLength", 3).toString())
        weightGrStart.setText(prefs.getInt("weightGrStart", 6).toString())
        weightGrLength.setText(prefs.getInt("weightGrLength", 1).toString())
        countStart.setText(prefs.getInt("countStart", 7).toString())
        countLength.setText(prefs.getInt("countLength", 2).toString())
        delayMs.setText(prefs.getInt("delayMs", 2000).toString())

        ftpHost.setText(prefs.getString("ftpHost", ""))
        ftpPort.setText(prefs.getInt("ftpPort", 21).toString())
        ftpUser.setText(prefs.getString("ftpUser", ""))
        ftpPass.setText(prefs.getString("ftpPass", ""))

        // Зберігаємо введені дані у SharedPreferences
        btnSave.setOnClickListener {
            prefs.edit().apply {
                putInt("codeStart", codeStart.text.toString().toInt())
                putInt("codeLength", codeLength.text.toString().toInt())
                putInt("weightKgStart", weightKgStart.text.toString().toInt())
                putInt("weightKgLength", weightKgLength.text.toString().toInt())
                putInt("weightGrStart", weightGrStart.text.toString().toInt())
                putInt("weightGrLength", weightGrLength.text.toString().toInt())
                putInt("countStart", countStart.text.toString().toInt())
                putInt("countLength", countLength.text.toString().toInt())
                putInt("delayMs", delayMs.text.toString().toInt())
                putString("ftpHost", ftpHost.text.toString())
                putInt("ftpPort", ftpPort.text.toString().toInt())
                putString("ftpUser", ftpUser.text.toString())
                putString("ftpPass", ftpPass.text.toString())
                apply()
            }
            Toast.makeText(this, "Налаштування збережено", Toast.LENGTH_SHORT).show()
        }

        // Перевіряємо з'єднання з FTP-сервером та наявність каталогів
        btnTestFtp.setOnClickListener {
            // Зчитуємо дані з полів. Якщо порт не введено, використаємо 21
            val host = ftpHost.text.toString().trim()
            val port = ftpPort.text.toString().toIntOrNull() ?: 21
            val user = ftpUser.text.toString().trim()
            val pass = ftpPass.text.toString().trim()
            val importDir = ftpImportDir.text.toString().trim()
            val exportDir = ftpExportDir.text.toString().trim()

            // Запускаємо корутину в IO-потоці для роботи з мережею
            CoroutineScope(Dispatchers.IO).launch {
                val ftpClient = FTPClient()
                try {
                    // Підключення до FTP-сервера
                    ftpClient.connect(host, port)
                    // Спроба авторизації
                    val success = ftpClient.login(user, pass)

                    if (success) {
                        // Перевіряємо існування вказаних каталогів
                        val importExists = ftpClient.changeWorkingDirectory(importDir)
                        val exportExists = ftpClient.changeWorkingDirectory(exportDir)

                        // Повертаємося на головний потік для відображення результату
                        runOnUiThread {
                            // Формуємо докладне повідомлення про результати перевірки
                            val statusMessage = StringBuilder()
                                .append("✅ З'єднання успішне\n\n")
                                .apply {
                                    if (importExists) {
                                        append("📂 Папка імпорту існує\n")
                                    } else {
                                        append("❌ Папку імпорту не знайдено\n")
                                    }

                                    if (exportExists) {
                                        append("📂 Папка експорту існує\n")
                                    } else {
                                        append("❌ Папку експорту не знайдено\n")
                                    }
                                }

                            // Показуємо результат у спливаючому діалоговому вікні
                            AlertDialog.Builder(this@SettingsActivity)
                                .setTitle("Результат перевірки FTP")
                                .setMessage(statusMessage.toString())
                                .setPositiveButton("ОК", null)
                                .show()
                        }
                    } else {
                        // Логін або пароль невірні
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "❌ Невірний логін або пароль", Toast.LENGTH_LONG).show()
                        }
                    }

                    ftpClient.logout()
                    ftpClient.disconnect()

                } catch (e: Exception) {
                    // Випадок, коли не вдалося з'єднатися або виникла інша помилка
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "❌ Помилка: " + e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

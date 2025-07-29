package ua.company.tzd

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

        // Перевіряємо з'єднання з FTP-сервером
        btnTestFtp.setOnClickListener {
            // Зчитуємо дані з полів і підставляємо типові значення, якщо щось не введено
            val host = ftpHost.text.toString().trim()
            val port = ftpPort.text.toString().toIntOrNull() ?: 21
            val user = ftpUser.text.toString().trim()
            val pass = ftpPass.text.toString().trim()

            // Створюємо корутину в IO-потоці, щоб не блокувати UI
            CoroutineScope(Dispatchers.IO).launch {
                // Клієнт для роботи з FTP
                val ftpClient = FTPClient()
                try {
                    // Підключаємось до сервера за вказаними параметрами
                    ftpClient.connect(host, port)
                    // Пробуємо виконати вхід
                    val success = ftpClient.login(user, pass)

                    // Перемикаємось на головний потік щоб показати повідомлення
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this@SettingsActivity, "✅ FTP вхід успішний", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SettingsActivity, "❌ Логін або пароль невірний", Toast.LENGTH_LONG).show()
                        }
                    }
                    // Закриваємо сесію
                    ftpClient.logout()
                    ftpClient.disconnect()
                } catch (e: Exception) {
                    // У разі помилки також показуємо повідомлення на головному потоці
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "❌ Помилка з'єднання: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

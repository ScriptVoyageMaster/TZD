package ua.company.tzd.ui.orders

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import ua.company.tzd.utils.FTPManager
import java.time.LocalDateTime

/**
 * Екран сканування окремого замовлення.
 * Під час роботи ми переміщуємо файл у папку processing та
 * періодично зберігаємо його на сервері.
 */
class OrderScanActivity : AppCompatActivity() {
    private var order: Order? = null
    private var ftp: FTPManager? = null
    private var autosaveJob: Job? = null
    private var deviceName: String = ""
    private var autosaveInterval: Long = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // У реальному застосунку тут має бути розмітка зі списком позицій та кнопками
        val button = Button(this).apply { text = "Сканувати" }
        setContentView(button)

        deviceName = intent.getStringExtra("deviceName") ?: ""
        autosaveInterval = intent.getLongExtra("autosaveInterval", 2)

        // При натисканні "Сканувати" оновлюємо lock та переміщуємо файл
        button.setOnClickListener {
            order?.let { ord ->
                ord.lockedBy = deviceName
                ord.lockTime = LocalDateTime.now()
                // Зберігаємо файл у папку processing
                saveOrder(true)
            }
        }
    }

    /**
     * Запускає автозбереження кожні [autosaveInterval] хвилин.
     */
    private fun startAutosave() {
        autosaveJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(autosaveInterval * 60 * 1000)
                saveOrder(true)
            }
        }
    }

    /**
     * Записуємо поточне замовлення на FTP-сервер.
     * Якщо [updateLock] = true, оновлюємо час блокування.
     */
    private fun saveOrder(updateLock: Boolean) {
        val currentOrder = order ?: return
        if (updateLock) {
            currentOrder.lockTime = LocalDateTime.now()
        }
        // У реальному застосунку тут потрібно сформувати XML.
        // Для простоти ми записуємо порожній файл з номером замовлення.
        val content = "<замовлення номер=\"${currentOrder.number}\"/>".toByteArray()
        ftp?.uploadFile("/processing/", "${currentOrder.number}.xml", content)
    }

    override fun onStart() {
        super.onStart()
        startAutosave()
    }

    override fun onStop() {
        super.onStop()
        autosaveJob?.cancel()
        saveOrder(false)
    }
}


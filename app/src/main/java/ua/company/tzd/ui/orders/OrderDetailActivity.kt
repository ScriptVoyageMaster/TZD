package ua.company.tzd.ui.orders

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ua.company.tzd.R
import java.io.File
import java.io.FileInputStream

/**
 * Активність для детального перегляду замовлення.
 * Відкриває XML-файл та показує його вміст у текстовому полі.
 */
class OrderDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        // Знаходимо елементи інтерфейсу у розмітці
        val txtOrderInfo = findViewById<TextView>(R.id.txtOrderInfo)
        val btnStartScan = findViewById<Button>(R.id.btnStartScan)

        // Шлях до файлу передається через Intent
        val path = intent.getStringExtra("orderFilePath") ?: return
        val orderFile = File(path)

        // Зчитуємо файл повністю та відображаємо в текстовому полі
        val content = FileInputStream(orderFile).bufferedReader().use { it.readText() }
        txtOrderInfo.text = content

        // При натисканні запускаємо ScanActivity та передаємо шлях до файлу
        btnStartScan.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("orderFilePath", orderFile.absolutePath)
            startActivity(intent)
        }
    }
}


package ua.company.tzd.ui.orders

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import androidx.preference.PreferenceManager
import ua.company.tzd.R
import java.io.File

/**
 * Проста активність, що імітує процес сканування штрихкоду.
 * Після натискання на кнопку відбувається розбір тестового коду
 * та збереження результату у XML-файл.
 */
class ScanActivity : AppCompatActivity() {

    /** Поле, у якому показується результат сканування */
    private lateinit var txtResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        txtResult = findViewById(R.id.txtScanResult)
        val btnSimulateScan = findViewById<Button>(R.id.btnSimulateScan)

        // По натисканню імітуємо отримання штрихкоду і обробляємо його
        btnSimulateScan.setOnClickListener {
            val barcode = "1234567890123" // тестовий штрихкод
            val result = parseBarcode(barcode)
            txtResult.text = result
        }
    }

    /**
     * Розбирає штрихкод згідно з налаштуваннями користувача
     * та зберігає результат у XML-файл у каталозі filesDir/scans.
     */
    private fun parseBarcode(code: String): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Позиції і довжини різних частин штрихкоду можна налаштувати
        val posProduct = prefs.getInt("scanner_pos_product", 0)
        val lenProduct = prefs.getInt("scanner_len_product", 3)

        val posKg = prefs.getInt("scanner_pos_kg", 3)
        val lenKg = prefs.getInt("scanner_len_kg", 3)

        val posGr = prefs.getInt("scanner_pos_gr", 6)
        val lenGr = prefs.getInt("scanner_len_gr", 1)

        val posPack = prefs.getInt("scanner_pos_pack", 7)
        val lenPack = prefs.getInt("scanner_len_pack", 2)

        return try {
            // Витягуємо частини коду згідно з отриманими параметрами
            val productCode = code.substring(posProduct, posProduct + lenProduct)
            val kg = code.substring(posKg, posKg + lenKg).toInt()
            val gr = code.substring(posGr, posGr + lenGr).toInt()
            val packs = code.substring(posPack, posPack + lenPack).toInt()
            val weight = kg + gr / 10.0

            // Формуємо рядок для відображення користувачу
            val resultText = "Код товару: $productCode\nКількість упаковок: $packs\nВага: $weight кг"

            // Зберігаємо отримані дані у окремий XML-файл
            val resultXml = """<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<сканування>
    <код>$productCode</код>
    <упаковок>$packs</упаковок>
    <вага>$weight</вага>
</сканування>
"""
            val outputDir = File(filesDir, "scans")
            if (!outputDir.exists()) outputDir.mkdirs()
            val fileName = "scan_" + System.currentTimeMillis() + ".xml"
            val file = File(outputDir, fileName)
            file.writeText(resultXml)

            resultText
        } catch (e: Exception) {
            // Якщо щось пішло не так, повідомляємо користувача
            "Неможливо розпізнати штрихкод"
        }
    }
}


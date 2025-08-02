package ua.company.tzd.ui.orders

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Представляє одне замовлення, зчитане з FTP-сервера.
 * @property number номер документа
 * @property date дата створення
 * @property clientName назва клієнта
 * @property driver ім'я водія, якому належить замовлення
 * @property items список позицій замовлення
 * @property source з якої папки було отримано документ
 * @property lockedBy назва пристрою, що заблокувало замовлення
 * @property lockTime час встановлення блокування
 */
data class Order(
    val number: String,
    val date: LocalDate,
    val clientName: String,
    val driver: String,
    val items: MutableList<OrderItem> = mutableListOf(),
    val source: OrderSource = OrderSource.IMPORT,
    var lockedBy: String? = null,
    var lockTime: LocalDateTime? = null
) {
    companion object {
        /**
         * Зчитує XML та формує об'єкт [Order].
         * @param stream вхідний потік XML-файлу
         * @param source директорія, з якої був завантажений файл
         */
        fun fromXml(stream: InputStream, source: OrderSource): Order {
            var number = ""
            var date: LocalDate = LocalDate.now()
            var client = ""
            var driver = ""
            val items = mutableListOf<OrderItem>()
            var lockedBy: String? = null
            var lockTime: LocalDateTime? = null

            // Готуємо парсер XML
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(stream, null)

            var event = parser.eventType
            var currentItem: OrderItem? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "номер" -> number = parser.nextText()
                        "дата" -> date = LocalDate.parse(parser.nextText())
                        "клієнт" -> client = parser.nextText()
                        "водій" -> driver = parser.nextText()
                        "позиція" -> currentItem = OrderItem()
                        "код" -> currentItem?.code = parser.nextText()
                        "вага" -> currentItem?.weight = parser.nextText().toDoubleOrNull() ?: 0.0
                        "факт_вага" -> currentItem?.factWeight = parser.nextText().toDoubleOrNull()
                        "факт_кількість" -> currentItem?.factCount = parser.nextText().toDoubleOrNull()
                        "блокування" -> {
                            val parts = parser.nextText().split(",")
                            if (parts.size == 2) {
                                lockedBy = parts[0]
                                lockTime = try {
                                    LocalDateTime.parse(parts[1])
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> if (parser.name == "позиція" && currentItem != null) {
                        items.add(currentItem)
                        currentItem = null
                    }
                }
                event = parser.next()
            }

            return Order(number, date, client, driver, items, source, lockedBy, lockTime)
        }
    }
}

/**
 * Окрема позиція замовлення.
 */
data class OrderItem(
    var code: String = "",
    var weight: Double = 0.0,
    var factWeight: Double? = null,
    var factCount: Double? = null
)

/**
 * Звідки було отримано файл замовлення.
 */
enum class OrderSource { IMPORT, PROCESSING }


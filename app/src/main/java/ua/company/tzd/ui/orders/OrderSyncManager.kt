package ua.company.tzd.ui.orders

import ua.company.tzd.utils.FTPManager

/**
 * Відповідає за синхронізацію замовлень з FTP-сервера.
 * Зчитує XML-файли з папок /import та /processing та об'єднує їх у єдиний список.
 */
class OrderSyncManager(
    private val ftp: FTPManager,
    private val importDir: String,
    private val processingDir: String
) {
    /**
     * Виконує синхронізацію та повертає список замовлень.
     * Якщо замовлення з однаковим номером існує в обох папках,
     * перевага надається файлу з папки processing.
     */
    fun syncOrders(): List<Order> {
        val map = mutableMapOf<String, Order>()

        // Зчитуємо замовлення з папки import
        ftp.readXmlFiles(importDir).forEach { (_, stream) ->
            val order = Order.fromXml(stream, OrderSource.IMPORT)
            map[order.number] = order
        }

        // Зчитуємо замовлення з папки processing, перезаписуючи попередні
        ftp.readXmlFiles(processingDir).forEach { (_, stream) ->
            val order = Order.fromXml(stream, OrderSource.PROCESSING)
            map[order.number] = order
        }

        return map.values.toList()
    }
}


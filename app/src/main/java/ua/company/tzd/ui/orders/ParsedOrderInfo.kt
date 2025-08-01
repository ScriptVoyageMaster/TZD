package ua.company.tzd.ui.orders

import java.io.File

/**
 * Коротка інформація про замовлення, отримана під час парсингу XML.
 * @property number номер замовлення
 * @property date дата документа
 * @property client назва клієнта
 * @property totalWeight сумарна вага усіх позицій
 * @property isLocked чи містить файл тег <блокування>
 * @property file сам файл замовлення
 */
data class ParsedOrderInfo(
    val number: String,
    val date: String,
    val client: String,
    val totalWeight: Double,
    val isLocked: Boolean = false,
    val file: File
)

package ua.company.tzd.ui.orders

import java.time.Duration
import java.time.LocalDateTime

/**
 * Можливі стани замовлення для відображення в інтерфейсі.
 */
enum class OrderStatus { NEW, LOCKED_BY_ME, LOCKED_BY_OTHER, EXPIRED_LOCK }

/**
 * Визначаємо статус замовлення на основі блокування та часу.
 * @param order замовлення, що перевіряємо
 * @param currentDevice ім'я поточного пристрою
 * @param now поточний момент часу
 * @param autosaveInterval інтервал автозбереження у хвилинах
 */
fun getOrderStatus(
    order: Order,
    currentDevice: String,
    now: LocalDateTime,
    autosaveInterval: Long
): OrderStatus {
    val lockedBy = order.lockedBy
    val lockTime = order.lockTime
    val isActive = if (lockTime != null) {
        Duration.between(lockTime, now).toMinutes() < autosaveInterval
    } else false

    return when {
        lockedBy == null -> OrderStatus.NEW
        lockedBy == currentDevice && isActive -> OrderStatus.LOCKED_BY_ME
        lockedBy != currentDevice && isActive -> OrderStatus.LOCKED_BY_OTHER
        else -> OrderStatus.EXPIRED_LOCK
    }
}


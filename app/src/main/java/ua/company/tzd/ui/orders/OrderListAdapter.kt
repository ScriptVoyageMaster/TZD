package ua.company.tzd.ui.orders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime

/**
 * Адаптер для відображення списку замовлень із підсвічуванням стану.
 * @param orders список замовлень
 * @param currentDevice назва поточного пристрою
 * @param autosaveInterval інтервал автозбереження у хвилинах
 * @param onClick дія при натисканні на рядок
 */
class OrderListAdapter(
    private val orders: List<Order>,
    private val currentDevice: String,
    private val autosaveInterval: Long,
    private val onClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]
        holder.text.text = "№${order.number} | ${order.clientName}"

        val status = getOrderStatus(order, currentDevice, LocalDateTime.now(), autosaveInterval)
        val color = when {
            order.source == OrderSource.IMPORT && status == OrderStatus.NEW -> Color.GREEN
            order.source == OrderSource.PROCESSING && status == OrderStatus.LOCKED_BY_ME -> Color.YELLOW
            order.source == OrderSource.PROCESSING && status == OrderStatus.LOCKED_BY_OTHER -> Color.RED
            order.source == OrderSource.PROCESSING && status == OrderStatus.EXPIRED_LOCK -> 0xFFAEEA00.toInt()
            else -> Color.TRANSPARENT
        }
        holder.itemView.setBackgroundColor(color)
        holder.itemView.setOnClickListener { onClick(order) }
    }

    override fun getItemCount(): Int = orders.size
}


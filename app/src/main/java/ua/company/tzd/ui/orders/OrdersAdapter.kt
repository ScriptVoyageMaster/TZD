package ua.company.tzd.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color

/**
 * Адаптер для відображення списку файлів замовлень.
 * @param orders список файлів замовлень
 * @param onClick функція, яка буде викликана при натисканні на елемент
 */
class OrdersAdapter(
    private val orders: List<OrdersActivity.OrderInfo>,
    private val onClick: (OrdersActivity.OrderInfo) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    /**
     * ViewHolder зберігає посилання на TextView у стандартному макеті
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Створюємо простий рядок списку із стандартного ресурсу Android
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val info = orders[position]
        // Відображаємо назву файлу
        holder.text.text = info.file.name
        // Колір фону в залежності від прапора блокування
        if (info.isLocked) {
            holder.itemView.setBackgroundColor(Color.YELLOW)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        // Передаємо натискання разом з даними про файл
        holder.itemView.setOnClickListener { onClick(info) }
    }

    override fun getItemCount(): Int = orders.size
}


package ua.company.tzd.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * Адаптер для відображення списку файлів замовлень.
 * @param orders список файлів замовлень
 * @param onClick функція, яка буде викликана при натисканні на елемент
 */
class OrdersAdapter(
    private val orders: List<File>,
    private val onClick: (File) -> Unit
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
        // Заповнюємо текст назвою файлу
        holder.text.text = orders[position].name
        // Обробник натискання передає вибраний файл зовнішній функції
        holder.itemView.setOnClickListener {
            onClick(orders[position])
        }
    }

    override fun getItemCount(): Int = orders.size
}


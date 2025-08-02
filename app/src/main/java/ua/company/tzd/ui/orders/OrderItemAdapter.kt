package ua.company.tzd.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ua.company.tzd.R

/**
 * Дані однієї позиції для відображення на екрані замовлення.
 * Використовується тільки у списку та не впливає на основну модель даних.
 * @param code код товару
 * @param name назва товару
 * @param expectedWeight очікувана вага, яку має бути відвантажено
 * @param actualWeight фактична вага вже відсканованого товару
 * @param actualPacks фактична кількість упаковок
 */
data class DisplayOrderItem(
    val code: String,
    val name: String,
    val expectedWeight: Double,
    var actualWeight: Double = 0.0,
    var actualPacks: Int = 0
)

/**
 * Адаптер для таблиці позицій замовлення.
 * Показує код, назву, очікувану та фактичну вагу і кількість упаковок.
 */
class OrderItemAdapter(private val items: List<DisplayOrderItem>) :
    RecyclerView.Adapter<OrderItemAdapter.ViewHolder>() {

    /** ViewHolder зберігає посилання на текстові поля одного рядка */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCode: TextView = view.findViewById(R.id.txtCode)
        val txtName: TextView = view.findViewById(R.id.txtName)
        val txtExp: TextView = view.findViewById(R.id.txtExpected)
        val txtFact: TextView = view.findViewById(R.id.txtFact)
        val txtPack: TextView = view.findViewById(R.id.txtPack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_row, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtCode.text = item.code
        holder.txtName.text = item.name
        holder.txtExp.text = "%.2f".format(item.expectedWeight)
        holder.txtFact.text = "%.2f".format(item.actualWeight)
        holder.txtPack.text = item.actualPacks.toString()
    }
}

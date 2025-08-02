package ua.company.tzd.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ua.company.tzd.R

/**
 * Окремий рядок для відображення позиції замовлення у списку.
 * Цей клас призначений виключно для показу інформації на екрані,
 * щоб не плутати його з логічною моделлю [OrderItem] з файлу `Order.kt`.
 * @param code код товару, зручний для швидкого пошуку по списку
 * @param name читабельна назва товару, яку бачить користувач
 * @param expectedWeight вага, яку система очікує відвантажити
 * @param actualWeight поточна фактична вага вже відсканованого товару
 * @param actualPacks фактична кількість упаковок, що вже відскановані
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
 * Використовує [DisplayOrderItem], щоб відобразити код, назву та вагу товару.
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

package ua.company.tzd.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children

/**
 * Допоміжний об'єкт для зміни розміру тексту у всіх вкладених елементах.
 * Завдяки йому можна одним викликом збільшити або зменшити
 * шрифти усіх кнопок чи текстових полів в активності.
 */
object TextScaleHelper {
    /**
     * Рекурсивно проходить по всій ієрархії view та масштабує
     * розмір тексту для кожного TextView.
     *
     * @param context контекст, потрібний для перерахунку одиниць виміру
     * @param root кореневий елемент, з якого починаємо обходити дочірні
     * @param scale коефіцієнт масштабування (1f означає 100 % розміру)
     */
    fun applyTextScale(context: Context, root: View, scale: Float) {
        when (root) {
            is TextView -> {
                // textSize повертає значення в пікселях, тому переводимо його
                // з урахуванням щільності екрану
                root.textSize = root.textSize * scale / context.resources.displayMetrics.scaledDensity
            }
            is ViewGroup -> {
                // Якщо елемент містить дітей, обходимо кожного з них
                root.children.forEach { applyTextScale(context, it, scale) }
            }
        }
    }
}

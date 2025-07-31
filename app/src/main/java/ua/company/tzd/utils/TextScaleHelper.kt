package ua.company.tzd.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.children
import androidx.preference.PreferenceManager
import android.util.TypedValue

/**
 * Допоміжний об'єкт для зміни розміру тексту у всіх вкладених елементах.
 * Завдяки йому можна одним викликом збільшити або зменшити
 * шрифти усіх кнопок чи текстових полів в активності.
 */
object TextScaleHelper {

    /**
     * Повертає коефіцієнт масштабування звичайного тексту з налаштувань.
     * Значення зберігається у відсотках, тому ділимо на 100.
     */
    fun getTextScale(context: Context): Float {
        val percent = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt("textZoomPercent", 100)
        return percent / 100f
    }

    /**
     * Повертає коефіцієнт масштабування для кнопок з налаштувань.
     * Так само ділиться на 100, щоб отримати множник.
     */
    fun getButtonScale(context: Context): Float {
        val percent = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt("buttonZoomPercent", 100)
        return percent / 100f
    }

    /**
     * Рекурсивно проходить по всій ієрархії view та масштабує
     * розмір тексту для кожного TextView або Button.
     * Метод сам зчитує потрібний коефіцієнт масштабування з налаштувань.
     *
     * @param context контекст, потрібний для перерахунку одиниць виміру
     * @param root кореневий елемент, з якого починаємо обходити дочірні
     */
    fun applyTextScale(context: Context, root: View) {
        when (root) {
            is Button -> {
                val scale = getButtonScale(context)
                val base = root.textSize / context.resources.configuration.fontScale
                root.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * scale)
            }
            is TextView -> {
                val scale = getTextScale(context)
                val base = root.textSize / context.resources.configuration.fontScale
                root.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * scale)
            }
            is ViewGroup -> {
                // Якщо елемент містить дітей, обходимо кожного з них
                root.children.forEach { applyTextScale(context, it) }
            }
        }
    }
}

package ua.company.tzd.utils

import android.content.Context
import android.util.TypedValue

/**
 * Перетворення значень з dp у пікселі. Використовується для задання
 * фіксованої ширини елементів у коді.
 */
fun Int.dpToPx(context: Context): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    context.resources.displayMetrics
).toInt()

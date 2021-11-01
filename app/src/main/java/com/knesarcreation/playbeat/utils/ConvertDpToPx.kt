package com.knesarcreation.playbeat.utils

import android.content.Context
import android.widget.Toast
import kotlin.math.roundToInt

object ConvertDpToPx {
    fun dpToPx(dp: Int, context: Context): Int {
        val density: Float = context.resources
            .displayMetrics.density
        return (dp.toFloat() * density).roundToInt()
    }
}
package com.knesarcreation.playbeat.utils

import android.app.Activity
import android.graphics.Insets
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowInsets
import android.view.WindowMetrics
import kotlin.math.pow
import kotlin.math.sqrt

class GetScreenSizes(val activity: Activity) {

    fun getScreenInches(): Double {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val windowMetrics: WindowMetrics = activity.windowManager.currentWindowMetrics
            val insets: Insets = windowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            val x =
                ((windowMetrics.bounds.width() - insets.left - insets.right) / displayMetrics.xdpi).toDouble()
                    .pow(2.0)
            val y =
                ((windowMetrics.bounds.height() - insets.left - insets.right) / displayMetrics.ydpi).toDouble()
                    .pow(2.0)
            val screenInches = sqrt((x + y))
            val format = "%.2f".format(screenInches).toDouble()
            Log.d("heightOnPreDrawRGBNew1111", "onPreDraw: w:$x h:$y inches:$format")
            //Toast.makeText(activity, "Display: $format inch", Toast.LENGTH_SHORT).show()
            format
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val x = (displayMetrics.widthPixels / displayMetrics.xdpi).toDouble().pow(2.0)
            val y = ((displayMetrics.heightPixels + 110) / displayMetrics.ydpi).toDouble().pow(2.0)
            val screenInches = sqrt(x + y)
            val format = "%.2f".format(screenInches).toDouble()
            Log.d("heightOnPreDrawRGB", "onPreDraw: screenInches: $format")

            Log.d(
                "heightOnPreDrawRGB001",
                "onPreDraw: screenInches: widthPixels:${displayMetrics.widthPixels}" +
                        " xdpi: ${displayMetrics.xdpi} heightPixels: ${displayMetrics.heightPixels + 110} ydpi: ${displayMetrics.ydpi} x: $x y:$y"
            )

            format
        }
    }
}
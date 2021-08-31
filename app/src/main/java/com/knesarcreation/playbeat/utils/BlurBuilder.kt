package com.knesarcreation.playbeat.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.renderscript.Allocation
import androidx.renderscript.Element
import androidx.renderscript.RenderScript
import androidx.renderscript.ScriptIntrinsicBlur
import kotlin.math.roundToInt

class BlurBuilder {

    companion object {
        const val BITMAP_SCALE = 0.4f
        const val BLUR_RADIUS = 25f
    }

    fun blur(context: Context, image: Bitmap,blurRadius:Float):Bitmap {
        val width = image.width.toDouble().roundToInt() * BITMAP_SCALE
        val height = image.height.toDouble().roundToInt() * BITMAP_SCALE

        val inputBitmap = Bitmap.createScaledBitmap(image, width.toInt(), height.toInt(), false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val tempIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tempOut = Allocation.createFromBitmap(rs, outputBitmap)
        scriptIntrinsicBlur.setRadius(blurRadius)
        scriptIntrinsicBlur.setInput(tempIn)
        scriptIntrinsicBlur.forEach(tempOut)
        tempOut.copyTo(outputBitmap)

        return outputBitmap

    }
}
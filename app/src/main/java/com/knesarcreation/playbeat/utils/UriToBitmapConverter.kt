package com.knesarcreation.playbeat.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


object UriToBitmapConverter {

    @Throws(FileNotFoundException::class, IOException::class)
    fun getBitmap(cr: ContentResolver, uri: Uri?): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val input: InputStream? = cr.openInputStream(uri!!)
            bitmap = BitmapFactory.decodeStream(input)
            input!!.close()
        } catch (e: FileNotFoundException) {

        }
        return bitmap
    }

}
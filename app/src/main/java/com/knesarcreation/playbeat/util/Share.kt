package com.knesarcreation.playbeat.util;


import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File


object Share {
    fun shareStoryToSocial(context: Context, uri: Uri) {
        val feedIntent = Intent(Intent.ACTION_SEND)
        feedIntent.type = "image/*"
        feedIntent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(feedIntent, null)
    }

    fun shareFile(context: Context, file: File) {
        val attachmentUri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName,
            file
        )
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/*"
        sharingIntent.putExtra(Intent.EXTRA_STREAM, attachmentUri)
        context.startActivity(Intent.createChooser(sharingIntent, "send bug report"))
    }
}
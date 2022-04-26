package com.knesarcreation.playbeat.util;

import android.content.Context
import android.content.Intent
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.MusicUtil.getSongFileUri

class RingtoneManager(val context: Context) {
    fun setRingtone(song: Song) {
        val resolver = context.contentResolver
        val uri = getSongFileUri(song.id)

        try {
            val cursor = resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns.TITLE),
                BaseColumns._ID + "=?",
                arrayOf(song.id.toString()), null
            )
            cursor.use { cursorSong ->
                if (cursorSong != null && cursorSong.count == 1) {
                    cursorSong.moveToFirst()
                    Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString())
                    val message = context
                        .getString(R.string.x_has_been_set_as_ringtone, cursorSong.getString(0))
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (ignored: SecurityException) {
        }
    }

    companion object {

        fun requiresDialog(context: Context): Boolean {
            if (VersionUtils.hasMarshmallow()) {
                if (!Settings.System.canWrite(context)) {
                    return true
                }
            }
            return false
        }

        fun getDialog(context: Context) {
            return MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialogTheme)
                .setTitle(R.string.dialog_title_set_ringtone)
                .setMessage(R.string.dialog_message_set_ringtone)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = ("package:" + context.applicationContext.packageName).toUri()
                    context.startActivity(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create().show()
        }
    }
}
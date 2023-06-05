package com.knesarcreation.playbeat.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.Audio.Media
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.Constants.IS_MUSIC
import com.knesarcreation.playbeat.Constants.baseProjection
import com.knesarcreation.playbeat.extensions.getInt
import com.knesarcreation.playbeat.extensions.getLong
import com.knesarcreation.playbeat.extensions.getString
import com.knesarcreation.playbeat.extensions.getStringOrNull
import com.knesarcreation.playbeat.helper.SortOrder
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.providers.BlacklistStore
import com.knesarcreation.playbeat.util.PreferenceUtil
import java.text.Collator

interface SongRepository {

    fun songs(): List<Song>

    fun songs(cursor: Cursor?): List<Song>

    fun sortedSongs(cursor: Cursor?): List<Song>

    fun songs(query: String): List<Song>

    fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean = false): List<Song>

    fun song(cursor: Cursor?): Song

    fun song(songId: Long): Song

    fun songsIgnoreBlacklist(uri: Uri): List<Song>
}

class RealSongRepository(private val context: Context) : SongRepository {

    override fun songs(): List<Song> {
        return sortedSongs(makeSongCursor(null, null))
    }

    override fun songs(cursor: Cursor?): List<Song> {
        val songs = arrayListOf<Song>()
        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(getSongFromCursorImpl(cursor))
            } while (cursor.moveToNext())
        }
        cursor?.close()
        return songs
    }

    override fun sortedSongs(cursor: Cursor?): List<Song> {
        val collator = Collator.getInstance()
        val songs = songs(cursor)
        return when (PreferenceUtil.songSortOrder) {
            SortOrder.SongSortOrder.SONG_A_Z -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.title, s2.title) }
            }
            SortOrder.SongSortOrder.SONG_Z_A -> {
                songs.sortedWith { s1, s2 -> collator.compare(s2.title, s1.title) }
            }
            SortOrder.SongSortOrder.SONG_ALBUM_ASC -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.albumName, s2.albumName) }
            }
            SortOrder.SongSortOrder.SONG_ALBUM_DESC -> {
                songs.sortedWith { s1, s2 -> collator.compare(s2.albumName, s1.albumName) }
            }
            SortOrder.SongSortOrder.SONG_ALBUM_ARTIST_ASC -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.albumArtist, s2.albumArtist) }
            }
            SortOrder.SongSortOrder.SONG_ALBUM_ARTIST_DESC -> {
                songs.sortedWith { s1, s2 -> collator.compare(s2.albumArtist, s1.albumArtist) }
            }
            SortOrder.SongSortOrder.SONG_ARTIST_ASC -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.artistName, s2.artistName) }
            }
            SortOrder.SongSortOrder.SONG_ARTIST_DESC -> {
                songs.sortedWith { s1, s2 -> collator.compare(s2.artistName, s1.artistName) }
            }
            SortOrder.SongSortOrder.COMPOSER_ACS -> {
                songs.sortedWith { s1, s2 -> collator.compare(s1.composer, s2.composer) }
            }
            SortOrder.SongSortOrder.COMPOSER_DESC -> {
                songs.sortedWith { s1, s2 -> collator.compare(s2.composer, s1.composer) }
            }
            /* SortOrder.SongSortOrder.SONG_DATE_MODIFIED_ASC -> {
                 songs.sortedWith { s1, s2 -> collator.compare(s1.dateModified, s2.dateModified) }
             }
             SortOrder.SongSortOrder.SONG_DATE_MODIFIED_DESC -> {
                 songs.sortedWith { s1, s2 -> collator.compare(s2.dateModified, s1.dateModified) }
             }*/

            else -> songs
        }
    }

    override fun song(cursor: Cursor?): Song {
        val song: Song = if (cursor != null && cursor.moveToFirst()) {
            getSongFromCursorImpl(cursor)
        } else {
            Song.emptySong
        }
        cursor?.close()
        return song
    }

    override fun songs(query: String): List<Song> {
        return songs(makeSongCursor(AudioColumns.TITLE + " LIKE ?", arrayOf("%$query%")))
    }

    override fun song(songId: Long): Song {
        return song(makeSongCursor(AudioColumns._ID + "=?", arrayOf(songId.toString())))
    }

    override fun songsByFilePath(filePath: String, ignoreBlacklist: Boolean): List<Song> {
        return songs(
            makeSongCursor(
                AudioColumns.DATA + "=?",
                arrayOf(filePath),
                ignoreBlacklist = ignoreBlacklist
            )
        )
    }

    override fun songsIgnoreBlacklist(uri: Uri): List<Song> {
        var filePath = ""
        context.contentResolver.query(
            uri,
            arrayOf(AudioColumns.DATA),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor != null) {
                if (cursor.count != 0) {
                    cursor.moveToFirst()
                    filePath = cursor.getString(AudioColumns.DATA)
                    println("File Path: $filePath")
                }
            }
        }
        return songsByFilePath(
            filePath, true
        )
    }

    private fun getSongFromCursorImpl(
        cursor: Cursor
    ): Song {
        val id = cursor.getLong(AudioColumns._ID)
        val title = cursor.getString(AudioColumns.TITLE)
        val trackNumber = cursor.getInt(AudioColumns.TRACK)
        val year = cursor.getInt(AudioColumns.YEAR)
        val duration = cursor.getLong(AudioColumns.DURATION)
        val data = cursor.getString(AudioColumns.DATA)
        val dateModified = cursor.getLong(AudioColumns.DATE_MODIFIED)
        val albumId = cursor.getLong(AudioColumns.ALBUM_ID)
        val albumName = cursor.getStringOrNull(AudioColumns.ALBUM)
        val artistId = cursor.getLong(AudioColumns.ARTIST_ID)
        val artistName = cursor.getStringOrNull(AudioColumns.ARTIST)
        val composer = cursor.getStringOrNull(AudioColumns.COMPOSER)
        val albumArtist = cursor.getStringOrNull("album_artist")
        return Song(
            id,
            title,
            trackNumber,
            year,
            duration,
            data,
            dateModified,
            albumId,
            albumName ?: "",
            artistId,
            artistName ?: "",
            composer ?: "",
            albumArtist ?: ""
        )
    }

    @JvmOverloads
    fun makeSongCursor(
        selection: String?,
        selectionValues: Array<String>?,
        sortOrder: String = PreferenceUtil.songSortOrder,
        ignoreBlacklist: Boolean = false
    ): Cursor? {
        var selectionFinal = selection
        var selectionValuesFinal = selectionValues
        if (!ignoreBlacklist) {
            selectionFinal = if (selection != null && selection.trim { it <= ' ' } != "") {
                "$IS_MUSIC AND $selectionFinal"
            } else {
                IS_MUSIC
            }

            // Whitelist
            if (PreferenceUtil.isWhiteList) {
                selectionFinal =
                    selectionFinal + " AND " + AudioColumns.DATA + " LIKE ?"
                selectionValuesFinal = addSelectionValues(
                    selectionValuesFinal, arrayListOf(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).canonicalPath
                    )
                )
            } else {
                // Blacklist
                val paths = BlacklistStore.getInstance(context).paths
                if (paths.isNotEmpty()) {
                    selectionFinal = generateBlacklistSelection(selectionFinal, paths.size)
                    selectionValuesFinal = addSelectionValues(selectionValuesFinal, paths)
                }
            }

            selectionFinal =
                selectionFinal + " AND " + Media.DURATION + ">= " + (PreferenceUtil.filterLength * 1000)
        }
        val uri = if (VersionUtils.hasQ()) {
            Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            Media.EXTERNAL_CONTENT_URI
        }
        return try {
            context.contentResolver.query(
                uri,
                baseProjection,
                selectionFinal,
                selectionValuesFinal,
                sortOrder
            )
        } catch (ex: SecurityException) {
            return null
        }
    }

    private fun generateBlacklistSelection(
        selection: String?,
        pathCount: Int
    ): String {
        val newSelection = StringBuilder(
            if (selection != null && selection.trim { it <= ' ' } != "") "$selection AND " else "")
        newSelection.append(AudioColumns.DATA + " NOT LIKE ?")
        for (i in 0 until pathCount - 1) {
            newSelection.append(" AND " + AudioColumns.DATA + " NOT LIKE ?")
        }
        return newSelection.toString()
    }

    private fun addSelectionValues(
        selectionValues: Array<String>?,
        paths: ArrayList<String>
    ): Array<String> {
        var selectionValuesFinal = selectionValues
        if (selectionValuesFinal == null) {
            selectionValuesFinal = emptyArray()
        }
        val newSelectionValues = Array(selectionValuesFinal.size + paths.size) {
            "n = $it"
        }
        System.arraycopy(selectionValuesFinal, 0, newSelectionValues, 0, selectionValuesFinal.size)
        for (i in selectionValuesFinal.size until newSelectionValues.size) {
            newSelectionValues[i] = paths[i - selectionValuesFinal.size] + "%"
        }
        return newSelectionValues
    }
}

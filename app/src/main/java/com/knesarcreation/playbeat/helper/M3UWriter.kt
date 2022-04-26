package com.knesarcreation.playbeat.helper

import com.knesarcreation.playbeat.db.PlaylistWithSongs
import com.knesarcreation.playbeat.db.toSongs
import com.knesarcreation.playbeat.model.Playlist
import com.knesarcreation.playbeat.model.Song
import java.io.*

object M3UWriter : M3UConstants {
    @JvmStatic
    @Throws(IOException::class)
    fun write(
        dir: File,
        playlist: Playlist
    ): File {
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, playlist.name + "." + M3UConstants.EXTENSION)
        val songs = playlist.getSongs()
        if (songs.isNotEmpty()) {
            BufferedWriter(FileWriter(file)).use { bw ->
                bw.write(M3UConstants.HEADER)
                for (song in songs) {
                    bw.newLine()
                    bw.write(M3UConstants.ENTRY + song.duration + M3UConstants.DURATION_SEPARATOR + song.artistName + " - " + song.title)
                    bw.newLine()
                    bw.write(song.data)
                }
            }
        }
        return file
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeIO(dir: File, playlistWithSongs: PlaylistWithSongs): File {
        if (!dir.exists()) dir.mkdirs()
        val fileName = "${playlistWithSongs.playlistEntity.playlistName}.${M3UConstants.EXTENSION}"
        val file = File(dir, fileName)
        val songs: List<Song> = playlistWithSongs.songs.sortedBy {
            it.songPrimaryKey
        }.toSongs()
        if (songs.isNotEmpty()) {
            BufferedWriter(FileWriter(file)).use { bw ->
                bw.write(M3UConstants.HEADER)
                songs.forEach {
                    bw.newLine()
                    bw.write(M3UConstants.ENTRY + it.duration + M3UConstants.DURATION_SEPARATOR + it.artistName + " - " + it.title)
                    bw.newLine()
                    bw.write(it.data)
                }
            }
        }
        return file
    }

    fun writeIO(outputStream: OutputStream, playlistWithSongs: PlaylistWithSongs) {
        val songs: List<Song> = playlistWithSongs.songs.sortedBy {
            it.songPrimaryKey
        }.toSongs()
        if (songs.isNotEmpty()) {
            outputStream.use { os ->
                os.bufferedWriter().use { bw ->
                    bw.write(M3UConstants.HEADER)
                    songs.forEach {
                        bw.newLine()
                        bw.write(M3UConstants.ENTRY + it.duration + M3UConstants.DURATION_SEPARATOR + it.artistName + " - " + it.title)
                        bw.newLine()
                        bw.write(it.data)
                    }
                }
            }
        }
    }
}

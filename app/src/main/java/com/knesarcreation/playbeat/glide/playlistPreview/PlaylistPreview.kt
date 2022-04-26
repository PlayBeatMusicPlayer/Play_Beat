package com.knesarcreation.playbeat.glide.playlistPreview

import com.knesarcreation.playbeat.db.PlaylistEntity
import com.knesarcreation.playbeat.db.PlaylistWithSongs
import com.knesarcreation.playbeat.db.toSongs
import com.knesarcreation.playbeat.model.Song

class PlaylistPreview(val playlistWithSongs: PlaylistWithSongs) {

    val playlistEntity: PlaylistEntity get() = playlistWithSongs.playlistEntity
    val songs: List<Song> get() = playlistWithSongs.songs.toSongs()

    override fun equals(other: Any?): Boolean {
        if (other is PlaylistPreview) {
            if (other.playlistEntity.playListId != playlistEntity.playListId) {
                return false
            }
            if (other.songs.size != songs.size) {
                return false
            }
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        var result = playlistEntity.playListId.hashCode()
        result = 31 * result + playlistWithSongs.songs.size
        return result
    }
}
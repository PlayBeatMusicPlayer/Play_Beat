package com.knesarcreation.playbeat.glide.playlistSongsPreview;

import com.knesarcreation.playbeat.model.Song

class PlaylistSongsPreview(val playlistSongs: List<Song>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaylistSongsPreview

        if (playlistSongs != other.playlistSongs) return false

        return true
    }

    override fun hashCode(): Int {
        return playlistSongs.hashCode()
    }


}
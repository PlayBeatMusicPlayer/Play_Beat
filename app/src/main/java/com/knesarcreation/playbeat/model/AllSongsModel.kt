package com.knesarcreation.playbeat.model

import android.graphics.Bitmap
import android.net.Uri

data class AllSongsModel(
    var albumId:Long,
    var albumArt: ByteArray?,
    var songName: String,
    var artistsName: String,
    var albumName: String,
    var size: Int,
    var duration: Int,
    var bitmap:Bitmap?,
    var uri:Uri?
) {
    constructor() : this(0L,null, "", "", "", 0, 0,null,null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AllSongsModel

        if (albumId != other.albumId) return false
        if (albumArt != null) {
            if (other.albumArt == null) return false
            if (!albumArt.contentEquals(other.albumArt)) return false
        } else if (other.albumArt != null) return false
        if (songName != other.songName) return false
        if (artistsName != other.artistsName) return false
        if (albumName != other.albumName) return false
        if (size != other.size) return false
        if (duration != other.duration) return false
        if (bitmap != other.bitmap) return false
        if (uri != other.uri) return false

        return true
    }

    override fun hashCode(): Int {
        var result = albumId.hashCode()
        result = 31 * result + (albumArt?.contentHashCode() ?: 0)
        result = 31 * result + songName.hashCode()
        result = 31 * result + artistsName.hashCode()
        result = 31 * result + albumName.hashCode()
        result = 31 * result + size
        result = 31 * result + duration
        result = 31 * result + (bitmap?.hashCode() ?: 0)
        result = 31 * result + (uri?.hashCode() ?: 0)
        return result
    }
}
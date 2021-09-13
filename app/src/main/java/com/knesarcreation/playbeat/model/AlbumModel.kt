package com.knesarcreation.playbeat.model

import android.graphics.Bitmap

data class AlbumModel(
    var id: Long,
    var albumName: String,
    var artistName: String,
    var artUri: String,
    var albumBitmap: Bitmap?
) {
    constructor() : this(0L, "", "", "",null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlbumModel

        if (albumName != other.albumName) return false

        return true
    }

    override fun hashCode(): Int {
        return albumName.hashCode()
    }


}
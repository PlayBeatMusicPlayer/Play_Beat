package com.knesarcreation.playbeat.model

data class AlbumModel(
    var albumId: Long,
    var albumName: String,
    var artistName: String,
    var artUri: String,
) {
    constructor() : this(0L, "", "", "")

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
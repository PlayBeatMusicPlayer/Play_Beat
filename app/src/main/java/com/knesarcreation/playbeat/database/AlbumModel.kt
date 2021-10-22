package com.knesarcreation.playbeat.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "albumModel", indices = [Index(value = ["albumName"], unique = true)])
data class AlbumModel(
    @PrimaryKey
    var id: Long,
    @ColumnInfo(name = "albumName")
    var albumName: String,
    var artistName: String,
    var artUri: String,
    //var albumBitmap: Bitmap?,
    var lastYear: Int,
    var songCount: Int,
    /* var dateAdded: String,*/
) {
    constructor() : this(0L, "", "", "", 0, 0)

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
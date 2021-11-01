package com.knesarcreation.playbeat.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "artistsModel", indices = [Index(value = ["artistName"], unique = true)])
data class ArtistsModel(
    var artistId: Long,
    @ColumnInfo(name = "artistName")
    var artistName: String,
    //var noOfAlbums: Int,
    //var noOfTracks: Int,
    /*var albumId: Long,
    var albumName: String,
    var subArtistName: String*/
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    constructor() : this(0L, "" /*0, 0*//*, 0L, "", ""*/)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArtistsModel

        if (artistName != other.artistName) return false

        return true
    }

    override fun hashCode(): Int {
        return artistName.hashCode()
    }


}
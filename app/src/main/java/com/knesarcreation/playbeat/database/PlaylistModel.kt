package com.knesarcreation.playbeat.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class PlaylistModel(
    var playlistName: String,
    var songIds: String,
    var dateAdded:Long
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0

    constructor() : this("", "",0L)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaylistModel

        if (playlistName != other.playlistName) return false
        if (songIds != other.songIds) return false
      /*  if (dateAdded != other.dateAdded) return false
        if (id != other.id) return false*/

        return true
    }

    override fun hashCode(): Int {
        var result = playlistName.hashCode()
        result = 31 * result + songIds.hashCode()
        /* result = 31 * result + dateAdded.hashCode()
         result = 31 * result + id*/
        return result
    }


}
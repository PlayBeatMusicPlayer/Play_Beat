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

}
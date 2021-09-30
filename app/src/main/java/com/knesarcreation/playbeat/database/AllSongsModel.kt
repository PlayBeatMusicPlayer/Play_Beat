package com.knesarcreation.playbeat.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allSongsModel")
data class AllSongsModel(
    var songId: Long,
    var albumId: Long,
    var songName: String,
    var artistsName: String,
    var albumName: String,
    var size: Int,
    var duration: Int,
    var data: String,
    var audioUri: String,
    var artUri: String,
    var dateAdded: String,
    var isFavourite: Boolean,
    var favAudioAddedTime: Long
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var playingOrPause: Int = -1 // 0 for pause , 1 for play and -1 for default

    constructor() : this(0L, 0L, "", "", "", 0, 0/*null*/, "", "", "", "", false,0L)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AllSongsModel

        if (songName != other.songName) return false
        if (playingOrPause != other.playingOrPause) return false

        return true
    }

    override fun hashCode(): Int {
        var result = songName.hashCode()
        result = 31 * result + playingOrPause.hashCode()
        return result
    }


}
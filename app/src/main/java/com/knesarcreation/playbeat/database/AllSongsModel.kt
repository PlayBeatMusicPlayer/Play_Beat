package com.knesarcreation.playbeat.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allSongsModel")
data class  AllSongsModel(
    val songId: Long,
    val albumId: Long,
    val songName: String,
    val artistsName: String,
    val albumName: String,
    val size: Int,
    val duration: Int,
    val data: String,
    val audioUri: String,
    val artUri: String
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var playingOrPause :Int = -1 // 0 for pause , 1 for play and -1 for default
    //var isPlayingOrPause = false

    constructor() : this(0L, 0L, "", "", "", 0, 0/*null*/, "", "", "")

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
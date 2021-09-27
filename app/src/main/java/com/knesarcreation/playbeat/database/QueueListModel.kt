package com.knesarcreation.playbeat.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queueList")
data class QueueListModel(
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
    var isPlayingOrPause: Int, // 0 for pause , 1 for play and -1 for default
    val dateAdded: String
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    //var isPlayingOrPause = false

    constructor() : this(0L, 0L, "", "", "", 0, 0/*null*/, "", "", "", -1, "")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AllSongsModel

        if (songName != other.songName) return false
        if (isPlayingOrPause != other.playingOrPause) return false

        return true
    }

    override fun hashCode(): Int {
        var result = songName.hashCode()
        result = 31 * result + isPlayingOrPause.hashCode()
        return result
    }

}
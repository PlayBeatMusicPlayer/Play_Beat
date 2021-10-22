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
    var contentUri: String,
    var artUri: String,
    var dateAdded: String,
    var isFavourite: Boolean,
    var favAudioAddedTime: Long,
    var artistId: Long,
    var displayName: String,
    var contentType: String,
    var year: Int,

    ) {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var playingOrPause: Int = -1 // 0 for pause , 1 for play and -1 for default
    var currentPlayedAudioTime: Long = 0L
    var mostPlayedCount: Int = 0
    var isChecked: Boolean = false

    constructor() : this(0L, 0L, "", "", "", 0, 0/*null*/, "", "", "", "", false, 0L, 0L, "", "", 0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AllSongsModel

        if (songId != other.songId) return false
        if (songName != other.songName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = songId.hashCode()
        result = 31 * result + songName.hashCode()
        return result
    }


}
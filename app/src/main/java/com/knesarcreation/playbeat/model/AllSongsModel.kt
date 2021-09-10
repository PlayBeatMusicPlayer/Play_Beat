package com.knesarcreation.playbeat.model

import android.net.Uri

data class AllSongsModel(
    var albumId: Long,
    var songName: String,
    var artistsName: String,
    var albumName: String,
    var size: Int,
    var duration: Int,
    var data: String,
    var audioUri: String,
    var artUri: String
) {
    constructor() : this(0L, "", "", "", 0, 0/*null*/, "", "", "")


}
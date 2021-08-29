package com.knesarcreation.playbeat.model

import android.graphics.Bitmap
import android.net.Uri

data class AllSongsModel(
    var albumId:Long,
    var uri: Uri?,
    var songName: String,
    var artistsName: String,
    var albumName: String,
    var size: Int,
    var duration: Int,
    var bitmap:Bitmap?
) {
    constructor() : this(0L,null, "", "", "", 0, 0,null)
}
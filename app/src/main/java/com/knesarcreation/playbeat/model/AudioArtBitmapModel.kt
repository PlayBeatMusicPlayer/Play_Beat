package com.knesarcreation.playbeat.model

import android.graphics.Bitmap

data class AudioArtBitmapModel(var audioId: Long, var audioName: String, var bitMapImg: Bitmap?) {

    constructor() : this(0L, "", null)
}
package com.knesarcreation.playbeat.utils

import android.media.MediaMetadataRetriever

object SongAlbumArt {
     fun get(uri: String): ByteArray? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri)
        val art = retriever.embeddedPicture
        retriever.release()
        return art
    }
}
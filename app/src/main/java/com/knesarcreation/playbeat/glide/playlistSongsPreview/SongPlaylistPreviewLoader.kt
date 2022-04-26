package com.knesarcreation.playbeat.glide.playlistSongsPreview

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey

class SongPlaylistPreviewLoader(val context: Context) : ModelLoader<PlaylistSongsPreview, Bitmap> {
    override fun buildLoadData(
        model: PlaylistSongsPreview,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<Bitmap> {
        return LoadData(
            ObjectKey(model),
            SongPlaylistPreviewFetcher(context, model)
        )
    }

    override fun handles(model: PlaylistSongsPreview): Boolean {
        return true
    }

    class Factory(val context: Context) : ModelLoaderFactory<PlaylistSongsPreview, Bitmap> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<PlaylistSongsPreview, Bitmap> {
            return SongPlaylistPreviewLoader(context)
        }

        override fun teardown() {}
    }
}

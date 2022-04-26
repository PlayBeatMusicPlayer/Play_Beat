package com.knesarcreation.playbeat.glide

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.knesarcreation.playbeat.glide.artistimage.ArtistImage
import com.knesarcreation.playbeat.glide.artistimage.Factory
import com.knesarcreation.playbeat.glide.audiocover.AudioFileCover
import com.knesarcreation.playbeat.glide.audiocover.AudioFileCoverLoader
import com.knesarcreation.playbeat.glide.palette.BitmapPaletteTranscoder
import com.knesarcreation.playbeat.glide.palette.BitmapPaletteWrapper
import com.knesarcreation.playbeat.glide.playlistPreview.PlaylistPreview
import com.knesarcreation.playbeat.glide.playlistPreview.PlaylistPreviewLoader
import com.knesarcreation.playbeat.glide.playlistSongsPreview.PlaylistSongsPreview
import com.knesarcreation.playbeat.glide.playlistSongsPreview.SongPlaylistPreviewLoader
import java.io.InputStream

@GlideModule
class PlayBeatGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            PlaylistPreview::class.java,
            Bitmap::class.java,
            PlaylistPreviewLoader.Factory(context)
        )

        registry.prepend(
            PlaylistSongsPreview::class.java,
            Bitmap::class.java,
            SongPlaylistPreviewLoader.Factory(context)
        )

        registry.prepend(
            AudioFileCover::class.java,
            InputStream::class.java,
            AudioFileCoverLoader.Factory()
        )
        registry.prepend(ArtistImage::class.java, InputStream::class.java, Factory(context))
        registry.register(
            Bitmap::class.java,
            BitmapPaletteWrapper::class.java, BitmapPaletteTranscoder()
        )
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
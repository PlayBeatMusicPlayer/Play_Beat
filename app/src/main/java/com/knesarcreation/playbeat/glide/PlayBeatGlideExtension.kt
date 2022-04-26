package com.knesarcreation.playbeat.glide

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.bumptech.glide.annotation.GlideType
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.BaseRequestOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.MediaStoreSignature
import com.knesarcreation.appthemehelper.ThemeStore.Companion.accentColor
import com.knesarcreation.appthemehelper.util.TintHelper
import com.knesarcreation.playbeat.App.Companion.getContext
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.glide.artistimage.ArtistImage
import com.knesarcreation.playbeat.glide.audiocover.AudioFileCover
import com.knesarcreation.playbeat.glide.palette.BitmapPaletteWrapper
import com.knesarcreation.playbeat.model.Artist
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.ArtistSignatureUtil
import com.knesarcreation.playbeat.util.CustomArtistImageUtil.Companion.getFile
import com.knesarcreation.playbeat.util.CustomArtistImageUtil.Companion.getInstance
import com.knesarcreation.playbeat.util.MusicUtil.getMediaStoreAlbumCoverUri
import com.knesarcreation.playbeat.util.PreferenceUtil
import java.io.File


@GlideExtension
object PlayBeatGlideExtension {

    private const val DEFAULT_ARTIST_IMAGE =
        R.drawable.default_artist_art
    private const val DEFAULT_SONG_IMAGE: Int = R.drawable.default_audio_art
    private const val DEFAULT_ALBUM_IMAGE = R.drawable.default_album_art
    private const val DEFAULT_ERROR_IMAGE_BANNER = R.drawable.material_design_default

    private val DEFAULT_DISK_CACHE_STRATEGY_ARTIST = DiskCacheStrategy.RESOURCE
    private val DEFAULT_DISK_CACHE_STRATEGY = DiskCacheStrategy.NONE

    private const val DEFAULT_ANIMATION = android.R.anim.fade_in

    @JvmStatic
    @GlideType(BitmapPaletteWrapper::class)
    fun asBitmapPalette(requestBuilder: RequestBuilder<BitmapPaletteWrapper>): RequestBuilder<BitmapPaletteWrapper> {
        return requestBuilder
    }

    private fun getSongModel(song: Song, ignoreMediaStore: Boolean): Any {
        return if (ignoreMediaStore) {
            AudioFileCover(song.data)
        } else {
            getMediaStoreAlbumCoverUri(song.albumId)
        }
    }

    fun getSongModel(song: Song): Any {
        return getSongModel(song, PreferenceUtil.isIgnoreMediaStoreArtwork)
    }

    fun getArtistModel(artist: Artist): Any {
        return getArtistModel(
            artist,
            getInstance(getContext()).hasCustomArtistImage(artist),
            false
        )
    }

    fun getArtistModel(artist: Artist, forceDownload: Boolean): Any {
        return getArtistModel(
            artist,
            getInstance(getContext()).hasCustomArtistImage(artist),
            forceDownload
        )
    }

    private fun getArtistModel(
        artist: Artist,
        hasCustomImage: Boolean,
        forceDownload: Boolean
    ): Any {
        return if (!hasCustomImage) {
            ArtistImage(artist)
        } else {
            getFile(artist)
        }
    }

    @JvmStatic
    @GlideOption
    fun artistImageOptions(
        baseRequestOptions: BaseRequestOptions<*>,
        artist: Artist
    ): BaseRequestOptions<*> {
        return baseRequestOptions
            .diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY_ARTIST)
            .priority(Priority.LOW)
            .error(DEFAULT_ARTIST_IMAGE)
            .placeholder(DEFAULT_ARTIST_IMAGE)
            .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
            .signature(createSignature(artist))
    }

    @JvmStatic
    @GlideOption
    fun songCoverOptions(
        baseRequestOptions: BaseRequestOptions<*>,
        song: Song
    ): BaseRequestOptions<*> {
        return baseRequestOptions.diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .error(DEFAULT_SONG_IMAGE)
            .placeholder(DEFAULT_SONG_IMAGE)
            .signature(createSignature(song))
    }

    @JvmStatic
    @GlideOption
    fun simpleSongCoverOptions(
        baseRequestOptions: BaseRequestOptions<*>,
        song: Song
    ): BaseRequestOptions<*> {
        return baseRequestOptions.diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .signature(createSignature(song))
    }

    @JvmStatic
    @GlideOption
    fun albumCoverOptions(
        baseRequestOptions: BaseRequestOptions<*>,
        song: Song
    ): BaseRequestOptions<*> {
        return baseRequestOptions.diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .error(DEFAULT_ALBUM_IMAGE)
            .placeholder(DEFAULT_ALBUM_IMAGE)
            .signature(createSignature(song))
    }

    @JvmStatic
    @GlideOption
    fun userProfileOptions(
        baseRequestOptions: BaseRequestOptions<*>,
        file: File,
        context: Context
    ): BaseRequestOptions<*> {
        return baseRequestOptions.diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .error(getErrorUserProfile(context))
            .signature(createSignature(file))
    }

    @JvmStatic
    @GlideOption
    fun profileBannerOptions(
        baseRequestOptions: BaseRequestOptions<*>,
        file: File
    ): BaseRequestOptions<*> {
        return baseRequestOptions.diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .placeholder(DEFAULT_ERROR_IMAGE_BANNER)
            .error(DEFAULT_ERROR_IMAGE_BANNER)
            .signature(createSignature(file))
    }

    @JvmStatic
    @GlideOption
    fun playlistOptions(
        baseRequestOptions: BaseRequestOptions<*>
    ): BaseRequestOptions<*> {
        return baseRequestOptions.diskCacheStrategy(DEFAULT_DISK_CACHE_STRATEGY)
            .error(DEFAULT_ALBUM_IMAGE)
    }

    private fun createSignature(song: Song): Key {
        return MediaStoreSignature("", song.dateModified, 0)
    }

    private fun createSignature(file: File): Key {
        return MediaStoreSignature("", file.lastModified(), 0)
    }

    private fun createSignature(artist: Artist): Key {
        return ArtistSignatureUtil.getInstance(getContext())
            .getArtistSignature(artist.name)
    }


    private fun getErrorUserProfile(context: Context): Drawable {
        return TintHelper.createTintedDrawable(
            getContext(),
            R.drawable.ic_account,
            accentColor(context)
        )
    }

    fun <TranscodeType> getDefaultTransition(): GenericTransitionOptions<TranscodeType> {
        return GenericTransitionOptions<TranscodeType>().transition(DEFAULT_ANIMATION)
    }
}

// https://github.com/bumptech/glide/issues/527#issuecomment-148840717
fun GlideRequest<Drawable>.crossfadeListener(): GlideRequest<Drawable> {
    return listener(object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            return if (isFirstResource) {
                false // thumbnail was not shown, do as usual
            } else DrawableCrossFadeFactory.Builder()
                .setCrossFadeEnabled(true).build()
                .build(dataSource, isFirstResource)
                .transition(resource, target as Transition.ViewAdapter)
        }
    })
}
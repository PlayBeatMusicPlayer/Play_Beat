package com.knesarcreation.playbeat.activities.tageditor

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.Slide
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.shape.MaterialShapeDrawable
import com.knesarcreation.appthemehelper.util.MaterialValueHelper
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.ActivityAlbumTagEditorBinding
import com.knesarcreation.playbeat.extensions.appHandleColor
import com.knesarcreation.playbeat.extensions.defaultFooterColor
import com.knesarcreation.playbeat.extensions.isColorLight
import com.knesarcreation.playbeat.extensions.setTint
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.palette.BitmapPaletteWrapper
import com.knesarcreation.playbeat.model.ArtworkInfo
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.ColorUtil.generatePalette
import com.knesarcreation.playbeat.util.ColorUtil.getColor
import com.knesarcreation.playbeat.util.ImageUtil
import com.knesarcreation.playbeat.util.MusicUtil
import org.jaudiotagger.tag.FieldKey
import java.util.*

class AlbumTagEditorActivity : AbsTagEditorActivity<ActivityAlbumTagEditorBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityAlbumTagEditorBinding =
        ActivityAlbumTagEditorBinding::inflate

    private fun windowEnterTransition() {
        val slide = Slide()
        slide.excludeTarget(R.id.appBarLayout, true)
        slide.excludeTarget(R.id.status_bar, true)
        slide.excludeTarget(android.R.id.statusBarBackground, true)
        slide.excludeTarget(android.R.id.navigationBarBackground, true)

        window.enterTransition = slide
    }

    private var albumArtBitmap: Bitmap? = null
    private var deleteAlbumArt: Boolean = false

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.appBarLayout?.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.sharedElementsUseOverlay = true
        binding.imageContainer.transitionName = getString(R.string.transition_album_art)
        windowEnterTransition()
        setUpViews()
        setupToolbar()
    }

    private fun setUpViews() {
        fillViewsWithFileTags()

        binding.yearContainer.setTint(false)
        binding.genreContainer.setTint(false)
        binding.albumTitleContainer.setTint(false)
        binding.albumArtistContainer.setTint(false)

        binding.albumText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.albumArtistText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.genreTitle.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.yearTitle.appHandleColor().doAfterTextChanged { dataChanged() }
    }

    private fun fillViewsWithFileTags() {
        binding.albumText.setText(albumTitle)
        binding.albumArtistText.setText(albumArtistName)
        binding.genreTitle.setText(genreName)
        binding.yearTitle.setText(songYear)
        println(albumTitle + albumArtistName)
    }

    override fun loadCurrentImage() {
        val bitmap = albumArt
        setImageBitmap(
            bitmap,
            getColor(
                generatePalette(bitmap),
                defaultFooterColor()
            )
        )
        deleteAlbumArt = false
    }

    private fun toastLoadingFailed() {
        Toast.makeText(
            this@AlbumTagEditorActivity,
            R.string.could_not_download_album_cover,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun searchImageOnWeb() {
        searchWebFor(binding.albumText.text.toString(), binding.albumArtistText.text.toString())
    }

    override fun deleteImage() {
        setImageBitmap(
            BitmapFactory.decodeResource(resources, R.drawable.default_audio_art),
            defaultFooterColor()
        )
        deleteAlbumArt = true
        dataChanged()
    }

    override fun loadImageFromFile(selectedFile: Uri?) {
        GlideApp.with(this@AlbumTagEditorActivity).asBitmapPalette().load(selectedFile)
            .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
            .into(object : ImageViewTarget<BitmapPaletteWrapper>(binding.editorImage) {
                override fun onResourceReady(
                    resource: BitmapPaletteWrapper,
                    transition: Transition<in BitmapPaletteWrapper>?
                ) {
                    getColor(resource.palette, Color.TRANSPARENT)
                    albumArtBitmap = resource.bitmap?.let { ImageUtil.resizeBitmap(it, 2048) }
                    setImageBitmap(
                        albumArtBitmap,
                        getColor(
                            resource.palette,
                            defaultFooterColor()
                        )
                    )
                    deleteAlbumArt = false
                    dataChanged()
                    setResult(Activity.RESULT_OK)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    Toast.makeText(this@AlbumTagEditorActivity, "Load Failed", Toast.LENGTH_LONG)
                        .show()
                }

                override fun setResource(resource: BitmapPaletteWrapper?) {}
            })
    }

    override fun save() {
        val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
        fieldKeyValueMap[FieldKey.ALBUM] = binding.albumText.text.toString()
        // android seems not to recognize album_artist field so we additionally write the normal artist field
        fieldKeyValueMap[FieldKey.ARTIST] = binding.albumArtistText.text.toString()
        fieldKeyValueMap[FieldKey.ALBUM_ARTIST] = binding.albumArtistText.text.toString()
        fieldKeyValueMap[FieldKey.GENRE] = binding.genreTitle.text.toString()
        fieldKeyValueMap[FieldKey.YEAR] = binding.yearTitle.text.toString()

        writeValuesToFiles(
            fieldKeyValueMap,
            when {
                deleteAlbumArt -> ArtworkInfo(id, null)
                albumArtBitmap == null -> null
                else -> ArtworkInfo(id, albumArtBitmap!!)
            }
        )
    }

    override fun getSongPaths(): List<String> {
        return repository.albumById(id).songs
            .map(Song::data)
    }

    override fun getSongUris(): List<Uri> = repository.albumById(id).songs.map {
        MusicUtil.getSongFileUri(it.id)
    }

    override fun setColors(color: Int) {
        super.setColors(color)
        saveFab.backgroundTintList = ColorStateList.valueOf(color)
        saveFab.backgroundTintList = ColorStateList.valueOf(color)
        ColorStateList.valueOf(
            MaterialValueHelper.getPrimaryTextColor(
                this,
                color.isColorLight
            )
        ).also {
            saveFab.iconTint = it
            saveFab.setTextColor(it)
        }
    }


    override val editorImage: ImageView
        get() = binding.editorImage

    companion object {

        val TAG: String = AlbumTagEditorActivity::class.java.simpleName
    }
}
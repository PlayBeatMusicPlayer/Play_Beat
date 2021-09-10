package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.RecyclerGridAlbumItemsBinding
import com.knesarcreation.playbeat.model.AlbumModel
import com.knesarcreation.playbeat.utils.UriToBitmapConverter
import java.util.concurrent.CopyOnWriteArrayList

class ArtistsAlbumAdapter(var context: Context, var albumList: CopyOnWriteArrayList<AlbumModel>) :
    RecyclerView.Adapter<ArtistsAlbumAdapter.ArtistsAlbumViewHolder>() {

    class ArtistsAlbumViewHolder(binding: RecyclerGridAlbumItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val albumNameTV = binding.albumNameTV
        val artisNameTV = binding.artisNameTV
        val albumArtIV = binding.albumArtIV
        val playAlbumBtn = binding.playAlbum
        val albumArtOverlay = binding.albumArtOverlay
        val albumArtLeftDarkBG = binding.albumArtLeftDarkBG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistsAlbumViewHolder {
        return ArtistsAlbumViewHolder(
            RecyclerGridAlbumItemsBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ArtistsAlbumViewHolder, position: Int) {
        val albumModel = albumList[position]
        holder.albumNameTV.text = albumModel.albumName
        holder.artisNameTV.text = albumModel.artistName

        val artUri = albumModel.artUri

        Glide.with(context).load(artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.album_png).centerCrop())
            .into(holder.albumArtIV)

        val albumArt = UriToBitmapConverter.getBitmap(
            (context).contentResolver!!,
            artUri.toUri()
        )
        if (albumArt != null) {
            createPaletteColor(albumArt, holder)
        } else {
            val albumArtPlaceHolder =
                BitmapFactory.decodeResource(context.resources, R.drawable.album_png)
            createPaletteColor(albumArtPlaceHolder, holder)
        }
    }

    private fun createPaletteColor(albumArt: Bitmap, holder: ArtistsAlbumViewHolder) {
        Palette.from(albumArt).generate {
            val swatch = it?.dominantSwatch
            if (swatch != null) {
                holder.albumArtOverlay.setBackgroundResource(R.drawable.shadow_from_left)
                holder.albumArtLeftDarkBG.setBackgroundResource(R.drawable.album_card_dark_bg)

                val gradientDrawableRight = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(swatch.rgb, 0x00000000)
                )

                val gradientDrawableLeft = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(swatch.rgb, swatch.rgb)
                )


                holder.albumArtOverlay.background = gradientDrawableRight
                holder.albumArtLeftDarkBG.background = gradientDrawableLeft

                holder.albumNameTV.setTextColor(swatch.bodyTextColor)
                holder.artisNameTV.setTextColor(swatch.titleTextColor)
            }
        }

    }

    override fun getItemCount() = albumList.size
}
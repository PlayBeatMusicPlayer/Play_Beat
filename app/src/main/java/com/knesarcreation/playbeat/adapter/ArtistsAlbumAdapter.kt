package com.knesarcreation.playbeat.adapter

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.RecyclerGridAlbumItemsBinding
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.model.AlbumModel
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.utils.AudioPlayingFromCategory
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList

class ArtistsAlbumAdapter(
    var context: Context,
    var albumList: CopyOnWriteArrayList<AlbumModel>,
    var listener: AllAlbumsAdapter.OnAlbumClicked
) :
    RecyclerView.Adapter<ArtistsAlbumAdapter.ArtistsAlbumViewHolder>() {

    private var audioList = CopyOnWriteArrayList<AllSongsModel>()

    /*interface OnArtistAlbumClicked {
        fun onClicked(albumModel: AlbumModel)
    }*/

    class ArtistsAlbumViewHolder(binding: RecyclerGridAlbumItemsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val albumNameTV = binding.albumNameTV
        val artisNameTV = binding.artisNameTV
        val albumArtIV = binding.albumArtIV
        val playAlbumBtn = binding.playAlbum
        val albumArtOverlay = binding.albumArtOverlay
        val albumArtLeftDarkBG = binding.albumArtLeftDarkBG
        val cvAlbumCard = binding.cvAlbumCard
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

        val albumArt = albumModel?.albumBitmap

        if (albumArt != null) {
            createPaletteColor(albumArt, holder)
        } else {
            val albumArtPlaceHolder =
                BitmapFactory.decodeResource(context.resources, R.drawable.album_png)
            createPaletteColor(albumArtPlaceHolder, holder)
        }

        holder.playAlbumBtn.setOnClickListener {
            StorageUtil(context).saveIsShuffled(false)
            loadAlbumAudio(albumModel)
        }

        holder.cvAlbumCard.setOnClickListener {
            listener.onClicked(albumModel)
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


                //holder.albumArtOverlay.background = gradientDrawableRight
                //holder.albumArtLeftDarkBG.background = gradientDrawableLeft
                Glide.with(context).load(gradientDrawableRight)
                    //.apply(RequestOptions.placeholderOf(R.drawable.shadow_from_left))
                    .into(holder.albumArtOverlay)

                Glide.with(context).load(gradientDrawableLeft)
                    //.apply(RequestOptions.placeholderOf(R.drawable.album_card_dark_bg))
                    .into(holder.albumArtLeftDarkBG)
                holder.albumNameTV.setTextColor(swatch.bodyTextColor)
                holder.artisNameTV.setTextColor(swatch.titleTextColor)
            }
        }

    }

    private fun loadAlbumAudio(albumModel: AlbumModel) {
        audioList.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA, // path
        )

        // Show only audios that are at least 1 minutes in duration.
        //val selection =
        //  "${MediaStore.Audio.Media.DURATION} >= ? AND ${MediaStore.Audio.Albums.ALBUM} =?"
        val selection =
            "${MediaStore.Audio.Albums.ALBUM} =?"
        val selectionArgs = arrayOf(
            //TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString(),
            albumModel.albumName
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        val query =
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val data = cursor.getString(dataColumn)

                Log.d("SongDetails", "loadAlbumSongs: $name, $artist")

                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                val albumArtUri =
                    Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                // getting audio uri
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val allSongsModel =
                    AllSongsModel(
                        id,
                        albumId,
                        name,
                        artist,
                        album,
                        size,
                        duration,
                        data,
                        contentUri.toString(),
                        albumArtUri,

                    )
                allSongsModel.playingOrPause = -1
                audioList.add(allSongsModel)

            }

            // Stuff that updates the UI
            (context as AppCompatActivity).runOnUiThread {
                playAudio() // from start
            }
            cursor.close()
        }

    }

    private fun playAudio() {
        val storageUtil = StorageUtil(context)
        AudioPlayingFromCategory.audioPlayingFromAlbumORArtist = true

        storageUtil.storeAudio(audioList)
        storageUtil.storeAudioIndex(0)

        //Send a broadcast to the service -> PLAY_NEW_AUDIO
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (context).sendBroadcast(broadcastIntent)
    }

    override fun getItemCount() = albumList.size
}
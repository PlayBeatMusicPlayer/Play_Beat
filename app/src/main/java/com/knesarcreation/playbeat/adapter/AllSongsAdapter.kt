package com.knesarcreation.playbeat.adapter

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.model.AllSongsModel
import java.io.FileNotFoundException
import java.io.IOException

class AllSongsAdapter(
    var context: Context,
    var allSongList: ArrayList<AllSongsModel>,
    var listener: OnClickSongItem
) :
    RecyclerView.Adapter<AllSongsAdapter.AllSongsViewHolder>() {

//    private var isPlaying = false

    interface OnClickSongItem {
        fun onClick(allSongModel: AllSongsModel, position: Int)
    }

    class AllSongsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songName: TextView = view.findViewById(R.id.songNameTV)
        val artistName: TextView = view.findViewById(R.id.artistNameTV)
        val albumName: TextView = view.findViewById(R.id.albumNameTv)
        val albumArtIV: ImageView = view.findViewById(R.id.songPosterIV)
        val rlAudio: RelativeLayout = view.findViewById(R.id.rlAudio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllSongsViewHolder {
        return AllSongsViewHolder(
            LayoutInflater.from(context).inflate(R.layout.recycler_all_songs_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AllSongsViewHolder, position: Int) {
        val allSongModel = allSongList[position]
        holder.songName.text = allSongModel.songName
        holder.artistName.text = allSongModel.artistsName
        holder.albumName.text = allSongModel.albumName

//        val albumArt = SongAlbumArt.get((allSongModel.path))
        val albumArt = allSongModel.albumArt
        if (albumArt != null) {
            Glide.with(context).asBitmap()
                .load(albumArt)
                .into(holder.albumArtIV)
        } else {
            Glide.with(context).load(R.drawable.music_note_1).into(holder.albumArtIV)
        }
//        Glide.with(context).asBitmap().load(allSongModel.bitmap).into(holder.albumArtIV)

        holder.rlAudio.setOnClickListener {
            listener.onClick(allSongModel, position)
        }
    }

    override fun getItemCount() = allSongList.size

    private fun getAlbumUri(albumId: Long): Bitmap? {
        //getting album art uri
        var bitmap: Bitmap? = null
        val sArtworkUri = Uri
            .parse("content://media/external/audio/albumart")
        val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)

        try {
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(
                    context.contentResolver, albumArtUri
                )
                bitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
            } else {
                val source =
                    ImageDecoder.createSource(context.contentResolver, albumArtUri)
                bitmap = ImageDecoder.decodeBitmap(source)
            }

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.music_note_1)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap!!
    }

}
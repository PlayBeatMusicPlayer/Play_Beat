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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.utils.StorageUtil
import java.io.FileNotFoundException
import java.io.IOException

class AllSongsAdapter(
    var context: Context,
    //var allSongList: CopyOnWriteArrayList<AllSongsModel>,
    private var onClickListener: OnClickListener
) : ListAdapter<AllSongsModel, AllSongsAdapter.AllSongsViewHolder>(AllSongItemCallback()) {
    // RecyclerView.Adapter<AllSongsAdapter.AllSongsViewHolder>() {


    private var mViewModelClass: ViewModelClass =
        ViewModelProvider((context as AppCompatActivity))[ViewModelClass::class.java]

    private val storageUtil = StorageUtil(context)

    // interface OnClickSongItem {
    //   fun onClick(allSongModel: AllSongsModel, position: Int)
    //}

    class AllSongsViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val songName: TextView = view.findViewById(R.id.songNameTV)
        val artistName: TextView = view.findViewById(R.id.artistNameTV)
        val albumName: TextView = view.findViewById(R.id.albumNameTv)
        val albumArtIV: ImageView = view.findViewById(R.id.album_art_iv)
        val rlAudio: RelativeLayout = view.findViewById(R.id.rlAudio)
        val currentPlayingAudioLottie: LottieAnimationView =
            view.findViewById(R.id.currentPlayingAudioLottie)
        val rlCurrentPlayingLottie: RelativeLayout = view.findViewById(R.id.rlCurrentPlayingLottie)
        val currentPlayingAudioIndicator: ImageView =
            view.findViewById(R.id.currentPlayingAudioIndicator)

        fun bind(allSongModel: AllSongsModel) {

            songName.text = allSongModel.songName
            artistName.text = allSongModel.artistsName
            albumName.text = allSongModel.albumName

            val artUri = allSongModel.artUri


            currentPlayingAudioLottie.setAnimation(R.raw.playing_audio_indicator)
            when (allSongModel.playingOrPause) {
                1 /* 1 for play */ -> {
                    rlCurrentPlayingLottie.visibility = View.VISIBLE
                    //r.currentPlayingAudioIndicator.visibility = View.VISIBLE
                    currentPlayingAudioLottie.playAnimation()
                    songName.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.teal_200
                        )
                    )
                    artistName.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.teal_200
                        )
                    )
                    albumName.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.teal_200
                        )
                    )
                }
                0 /* 0 for pause*/ -> {
                    rlCurrentPlayingLottie.visibility = View.VISIBLE
                    //r.currentPlayingAudioIndicator.visibility = View.VISIBLE
                    currentPlayingAudioLottie.pauseAnimation()
                    songName.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.teal_200
                        )
                    )
                    artistName.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.teal_200
                        )
                    )
                    albumName.setTextColor(
                        ContextCompat.getColor(
                            itemView.context,
                            R.color.teal_200
                        )
                    )
                }
                -1 /*default*/ -> {
                    currentPlayingAudioLottie.pauseAnimation()
                    rlCurrentPlayingLottie.visibility = View.GONE
                    songName.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    artistName.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    albumName.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                    //holder.currentPlayingAudioIndicator.visibility = View.GONE
                }
            }


            Glide.with(albumArtIV).load(artUri)
                .apply(RequestOptions.placeholderOf(R.drawable.audio_icon_placeholder).centerCrop())
                .into(albumArtIV)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllSongsViewHolder {
        return AllSongsViewHolder(
            LayoutInflater.from(context).inflate(R.layout.recycler_all_songs_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AllSongsViewHolder, position: Int) {
        val allSongModel = getItem(position)

        holder.rlAudio.setOnClickListener {
            onClickListener.onClick(allSongModel, position)
        }

        holder.bind(allSongModel)
    }

    //override fun getItemCount() = allSongList.size

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

    class AllSongItemCallback : DiffUtil.ItemCallback<AllSongsModel>() {
        override fun areItemsTheSame(oldItem: AllSongsModel, newItem: AllSongsModel) =
            oldItem.songName == newItem.songName

        override fun areContentsTheSame(
            oldItem: AllSongsModel,
            newItem: AllSongsModel
        ) = oldItem == newItem

    }

    class OnClickListener(val clickListener: (allSongModel: AllSongsModel, position: Int) -> Unit) {
        fun onClick(allSongModel: AllSongsModel, position: Int) =
            clickListener(allSongModel, position)
    }
}
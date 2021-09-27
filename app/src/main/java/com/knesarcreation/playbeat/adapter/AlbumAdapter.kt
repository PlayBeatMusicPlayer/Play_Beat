package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import java.util.concurrent.CopyOnWriteArrayList

class AlbumAdapter(
    var context: Context,
    var audioList: CopyOnWriteArrayList<AllSongsModel>,
    private var onClickListener: OnClickListener
) :
    DragDropSwipeAdapter<AllSongsModel, AlbumAdapter.AlbumViewHolder>(audioList) {

    class AlbumViewHolder(view: View) :
        DragDropSwipeAdapter.ViewHolder(view) {
        private val songName: TextView = view.findViewById(R.id.songNameTV)
        private val artistName: TextView = view.findViewById(R.id.artistNameTV)
        private val albumName: TextView = view.findViewById(R.id.albumNameTv)
        private val albumArtIV: ImageView = view.findViewById(R.id.album_art_iv)
        val rlAudio: RelativeLayout = view.findViewById(R.id.rlCurrentPlayingAudio)
        private val currentPlayingAudioLottie: LottieAnimationView =
            view.findViewById(R.id.currentPlayingAudioLottie)
        private val rlCurrentPlayingLottie: RelativeLayout =
            view.findViewById(R.id.rlCurrentPlayingLottie)
        private val currentPlayingAudioIndicator: ImageView =
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

//    interface OnAlbumSongClicked {
//        fun onAudioPlayed(audioModel: AllSongsModel, position: Int)
//    }

    /* override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
         return AlbumViewHolder(
             RecyclerAlbumItemsBinding.inflate(
                 LayoutInflater.from(parent.context),
                 parent,
                 false
             )
         )
     }

     override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
         val audioModel = audioList[position]

         holder.audioNameTv.text = audioModel.songName
         holder.artistNameTV.text = audioModel.artistsName
         holder.rlAudio.setOnClickListener {
             listener.onAudioPlayed(audioModel, position)
         }
     }*/

    override fun getViewHolder(itemView: View) = AlbumViewHolder(itemView)

    override fun getViewToTouchToStartDraggingItem(
        item: AllSongsModel,
        viewHolder: AlbumViewHolder,
        position: Int
    ): View? {
        return null
    }

    override fun onBindViewHolder(item: AllSongsModel, viewHolder: AlbumViewHolder, position: Int) {
        viewHolder.itemView.setOnClickListener {
            onClickListener.onClick(item, position)
        }

        viewHolder.bind(item)
    }

    class OnClickListener(val clickListener: (allSongModel: AllSongsModel, position: Int) -> Unit) {
        fun onClick(allSongModel: AllSongsModel, position: Int) =
            clickListener(allSongModel, position)
    }
}
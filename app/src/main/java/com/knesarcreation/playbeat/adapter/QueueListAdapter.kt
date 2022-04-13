package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.utils.StorageUtil
import eu.gsottbauer.equalizerview.EqualizerView
import java.util.concurrent.CopyOnWriteArrayList

class QueueListAdapter(
    var context: Context,
    queueList: ArrayList<AllSongsModel>,
    var listener: OnClickQueueItem
) :
    DragDropSwipeAdapter<AllSongsModel, QueueListAdapter.QueueListViewHolder>(queueList) {

    private val mViewModelClass =
        ViewModelProvider(context as AppCompatActivity)[ViewModelClass::class.java]

    interface OnClickQueueItem {
        fun onClick(allSongModel: AllSongsModel, position: Int)
    }

    inner class QueueListViewHolder(itemView: View) :
        DragDropSwipeAdapter.ViewHolder(itemView) {
        val songName: TextView = itemView.findViewById(R.id.songNameTV)
        val artistName: TextView = itemView.findViewById(R.id.artistNameTV)
        val albumName: TextView = itemView.findViewById(R.id.albumNameTv)
        val albumArtIV: ImageView = itemView.findViewById(R.id.album_art_iv)
        val rlAudio: RelativeLayout = itemView.findViewById(R.id.rlAudio)
        val dragIcon: ImageView = itemView.findViewById(R.id.dragIconIV)

        //val currentPlayingAudioLottie: LottieAnimationView =
        //  itemView.findViewById(R.id.currentPlayingAudioLottie)
        val equalizerView: EqualizerView = itemView.findViewById(R.id.equalizerView)

        val rlCurrentPlayingLottie: RelativeLayout =
            itemView.findViewById(R.id.rlCurrentPlayingLottie)
    }

    override fun getViewHolder(itemView: View) = QueueListViewHolder(itemView)

    override fun onBindViewHolder(
        item: AllSongsModel,
        viewHolder: QueueListViewHolder,
        position: Int
    ) {
        viewHolder.songName.text = item.songName
        viewHolder.albumName.text = item.albumName
        viewHolder.artistName.text = item.artistsName
        viewHolder.dragIcon.visibility = View.VISIBLE
        viewHolder.rlAudio.setOnClickListener {
            listener.onClick(item, position)
            Log.d(
                "currentQueueItemPos",
                "onBindViewHolder:${item.playingOrPause} ,   Pos: $position "
            )
        }

        viewHolder.rlAudio.setBackgroundColor(
            ContextCompat.getColor(
                context,
                R.color.app_theme_color
            )
        )

        val artUri = item.artUri


        //viewHolder.currentPlayingAudioLottie.setAnimation(R.raw.playing_audio_indicator)
        when (item.playingOrPause) {
            1 /* 1 for play */ -> {
                if (AllSongFragment.musicService?.mediaPlayer != null) {
                    if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                        viewHolder.rlCurrentPlayingLottie.visibility = View.VISIBLE
                        //holder.currentPlayingAudioIndicator.visibility = View.VISIBLE
                        viewHolder.equalizerView.animateBars()

                        viewHolder.songName.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.cyanea_accent_reference
                            )
                        )
                        viewHolder.artistName.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.cyanea_accent_reference
                            )
                        )
                        viewHolder.albumName.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.cyanea_accent_reference
                            )
                        )
                    } else {
                        viewHolder.rlCurrentPlayingLottie.visibility = View.VISIBLE
                        //holder.currentPlayingAudioIndicator.visibility = View.VISIBLE
                        viewHolder.equalizerView.stopBars()

                        viewHolder.songName.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.cyanea_accent_reference
                            )
                        )
                        viewHolder.artistName.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.cyanea_accent_reference
                            )
                        )
                        viewHolder.albumName.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.cyanea_accent_reference
                            )
                        )
                    }
                }

            }
            0 /* 0 for pause*/ -> {
                viewHolder.rlCurrentPlayingLottie.visibility = View.VISIBLE
                //holder.currentPlayingAudioIndicator.visibility = View.VISIBLE
                viewHolder.equalizerView.stopBars()
                viewHolder.songName.setTextColor(ContextCompat.getColor(context, R.color.cyanea_accent_reference))
                viewHolder.artistName.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.cyanea_accent_reference
                    )
                )
                viewHolder.albumName.setTextColor(ContextCompat.getColor(context, R.color.cyanea_accent_reference))
            }
            -1 /*default*/ -> {
                viewHolder.equalizerView.stopBars()
                viewHolder.rlCurrentPlayingLottie.visibility = View.GONE
                viewHolder.songName.setTextColor(ContextCompat.getColor(context, R.color.white))
                viewHolder.artistName.setTextColor(ContextCompat.getColor(context, R.color.white))
                viewHolder.albumName.setTextColor(ContextCompat.getColor(context, R.color.white))
                //holder.currentPlayingAudioIndicator.visibility = View.GONE
            }

            else -> {
                viewHolder.songName.setTextColor(ContextCompat.getColor(context, R.color.white))
                viewHolder.artistName.setTextColor(ContextCompat.getColor(context, R.color.white))
                viewHolder.albumName.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
        }

        Glide.with(context).load(artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.music_note_icon).centerCrop())
            .into(viewHolder.albumArtIV)

    }

    override fun getViewToTouchToStartDraggingItem(
        item: AllSongsModel,
        viewHolder: QueueListViewHolder,
        position: Int
    ): View {
        return viewHolder.dragIcon
    }

    override fun onDragFinished(item: AllSongsModel, viewHolder: QueueListViewHolder) {
        super.onDragFinished(item, viewHolder)
        // mViewModelClass.deleteQueue((context as AppCompatActivity).lifecycleScope)
        val storageUtil = StorageUtil(context)
        val currentPlayingAudioIndex = storageUtil.loadAudioIndex()
        val loadAudio = storageUtil.loadQueueAudio()
        val currentPlayingAudio = loadAudio[currentPlayingAudioIndex]

        val list = CopyOnWriteArrayList<AllSongsModel>()
        list.addAll(dataSet)
        dataSet =
            list  //updating dataSet : for a rare case if adapter items is not notified that its rearranged

        val newCurrentPlayingAudioIndex = list.indexOf(currentPlayingAudio)
        //Toast.makeText(context, "new: $newCurrentPlayingAudioIndex, old: $currentPlayingAudioIndex", Toast.LENGTH_SHORT).show()
        StorageUtil(context).storeQueueAudio(list)
        if (item.songName == currentPlayingAudio.songName) {
            //if dragged item is current playing audio then store the index of current playing audio
            storageUtil.storeAudioIndex(viewHolder.layoutPosition)
        } else if (currentPlayingAudioIndex != newCurrentPlayingAudioIndex) {
            storageUtil.storeAudioIndex(newCurrentPlayingAudioIndex)
        }

        Log.d(
            "DragDropAdapter",
            "onDragFinished:currentPlayingAudio:  old pos: $currentPlayingAudioIndex , new pos: $newCurrentPlayingAudioIndex "
        )

    }
}
package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.fragment.BottomSheetAudioMoreOptions
import com.knesarcreation.playbeat.model.AudioArtBitmapModel
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Job
import java.util.concurrent.CopyOnWriteArrayList


class AllSongsAdapter(
    private var context: Context,
    //var allSongList: CopyOnWriteArrayList<AllSongsModel>,
    private var onClickListener: OnClickListener,
    private var onLongClickListener: OnLongClickListener,
    private var isAdapterUsingFromCustomPlaylist: Boolean,
) : ListAdapter<AllSongsModel, AllSongsAdapter.AllSongsViewHolder>(AllSongItemCallback()) {
    // RecyclerView.Adapter<AllSongsAdapter.AllSongsViewHolder>() {

    var isSearching = false
    var queryText = ""
    private val mViewModelClass =
        ViewModelProvider(context as AppCompatActivity)[ViewModelClass::class.java]

    companion object {
        var isContextMenuEnabled = false
    }

    class AllSongsViewHolder(view: View, var context: Context) :
        RecyclerView.ViewHolder(view) {
        private val songName: TextView = view.findViewById(R.id.songNameTV)
        private val artistName: TextView = view.findViewById(R.id.artistNameTV)
        private val duration: TextView = view.findViewById(R.id.albumNameTv)
        private val albumArtIV: ImageView = view.findViewById(R.id.album_art_iv)
        val moreIconIV: ImageView = view.findViewById(R.id.moreIcon)
        val rlAudio: RelativeLayout = view.findViewById(R.id.rlAudio)
        val selectedAudioFL: FrameLayout = view.findViewById(R.id.selectedAudioFL)
        private val currentPlayingAudioLottie: LottieAnimationView =
            view.findViewById(R.id.currentPlayingAudioLottie)
        private val rlCurrentPlayingLottie: RelativeLayout =
            view.findViewById(R.id.rlCurrentPlayingLottie)
        private var loadingArtJob: Job? = null
        private var storageUtil = StorageUtil(context)
        private val loadAudioArtBitmapImage = storageUtil.loadAudioArtBitmapImage()

        fun bind(
            allSongModel: AllSongsModel,
            isSearching: Boolean,
            queryText: String
        ) {
            moreIconIV.visibility = View.VISIBLE

            if (isSearching) {
                highlightSearchedAudioText(queryText, allSongModel)
            } else {
                songName.text = allSongModel.songName
            }
            if (allSongModel.artistsName.length >= 28) {
                val dropLastValue = allSongModel.artistsName.length - 25
                artistName.text = "${allSongModel.artistsName.dropLast(dropLastValue)}..."
            } else {
                artistName.text = allSongModel.artistsName
            }
            duration.text = millisToMinutesAndSeconds(allSongModel.duration)

            //val artUri = allSongModel.artUri

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
                    duration.setTextColor(
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
                    duration.setTextColor(
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
                    artistName.setTextColor(ContextCompat.getColor(itemView.context, R.color.grey))
                    duration.setTextColor(ContextCompat.getColor(itemView.context, R.color.grey))
                    //holder.currentPlayingAudioIndicator.visibility = View.GONE
                }
            }

            var audioArtBitmapModel: AudioArtBitmapModel? = null
            val factory = DrawableCrossFadeFactory.Builder(200)
            // try {
            //     audioArtBitmapModel =
            //        loadAudioArtBitmapImage.find { audioArtBitmapModel1 -> audioArtBitmapModel1.audioId == allSongModel.songId }
            //} catch (e: Exception) {

            //}
            //Log.d("audioArtBitmapModel", "bind:$audioArtBitmapModel ")
            //if (audioArtBitmapModel != null) {

            //  Glide.with(context).load(audioArtBitmapModel.bitMapImg)
            //     .placeholder(R.drawable.music_note_icon)
            //.transition(DrawableTransitionOptions.withCrossFade(factory))
            //   .error(R.drawable.music_note_icon)
            //.thumbnail(0.1f)
            //.apply { RequestOptions().downsample(DownsampleStrategy.DEFAULT) }
            // .into(albumArtIV)

            //} else {
            Glide.with(context).load(allSongModel.artUri)
                .placeholder(R.drawable.music_note_icon)
                .transition(DrawableTransitionOptions.withCrossFade(factory))
                .error(R.drawable.music_note_icon)
                //.thumbnail(0.1f)
                //.apply { RequestOptions().downsample(DownsampleStrategy.DEFAULT) }
                .into(albumArtIV)
            //}

        }

        private fun highlightSearchedAudioText(queryText: String, allSongModel: AllSongsModel) {
            if (queryText.isNotEmpty()) {
                val startPos = allSongModel.songName.lowercase().indexOf(queryText)
                val endPos = startPos + queryText.length

                if (startPos != -1) {
                    val spannable = SpannableStringBuilder(allSongModel.songName)
                    spannable.setSpan(
                        ForegroundColorSpan(Color.CYAN),
                        startPos,
                        endPos,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    songName.text = spannable
                } else {
                    songName.text = allSongModel.songName
                }
            } else {
                songName.text = allSongModel.songName
            }
        }

        private fun millisToMinutesAndSeconds(millis: Int): String {
            val minutes = kotlin.math.floor((millis / 60000).toDouble())
            val seconds = ((millis % 60000) / 1000)
            return if (seconds == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds "
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllSongsViewHolder {
        return AllSongsViewHolder(
            LayoutInflater.from(context).inflate(R.layout.recycler_all_songs_item, parent, false),
            context
        )
    }


    override fun onBindViewHolder(holder: AllSongsViewHolder, position: Int) {
        val allSongModel = getItem(position)

        holder.rlAudio.setOnClickListener {
            if (isContextMenuEnabled) {
                if (!isSearching) {
                    allSongModel.isChecked = !allSongModel.isChecked
                    onLongClickListener.onLongClick(allSongModel, holder.bindingAdapterPosition)
                    notifyItemHasChanged(holder.bindingAdapterPosition, allSongModel)
                }
            } else {
                onClickListener.onClick(allSongModel, holder.bindingAdapterPosition)
            }
        }

        if (allSongModel.isChecked) {
            holder.selectedAudioFL.visibility = View.VISIBLE
        } else {
            holder.selectedAudioFL.visibility = View.GONE
        }

        holder.rlAudio.setOnLongClickListener {
            if (!isSearching) {
                isContextMenuEnabled = true
                allSongModel.isChecked = !allSongModel.isChecked
                onLongClickListener.onLongClick(allSongModel, holder.bindingAdapterPosition)
                notifyItemHasChanged(holder.bindingAdapterPosition, allSongModel)
            }
            return@setOnLongClickListener true
        }

        holder.bind(
            allSongModel,
            isSearching,
            queryText
        )

        holder.moreIconIV.setOnClickListener {
            if (!isContextMenuEnabled) {
                val bottomSheetMoreOptions = BottomSheetAudioMoreOptions(
                    context,
                    allSongModel,
                    false
                )
                bottomSheetMoreOptions.show(
                    (context as AppCompatActivity).supportFragmentManager,
                    "bottomSheetMoreOptions"
                )
            }
        }
    }

    //override fun getItemCount() = allSongList.size

    /* private fun getAlbumUri(albumId: Long): Bitmap? {
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
     }*/


    class AllSongItemCallback : DiffUtil.ItemCallback<AllSongsModel>() {
        override fun areItemsTheSame(oldItem: AllSongsModel, newItem: AllSongsModel) =
            oldItem.songName == newItem.songName

        override fun areContentsTheSame(
            oldItem: AllSongsModel,
            newItem: AllSongsModel
        ) = oldItem.playingOrPause == newItem.playingOrPause

    }

    class OnClickListener(val clickListener: (allSongModel: AllSongsModel, position: Int) -> Unit) {
        fun onClick(allSongModel: AllSongsModel, position: Int) =
            clickListener(allSongModel, position)
    }

    class OnLongClickListener(
        val longClickListener: (allSongModel: AllSongsModel, position: Int) -> Unit
    ) {
        fun onLongClick(allSongModel: AllSongsModel, position: Int) =
            longClickListener(allSongModel, position)
    }

    private fun notifyItemHasChanged(position: Int, audioModel: AllSongsModel) {
        val allSongsModel = currentList[position]
        allSongsModel.isChecked = audioModel.isChecked
        notifyItemChanged(position)
    }

    fun updateChanges(selectedAudioPos: ArrayList<Int>) {
        if (selectedAudioPos.isNotEmpty()) {
            for (pos in selectedAudioPos) {
                val item = getItem(pos)
                item.isChecked = false
                notifyItemChanged(pos)
            }
        }
    }

    fun selectAllAudios() {
        for (audio in currentList) {
            audio.isChecked = true
        }
        notifyItemRangeChanged(0, currentList.size)
    }

    /*fun updateAudioDeleted(selectedAudioPos: ArrayList<Int>) {
        if (selectedAudioPos.isNotEmpty()) {
            for (pos in selectedAudioPos) {
                val item = getItem(pos)
                item.isChecked = false
                notifyItemRemoved(pos)
            }
        }
    }*/

    fun unSelectAllAudios() {
        for ((index, audio) in currentList.withIndex()) {
            audio.isChecked = false
            notifyItemChanged(index)
        }
    }

    private fun convertStringToList(songIdsListString: String): CopyOnWriteArrayList<Long> {
        val gson = Gson()
        val type = object : TypeToken<CopyOnWriteArrayList<Long>>() {}.type
        return gson.fromJson(songIdsListString, type)
    }

}
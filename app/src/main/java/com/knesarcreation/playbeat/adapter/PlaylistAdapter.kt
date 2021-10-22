package com.knesarcreation.playbeat.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.fragment.BottomSheetPlaylistMoreOptions
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList

class PlaylistAdapter(
    val context: Context,
    var listener: OnPlaylistClicked,
    var dataset: ArrayList<PlaylistModel>
) :
    DragDropSwipeAdapter<PlaylistModel, PlaylistAdapter.PlayListViewHolder>(dataset) {

    private var storage: StorageUtil = StorageUtil(context)
    private var mViewModelClass: ViewModelClass =
        ViewModelProvider(context as AppCompatActivity)[ViewModelClass::class.java]
    private var currentPlayingAudioIndex = 0

    interface OnPlaylistClicked {
        fun onClicked(playlistModel: PlaylistModel)
    }

    inner class PlayListViewHolder(view: View) :
        DragDropSwipeAdapter.ViewHolder(view) {
        private val playListIV: ImageView = view.findViewById(R.id.playListIV)
        private val moreOptionIV: ImageView = view.findViewById(R.id.moreOptionIV)
        private val playLisName: TextView = view.findViewById(R.id.playlistName)
        private val songCountTV: TextView = view.findViewById(R.id.songCountTV)
        val rlPlaylistContainer: RelativeLayout = view.findViewById(R.id.rlPlaylistItemContainer)

        fun bind(playlistModel: PlaylistModel) {

            moreOptionIV.setOnClickListener {
                val bottomSheetMoreOptions = BottomSheetPlaylistMoreOptions(playlistModel)
                bottomSheetMoreOptions.show(
                    (context as AppCompatActivity).supportFragmentManager,
                    "bottomSheetMoreOptions"
                )

                bottomSheetMoreOptions.listener =
                    object : BottomSheetPlaylistMoreOptions.OnPlaylistMoreOptionsClicked {
                        override fun playNext(playlistAudioList: CopyOnWriteArrayList<AllSongsModel>) {
                            if (playlistAudioList.isNotEmpty()) {
                                var playingQueueAudioList = CopyOnWriteArrayList<AllSongsModel>()
                                var audioIndex: Int
                                try {
                                    playingQueueAudioList = storage.loadQueueAudio()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                audioIndex = storage.loadAudioIndex()

                                // if queue list is empty then index will be -1 and so audio will be added from 0th pos
                                if (playingQueueAudioList.isEmpty()) {
                                    audioIndex = -1
                                }

                                mViewModelClass.deleteQueue(context.lifecycleScope)
                                //val newAudiosForQueue = CopyOnWriteArrayList<AllSongsModel>()
                                for (audio in playlistAudioList) {
                                    if (audio.playingOrPause != 1 && audio.playingOrPause != 0) {
                                        // selected audio is not playing then only add to play next
                                        audioIndex++
                                        if (playingQueueAudioList.contains(audio)) {
                                            if (playingQueueAudioList.indexOf(audio) < audioIndex) {
                                                audioIndex--
                                            }
                                            playingQueueAudioList.remove(audio)

                                            /* mViewModelClass.deleteOneQueueAudio(
                                                 audio.songId,
                                                 context.lifecycleScope
                                             )*/
                                        }
                                        // adding next to playing index
                                        playingQueueAudioList.add(audioIndex, audio)
                                        // this list is for adding audio into database
                                        //newAudiosForQueue.add(audio)
                                        Log.d(
                                            "PlalistAudioTesting",
                                            "playNext: Index: $audioIndex , $audio , playingOrPause: ${audio.playingOrPause} "
                                        )
                                    }
                                }

                                if (playingQueueAudioList.isNotEmpty()) {
                                    //insert into database
                                    for (audio in playingQueueAudioList) {
                                        val queueListModel = QueueListModel(
                                            audio.songId,
                                            audio.albumId,
                                            audio.songName,
                                            audio.artistsName,
                                            audio.albumName,
                                            audio.size,
                                            audio.duration,
                                            audio.data,
                                            audio.contentUri,
                                            audio.artUri,
                                            audio.playingOrPause,
                                            audio.dateAdded,
                                            audio.isFavourite,
                                            audio.favAudioAddedTime,
                                            audio.mostPlayedCount,
                                            audio.artistId
                                        )
                                        queueListModel.currentPlayedAudioTime =
                                            audio.currentPlayedAudioTime
                                        mViewModelClass.insertQueue(
                                            queueListModel,
                                            context.lifecycleScope
                                        )
                                    }
                                    val playingAudio =
                                        playingQueueAudioList.find { allSongsModel -> allSongsModel.playingOrPause == 1 || allSongsModel.playingOrPause == 0 }
                                    val playingAudioIndex =
                                        playingQueueAudioList.indexOf(playingAudio)
                                    if (playingAudioIndex != -1) {
                                        Log.d(
                                            "playingQueueAudioListaaaa",
                                            "playNext:$playingAudioIndex "
                                        )
                                        storage.storeAudioIndex(playingAudioIndex)
                                    } else {
                                        // -1 index
                                        Log.d(
                                            "playingQueueAudioListaaaa",
                                            "playNext:$playingAudioIndex "
                                        )
                                    }
                                    storage.storeQueueAudio(playingQueueAudioList)
                                }
                                Toast.makeText(
                                    context,
                                    "Added ${playlistAudioList.size} songs to playing queue",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            bottomSheetMoreOptions.dismiss()
                        }

                        override fun addToPlayingQueue(playlistAudioList: CopyOnWriteArrayList<AllSongsModel>) {
                            if (playlistAudioList.isNotEmpty()) {
                                var playingQueueAudioList = CopyOnWriteArrayList<AllSongsModel>()
                                try {
                                    playingQueueAudioList = storage.loadQueueAudio()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                mViewModelClass.deleteQueue(context.lifecycleScope)
                                // val newAudiosForQueue = CopyOnWriteArrayList<AllSongsModel>()
                                for (audio in playlistAudioList) {
                                    if (audio.playingOrPause != 1 && audio.playingOrPause != 0) {
                                        // selected audio is not playing then only add to play next
                                        if (playingQueueAudioList.contains(audio)) {
                                            playingQueueAudioList.remove(audio)
                                            //mViewModelClass.deleteOneQueueAudio(audio.songId, lifecycleScope)
                                        }

                                        // adding to last index
                                        playingQueueAudioList.add(audio)
                                        // this list is for adding audio into database
                                        // newAudiosForQueue.add(audio)
                                    }
                                }

                                if (playingQueueAudioList.isNotEmpty()) {
                                    for (audio in playingQueueAudioList) {
                                        val queueListModel = QueueListModel(
                                            audio.songId,
                                            audio.albumId,
                                            audio.songName,
                                            audio.artistsName,
                                            audio.albumName,
                                            audio.size,
                                            audio.duration,
                                            audio.data,
                                            audio.contentUri,
                                            audio.artUri,
                                            audio.playingOrPause,
                                            audio.dateAdded,
                                            audio.isFavourite,
                                            audio.favAudioAddedTime,
                                            audio.mostPlayedCount,
                                            audio.artistId
                                        )
                                        queueListModel.currentPlayedAudioTime =
                                            audio.currentPlayedAudioTime
                                        mViewModelClass.insertQueue(
                                            queueListModel,
                                            context.lifecycleScope
                                        )
                                    }

                                    val playingAudio =
                                        playingQueueAudioList.find { allSongsModel -> allSongsModel.playingOrPause == 1 || allSongsModel.playingOrPause == 0 }
                                    val playingAudioIndex =
                                        playingQueueAudioList.indexOf(playingAudio)
                                    if (playingAudioIndex != -1) {
                                        Log.d(
                                            "playingQueueAudioListaaaa",
                                            "playNext:$playingAudioIndex "
                                        )
                                        storage.storeAudioIndex(playingAudioIndex)
                                    } else {
                                        // -1 index
                                        Log.d(
                                            "playingQueueAudioListaaaa",
                                            "playNext:$playingAudioIndex "
                                        )
                                    }
                                    storage.storeQueueAudio(playingQueueAudioList)
                                }

                                Toast.makeText(
                                    context,
                                    "Added ${playlistAudioList.size} songs to playing queue",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            bottomSheetMoreOptions.dismiss()
                        }
                    }

            }

            playListIV.setOnClickListener {
                if (playlistModel.songIds.isNotEmpty()) {
                    val playlistSongIds = convertStringToList(playlistModel.songIds)
                    val playlistAudiosList = getPlaylistAudios(playlistSongIds)
                    val sortedAudio = getSortedAudio(playlistModel, playlistAudiosList)
                    Log.d("GetSortedAudio11", "bind: $sortedAudio")
                    Toast.makeText(context, "${sortedAudio[0].songName}", Toast.LENGTH_SHORT).show()
                    onClickAudio(sortedAudio[0], sortedAudio)
                }
            }

            playLisName.text = playlistModel.playlistName
            var songIdsList = ArrayList<Long>()
            if (playlistModel.songIds != "") {
                songIdsList = convertStringToList(playlistModel.songIds)
            }
            songCountTV.text = "${songIdsList.count()} Songs"

        }

        private fun convertStringToList(songId: String): ArrayList<Long> {
            val gson = Gson()
            val type = object : TypeToken<ArrayList<Long>>() {}.type
            return gson.fromJson(songId, type)
        }

        private fun onClickAudio(
            allSongModel: AllSongsModel,
            playlistAudiosList: ArrayList<AllSongsModel>,
        ) {
            storage.saveIsShuffled(false)
            val prevPlayingAudioIndex = storage.loadAudioIndex()
            val prevQueueList = storage.loadQueueAudio()
            var prevPlayingAudioModel: AllSongsModel? = null

            mViewModelClass.deleteQueue((context as AppCompatActivity).lifecycleScope)

            if (prevQueueList.isNotEmpty()) {
                prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]

                mViewModelClass.updateSong(
                    prevPlayingAudioModel.songId,
                    prevPlayingAudioModel.songName,
                    -1,
                    context.lifecycleScope
                )

                mViewModelClass.updateSong(
                    allSongModel.songId,
                    allSongModel.songName,
                    1,
                    context.lifecycleScope
                )
            }

            playAudio(playlistAudiosList)

            // adding queue list to DB and show highlight of current audio
            for (audio in playlistAudiosList) {
                val queueListModel = QueueListModel(
                    audio.songId,
                    audio.albumId,
                    audio.songName,
                    audio.artistsName,
                    audio.albumName,
                    audio.size,
                    audio.duration,
                    audio.data,
                    audio.contentUri,
                    audio.artUri,
                    -1,
                    audio.dateAdded,
                    audio.isFavourite,
                    audio.favAudioAddedTime,
                    audio.mostPlayedCount,
                    audio.artistId
                )
                queueListModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
                mViewModelClass.insertQueue(
                    queueListModel,
                    (context as AppCompatActivity).lifecycleScope
                )
            }

            if (prevQueueList.isNotEmpty()) {
                mViewModelClass.updateQueueAudio(
                    prevPlayingAudioModel!!.songId,
                    prevPlayingAudioModel.songName,
                    -1,
                    (context as AppCompatActivity).lifecycleScope
                )
            }

            mViewModelClass.updateQueueAudio(
                allSongModel.songId,
                allSongModel.songName,
                1,
                (context as AppCompatActivity).lifecycleScope
            )
        }

        private fun playAudio(playlistAudiosList: ArrayList<AllSongsModel>) {
            //this.currentPlayingAudioIndex = 0
            //store audio to prefs

            val playlist = CopyOnWriteArrayList<AllSongsModel>()
            playlist.addAll(playlistAudiosList)
            storage.storeQueueAudio(playlist)
            //Store the new audioIndex to SharedPreferences
            storage.storeAudioIndex(0)

            //Service is active send broadcast
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            (context as AppCompatActivity).sendBroadcast(broadcastIntent)

        }

        private fun getPlaylistAudios(songsIdList: ArrayList<Long>): ArrayList<AllSongsModel> {
            val playlistAudioList = ArrayList<AllSongsModel>()
            val audioList = storage.loadAudio()
            for (songId in songsIdList) {
                for (audio in audioList) {
                    if (audio.songId == songId) {
                        playlistAudioList.add(audio)
                        break
                    }
                }
            }
            return playlistAudioList
        }

        private fun getSortedAudio(
            playlistModel: PlaylistModel,
            audio: ArrayList<AllSongsModel>
        ): ArrayList<AllSongsModel> {
            val sortedPlaylistAudio = ArrayList<AllSongsModel>()
            val sortedAudio: List<AllSongsModel>
            when (storage.getAudioSortedValue(playlistModel.playlistName)) {
                "Name" -> {
                    sortedAudio = audio.sortedBy { allSongsModel -> allSongsModel.songName.trim() }
                }

                "DateAdded" -> {
                    sortedAudio =
                        audio.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
                }

                "ArtistName" -> {
                    sortedAudio = audio.sortedBy { allSongsModel -> allSongsModel.artistsName }
                }

                else -> {
                    sortedAudio = audio.sortedBy { allSongsModel -> allSongsModel.songName.trim() }
                }
            }
            sortedPlaylistAudio.addAll(sortedAudio)
            return sortedPlaylistAudio
        }
    }

    class PlayListItemCallback : DiffUtil.ItemCallback<PlaylistModel>() {
        override fun areItemsTheSame(oldItem: PlaylistModel, newItem: PlaylistModel) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PlaylistModel,
            newItem: PlaylistModel
        ) = oldItem.playlistName == newItem.playlistName

    }

    /*override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListViewHolder {
        return PlayListViewHolder(
            RecyclerPlaylistItemsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }*/

    override fun getViewHolder(itemView: View) = PlayListViewHolder(itemView)

    override fun getViewToTouchToStartDraggingItem(
        item: PlaylistModel,
        viewHolder: PlayListViewHolder,
        position: Int
    ): View? {
        return null
    }

    override fun onBindViewHolder(
        item: PlaylistModel,
        viewHolder: PlayListViewHolder,
        position: Int
    ) {
        viewHolder.bind(item)
        viewHolder.rlPlaylistContainer.setOnClickListener {
            listener.onClicked(item)
        }
    }

    /* override fun onBindViewHolder(holder: PlayListViewHolder, position: Int) {
         holder.bind(getItem(position))
         holder.rlPlaylistContainer.setOnClickListener {
             listener.onClicked(getItem(position))
         }
     }*/

}
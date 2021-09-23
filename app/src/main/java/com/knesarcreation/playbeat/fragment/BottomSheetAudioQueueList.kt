package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.QueueListAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetQueueListBinding
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList


class BottomSheetAudioQueueList(var mContext: Context) : BottomSheetDialogFragment(),
    QueueListAdapter.OnClickQueueItem {
    private var _binding: BottomSheetQueueListBinding? = null
    private val binding get() = _binding
    private var storageUtil: StorageUtil? = null
    private var audioList = ArrayList<AllSongsModel>()
    private var queueList = ArrayList<QueueListModel>()
    private var currentPlayingAudioIndex = -1
    var listener: OnRepeatAudioListener? = null
    private lateinit var queueLisAdapter: QueueListAdapter
    private lateinit var audioModel: AllSongsModel
    private lateinit var mViewModelClass: ViewModelClass

    interface OnRepeatAudioListener {
        fun onRepeatIconClicked()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetQueueListBinding.inflate(inflater, container, false)
        val view = binding?.root

        mViewModelClass =
            ViewModelProvider((mContext as AppCompatActivity))[ViewModelClass::class.java]

        storageUtil = StorageUtil(mContext)
        val loadAudioList = storageUtil?.loadAudio()!!
        audioList.addAll(loadAudioList)
        currentPlayingAudioIndex = storageUtil?.loadAudioIndex()!!

        /* mViewModelClass.deleteQueue(lifecycleScope)
         for (audio in loadAudioList) {
             val queueListModel = QueueListModel(
                 audio.albumId,
                 audio.songName,
                 audio.artistsName,
                 audio.albumName,
                 audio.size,
                 audio.duration,
                 audio.data,
                 audio.audioUri,
                 audio.artUri,
                 audio.isPlayingOrPause
             )
             queueList.add(queueListModel)
             mViewModelClass.insertQueue(queueListModel, lifecycleScope)
         }*/

        setupUpNextAdapter()
        updateCurrentPlayingAudio()
        manageRepeatAudio()

        binding?.rvUpNext?.disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.LEFT)


        /*mViewModel.getAllSong().observe(viewLifecycleOwner, {
            if (it != null) {
                Log.d("QueueListNowPlaying", "onCreateView:$it ")
                val queueList = ArrayList<AllSongsModel>()
                queueList.addAll(storageUtil?.loadAudio()!!)

                for (viewModelList in it) {
                    // checking if queue list have same audio or not
                    for ((index, list) in storageUtil?.loadAudio()!!.withIndex()) {
                        if (list.songName == viewModelList.songName) {
                            queueList.removeAt(index)
                            queueList.add(index, viewModelList)
                        }
                    }
                }
                queueLisAdapter.dataSet =
                    queueList.sortedBy { allSongsModel -> allSongsModel.songName }

            }
        })*/
        return view
    }

    private fun manageRepeatAudio() {
        if (storageUtil?.getIsRepeatAudio()!!) {
            binding?.repeatQueueIV?.setImageResource(R.drawable.repeat_one_on_24)
        } else {
            binding?.repeatQueueIV?.setImageResource(R.drawable.ic_repeat_24)
        }
        binding?.repeatQueueIV?.setOnClickListener {
            if (!storageUtil?.getIsRepeatAudio()!!) {
                //repeat current audio
                Toast.makeText(mContext, "Repeat current audio", Toast.LENGTH_SHORT).show()
                binding?.repeatQueueIV?.setImageResource(R.drawable.repeat_one_on_24)
                storageUtil?.saveIsRepeatAudio(true)
                listener?.onRepeatIconClicked()
            } else {
                //repeat audio list
                Toast.makeText(mContext, "Repeat audio list", Toast.LENGTH_SHORT).show()
                binding?.repeatQueueIV?.setImageResource(R.drawable.ic_repeat_24)
                storageUtil?.saveIsRepeatAudio(false)
                listener?.onRepeatIconClicked()
            }
        }
    }

    private fun setupUpNextAdapter() {
        queueLisAdapter = QueueListAdapter(mContext, audioList, this)
        binding?.rvUpNext?.layoutManager = LinearLayoutManager(mContext)
        binding?.rvUpNext?.adapter = queueLisAdapter
        binding?.rvUpNext?.orientation =
            DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_UNCONSTRAINED_DRAGGING
        binding?.rvUpNext?.reduceItemAlphaOnSwiping = true
        binding?.rvUpNext?.scrollToPosition(currentPlayingAudioIndex)

        val onItemSwipeListener = object : OnItemSwipeListener<AllSongsModel> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: AllSongsModel
            ): Boolean {
                Log.d("QueueListSwipeListener", "onItemSwiped:pos: $position , item: $item ")
                when (direction) {
                    OnItemSwipeListener.SwipeDirection.LEFT_TO_RIGHT -> {
                        Log.d(
                            "QueueItemDeleted",
                            "onItemSwiped:Position: $position , ${item.playingOrPause} deleted ******* audioModel: $audioModel "
                        )

                        audioList.clear()
                        audioList.addAll(storageUtil?.loadAudio()!!)
                        currentPlayingAudioIndex =
                            audioList.indexOf(audioModel) // getting current playing audio index from a arranged list
                        Log.d(
                            "saveAudioListQueue",
                            "saveAudioList: Name:$currentPlayingAudioIndex  ${audioList[1].songName} , 2.${audioModel.songName} ,  PlayingOrPause ${audioList[1].playingOrPause} 2. ${audioModel.playingOrPause}"
                        )

                        if (position == currentPlayingAudioIndex) {
                            mViewModelClass.updateSong(
                                audioList[position].songId,
                                audioList[position].songName,
                                -1,
                                (context as AppCompatActivity).lifecycleScope
                            )

                            mViewModelClass.updateQueueAudio(
                                audioList[position].songId,
                                audioList[position].songName,
                                1,
                                (context as AppCompatActivity).lifecycleScope
                            )
                        } else {
                            if (AllSongFragment.musicService?.mediaPlayer != null) {
                                if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                                    mViewModelClass.updateQueueAudio(
                                        audioModel.songId,
                                        audioModel.songName,
                                        1,
                                        (context as AppCompatActivity).lifecycleScope
                                    )
                                }
                            }
                        }

                        val deleteModel = audioList[position]
                        audioList.removeAt(position)

                        // getting current playing audio index after removing any audio
                        if (currentPlayingAudioIndex != 0) {
                            Log.d(
                                "CurrentPlayingModel",
                                "onItemSwiped: pos: $position -model $deleteModel "
                            )
                            if (deleteModel.songName != audioModel.songName) {
                                currentPlayingAudioIndex = audioList.indexOf(audioModel)
                            }
                        }

                        //save audio list
                        saveAudioList(audioList)

                        // if deleted audio is a current playing audio
                        if (position == currentPlayingAudioIndex) {
                            if (deleteModel.songName == audioModel.songName) {
                                if (AllSongFragment.musicService?.mediaPlayer != null) {
                                    //audioIndex = audioList.indexOf(audioModel)
                                    storageUtil?.storeAudioIndex(position)
                                    AllSongFragment.musicService?.pausedByManually = true
                                    val broadcastIntent =
                                        Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
                                    (mContext as AppCompatActivity).sendBroadcast(broadcastIntent)
                                    audioModel = audioList[position]

                                    //audioModel = item
                                }
                            }
                        } else {
                            // getting current playing audio index after removing any audio
                            currentPlayingAudioIndex =
                                audioList.indexOf(audioModel)
                            // saving current audio pos if somehow its changed while deleting
                            storageUtil?.storeAudioIndex(currentPlayingAudioIndex)
                            Log.d(
                                "deletedAudio",
                                "onItemSwiped:$currentPlayingAudioIndex , model: $audioModel "
                            )
                            Toast.makeText(
                                mContext,
                                "$currentPlayingAudioIndex",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // queueLisAdapter.dataSet = audioList
                        // snackbar.show()

                        /*snackbar.setAction("Undo") {
                            // mViewModelClass.deleteQueue(lifecycleScope)

                            // queueLisAdapter.updateItem(item, position)
                            queueLisAdapter.insertItem(position, item)
                            audioList.add(position, item)
                            //save audio list
                            list.clear()
                            list.addAll(audioList)
                            storageUtil?.storeAudio(list)

                            //queueLisAdapter.dataSet = audioList

                            if (currentAudioDel) {
                                storageUtil?.storeAudioIndex(position)

                                mViewModelClass.updateSong(
                                    audioList[position + 1].songId,
                                    audioList[position + 1].songName,
                                    -1,
                                    (context as AppCompatActivity).lifecycleScope
                                )
                                mViewModelClass.updateQueueAudio(
                                    audioList[position + 1].songId,
                                    audioList[position + 1].songName,
                                    -1,
                                    (context as AppCompatActivity).lifecycleScope
                                )

                                AllSongFragment.musicService?.pausedByManually = true
                                val broadcastIntent =
                                    Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
                                (mContext as AppCompatActivity).sendBroadcast(broadcastIntent)
                                audioModel = item
                            } else {
                                // saving current audio pos if somehow its changed while deleting
                                audioIndex = audioList.indexOf(audioModel)
                                storageUtil?.storeAudioIndex(audioIndex)
                            }

                        }*/

                    }

                    else -> return false
                }
                return false
            }
        }

        binding?.rvUpNext?.swipeListener = onItemSwipeListener

        mViewModelClass.getQueueAudio().observe(viewLifecycleOwner, {
            if (it != null) {
                val loadAudio = storageUtil?.loadAudio()
                val list = CopyOnWriteArrayList<AllSongsModel>()
                Log.d("updateCurrentPlayingAudioList", "updateCurrentPlayingAudio: $loadAudio")
                for (nowPlayingAudios in loadAudio!!) {
                    for (audio in it) {
                        if (nowPlayingAudios.songId == audio.songId) {
                            // sorted list
                            val queueListModel = AllSongsModel(
                                audio.songId,
                                audio.albumId,
                                audio.songName,
                                audio.artistsName,
                                audio.albumName,
                                audio.size,
                                audio.duration,
                                audio.data,
                                audio.audioUri,
                                audio.artUri
                            )
                            queueListModel.playingOrPause = audio.isPlayingOrPause
                            list.add(queueListModel)
                        }
                    }
                }
                Log.d("updateCurrentPlayingAudioList1111", "updateCurrentPlayingAudio: $list")

                queueLisAdapter.dataSet = list
                audioList.clear()
                audioList.addAll(list)
                currentPlayingAudioIndex = storageUtil?.loadAudioIndex()!!

                updateCurrentPlayingAudio()

                // audioModel = audioList[audioIndex]
                // binding?.rvUpNext?.scrollToPosition(currentPlayingAudioIndex)
            }
        })

    }

    private fun updateCurrentPlayingAudio() {

        if (currentPlayingAudioIndex == -1) currentPlayingAudioIndex =
            0 else currentPlayingAudioIndex
        audioModel = audioList[currentPlayingAudioIndex]
        Log.d(
            "updateCurrentPlayingAudioIndex",
            "updateCurrentPlayingAudio: $currentPlayingAudioIndex , ${audioModel.songName}  , PlayOrPause ${audioModel.playingOrPause}"
        )
        binding?.songNameTV!!.text = audioModel.songName
        binding?.artistNameTV!!.text = audioModel.artistsName
        binding?.albumNameTv!!.text = audioModel.albumName

        Glide.with(mContext).load(audioModel.artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.audio_icon_placeholder))
            .into(binding?.albumArtIv!!)
    }

    private fun playAudio(audioIndex: Int) {
        //store audio to prefs
        //storageUtil!!.storeAudio(audioList)
        //Store the new audioIndex to SharedPreferences
        storageUtil!!.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        (activity as Context).sendBroadcast(updatePlayer)
    }

    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        currentPlayingAudioIndex = position

        val currentPlayingAudioIndex = storageUtil!!.loadAudioIndex()
        val loadAudioList = storageUtil!!.loadAudio()
        audioList.clear()
        audioList.addAll(loadAudioList)
        val prevPlayingAudioIndex = audioList[currentPlayingAudioIndex]

        mViewModelClass.updateQueueAudio(
            prevPlayingAudioIndex.songId,
            prevPlayingAudioIndex.songName,
            -1,
            (context as AppCompatActivity).lifecycleScope
        )

        mViewModelClass.updateQueueAudio(
            allSongModel.songId,
            allSongModel.songName,
            1,
            (context as AppCompatActivity).lifecycleScope
        )

        mViewModelClass.updateSong(
            prevPlayingAudioIndex.songId,
            prevPlayingAudioIndex.songName,
            -1,
            (context as AppCompatActivity).lifecycleScope
        )

        mViewModelClass.updateSong(
            allSongModel.songId,
            allSongModel.songName,
            1,
            (context as AppCompatActivity).lifecycleScope
        )


        saveAudioList(audioList)

        playAudio(position)
        updateCurrentPlayingAudio()
    }

    private fun saveAudioList(queueAudioList: ArrayList<AllSongsModel>) {
        // save audio list with current playing audio STATUS

        val list = CopyOnWriteArrayList<AllSongsModel>()
        for ((index, audio) in queueAudioList.withIndex()) {
            val allSongsModel = AllSongsModel(
                audio.songId,
                audio.albumId,
                audio.songName,
                audio.artistsName,
                audio.albumName,
                audio.size,
                audio.duration,
                audio.data,
                audio.audioUri,
                audio.artUri
            )
            if (index == this.currentPlayingAudioIndex) {
                // if current playing audio index matched
                if (AllSongFragment.musicService?.mediaPlayer != null) {
                    if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                        allSongsModel.playingOrPause = 1 /*playing*/
                        // Toast.makeText(mContext, "Playing", Toast.LENGTH_SHORT).show()
                    } else {
                        allSongsModel.playingOrPause = 0 /*pause*/
                        //Toast.makeText(mContext, "pause", Toast.LENGTH_SHORT).show()

                    }
                }

                list.add(allSongsModel)
            } else {
                list.add(allSongsModel)
            }

        }

        audioList.clear()
        audioList.addAll(list)
        storageUtil!!.storeAudio(list)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnRepeatAudioListener
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}
package com.knesarcreation.playbeat.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.QueueListAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.BottomSheetQueueListBinding
import com.knesarcreation.playbeat.utils.StorageUtil
import java.io.File
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
    private lateinit var currentPlayingAudioModel: AllSongsModel
    private lateinit var mViewModelClass: ViewModelClass
    private var isSwipedToDel = false
    private var buttonLayoutParams: ConstraintLayout.LayoutParams? = null
    private var expandedHeight = 0
    private var collapsedMargin = 0
    private var buttonHeight = 0

    interface OnRepeatAudioListener {
        fun onRepeatIconClicked()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener { dialogInterface -> setupRatio((dialogInterface as BottomSheetDialog)) }

        (dialog as BottomSheetDialog).behavior.addBottomSheetCallback(object :
            BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0) //Sliding happens from 0 (Collapsed) to 1 (Expanded) - if so, calculate margins
                    buttonLayoutParams!!.topMargin =
                        ((expandedHeight - buttonHeight - collapsedMargin) * slideOffset + collapsedMargin).toInt() else  //If not sliding above expanded, set initial margin
                    buttonLayoutParams!!.topMargin = collapsedMargin
                binding?.cancelQueueSheetBtn!!.layoutParams =
                    buttonLayoutParams //Set layout params to button (margin from top)
            }
        })


        return dialog

    }

    private fun setupRatio(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet =
            bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        //Retrieve button parameters
        buttonLayoutParams =
            binding?.cancelQueueSheetBtn!!.layoutParams as ConstraintLayout.LayoutParams

        //Retrieve bottom sheet parameters
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_COLLAPSED
        val bottomSheetLayoutParams = bottomSheet.layoutParams
        bottomSheetLayoutParams.height = getBottomSheetDialogDefaultHeight()
        expandedHeight = bottomSheetLayoutParams.height
        val peekHeight =
            (expandedHeight / 1.3).toInt() //Peek height to 70% of expanded height (Change based on your view)

        //Setup bottom sheet
        bottomSheet.layoutParams = bottomSheetLayoutParams
        BottomSheetBehavior.from(bottomSheet).skipCollapsed = false
        BottomSheetBehavior.from(bottomSheet).peekHeight = peekHeight
        BottomSheetBehavior.from(bottomSheet).isHideable = true

        //Calculate button margin from top
        buttonHeight =
            binding?.cancelQueueSheetBtn!!.height  //How tall is the button + experimental distance from bottom (Change based on your view)
        collapsedMargin = peekHeight - buttonHeight //Button margin in bottom sheet collapsed state
        buttonLayoutParams!!.topMargin = collapsedMargin
        binding?.cancelQueueSheetBtn!!.layoutParams = buttonLayoutParams

        //OPTIONAL - Setting up margins
        /* val recyclerLayoutParams =
             binding?.rvUpNext!!.layoutParams as ConstraintLayout.LayoutParams
         val k: Float =
             (buttonHeight) / buttonHeight.toFloat() //60 is amount that you want to be hidden behind button
         recyclerLayoutParams.bottomMargin =
             (k * buttonHeight).toInt() //Recyclerview bottom margin (from button)
         binding?.rvUpNext!!.layoutParams = recyclerLayoutParams*/
    }

    //Calculates height of fullscreen
    private fun getBottomSheetDialogDefaultHeight(): Int {
        return getWindowHeight()
    }

    //Calculates window height for fullscreen use
    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        (requireContext() as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
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
        val loadAudioList = storageUtil?.loadQueueAudio()!!
        audioList.addAll(loadAudioList)
        currentPlayingAudioIndex = storageUtil?.loadAudioIndex()!!

        setupUpNextAudioAdapter()
        updateCurrentPlayingAudio()
        manageRepeatAudio()

        binding?.rvUpNext?.disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.LEFT)

        binding?.cancelQueueSheetBtn?.setOnClickListener {
            dismiss()
        }

        binding?.rlAudio?.setOnClickListener {
            binding?.rvUpNext?.smoothScrollToPosition(currentPlayingAudioIndex)
        }

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
                Snackbar.make(
                    dialog!!.window!!.decorView,
                    "Repeat current audio", Snackbar.LENGTH_LONG
                ).show()
                binding?.repeatQueueIV?.setImageResource(R.drawable.repeat_one_on_24)
                storageUtil?.saveIsRepeatAudio(true)
                listener?.onRepeatIconClicked()
            } else {
                //repeat audio list
                Snackbar.make(
                    dialog!!.window!!.decorView,
                    "Repeat audio list", Snackbar.LENGTH_LONG
                ).show()
                binding?.repeatQueueIV?.setImageResource(R.drawable.ic_repeat_24)
                storageUtil?.saveIsRepeatAudio(false)
                listener?.onRepeatIconClicked()
            }
        }
    }

    private fun setupUpNextAudioAdapter() {
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
                        /* Log.d(
                             "QueueItemDeleted",
                             "onItemSwiped:Position: $position , ${currentPlayingAudioModel.playingOrPause} deleted ******* audioModel: $currentPlayingAudioModel "
                         )*/

                        audioList.clear()
                        audioList.addAll(storageUtil?.loadQueueAudio()!!)
                        currentPlayingAudioIndex =
                            audioList.indexOf(currentPlayingAudioModel) // getting current playing audio index from a arranged list
                        Log.d(
                            "saveAudioListQueue",
                            "saveAudioList: Name: ${audioList.size} $currentPlayingAudioIndex  $/*audioList[1].songName*/} , 2.${currentPlayingAudioModel.songName} ,  PlayingOrPause $/*audioList[1].playingOrPause*/} 2. ${currentPlayingAudioModel.playingOrPause}"
                        )

                        //if (position != audioList.size - 1) {
                        if (position == currentPlayingAudioIndex) {

                            mViewModelClass.updateSong(
                                audioList[position].songId,
                                audioList[position].songName,
                                -1,
                                (context as AppCompatActivity).lifecycleScope
                            )

                            /* Snackbar.make(
                                 (activity as AppCompatActivity).window.decorView,
                                 "updated: audio ${audioList[position].songName}", Snackbar.LENGTH_LONG
                             ).show()*/

                            if (currentPlayingAudioIndex == audioList.size - 1) {
                                // if current playing audio is last in a queue then after deleting audio update
                                // the queue audio - (position -1)
                                if (position != 0) {
                                    // if swiped position is not zero then only update queue audio
                                    mViewModelClass.updateQueueAudio(
                                        audioList[position - 1].songId,
                                        audioList[position - 1].songName,
                                        1,
                                        (context as AppCompatActivity).lifecycleScope
                                    )
                                }
                            } else {
                                mViewModelClass.updateQueueAudio(
                                    audioList[position].songId,
                                    audioList[position].songName,
                                    1,
                                    (context as AppCompatActivity).lifecycleScope
                                )
                            }

                        } else {
                            if (AllSongFragment.musicService?.mediaPlayer != null) {
                                if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                                    isSwipedToDel = true
                                    mViewModelClass.updateQueueAudio(
                                        currentPlayingAudioModel.songId,
                                        currentPlayingAudioModel.songName,
                                        1,
                                        (context as AppCompatActivity).lifecycleScope
                                    )
                                }
                            }
                        }
                        //}

                        val deleteModel = audioList[position]
                        audioList.removeAt(position)
                        mViewModelClass.deleteOneQueueAudio(deleteModel.songId, lifecycleScope)

                        // getting current playing audio index after removing any audio
                        if (currentPlayingAudioIndex != 0) {
                            Log.d(
                                "CurrentPlayingModel",
                                "onItemSwiped: pos: $position -model $deleteModel "
                            )
                            if (deleteModel.songId != currentPlayingAudioModel.songId) {
                                currentPlayingAudioIndex =
                                    audioList.indexOf(currentPlayingAudioModel)
                                storageUtil?.storeAudioIndex(currentPlayingAudioIndex)
                              /*  Toast.makeText(
                                    mContext,
                                    "del: $isSwipedToDel $currentPlayingAudioIndex",
                                    Toast.LENGTH_SHORT
                                ).show()*/
                            }
                        }

                        //save audio list
                        saveAudioList(audioList)

                        // if deleted audio is a current playing audio
                        if (position == currentPlayingAudioIndex) {
                            if (deleteModel.songName == currentPlayingAudioModel.songName) {
                                if (AllSongFragment.musicService?.mediaPlayer != null) {
                                    //audioIndex = audioList.indexOf(audioModel)
                                    if (currentPlayingAudioIndex == audioList.size /* there is no need to subtract size by 1 here, since one audio is already deleted */) {
                                        // last audio deleted which was playing
                                        // so play a prev audio, for that save a new index
                                        val newPos = position - 1
                                        storageUtil?.storeAudioIndex(newPos)
                                        Log.d("NewPosAfterDEl", "onItemSwiped: new $newPos")
                                       /* Toast.makeText(
                                            mContext,
                                            "size ${audioList.size}",
                                            Toast.LENGTH_SHORT
                                        ).show()*/
                                        if (audioList.size != 0)
                                            currentPlayingAudioModel = audioList[position - 1]
                                    } else {
                                        storageUtil?.storeAudioIndex(position)
                                        if (audioList.size != 0)
                                            currentPlayingAudioModel = audioList[position]
                                    }
                                    AllSongFragment.musicService?.pausedByManually = true
                                    val broadcastIntent =
                                        Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
                                    (mContext as AppCompatActivity).sendBroadcast(broadcastIntent)

                                    //audioModel = item
                                }
                            }
                        } else {
                            // getting current playing audio index after removing any audio
                            currentPlayingAudioIndex =
                                audioList.indexOf(currentPlayingAudioModel)
                            // saving current audio pos if somehow its changed while deleting
                            storageUtil?.storeAudioIndex(currentPlayingAudioIndex)
                            Log.d(
                                "deletedAudio",
                                "onItemSwiped:$currentPlayingAudioIndex , model: $currentPlayingAudioModel "
                            )
                           /* Toast.makeText(
                                mContext,
                                "$currentPlayingAudioIndex",
                                Toast.LENGTH_SHORT
                            ).show()*/
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
                val loadAudio = storageUtil?.loadQueueAudio()
                val list = CopyOnWriteArrayList<AllSongsModel>()
                Log.d("updateCurrentPlayingAudioList", "updateCurrentPlayingAudio: $it")
                for (nowPlayingAudios in loadAudio!!) {
                    for (audio in it.sortedBy { queueListModel -> queueListModel.songName }) {
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
                                audio.artUri,
                                audio.dateAdded,
                                audio.isFavourite,
                                audio.favAudioAddedTime,
                                audio.artistId,
                                "",
                                "",
                                0
                            )
                            queueListModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
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


                if (!isSwipedToDel) {
                    if (audioList.isNotEmpty())
                        updateCurrentPlayingAudio()
                } else {
                    isSwipedToDel = false
                }


                if (AllSongFragment.musicService?.mediaPlayer != null) {
                    if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                        binding?.currentPlayingAudioLottie?.playAnimation()
                    } else {
                        binding?.currentPlayingAudioLottie?.pauseAnimation()
                    }
                } else {
                    binding?.currentPlayingAudioLottie?.pauseAnimation()
                }
            }
        })

    }

    private fun updateCurrentPlayingAudio() {
        if (currentPlayingAudioIndex == -1) currentPlayingAudioIndex =
            0 else currentPlayingAudioIndex
        currentPlayingAudioModel = audioList[currentPlayingAudioIndex]
        Log.d(
            "updateCurrentPlayingAudioIndex",
            "updateCurrentPlayingAudio: $currentPlayingAudioIndex , ${currentPlayingAudioModel.songName}  , PlayOrPause ${currentPlayingAudioModel.playingOrPause}"
        )
        binding?.songNameTV!!.text = currentPlayingAudioModel.songName
        binding?.artistNameTV!!.text = currentPlayingAudioModel.artistsName
        binding?.albumNameTv!!.text = currentPlayingAudioModel.albumName

        Glide.with(mContext).load(currentPlayingAudioModel.artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.music_note_icon))
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

        /*val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }

    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        if (File(Uri.parse(allSongModel.data).path!!).exists()) {
            currentPlayingAudioIndex = position

            val currentPlayingAudioIndex = storageUtil!!.loadAudioIndex()
            val loadAudioList = storageUtil!!.loadQueueAudio()
            audioList.clear()
            audioList.addAll(loadAudioList)

            /*Toast.makeText(
                activity as Context,
                "onClick: $currentPlayingAudioIndex",
                Toast.LENGTH_SHORT
            ).show()*/
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
        } else {
            Snackbar.make(
                dialog!!.window!!.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
        }

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
                audio.contentUri,
                audio.artUri,
                audio.dateAdded,
                audio.isFavourite,
                audio.favAudioAddedTime,
                audio.artistId,
                audio.displayName,
                audio.contentType,
                audio.year
            )
            allSongsModel.currentPlayedAudioTime = audio.currentPlayedAudioTime

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
        storageUtil!!.storeQueueAudio(list)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnRepeatAudioListener
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }


    /* private fun initString(): List<String> {
         val list: MutableList<String> = ArrayList()
         for (i in 0..34) list.add("Item $i")
         return list
     }*/
}
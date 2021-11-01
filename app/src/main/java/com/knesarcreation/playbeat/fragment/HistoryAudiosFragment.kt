package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentPlayListAudiosBinding
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.StorageUtil
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class HistoryAudiosFragment : Fragment() {

    private var currentPlayingAudioIndex = 0
    private var _binding: FragmentPlayListAudiosBinding? = null
    private val binding get() = _binding
    private var historyAdapter: AllSongsAdapter? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var historyAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private lateinit var storage: StorageUtil
    private lateinit var viewModel: DataObservableClass
    private var selectedAudioIdList = ArrayList<Long>()
    private var selectedPositionList = ArrayList<Int>()
    private lateinit var textCountTV: TextView
    private var selectedAudioList = ArrayList<AllSongsModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
            duration = 200L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
            duration = 200L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentPlayListAudiosBinding.inflate(inflater, container, false)
        val view = binding?.root

        storage = StorageUtil(activity as Context)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        viewModel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.playlistCategory.observe(viewLifecycleOwner, {
            if (it != null) {
                when (it) {
                    "history" -> {
                        binding!!.rvFavSongs.visibility = View.GONE
                        binding!!.rvHistoryAdded.visibility = View.VISIBLE
                        binding!!.rvLastPlayedAudio.visibility = View.GONE
                        binding!!.rvMostPlayed.visibility = View.GONE
                        //binding!!.sortIV.visibility = View.GONE
                        binding!!.sortedTextTV.visibility = View.GONE
                        binding!!.rvCustomPlaylist.visibility = View.GONE
                        binding!!.titleNameTV.text = "History"
                        binding!!.artisNameTVToolbar.text = "History"
                    }
                }
            }
        })

        setUpHistoryRecyclerAdapter()
        observeHistoryAudio()

        binding?.arrowBackIV?.setOnClickListener {
            if (AllSongsAdapter.isContextMenuEnabled) {
                disableContextMenu()
            } else {
                (activity as AppCompatActivity).onBackPressed()
            }
        }

        binding?.arrowBack?.setOnClickListener {
            //if no audio present then this back btn will work
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.playBtn?.setOnClickListener {
            onClickAudio(historyAudioList[0], 0)
        }

        moreOptionMenu()

        binding?.closeContextMenu?.setOnClickListener {
            disableContextMenu()
        }

        binding?.selectAllAudios?.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                historyAdapter!!.selectAllAudios()
                for ((position, audio) in historyAudioList.withIndex()) {
                    if (!selectedAudioIdList.contains(audio.songId)) {
                        selectedAudioIdList.add(audio.songId)
                        selectedAudioList.add(audio)
                    }
                    if (!selectedPositionList.contains(position)) {
                        selectedPositionList.add(position)
                    }
                    textSwitcherIncrementTextAnim()
                }
                Log.d("selectAllAudiosSize", "onCreateView:${selectedAudioIdList.size} ")
            } else {
                historyAdapter!!.unSelectAllAudios()
                for ((position, audio) in historyAudioList.withIndex()) {
                    selectedAudioIdList.remove(audio.songId)
                    selectedPositionList.remove(position)
                    selectedAudioList.remove(audio)
                }
                binding?.totalSongsTV?.visibility = View.VISIBLE
                binding?.rlContextMenu?.visibility = View.INVISIBLE
                AllSongsAdapter.isContextMenuEnabled = false
                binding?.selectedAudiosTS!!.setText("0")
                viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
            }
        }

        setTextSwitcherFactory()

        viewModel.onBackPressed.observe(viewLifecycleOwner, {
            if (it != null) {
                disableContextMenu()
            }
        })

        shareAudios()

        return view
    }

    private fun shareAudios() {
        binding?.shareAudioIV?.setOnClickListener {
            val shareAlertdialog =
                AlertDialog.Builder(activity as Context, R.style.CustomAlertDialog)
            val viewGroup: ViewGroup =
                (activity as AppCompatActivity).findViewById(android.R.id.content)
            val customView =
                layoutInflater.inflate(R.layout.custom_alert_dialog, viewGroup, false)
            val dialogTitleTV = customView.findViewById<TextView>(R.id.dialogTitleTV)
            val dialogMessageTV =
                customView.findViewById<TextView>(R.id.dialogMessageTV)
            val cancelButton =
                customView.findViewById<MaterialButton>(R.id.cancelButton)
            val positiveBtn = customView.findViewById<MaterialButton>(R.id.positiveBtn)
            shareAlertdialog.setView(customView)

            positiveBtn.text = getString(R.string.share)
            dialogTitleTV.text = getString(R.string.share_audio_dialog_title)
            dialogMessageTV.text = getString(R.string.share_audio_dialog_msg)

            val dialog = shareAlertdialog.create()

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            positiveBtn.setOnClickListener {
                val uriList = ArrayList<Uri>()

                for (audio in historyAudioList) {
                    uriList.add(Uri.parse(audio.contentUri))
                }

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                    type = "audio/*"
                }
                startActivity(Intent.createChooser(shareIntent, null))
                dialog.dismiss()
            }

            dialog.show()

        }
    }

    private fun moreOptionMenu() {
        binding?.moreOptionIV?.setOnClickListener {
            val bottomSheetMultiSelectMoreOptions = BottomSheetMultiSelectMoreOptions(false)
            bottomSheetMultiSelectMoreOptions.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheetMultiSelectMoreOptions"
            )

            bottomSheetMultiSelectMoreOptions.listener =
                object : BottomSheetMultiSelectMoreOptions.MultiSelectAudioMenuOption {
                    override fun playNext() {
                        addToPlayNextAudiosToQueue()
                        bottomSheetMultiSelectMoreOptions.dismiss()
                        disableContextMenu()
                    }

                    override fun addToPlaylist() {
                        addMultipleAudiosToPlaylist()
                        bottomSheetMultiSelectMoreOptions.dismiss()
                    }

                    override fun addToPlayingQueue() {
                        addAudiosToPlayingQueue()
                        bottomSheetMultiSelectMoreOptions.dismiss()
                        disableContextMenu()
                    }

                    override fun deleteFromDevice() {
                        Snackbar.make(
                            (activity as AppCompatActivity).window.decorView,
                            "Sorry for inconvenience, feature is under development",
                            Snackbar.LENGTH_LONG
                        ).show()
                        /* Toast.makeText(
                             activity as Context,
                             "Sorry for inconvenience, feature is under development",
                             Toast.LENGTH_LONG
                         ).show()*/
                        bottomSheetMultiSelectMoreOptions.dismiss()
                    }
                }
        }
    }

    private fun addAudiosToPlayingQueue() {
        if (selectedAudioList.isNotEmpty()) {
            var playingQueueAudioList = CopyOnWriteArrayList<AllSongsModel>()
            try {
                playingQueueAudioList = storage.loadQueueAudio()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // mViewModelClass.deleteQueue(lifecycleScope)
            val newAudiosForQueue = CopyOnWriteArrayList<AllSongsModel>()
            for (audio in selectedAudioList) {
                if (audio.playingOrPause != 1 && audio.playingOrPause != 0) {
                    // selected audio is not playing then only add to play next
                    if (playingQueueAudioList.contains(audio)) {
                        playingQueueAudioList.remove(audio)
                        /*mViewModelClass.deleteOneQueueAudio(
                            audio.songId,
                            lifecycleScope
                        )*/
                    } else {
                        // this list is for adding audio into database
                        newAudiosForQueue.add(audio)
                    }

                    // adding to last index
                    playingQueueAudioList.add(audio)

                }
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

            if (newAudiosForQueue.isNotEmpty()) {
                for (audio in newAudiosForQueue) {
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
                    mViewModelClass.insertQueue(queueListModel, lifecycleScope)
                }

            }

            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "Added ${selectedAudioList.size} songs to playing queue", Snackbar.LENGTH_LONG
            ).show()

        }
    }

    private fun addToPlayNextAudiosToQueue() {
        if (selectedAudioList.isNotEmpty()) {
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

            //mViewModelClass.deleteQueue(lifecycleScope)
            val newAudiosForQueue = CopyOnWriteArrayList<AllSongsModel>()
            for (audio in selectedAudioList) {
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
                             lifecycleScope
                         )*/
                    } else {
                        // this list is for adding audio into database
                        newAudiosForQueue.add(audio)
                        Log.d(
                            "PlalistAudioTesting",
                            "playNext: Index: $audioIndex , $audio , playingOrPause: ${audio.playingOrPause} "
                        )
                    }
                    // adding next to playing index
                    playingQueueAudioList.add(audioIndex, audio)

                }
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

            if (newAudiosForQueue.isNotEmpty()) {
                //insert into database
                for (audio in newAudiosForQueue) {
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
                    mViewModelClass.insertQueue(queueListModel, lifecycleScope)
                }

            }
            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "Added ${selectedAudioList.size} songs to playing queue", Snackbar.LENGTH_LONG
            ).show()

        }
    }

    private fun disableContextMenu() {
        binding?.totalSongsTV?.visibility = View.VISIBLE
        binding?.rlContextMenu?.visibility = View.INVISIBLE
        AllSongsAdapter.isContextMenuEnabled = false
        binding?.selectAllAudios?.isChecked = false
        historyAdapter!!.updateChanges(selectedPositionList)
        selectedPositionList.clear()
        selectedAudioIdList.clear()
        selectedAudioList.clear()
        binding?.selectedAudiosTS!!.setText("0")
        viewModel.isContextMenuEnabled.value = false
    }

    private fun addMultipleAudiosToPlaylist() {
        val bottomSheetChooseToPlaylist =
            BottomSheetChoosePlaylist(null, false, selectedAudioIdList)
        bottomSheetChooseToPlaylist.show(
            (activity as AppCompatActivity).supportFragmentManager,
            "bottomSheetChooseToPlaylist"
        )
        bottomSheetChooseToPlaylist.listener =
            object : BottomSheetChoosePlaylist.PlaylistSelected {
                override fun onSelected() {
                    binding?.totalSongsTV?.visibility = View.VISIBLE
                    binding?.rlContextMenu?.visibility = View.INVISIBLE
                    AllSongsAdapter.isContextMenuEnabled = false
                    binding?.selectAllAudios?.isChecked = false
                    historyAdapter!!.updateChanges(selectedPositionList)
                    selectedPositionList.clear()
                    selectedAudioIdList.clear()
                    binding?.selectedAudiosTS!!.setText("0")
                    viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
                }
            }

    }

    private fun observeHistoryAudio() {
        mViewModelClass.getPrevPlayedAudios().observe(viewLifecycleOwner) {
            if (it != null) {
                historyAudioList.clear()
                /*val tempList = CopyOnWriteArrayList<AllSongsModel>()
                for (audioData in it) {
                    if (audioData.currentPlayedAudioTime != 0L) {
                        tempList.add(audioData)
                    }
                }*/

                historyAudioList.addAll(it.sortedByDescending { allSongsModel -> allSongsModel.currentPlayedAudioTime })
                if (AllSongsAdapter.isContextMenuEnabled) {
                    historyAdapter!!.submitList(getCheckedAudioList(historyAudioList).toMutableList())
                } else {
                    historyAdapter!!.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.currentPlayedAudioTime })
                }

                // binding?.rvHistoryAdded?.scrollToPosition(0)

                if (it.size >= 2) {
                    binding?.totalSongsTV?.text = "${historyAudioList.size} Songs"
                } else {
                    binding?.totalSongsTV?.text = "${historyAudioList.size} Song"
                }

                if (historyAudioList.isNotEmpty()) {
                    val factory =
                        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                    binding?.rlNoSongsPresent?.visibility = View.GONE
                    binding?.motionLayoutPlayListAudios?.visibility = View.VISIBLE
                    binding?.noSongDescription?.visibility = View.GONE
                    Glide.with(binding?.coverArtistImage!!).load(historyAudioList[0].artUri).apply(
                        RequestOptions.placeholderOf(R.drawable.music_note_icon).centerCrop()
                    ).transition(DrawableTransitionOptions.withCrossFade(factory))
                        .into(binding?.coverArtistImage!!)
                } else {
                    // no audio present
                    binding?.rlNoSongsPresent?.visibility = View.VISIBLE
                    binding?.motionLayoutPlayListAudios?.visibility = View.GONE
                    binding?.noSongDescription?.visibility = View.GONE
                    binding?.image?.setImageResource(R.drawable.music_note_icon)
                }
            }
        }
    }

    private fun setUpHistoryRecyclerAdapter() {
        historyAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position)
                }, AllSongsAdapter.OnLongClickListener { allSongModel, position ->
                    if (allSongModel.isChecked) {
                        binding?.sortedTextTV?.visibility = View.INVISIBLE
                        binding?.totalSongsTV?.visibility = View.INVISIBLE
                        binding?.rlContextMenu?.visibility = View.VISIBLE
                        selectedAudioIdList.add(allSongModel.songId)
                        selectedPositionList.add(position)
                        selectedAudioList.add(allSongModel)

                        textSwitcherIncrementTextAnim()

                    } else {
                        selectedAudioIdList.remove(allSongModel.songId)
                        //textToShowSelectedCount.remove("${selectedPositionList.size} Selected")
                        selectedPositionList.remove(position)
                        selectedAudioList.remove(allSongModel)

                        textSwitcherDecrementTextAnim()

                        if (selectedAudioIdList.isEmpty()) {
                            binding?.totalSongsTV?.visibility = View.VISIBLE
                            binding?.rlContextMenu?.visibility = View.INVISIBLE
                            AllSongsAdapter.isContextMenuEnabled = false
                        }
                    }
                    viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled

                }, false
            )
        historyAdapter!!.isSearching = false
        binding?.rvHistoryAdded?.adapter = historyAdapter
        //binding!!.rvHistoryAdded.itemAnimator = null
    }

    private fun onClickAudio(
        allSongModel: AllSongsModel,
        position: Int,
    ) {
        if (File(Uri.parse(allSongModel.data).path!!).exists()) {
            storage.saveIsShuffled(false)
            val prevPlayingAudioIndex = storage.loadAudioIndex()
            val prevQueueList = storage.loadQueueAudio()
            var prevPlayingAudioModel: AllSongsModel? = null
            //var restrictToUpdateAudio = false

            mViewModelClass.deleteQueue(lifecycleScope)

            if (prevQueueList.isNotEmpty()) {
                prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]
                Log.d(
                    "PlayListAudios111s",
                    "onClickAudio: allSongModel $allSongModel ,  historyAudioList $historyAudioList "
                )

                mViewModelClass.updateSong(
                    prevPlayingAudioModel.songId,
                    prevPlayingAudioModel.songName,
                    -1,
                    (context as AppCompatActivity).lifecycleScope
                )

                mViewModelClass.updateSong(
                    allSongModel.songId,
                    allSongModel.songName,
                    1,
                    (context as AppCompatActivity).lifecycleScope
                )
            }

            playAudio(historyAudioList.indexOf(allSongModel))

            // adding queue list to DB and show highlight of current audio
            for (audio in this.historyAudioList) {
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
                mViewModelClass.insertQueue(queueListModel, lifecycleScope)
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
        }else{
            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
        }

    }

    private fun playAudio(audioIndex: Int) {
        this.currentPlayingAudioIndex = audioIndex
        //store audio to prefs

        storage.storeQueueAudio(historyAudioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }

    private fun animateRecyclerView() {
        binding?.rvHistoryAdded!!.animate()
            .translationY(-10f)
            //translationYBy(30f)
            .alpha(1.0f)
            .setListener(null)
    }


    private fun setTextSwitcherFactory() {
        binding?.selectedAudiosTS!!.setFactory {
            textCountTV = TextView(activity as Context)
            textCountTV.setTextColor(Color.WHITE)
            textCountTV.textSize = 20f
            textCountTV.gravity = Gravity.CENTER_HORIZONTAL
            return@setFactory textCountTV
        }
    }

    private fun textSwitcherDecrementTextAnim() {
        binding?.selectedAudiosTS!!.setInAnimation(
            activity as Context,
            R.anim.slide_up
        )
        binding?.selectedAudiosTS!!.setOutAnimation(
            activity as Context,
            R.anim.slide_down
        )
        if (selectedAudioIdList.isNotEmpty()) {
            binding?.selectedAudiosTS!!.setText("${selectedPositionList.size}")
        }
    }

    private fun textSwitcherIncrementTextAnim() {
        // show selected audio count
        binding?.selectedAudiosTS!!.setOutAnimation(
            activity as Context,
            R.anim.slide_up
        )
        binding?.selectedAudiosTS!!.setInAnimation(
            activity as Context,
            R.anim.slide_down
        )

        binding?.selectedAudiosTS!!.setText("${selectedPositionList.size}")
    }

    private fun getCheckedAudioList(sortedList: List<AllSongsModel>): ArrayList<AllSongsModel> {
        val audioList = ArrayList<AllSongsModel>()
        for (audio in sortedList) {
            for (audioId in selectedAudioIdList) {
                if (audio.songId == audioId) {
                    audio.isChecked = true
                }
            }
            audioList.add(audio)
        }
        return audioList
    }

}
package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentPlayListAudiosBinding
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class MostPlayedFragment : Fragment() {

    private var currentPlayingAudioIndex = 0
    private var _binding: FragmentPlayListAudiosBinding? = null
    private val binding get() = _binding
    private var mostPlayedAudioAdapter: AllSongsAdapter? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var mostPlayedAudioList = CopyOnWriteArrayList<AllSongsModel>()
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
                    "mostPlayed" -> {
                        binding!!.rvFavSongs.visibility = View.GONE
                        binding!!.rvHistoryAdded.visibility = View.GONE
                        binding!!.rvLastPlayedAudio.visibility = View.GONE
                        binding!!.rvMostPlayed.visibility = View.VISIBLE
                        //binding!!.sortIV.visibility = View.GONE
                        binding!!.sortedTextTV.visibility = View.GONE
                        binding!!.rvCustomPlaylist.visibility = View.GONE
                        binding!!.titleNameTV.text = "Most played"
                        binding!!.artisNameTVToolbar.text = "Most played"
                    }
                }
            }
        })

        setUpMostPlayedAudioRecyclerAdapter()
        observeMostPlayedAudio()

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
            onClickAudio(mostPlayedAudioList[0], 0)
        }

        moreOptionMenu()

        binding?.closeContextMenu?.setOnClickListener {
            disableContextMenu()
        }

        binding?.selectAllAudios?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mostPlayedAudioAdapter!!.selectAllAudios()
                for ((position, audio) in mostPlayedAudioList.withIndex()) {
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
                mostPlayedAudioAdapter!!.unSelectAllAudios()
                for ((position, audio) in mostPlayedAudioList.withIndex()) {
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

        return view
    }

    private fun disableContextMenu() {
        binding?.totalSongsTV?.visibility = View.VISIBLE
        binding?.rlContextMenu?.visibility = View.INVISIBLE
        AllSongsAdapter.isContextMenuEnabled = false
        binding?.selectAllAudios?.isChecked = false
        mostPlayedAudioAdapter!!.updateChanges(selectedPositionList)
        selectedPositionList.clear()
        selectedAudioIdList.clear()
        selectedAudioList.clear()
        binding?.selectedAudiosTS!!.setText("0")
        viewModel.isContextMenuEnabled.value = false
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
                        Toast.makeText(
                            activity as Context,
                            "Sorry for inconvenience, feature is under development",
                            Toast.LENGTH_LONG
                        ).show()
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

            mViewModelClass.deleteQueue(lifecycleScope)
            //val newAudiosForQueue = CopyOnWriteArrayList<AllSongsModel>()
            for (audio in selectedAudioList) {
                if (audio.playingOrPause != 1) {
                    // selected audio is not playing then only add to play next
                    if (playingQueueAudioList.contains(audio)) {
                        playingQueueAudioList.remove(audio)
                        /* mViewModelClass.deleteOneQueueAudio(
                             audio.songId,
                             lifecycleScope
                         )*/
                    }

                    // adding to last index
                    playingQueueAudioList.add(audio)
                    // this list is for adding audio into database
                    //newAudiosForQueue.add(audio)
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
                    mViewModelClass.insertQueue(queueListModel, lifecycleScope)
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
                activity as Context,
                "Added ${selectedAudioList.size} songs to playing queue",
                Toast.LENGTH_SHORT
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

            mViewModelClass.deleteQueue(lifecycleScope)
            // val newAudiosForQueue = CopyOnWriteArrayList<AllSongsModel>()
            for (audio in selectedAudioList) {
                if (audio.playingOrPause != 1 && audio.playingOrPause != 0) {
                    // selected audio is not playing then only add to play next
                    audioIndex++
                    if (playingQueueAudioList.contains(audio)) {
                        if (playingQueueAudioList.indexOf(audio) < audioIndex) {
                            audioIndex--
                        }
                        playingQueueAudioList.remove(audio)
                        /*mViewModelClass.deleteOneQueueAudio(
                            audio.songId,
                            lifecycleScope
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
                    mViewModelClass.insertQueue(queueListModel, lifecycleScope)
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
                activity as Context,
                "Added ${selectedAudioList.size} songs to playing queue",
                Toast.LENGTH_SHORT
            )
                .show()
        }
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
                    mostPlayedAudioAdapter!!.updateChanges(selectedPositionList)
                    selectedPositionList.clear()
                    selectedAudioIdList.clear()
                    binding?.selectedAudiosTS!!.setText("0")
                    viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
                }
            }

    }

    private fun setUpMostPlayedAudioRecyclerAdapter() {
        mostPlayedAudioAdapter =
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
        mostPlayedAudioAdapter!!.isSearching = false
        binding?.rvMostPlayed?.adapter = mostPlayedAudioAdapter
    }

    private fun observeMostPlayedAudio() {
        mViewModelClass.getMostPlayedAudio().observe(viewLifecycleOwner) {
            if (it != null) {
                mostPlayedAudioList.clear()

                mostPlayedAudioList.addAll(it.sortedByDescending { allSongsModel -> allSongsModel.mostPlayedCount })
                mostPlayedAudioAdapter!!.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.mostPlayedCount })
                binding?.rvMostPlayed?.scrollToPosition(0)

                if (it.size >= 2) {
                    binding?.totalSongsTV?.text = "${mostPlayedAudioList.size} Songs"
                } else {
                    binding?.totalSongsTV?.text = "${mostPlayedAudioList.size} Song"
                }

                if (mostPlayedAudioList.isNotEmpty()) {
                    val factory =
                        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                    binding?.rlNoSongsPresent?.visibility = View.GONE
                    binding?.motionLayoutPlayListAudios?.visibility = View.VISIBLE
                    binding?.noSongDescription?.visibility = View.GONE
                    Glide.with(binding?.coverArtistImage!!).load(mostPlayedAudioList[0].artUri)
                        .transition(withCrossFade(factory))
                        .apply(
                            RequestOptions.placeholderOf(R.drawable.music_note_icon)
                                .centerCrop()
                        )
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


    private fun onClickAudio(
        allSongModel: AllSongsModel,
        position: Int,
    ) {
        storage.saveIsShuffled(false)
        val prevPlayingAudioIndex = storage.loadAudioIndex()
        val prevQueueList = storage.loadQueueAudio()
        var prevPlayingAudioModel: AllSongsModel? = null

        mViewModelClass.deleteQueue(lifecycleScope)

        if (prevQueueList.isNotEmpty()) {
            prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]

            Log.d(
                "PlayListAudios111s",
                "onClickAudio: allSongModel $allSongModel ,  mostPlayedAudioList $mostPlayedAudioList "
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

        playAudio(mostPlayedAudioList.indexOf(allSongModel))

        // adding queue list to DB and show highlight of current audio
        for (audio in this.mostPlayedAudioList) {
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
    }

    private fun playAudio(audioIndex: Int) {
        this.currentPlayingAudioIndex = audioIndex
        //store audio to prefs

        storage.storeQueueAudio(mostPlayedAudioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

    }

    private fun animateRecyclerView() {
        binding?.rvMostPlayed!!.animate()
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
}
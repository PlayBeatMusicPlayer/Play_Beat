package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentPlayListAudiosBinding
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.SavedAppTheme
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class CustomPlaylist : Fragment() {
    private var currentPlayingAudioIndex = 0
    private var _binding: FragmentPlayListAudiosBinding? = null
    private val binding get() = _binding
    private var customPlaylistAdapter: AllSongsAdapter? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var customPlaylist = CopyOnWriteArrayList<AllSongsModel>()
    private lateinit var storage: StorageUtil
    private lateinit var viewModel: DataObservableClass
    private var launchJob: Job? = null
    private var playlistModel: PlaylistModel? = null
    private var newAudioArtIndex = 0
    private lateinit var newAudioAddedModel: AllSongsModel
    private val tempAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private var selectedAudioIdList = ArrayList<Long>()
    private var selectedPositionList = ArrayList<Int>()
    private lateinit var textCountTV: TextView
    private var isFragHidden = false
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


        viewModel.customPlaylistData.observe(viewLifecycleOwner, {
            if (it != null) {

                binding!!.rvFavSongs.visibility = View.GONE
                binding!!.rvHistoryAdded.visibility = View.GONE
                binding!!.rvLastPlayedAudio.visibility = View.GONE
                binding!!.rvMostPlayed.visibility = View.GONE
                //binding!!.sortIV.visibility = View.GONE
                binding!!.sortedTextTV.visibility = View.VISIBLE
                binding!!.rvCustomPlaylist.visibility = View.VISIBLE

                playlistModel = convertStringTpPlaylistModel(it)
                binding!!.titleNameTV.text = "${playlistModel!!.playlistName}"
                binding!!.artisNameTVToolbar.text = "${playlistModel!!.playlistName}"

                setUpCustomPlaylistRecyclerAdapter()
                observerCustomPlaylistAudio()

                //getAudioArt(tempAudioList)

            }
        })

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
            onClickAudio(customPlaylist[0], 0)
        }

        binding?.sortedTextTV?.setOnClickListener {
            sortAudios()
        }


        moreOptionMenu()

        binding?.closeContextMenu?.setOnClickListener {
            disableContextMenu()
        }

        binding?.selectAllAudios?.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                customPlaylistAdapter!!.selectAllAudios()
                for ((position, audio) in customPlaylist.withIndex()) {
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
                customPlaylistAdapter!!.unSelectAllAudios()
                for ((position, audio) in customPlaylist.withIndex()) {
                    selectedAudioIdList.remove(audio.songId)
                    selectedPositionList.remove(position)
                    selectedAudioList.remove(audio)
                }
                binding?.sortedTextTV?.visibility = View.VISIBLE
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
                if (!isFragHidden)
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

                for (audio in customPlaylist) {
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
            val bottomSheetMultiSelectMoreOptions = BottomSheetMultiSelectMoreOptions(true)
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
                        // here this method will act as "deleteFromPlaylist"
                        deleteAudioFromPlaylist()
                        disableContextMenu()
                        bottomSheetMultiSelectMoreOptions.dismiss()
                    }
                }
        }
    }

    private fun disableContextMenu() {
        binding?.sortedTextTV?.visibility = View.VISIBLE
        binding?.totalSongsTV?.visibility = View.VISIBLE
        binding?.rlContextMenu?.visibility = View.INVISIBLE
        AllSongsAdapter.isContextMenuEnabled = false
        binding?.selectAllAudios?.isChecked = false
        customPlaylistAdapter!!.updateChanges(selectedPositionList)
        selectedPositionList.clear()
        selectedAudioIdList.clear()
        selectedAudioList.clear()
        binding?.selectedAudiosTS!!.setText("0")
        viewModel.isContextMenuEnabled.value = false
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
                        newAudiosForQueue.add(audio)
                        Log.d(
                            "PlalistAudioTesting",
                            "playNext: Index: $audioIndex , $audio , playingOrPause: ${audio.playingOrPause} "
                        )
                    }
                    // adding next to playing index
                    playingQueueAudioList.add(audioIndex, audio)
                    // this list is for adding audio into database

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
                    binding?.sortedTextTV?.visibility = View.VISIBLE
                    binding?.totalSongsTV?.visibility = View.VISIBLE
                    binding?.rlContextMenu?.visibility = View.INVISIBLE
                    AllSongsAdapter.isContextMenuEnabled = false
                    binding?.selectAllAudios?.isChecked = false
                    customPlaylistAdapter!!.updateChanges(selectedPositionList)
                    selectedPositionList.clear()
                    selectedAudioIdList.clear()
                    binding?.selectedAudiosTS!!.setText("0")
                    viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
                }
            }
    }

    private fun sortAudios() {
        val bottomSheetSortByOptions =
            BottomSheetSortBy(activity as Context, "playlistAudios", playlistModel?.playlistName!!)
        bottomSheetSortByOptions.show(
            (context as AppCompatActivity).supportFragmentManager,
            "bottomSheetSortByOptions"
        )

        bottomSheetSortByOptions.listener = object : BottomSheetSortBy.OnSortingAudio {
            override fun byName() {
                storage.saveAudioSortingMethod(playlistModel?.playlistName!!, "Name")
                val sortedBySongName =
                    tempAudioList.sortedBy { allSongsModel -> allSongsModel.songName }

                setUpCustomPlaylistRecyclerAdapter()
                binding?.rvCustomPlaylist?.alpha = 0.0f
                customPlaylistAdapter!!.submitList(sortedBySongName)
                customPlaylist.clear()
                customPlaylist.addAll(sortedBySongName)
                animateRecyclerView()
                bottomSheetSortByOptions.dismiss()
                binding?.sortedTextTV?.text = "Name"
            }

            override fun byArtistName() {
                storage.saveAudioSortingMethod(playlistModel?.playlistName!!, "ArtistName")
                val sortedByArtistName =
                    tempAudioList.sortedBy { allSongsModel -> allSongsModel.artistsName }
                setUpCustomPlaylistRecyclerAdapter()
                binding?.rvCustomPlaylist?.alpha = 0.0f
                customPlaylistAdapter!!.submitList(sortedByArtistName)
                customPlaylist.clear()
                customPlaylist.addAll(sortedByArtistName)
                animateRecyclerView()
                binding?.sortedTextTV?.text = "Artist Name"
                bottomSheetSortByOptions.dismiss()
            }

            override fun byDate() {
                storage.saveAudioSortingMethod(playlistModel?.playlistName!!, "DateAdded")
                val sortedByDateAdded =
                    tempAudioList.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }

                setUpCustomPlaylistRecyclerAdapter()
                binding?.rvCustomPlaylist?.alpha = 0.0f
                customPlaylistAdapter!!.submitList(sortedByDateAdded)
                customPlaylist.clear()
                customPlaylist.addAll(sortedByDateAdded)
                animateRecyclerView()
                bottomSheetSortByOptions.dismiss()
                binding?.sortedTextTV?.text = "Date Added"
            }
        }
    }

    private fun convertStringTpPlaylistModel(playlist: String): PlaylistModel {
        Log.d("playlist11111", "convertStringTpPlaylistModel: $playlist")
        val gson = Gson()
        val type = object : TypeToken<PlaylistModel>() {}.type
        return gson.fromJson(playlist, type)
    }

    private fun setUpCustomPlaylistRecyclerAdapter() {
        customPlaylistAdapter =
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
                            binding?.sortedTextTV?.visibility = View.VISIBLE
                            binding?.totalSongsTV?.visibility = View.VISIBLE
                            binding?.rlContextMenu?.visibility = View.INVISIBLE
                            AllSongsAdapter.isContextMenuEnabled = false
                        }
                    }
                    viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
                }, true
            )
        customPlaylistAdapter!!.isSearching = false
        binding?.rvCustomPlaylist?.adapter = customPlaylistAdapter
        // binding!!.rvCustomPlaylist.itemAnimator = null

    }

    private fun observerCustomPlaylistAudio() {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner) { _ ->
            if (launchJob != null && launchJob?.isActive!!) {
                launchJob?.cancel()
            }

            launchJob = lifecycleScope.launch(Dispatchers.IO) {
                val playlistAudios: List<PlaylistModel> =
                    mViewModelClass.getPlaylistAudios(playlistModel?.id!!)
                if (playlistAudios.isNotEmpty()) {
                    playlistModel = playlistAudios[0]
                    val songIdsListString: String = playlistModel!!.songIds
                    if (songIdsListString != "") {

                        val songIdsList = convertStringToList(songIdsListString)

                        val audio = mViewModelClass.getRangeOfPlaylistAudio(songIdsList)

                        if (audio.isNotEmpty()) {
                            customPlaylist.clear()
                            tempAudioList.clear()
                            tempAudioList.addAll(audio.sortedBy { allSongsModel ->
                                allSongsModel.songName
                            })

                            (activity as AppCompatActivity).runOnUiThread {
                                //Toast.makeText(activity as Context, "$audio", Toast.LENGTH_SHORT).show()
                                val sortedList: List<AllSongsModel>
                                when (storage.getAudioSortedValue(playlistModel?.playlistName!!)) {
                                    "Name" -> {
                                        sortedList =
                                            audio.sortedBy { allSongsModel -> allSongsModel.songName }
                                        customPlaylist.addAll(sortedList)

                                        if (AllSongsAdapter.isContextMenuEnabled) {
                                            customPlaylistAdapter!!.submitList(
                                                getCheckedAudioList(
                                                    sortedList
                                                ).toMutableList()
                                            )
                                        } else {
                                            customPlaylistAdapter!!.submitList(audio.sortedBy { allSongsModel -> allSongsModel.songName }
                                                .toMutableList())
                                        }

                                        binding?.sortedTextTV?.text = "Name"
                                    }
                                    "DateAdded" -> {
                                        sortedList =
                                            audio.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
                                        customPlaylist.addAll(sortedList)
                                        if (AllSongsAdapter.isContextMenuEnabled) {
                                            customPlaylistAdapter!!.submitList(
                                                getCheckedAudioList(
                                                    sortedList
                                                ).toMutableList()
                                            )
                                        } else {
                                            customPlaylistAdapter!!.submitList(audio.sortedByDescending { allSongsModel -> allSongsModel.dateAdded })
                                        }
                                        binding?.sortedTextTV?.text = "Date Added"
                                    }

                                    "ArtistName" -> {
                                        sortedList =
                                            audio.sortedBy { allSongsModel -> allSongsModel.artistsName }
                                        customPlaylist.addAll(sortedList)
                                        if (AllSongsAdapter.isContextMenuEnabled) {
                                            customPlaylistAdapter!!.submitList(
                                                getCheckedAudioList(
                                                    sortedList
                                                ).toMutableList()
                                            )
                                        } else {
                                            customPlaylistAdapter!!.submitList(audio.sortedBy { allSongsModel -> allSongsModel.artistsName })
                                        }
                                        binding?.sortedTextTV?.text = "Artist Name"
                                    }
                                    else -> {
                                        sortedList =
                                            audio.sortedBy { allSongsModel -> allSongsModel.songName }
                                        customPlaylist.addAll(sortedList)
                                        if (AllSongsAdapter.isContextMenuEnabled) {
                                            customPlaylistAdapter!!.submitList(
                                                getCheckedAudioList(
                                                    sortedList
                                                ).toMutableList()
                                            )
                                        } else {
                                            customPlaylistAdapter!!.submitList(audio.sortedBy { allSongsModel -> allSongsModel.songName })
                                        }
                                        binding?.sortedTextTV?.text = "Name"
                                        Log.d(
                                            "sortedListObserved",
                                            "observeAudioData:$sortedList "
                                        )
                                    }
                                }

                                if (audio.size >= 2) {
                                    binding?.totalSongsTV?.text = "${audio.size} Songs"
                                } else {
                                    binding?.totalSongsTV?.text = "${audio.size} Song"
                                }

                                if (customPlaylist.isNotEmpty()) {
                                    binding?.rlNoSongsPresent?.visibility = View.GONE
                                    binding?.motionLayoutPlayListAudios?.visibility =
                                        View.VISIBLE
                                    binding?.noSongDescription?.visibility = View.GONE
                                    val factory =
                                        DrawableCrossFadeFactory.Builder()
                                            .setCrossFadeEnabled(true)
                                            .build()
                                    Glide.with(binding?.coverArtistImage!!)
                                        .load(sortedList[0].artUri)
                                        .transition(DrawableTransitionOptions.withCrossFade(factory))
                                        .centerCrop()
                                        .apply(RequestOptions.placeholderOf(R.drawable.music_note_icon))
                                        .into(binding?.coverArtistImage!!)
                                }
                            }
                        }


                    } else {
                        (activity as AppCompatActivity).runOnUiThread {
                            // no audio present
                            binding?.rlNoSongsPresent?.visibility = View.VISIBLE
                            binding?.motionLayoutPlayListAudios?.visibility = View.GONE
                            binding?.noSongDescription?.visibility = View.GONE
                            binding?.image?.setImageResource(R.drawable.music_note_icon)
                        }
                    }
                }
            }
        }
    }

    private fun convertStringToList(songIdList: String): ArrayList<Long> {
        val gson = Gson()
        val type = object : TypeToken<ArrayList<Long>>() {}.type
        return gson.fromJson(songIdList, type)
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

            if (prevQueueList.isNotEmpty()) {
                prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]
                var restrictToUpdateAudio = allSongModel.songId == prevPlayingAudioModel.songId

                if (storage.getIsAudioPlayedFirstTime()) {
                    restrictToUpdateAudio = false
                }

                //update prev audio to not playing
                mViewModelClass.updateSong(
                    prevPlayingAudioModel!!.songId,
                    prevPlayingAudioModel.songName,
                    -1, //not playing
                    lifecycleScope
                )
            }

            Log.d(
                "customPlaylist111111",
                "onClickAudio: allSongModel  ,  favAudioList $customPlaylist "
            )
            // restricting to update if clicked audio is same
            /*  if (!restrictToUpdateAudio) {*/
            mViewModelClass.deleteQueue(lifecycleScope)

            mViewModelClass.updateSong(
                allSongModel.songId,
                allSongModel.songName,
                1,
                lifecycleScope
            )
            // }

            //Toast.makeText(activity as Context, "$position", Toast.LENGTH_SHORT).show()
            playAudio(position)


            // restricting to update if clicked audio is same
            // if (!restrictToUpdateAudio) {
            // adding queue list to DB and show highlight of current audio
            if (customPlaylist.isNotEmpty()) {
                for (audio in this.customPlaylist) {
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
            }

            if (prevQueueList.isNotEmpty()) {
                mViewModelClass.updateQueueAudio(
                    prevPlayingAudioModel!!.songId,
                    prevPlayingAudioModel.songName,
                    -1,
                    lifecycleScope
                )
            }

            mViewModelClass.updateQueueAudio(
                allSongModel.songId,
                allSongModel.songName,
                1,
                lifecycleScope
            )
            //}
        } else {
            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun playAudio(audioIndex: Int) {
        this.currentPlayingAudioIndex = audioIndex
        //store audio to prefs

        if (customPlaylist.isNotEmpty()) {
            storage.storeQueueAudio(customPlaylist)
        }
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }

    private fun animateRecyclerView() {
        binding?.rvCustomPlaylist!!.animate()
            .translationY(-10f)
            //translationYBy(30f)
            .alpha(1.0f)
            .setListener(null)
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

    private fun setTextSwitcherFactory() {
        binding?.selectedAudiosTS!!.setFactory {
            textCountTV = TextView(activity as Context)
            textCountTV.setTextColor(Color.WHITE)
            textCountTV.textSize = 20f
            textCountTV.gravity = Gravity.CENTER_HORIZONTAL
            return@setFactory textCountTV
        }
    }

    private fun deleteAudioFromPlaylist() {
        if (playlistModel != null) {
            // Toast.makeText(context, "$position", Toast.LENGTH_SHORT).show()
            val songIdsListString = playlistModel!!.songIds
            val convertStringToList = convertStringToList(songIdsListString)
            val songIdsList = CopyOnWriteArrayList<Long>()
            songIdsList.addAll(convertStringToList)

            val newSongIdsList = ArrayList<Long>()
            newSongIdsList.addAll(songIdsList)
            for (songId in songIdsList) {
                for (selectedId in selectedAudioIdList) {
                    if (songId == selectedId) {
                        newSongIdsList.remove(songId)
                    }
                }
            }

            if (newSongIdsList.isNotEmpty()) {
                //val convertListToString = convertListToString(songIdsList)
                val gson = Gson()
                val convertListToString = gson.toJson(newSongIdsList)
                mViewModelClass.updatePlaylist(
                    convertListToString,
                    playlistModel!!.id,
                    (context as AppCompatActivity).lifecycleScope
                )

                Handler(Looper.getMainLooper()).postDelayed({
                    observerCustomPlaylistAudio()
                }, 500)

            } else {
                mViewModelClass.updatePlaylist(
                    "", playlistModel!!.id, (context as AppCompatActivity).lifecycleScope
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    observerCustomPlaylistAudio()
                }, 500)
            }

        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        this.isFragHidden = hidden
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

    override fun onResume() {
        super.onResume()
        SavedAppTheme(
            activity as Context,
            null,
            null,
            null,
            isHomeFrag = false,
            isHostActivity = false,
            tagEditorsBG = null,
            isTagEditor = false,
            bottomBar = null,
            rlMiniPlayerBottomSheet = null,
            bottomShadowIVAlbumFrag = null,
            isAlbumFrag = false,
            topViewIV = null,
            bottomShadowIVArtistFrag = null,
            isArtistFrag = false,
            topViewIVArtistFrag = null,
            bottomShadowIVPlaylist = binding!!.bottomShadowIV,
            isPlaylistFragCategory = true,
            topViewIVPlaylist = binding!!.topViewIV,
            playlistBG = null,
            isPlaylistFrag = false,
              null,
            isSearchFrag = false,
            null,
            false

        ).settingSavedBackgroundTheme()
    }
}
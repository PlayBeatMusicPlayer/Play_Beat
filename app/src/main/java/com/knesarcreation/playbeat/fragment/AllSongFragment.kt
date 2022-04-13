package com.knesarcreation.playbeat.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.*
import android.provider.MediaStore
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentAllSongBinding
import com.knesarcreation.playbeat.service.PlayBeatMusicService
import com.knesarcreation.playbeat.utils.*
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CopyOnWriteArrayList


class AllSongFragment : Fragment(), ServiceConnection/*, AllSongsAdapter.OnClickSongItem */ {
    //private var mPermRequest: ActivityResultLauncher<String>? = null
    // private var mreqPermForManageAllFiles: ActivityResultLauncher<Intent>? = null
    private var _binding: FragmentAllSongBinding? = null
    private val binding get() = _binding
    private lateinit var allSongsAdapter: AllSongsAdapter
    lateinit var progressBar: CustomProgressDialog
    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var tempAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private lateinit var storage: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private var isAnimated = false
    private var shuffledList = CopyOnWriteArrayList<AllSongsModel>()
    private var count = 0
    private var selectedSongsIdList = ArrayList<Long>()
    private var selectedAudioList = ArrayList<AllSongsModel>()
    private var selectedPositionList = ArrayList<Int>()
    private lateinit var viewModel: DataObservableClass
    private lateinit var textCountTV: TextView

    companion object {
        const val Broadcast_PLAY_NEW_AUDIO = "com.knesarcreation.playbeat.utils.PlayNewAudio"
        const val Broadcast_UPDATE_MINI_PLAYER =
            "com.knesarcreation.playbeat.utils.UpdatePlayerUi"
        const val READ_STORAGE_PERMISSION = 101
        var musicService: PlayBeatMusicService? = null
        //var serviceBound = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAllSongBinding.inflate(inflater, container, false)
        val view = binding?.root

        //checkPermission(activity as Context)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]
        storage = StorageUtil(activity as AppCompatActivity)
        viewModel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")
        //loadAudio()

        if (storage.getIsAppOpenedInitially()) {
            //if yes : app opened initially first time
            //saving false to prefs becz app is now opened for the first time
            storage.saveAppOpenedInitially(false)
            val bottomSheetWhatsNew = BottomSheetWhatsNew()
            bottomSheetWhatsNew.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheetWhatsNew"
            )
        }

        progressBar = CustomProgressDialog(activity as Context)
        progressBar.show()
        progressBar.setIsCancelable(true)
        progressBar.setCanceledOnOutsideTouch(false)

        startService()

        initializeAddMob()

        //set up recycler view
        setUpAllSongRecyclerAdapter()

        observeAudioData()

        sortAudios()

        shuffleAudio()

        moreOptionMenu()

        binding?.closeContextMenu?.setOnClickListener {
            disableContextMenu()
        }

        selectAllAudio()

        setTextSwitcherFactory()

        viewModel.onBackPressed.observe(viewLifecycleOwner) {
            if (it != null) {
                disableContextMenu()
            }
        }

        binding?.swipeToRefresh?.setOnRefreshListener {
            binding?.swipeToRefresh?.isRefreshing = false
            val mAudioThread: Thread = object : Thread() {
                override fun run() {
                    super.run()
                    LoadAllAudios(activity as Context, false).loadAudio(false)
                }
            }
            mAudioThread.start()
        }

        return view!!

    }

    private fun initializeAddMob() {

        val adBanner = AdBanner(activity as Context, binding!!.adViewContainer)
        adBanner.initializeAddMob()

        //val id = Settings.Secure.getString(
        //(activity as AppCompatActivity).contentResolver,
        //Settings.Secure.ANDROID_ID
        //)
        //Toast.makeText(activity as Context, "$id", Toast.LENGTH_SHORT).show()

        //RequestConfiguration.Builder().setTestDeviceIds()


    }

    private fun selectAllAudio() {
        binding?.selectAllAudios?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                allSongsAdapter.selectAllAudios()
                for ((position, audio) in audioList.withIndex()) {
                    if (!selectedSongsIdList.contains(audio.songId)) {
                        selectedSongsIdList.add(audio.songId)
                        selectedAudioList.add(audio)
                    }
                    if (!selectedPositionList.contains(position)) {
                        selectedPositionList.add(position)
                    }
                    textSwitcherIncrementTextAnim()
                }
                Log.d("selectAllAudiosSize", "onCreateView:${selectedSongsIdList.size} ")
            } else {
                allSongsAdapter.unSelectAllAudios()
                for ((position, audio) in audioList.withIndex()) {
                    selectedSongsIdList.remove(audio.songId)
                    selectedPositionList.remove(position)
                    selectedAudioList.remove(audio)
                }
                binding?.toolbar?.visibility = View.VISIBLE
                binding?.rlContextMenu?.visibility = View.INVISIBLE
                AllSongsAdapter.isContextMenuEnabled = false
                binding?.selectedAudiosTS!!.setText("0")
                viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
            }
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
                        requestDeleteMultipleAudioPermission()
                        bottomSheetMultiSelectMoreOptions.dismiss()
                    }
                }
        }
    }

    private fun requestDeleteMultipleAudioPermission() {
        val uriList = ArrayList<Uri>()
        for (audio in selectedAudioList) {
            uriList.add(Uri.parse(audio.contentUri))
        }
        if (selectedAudioList.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi =
                    MediaStore.createDeleteRequest(
                        (activity as AppCompatActivity).contentResolver,
                        uriList
                    )

                try {
                    startIntentSenderForResult(pi.intentSender, 1002, null, 0, 0, 0, null)
                } catch (e: Exception) {
                    Log.d(
                        "startIntentSenderForResult",
                        "requestDeleteMultipleAudioPermission:${e.message} "
                    )
                }

            } else {
                deleteAudioReqBelowApi30()
            }
        }
    }

    // For api above 30
    private fun deleteAudioReqAboveApi30() {
        //Log.d("SongThatWillBeDelete", "deleteAudioFromDevice:$allSongsModel ")

        if (selectedAudioList.isNotEmpty()) {

            for (audio in selectedAudioList) {

                val loadQueueAudio = storage.loadQueueAudio()
                var currentPlayingAudioIndex = storage.loadAudioIndex()
                val currentPlayingAudio = loadQueueAudio[currentPlayingAudioIndex]

                loadQueueAudio.remove(audio)
                mViewModelClass.deleteOneSong(audio.songId, lifecycleScope)
                mViewModelClass.deleteOneQueueAudio(
                    audio.songId,
                    lifecycleScope
                )

                if (audio.songId == currentPlayingAudio.songId) {
                    // selected audio is found in current playing audio
                    if (currentPlayingAudio.playingOrPause == 1 || currentPlayingAudio.playingOrPause == 0) {
                        // if deleted audio was playing audio then play next audio
                        if (currentPlayingAudioIndex == loadQueueAudio.size /* there is no need to subtract size by 1 here, since one audio is already deleted */) {
                            // last audio deleted which was playing
                            // so play a prev audio, for that save a new index

                            currentPlayingAudioIndex = --currentPlayingAudioIndex
                            storage.storeAudioIndex(currentPlayingAudioIndex)
                            Log.d(
                                "SongThatWillBeDelete",
                                "deleteAudioFromDevice: isAudioPlaying : $currentPlayingAudioIndex "
                            )

                        }

                        loadQueueAudio[currentPlayingAudioIndex].playingOrPause = 1
                        storage.storeQueueAudio(loadQueueAudio)

                        if (musicService?.mediaPlayer != null) {
                            musicService?.pausedByManually = true
                            val broadcastIntent =
                                Intent(Broadcast_PLAY_NEW_AUDIO)
                            (activity as AppCompatActivity).sendBroadcast(
                                broadcastIntent
                            )
                            Log.d(
                                "SongThatWillBeDelete",
                                "deleteAudioFromDevice: New audio played "
                            )
                        }

                        //if (currentPlayingAudio.playingOrPause == 0) {
                        // audio is already paused
                        Handler(Looper.getMainLooper()).postDelayed({
                            musicService?.pauseMedia()
                            musicService?.pausedByManually = true
                            musicService?.updateNotification(isAudioPlaying = false)
                            ///binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_play_audio)
                            // highlight pause audio with pause anim
                            mViewModelClass.updateSong(
                                currentPlayingAudio.songId,
                                currentPlayingAudio.songName,
                                0,
                                lifecycleScope
                            )
                            mViewModelClass.updateQueueAudio(
                                currentPlayingAudio.songId,
                                currentPlayingAudio.songName,
                                0,
                                lifecycleScope
                            )
                        }, 500)
                    }
                } else {
                    // after deleting audio, current playing audio index might get changed
                    // so save a new index
                    val newIndex = loadQueueAudio.indexOf(currentPlayingAudio)
                    storage.storeAudioIndex(newIndex)
                    storage.storeQueueAudio(loadQueueAudio)
                    Log.d(
                        "SongThatWillBeDelete",
                        "deleteAudioFromDevice: audioIndexChanged: newIndex: $newIndex "
                    )
                }
            }

            Toast.makeText(activity as Context, "Songs deleted", Toast.LENGTH_SHORT).show()
        }


        //dismiss()
    }

    // For api below 30
    private fun deleteAudioReqBelowApi30() {

        try {
            //Log.d("SongThatWillBeDelete", "deleteAudioFromDevice: path: $audioFile ")

            val alertDialog =
                MaterialAlertDialogBuilder(activity as Context, R.style.CustomAlertDialog)
            val viewGroup: ViewGroup =
                (activity as AppCompatActivity).findViewById(android.R.id.content)
            val customView =
                layoutInflater.inflate(R.layout.custom_alert_dialog, viewGroup, false)
            val dialogTitleTV = customView.findViewById<TextView>(R.id.dialogTitleTV)
            val dialogMessageTV =
                customView.findViewById<TextView>(R.id.dialogMessageTV)
            val cancelButton =
                customView.findViewById<MaterialButton>(R.id.cancelButton)
            val deleteBtn = customView.findViewById<MaterialButton>(R.id.positiveBtn)
            alertDialog.setView(customView)
            val dialog = alertDialog.create()

            dialogTitleTV.text = getString(R.string.delete_song)
            dialogMessageTV.text =
                "Are you sure you want to delete selected song."

            deleteBtn.setOnClickListener {
                for (audio in selectedAudioList) {
                    val audioPath =
                        GetRealPathOfUri().getUriRealPath(
                            activity as Context,
                            Uri.parse(audio.contentUri)
                        )
                    val loadQueueAudio = storage.loadQueueAudio()
                    var currentPlayingAudioIndex = storage.loadAudioIndex()
                    val currentPlayingAudio = loadQueueAudio[currentPlayingAudioIndex]

                    Log.d("SongThatWillBeDelete", "deleteAudioFromDevice:$audio ")
                    loadQueueAudio.remove(audio)

                    mViewModelClass.deleteOneSong(audio.songId, lifecycleScope)
                    mViewModelClass.deleteOneQueueAudio(
                        audio.songId,
                        lifecycleScope
                    )

                    try {
                        val audioFile = File(audioPath!!)
                        if (audioFile.exists()) {
                            //delete file from storage
                            audioFile.delete()

                            MediaScannerConnection.scanFile(
                                activity as Context,
                                arrayOf(audioFile.path),
                                null,
                                null
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Log.d(
                        "SongThatWillBeDelete",
                        "deleteAudioFromDevice: isAudioPlaying : ${audio.playingOrPause} "
                    )
                    if (audio.songId == currentPlayingAudio.songId) {
                        if (currentPlayingAudio.playingOrPause == 1 || currentPlayingAudio.playingOrPause == 0) {
                            // if deleted audio was playing audio then play next audio
                            if (currentPlayingAudioIndex == loadQueueAudio.size /* there is no need to subtract size by 1 here, since one audio is already deleted */) {
                                // last audio deleted which was playing
                                // so play a prev audio, for that save a new index

                                currentPlayingAudioIndex = --currentPlayingAudioIndex
                                storage.storeAudioIndex(currentPlayingAudioIndex)
                                Log.d(
                                    "SongThatWillBeDelete",
                                    "deleteAudioFromDevice: isAudioPlaying : $currentPlayingAudioIndex "
                                )

                            }

                            loadQueueAudio[currentPlayingAudioIndex].playingOrPause = 1
                            storage.storeQueueAudio(loadQueueAudio)

                            if (musicService?.mediaPlayer != null) {
                                musicService?.pausedByManually = true
                                val broadcastIntent =
                                    Intent(Broadcast_PLAY_NEW_AUDIO)
                                (activity as Context as AppCompatActivity).sendBroadcast(
                                    broadcastIntent
                                )
                                Log.d(
                                    "SongThatWillBeDelete",
                                    "deleteAudioFromDevice: New audio played "
                                )
                            }

                            Handler(Looper.getMainLooper()).postDelayed({
                                musicService?.pauseMedia()
                                musicService?.pausedByManually = true
                                musicService?.updateNotification(isAudioPlaying = false)
                                ///binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_play_audio)
                                // highlight pause audio with pause anim
                                mViewModelClass.updateSong(
                                    currentPlayingAudio.songId,
                                    currentPlayingAudio.songName,
                                    0,
                                    lifecycleScope
                                )
                                mViewModelClass.updateQueueAudio(
                                    currentPlayingAudio.songId,
                                    currentPlayingAudio.songName,
                                    0,
                                    lifecycleScope
                                )
                            }, 500)

                        }

                    } else {
                        // after deleting audio, current playing audio index might get changed
                        // so save a new index
                        val newIndex = loadQueueAudio.indexOf(currentPlayingAudio)
                        storage.storeAudioIndex(newIndex)
                        storage.storeQueueAudio(loadQueueAudio)
                        Log.d(
                            "SongThatWillBeDelete",
                            "deleteAudioFromDevice: audioIndexChanged: newIndex- $newIndex "
                        )
                    }
                }

                Toast.makeText(activity as Context, "Song deleted", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                disableContextMenu()

            }
            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()

            //dismiss()//dismiss bottom sheet

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Log.d("FileNotFoundException", "deleteAudioFromDevice:${e.message} ")
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == -1) {
            // song deleted
            deleteAudioReqAboveApi30()
        }

        disableContextMenu()

        super.onActivityResult(requestCode, resultCode, data)
        Log.d("startIntentSenderForResult", "onActivityResult: .... ")
    }

    private fun disableContextMenu() {
        binding?.toolbar?.visibility = View.VISIBLE
        binding?.rlContextMenu?.visibility = View.INVISIBLE
        AllSongsAdapter.isContextMenuEnabled = false
        binding?.selectAllAudios?.isChecked = false
        allSongsAdapter.updateChanges(selectedPositionList)
        selectedPositionList.clear()
        selectedSongsIdList.clear()
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

            //mViewModelClass.deleteQueue(lifecycleScope)
            val newAudiosForQueue = CopyOnWriteArrayList<AllSongsModel>()
            for (audio in selectedAudioList) {
                // Toast.makeText(activity as Context, "${audio.playingOrPause}", Toast.LENGTH_SHORT)
                //   .show()
                if (audio.playingOrPause != 1 && audio.playingOrPause != 0) {
                    // selected audio is not playing then only add to play next
                    if (playingQueueAudioList.contains(audio)) {
                        playingQueueAudioList.remove(audio)
                        /*mViewModelClass.deleteOneQueueAudio(
                            audio.songId,
                            lifecycleScope
                        )*/
                    } else {
                        newAudiosForQueue.add(audio)
                    }

                    // adding to last index
                    playingQueueAudioList.add(audio)
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
                /* Toast.makeText(
                     activity as Context,
                     "playing audio index is $playingAudioIndex",
                     Toast.LENGTH_SHORT
                 ).show()*/
            } else {
                // -1 index
                Log.d(
                    "playingQueueAudioListaaaa",
                    "playNext:$playingAudioIndex "
                )
                /*Toast.makeText(
                    activity as Context,
                    "playing audio index is $playingAudioIndex",
                    Toast.LENGTH_SHORT
                ).show()*/
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

            val make = Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "Added ${selectedAudioList.size} songs to playing queue", Snackbar.LENGTH_LONG
            )
            make.show()

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

            // mViewModelClass.deleteQueue(lifecycleScope)
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
                    }
                    // adding next to playing index
                    playingQueueAudioList.add(audioIndex, audio)

                    Log.d(
                        "PlalistAudioTesting",
                        "playNext: Index: $audioIndex , $audio , playingOrPause: ${audio.playingOrPause} "
                    )
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
            BottomSheetChoosePlaylist(null, false, selectedSongsIdList)
        bottomSheetChooseToPlaylist.show(
            (activity as AppCompatActivity).supportFragmentManager,
            "bottomSheetChooseToPlaylist"
        )
        bottomSheetChooseToPlaylist.listener =
            object : BottomSheetChoosePlaylist.PlaylistSelected {
                override fun onSelected() {
                    /*binding?.toolbar?.visibility = View.VISIBLE
                    binding?.rlContextMenu?.visibility = View.INVISIBLE
                    AllSongsAdapter.isContextMenuEnabled = false
                    binding?.selectAllAudios?.isChecked = false
                    allSongsAdapter.updateChanges(selectedPositionList)
                    selectedPositionList.clear()
                    selectedSongsIdList.clear()
                    selectedAudioList.clear()
                    binding?.selectedAudiosTS!!.setText("0")
                    viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled*/
                    disableContextMenu()
                }
            }
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

    private fun observeAudioData() {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner, {
            if (it != null) {
                audioList.clear()
                tempAudioList.clear()
                tempAudioList.addAll(it)
                count++
                if (it.isEmpty()) {
                    binding?.rlAllSongContainer!!.visibility = View.GONE
                    binding?.rlNoSongsPresent!!.visibility = View.VISIBLE
                } else {
                    binding?.rlAllSongContainer!!.visibility = View.VISIBLE
                    binding?.rlNoSongsPresent!!.visibility = View.GONE
                    val sortedList: List<AllSongsModel>
                    when (storage.getAudioSortedValue(StorageUtil.AUDIO_KEY)) {
                        "Name" -> {
                            sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                            audioList.addAll(sortedList)
                            if (AllSongsAdapter.isContextMenuEnabled) {
                                allSongsAdapter.submitList(getCheckedAudioList(sortedList).toMutableList())
                            } else {
                                allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.songName })
                            }
                            binding?.sortAudioTV?.text = "Name"
                            Log.d("sortedListObserved", "observeAudioData:$sortedList ")
                        }
                        "Duration" -> {
                            sortedList = it.sortedBy { allSongsModel -> allSongsModel.duration }
                            audioList.addAll(sortedList)
                            if (AllSongsAdapter.isContextMenuEnabled) {
                                allSongsAdapter.submitList(getCheckedAudioList(sortedList).toMutableList())
                            } else {
                                allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.duration })
                            }
                            binding?.sortAudioTV?.text = "Duration"
                        }
                        "DateAdded" -> {
                            sortedList =
                                it.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
                            audioList.addAll(sortedList)
                            if (AllSongsAdapter.isContextMenuEnabled) {
                                allSongsAdapter.submitList(getCheckedAudioList(sortedList).toMutableList())
                            } else {
                                allSongsAdapter.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.dateAdded })
                            }
                            binding?.sortAudioTV?.text = "Date Added"
                        }
                        "ArtistName" -> {
                            sortedList =
                                it.sortedBy { allSongsModel -> allSongsModel.artistsName }
                            audioList.addAll(sortedList)
                            if (AllSongsAdapter.isContextMenuEnabled) {
                                allSongsAdapter.submitList(getCheckedAudioList(sortedList).toMutableList())
                            } else {
                                allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.artistsName })
                            }
                            binding?.sortAudioTV?.text = "Artist Name"
                        }
                        else -> {
                            storage.saveAudioSortingMethod(StorageUtil.AUDIO_KEY, "Name")
                            sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                            audioList.addAll(sortedList)
                            if (AllSongsAdapter.isContextMenuEnabled) {
                                allSongsAdapter.submitList(getCheckedAudioList(sortedList).toMutableList())
                            } else {
                                allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.songName })
                            }
                            binding?.sortAudioTV?.text = "Name"
                        }
                    }

                    if (!isAnimated) {
                        /*for ((index, _) in audioList.withIndex()) {
                            textToShowSelectedCount.add("${index + 1} Selected")
                        }*/
                        //animate once when app opens
                        if (it.isNotEmpty()) {
                            animateRecyclerView()
                            isAnimated = true
                        }
                    } else {
                        //animate once when app opens first time
                        if (storage.getIsAudioPlayedFirstTime()) {
                            animateRecyclerView()
                            isAnimated = true
                        }
                    }
                    if (it.size >= 2) {
                        binding?.totalAudioTV?.text = "${audioList.size} Songs"
                    } else {
                        binding?.totalAudioTV?.text = "${audioList.size} Song"
                    }
                }

            } else {
                binding?.totalAudioTV?.text = "0 Song"
                binding?.rlAllSongContainer!!.visibility = View.GONE
                binding?.rlNoSongsPresent!!.visibility = View.VISIBLE
            }
            Handler(Looper.getMainLooper()).postDelayed({
                if (progressBar.isShowing())
                    progressBar.dismiss()
            }, 800)
        })
    }

    private fun shuffleAudio() {
        binding?.shuffleAudio?.setOnClickListener {
            // storage.saveIsShuffled(true)
            shuffledList.clear()
            shuffledList.addAll(audioList.shuffled())
            audioList.clear()
            audioList.addAll(shuffledList)
            /* storage.storeAudio(audioList)
             audioIndexPos = 0
             storage.storeAudioIndex(audioIndexPos)*/

            onClickAudio(audioList[0], 0, true)
        }
    }

    private fun animateRecyclerView() {
        binding?.rvAllSongs!!.animate()
            .translationY(-10f)
            //translationYBy(30f)
            .alpha(1.0f)
            .setListener(null)
    }

    private fun sortAudios() {
        when (storage.getAudioSortedValue(StorageUtil.AUDIO_KEY)) {
            "Name" -> {
                binding?.sortAudioTV?.text = "Name"
            }
            "Duration" -> {
                binding?.sortAudioTV?.text = "Duration"
            }
            "DateAdded" -> {
                binding?.sortAudioTV?.text = "Date Added"
            }
            "ArtistName" -> {
                binding?.sortAudioTV?.text = "Artist Name"
            }
            else -> {
                binding?.sortAudioTV?.text = "Name"
            }
        }
        binding?.sortAudios?.setOnClickListener {
            val bottomSheetSortByOptions = BottomSheetSortBy(activity as Context, "audio", "")
            bottomSheetSortByOptions.show(
                (context as AppCompatActivity).supportFragmentManager,
                "bottomSheetSortByOptions"
            )

            bottomSheetSortByOptions.listener = object : BottomSheetSortBy.OnSortingAudio {
                override fun byDate() {
                    storage.saveAudioSortingMethod(StorageUtil.AUDIO_KEY, "DateAdded")
                    val sortedByDateAdded =
                        tempAudioList.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }

                    setUpAllSongRecyclerAdapter()
                    allSongsAdapter.submitList(sortedByDateAdded)
                    audioList.clear()
                    audioList.addAll(sortedByDateAdded)
                    animateRecyclerView()
                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Date Added"
                    for (name in audioList) {
                        Log.d("sortedListAllSongFrag", "observeAudioData: ${name.songName} ")
                    }
                }

                override fun byName() {
                    storage.saveAudioSortingMethod(StorageUtil.AUDIO_KEY, "Name")
                    val sortedBySongName =
                        tempAudioList.sortedBy { allSongsModel -> allSongsModel.songName }

                    setUpAllSongRecyclerAdapter()
                    allSongsAdapter.submitList(sortedBySongName)
                    audioList.clear()
                    audioList.addAll(sortedBySongName)
                    animateRecyclerView()
                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Name"
                }

                override fun byDuration() {
                    storage.saveAudioSortingMethod(StorageUtil.AUDIO_KEY, "Duration")
                    val sortedByDuration =
                        tempAudioList.sortedBy { allSongsModel -> allSongsModel.duration }
                    setUpAllSongRecyclerAdapter()
                    allSongsAdapter.submitList(sortedByDuration)
                    audioList.clear()
                    audioList.addAll(sortedByDuration)
                    animateRecyclerView()
                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Duration"
                }

                override fun byArtistName() {
                    storage.saveAudioSortingMethod(StorageUtil.AUDIO_KEY, "ArtistName")
                    val sortedByArtistName =
                        tempAudioList.sortedBy { allSongsModel -> allSongsModel.artistsName }
                    setUpAllSongRecyclerAdapter()
                    allSongsAdapter.submitList(sortedByArtistName)
                    audioList.clear()
                    audioList.addAll(sortedByArtistName)
                    animateRecyclerView()
                    binding?.sortAudioTV?.text = "Artist Name"
                    bottomSheetSortByOptions.dismiss()
                }
            }

        }
    }

    private fun setUpAllSongRecyclerAdapter() {
        allSongsAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position, false)
                }, AllSongsAdapter.OnLongClickListener { allSongModel, position ->
                    if (allSongModel.isChecked) {
                        binding?.toolbar?.visibility = View.INVISIBLE
                        binding?.rlContextMenu?.visibility = View.VISIBLE
                        selectedSongsIdList.add(allSongModel.songId)
                        selectedPositionList.add(position)
                        selectedAudioList.add(allSongModel)

                        textSwitcherIncrementTextAnim()

                    } else {
                        selectedSongsIdList.remove(allSongModel.songId)
                        selectedPositionList.remove(position)
                        selectedAudioList.remove(allSongModel)

                        textSwitcherDecrementTextAnim()

                        if (selectedSongsIdList.isEmpty()) {
                            binding?.toolbar?.visibility = View.VISIBLE
                            binding?.rlContextMenu?.visibility = View.INVISIBLE
                            AllSongsAdapter.isContextMenuEnabled = false
                        }
                    }
                    viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
                }, false
            )
        allSongsAdapter.isSearching = false
        AllSongsAdapter.isContextMenuEnabled = false
        //binding!!.rvAllSongs.setHasFixedSize(true)
        //allSongsAdapter.setHasStableIds(true)
        binding!!.rvAllSongs.adapter = allSongsAdapter
        // binding!!.rvAllSongs.itemAnimator = null
        binding!!.rvAllSongs.scrollToPosition(0)
        binding?.rvAllSongs?.alpha = 0.0f
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
        if (selectedSongsIdList.isNotEmpty()) {
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

    private fun startService() {
        // if (musicService == null) {
        if (storage.getIsAudioPlayedFirstTime()) {
            // if app opened first time
            val audioList = storage.loadAudio()
            storage.storeQueueAudio(audioList)
            storage.storeAudioIndex(0) // since service is creating firstTime
        } else {
            //audioList = storage.loadAudio()
            audioIndexPos = storage.loadAudioIndex()
            if (audioIndexPos == -1) {
                audioIndexPos = 0
                storage.storeAudioIndex(audioIndexPos)
            }

            //highlight the paused audio when app opens and service is closed
            var queueAudioList = CopyOnWriteArrayList<AllSongsModel>()
            try {
                queueAudioList = storage.loadQueueAudio()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (queueAudioList.isNotEmpty()) {
                val audio = storage.loadQueueAudio()
                mViewModelClass.updateSong(
                    audio[audioIndexPos].songId,
                    audio[audioIndexPos].songName,
                    0, /*pause*/
                    (context as AppCompatActivity).lifecycleScope
                )
            } /*else {
                    // if queue is empty then add audios to it
                    val audioList = storage.loadAudio()
                    storage.storeQueueAudio(audioList)
                    storage.storeAudioIndex(0)
                }*/
        }

        Log.d(
            "AlbumFragment.musicService",
            "playAudio: its null... service created : Service is  null"
        )
        val playerIntent = Intent(activity as Context, PlayBeatMusicService::class.java)
        (activity as AppCompatActivity).startService(playerIntent)
        (activity as AppCompatActivity).bindService(
            playerIntent,
            this,
            Context.BIND_AUTO_CREATE
        )
        Log.d(
            "AlbumFragment.musicService",
            "playAudio: its null... service created : Service is  null"
        )
        // } else {
        //musicService?.registerPlayNewAudio()
        //musicService?.registerBecomingNoisyReceiver()
        /* Toast.makeText(
             activity as Context,
             "AllSongFrag: Service is not null",
             Toast.LENGTH_SHORT
         ).show()*/
        // }

        if (!storage.getIsAudioPlayedFirstTime()) {
            val updatePlayer = Intent(Broadcast_UPDATE_MINI_PLAYER)
            (activity as Context).sendBroadcast(updatePlayer)
        }

    }

    private fun playAudio(audioIndex: Int) {
        this.audioIndexPos = audioIndex
        //val loadQueueAudio = storage.loadQueueAudio()
        //loadQueueAudio[audioIndex].playingOrPause = 1
        //store audio to prefs
        storage.storeQueueAudio(audioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /* val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer) */
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as PlayBeatMusicService.LocalBinder
        musicService = binder.getService()
        //serviceBound = true
        Log.d("AllSongServicesBounded", "onServiceConnected: connected service")
        //controlAudio()
        //Toast.makeText(activity as Context, "Service Bounded", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        musicService = null
        //Toast.makeText(activity as Context, "null service", Toast.LENGTH_SHORT).show()
        //serviceBound = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyedActivity = true
    }

    private fun onClickAudio(allSongModel: AllSongsModel, position: Int, isShuffled: Boolean) {

        if (File(Uri.parse(allSongModel.data).path!!).exists()) {
            storage.saveIsShuffled(isShuffled)

            var restrictToUpdateAudio = false
            val prevPlayingAudioIndex = storage.loadAudioIndex()
            val audioList = storage.loadQueueAudio()
            var prevPlayingAudioModel: AllSongsModel? = null

            if (audioList.isNotEmpty()) {
                prevPlayingAudioModel = audioList[prevPlayingAudioIndex]

                restrictToUpdateAudio = allSongModel.songId == prevPlayingAudioModel.songId

                Log.d(
                    "CompareSongID",
                    "onClickAudio: ${allSongModel.songId}  , ${prevPlayingAudioModel.songId} "
                )
                if (storage.getIsAudioPlayedFirstTime()) {
                    restrictToUpdateAudio = false
                }

                mViewModelClass.deleteQueue(lifecycleScope)

                // restricting to update if clicked audio is same
                if (!restrictToUpdateAudio) {
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
            }

            /*Toast.makeText(context, "$position , ${this.audioList.indexOf(allSongModel)}", Toast.LENGTH_SHORT)
            .show()*/


            playAudio(this.audioList.indexOf(allSongModel))

            // restricting to update if clicked audio is same
            //if (!restrictToUpdateAudio) {
            // adding queue list to DB and show highlight of current audio
            for (audio in this.audioList) {
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
                mViewModelClass.insertQueue(queueListModel, lifecycleScope)
            }

            if (audioList.isNotEmpty()) {
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
        } else {
            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
        }

    }

    private fun incrementMostPlayedCount(
        audioList: CopyOnWriteArrayList<AllSongsModel>,
        currentPlayingAudioIndex: Int
    ) {
        val mostPlayedCount = audioList[currentPlayingAudioIndex].mostPlayedCount + 1
        mViewModelClass.updateMostPlayedAudioCount(
            audioList[currentPlayingAudioIndex].songId,
            mostPlayedCount,
            lifecycleScope
        )

        val list = CopyOnWriteArrayList<AllSongsModel>()
        for ((index, audio) in audioList.withIndex()) {
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
                audio.year,
                audio.folderId,
                audio.folderName,
                audio.noOfSongs
            )

            allSongsModel.currentPlayedAudioTime = audio.currentPlayedAudioTime

            if (index == currentPlayingAudioIndex) {
                allSongsModel.mostPlayedCount = mostPlayedCount
                if (musicService?.mediaPlayer != null) {
                    if (musicService?.mediaPlayer!!.isPlaying) {
                        allSongsModel.playingOrPause = 1 /*playing*/
                    } else {
                        allSongsModel.playingOrPause = 0 /*pause*/
                    }
                }
                list.add(allSongsModel)
            } else {
                allSongsModel.mostPlayedCount = audio.mostPlayedCount
                list.add(allSongsModel)
            }

        }
        storage.storeQueueAudio(list)
    }

    private fun getCheckedAudioList(sortedList: List<AllSongsModel>): ArrayList<AllSongsModel> {
        val audioList = ArrayList<AllSongsModel>()
        for (audio in sortedList) {
            for (audioId in selectedSongsIdList) {
                if (audio.songId == audioId) {
                    audio.isChecked = true
                }
            }
            audioList.add(audio)
        }
        return audioList
    }

}
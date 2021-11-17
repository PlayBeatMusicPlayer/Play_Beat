package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentFoldersSpecificSongsBinding
import com.knesarcreation.playbeat.model.FolderModel
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.GetRealPathOfUri
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CopyOnWriteArrayList

class FoldersSpecificSongs : Fragment() {

    private var _binding: FragmentFoldersSpecificSongsBinding? = null
    private val binding get() = _binding
    private var allSongsAdapter: AllSongsAdapter? = null
    private lateinit var viewModel: DataObservableClass
    private lateinit var folderModelData: FolderModel

    private var audioIndexPos = -1

    // private var isDestroyedActivity = false
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
    private lateinit var textCountTV: TextView
    private var launchFolderData: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
            duration = 200L
        }
        reenterTransition =
            MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
                duration = 200L
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentFoldersSpecificSongsBinding.inflate(inflater, container, false)
        val view = binding!!.root

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        storage = StorageUtil(activity as AppCompatActivity)
        viewModel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")
        //loadAudio()

        viewModel.folderData.observe(viewLifecycleOwner, {

            convertGsonToAlbumModel(it)
            observeAudioData()
        })

        binding?.arrowBackIV?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        shuffleAudio()

        moreOptionMenu()

        binding?.closeContextMenu?.setOnClickListener {
            disableContextMenu()
        }

        selectAllAudio()

        setTextSwitcherFactory()

        viewModel.onBackPressed.observe(viewLifecycleOwner, {
            if (it != null) {
                disableContextMenu()
            }
        })

        return view
    }

    private fun convertGsonToAlbumModel(folderDataString: String) {
        val gson = Gson()
        val type = object : TypeToken<FolderModel>() {}.type
        folderModelData = gson.fromJson(folderDataString, type)

        binding?.folderNameTV!!.isSelected = true
        binding?.folderNameTV?.text = folderModelData.folderName

    }

    @SuppressLint("SetTextI18n")
    private fun observeAudioData() {
        //set up recycler view
        setUpAllSongRecyclerAdapter()

        mViewModelClass.getAllSong().observe(viewLifecycleOwner) {
            if (launchFolderData != null && launchFolderData?.isActive!!) {
                launchFolderData?.cancel()
            }
            launchFolderData = lifecycleScope.launch(Dispatchers.IO) {
                val audioListData: List<AllSongsModel> =
                    mViewModelClass.getAudioAccordingToFolders(folderModelData.folderId)
                audioList.clear()
                val sortedList = audioListData.sortedBy { allSongsModel -> allSongsModel.songName }
                audioList.addAll(sortedList)

                (activity as AppCompatActivity).runOnUiThread {
                    if (AllSongsAdapter.isContextMenuEnabled) {
                        allSongsAdapter!!.submitList(getCheckedAudioList(sortedList).toMutableList())
                    } else {
                        allSongsAdapter!!.submitList(audioListData.sortedBy { allSongsModel -> allSongsModel.songName }
                            .toMutableList())
                    }

                    if (audioListData.size > 1) {
                        binding?.totalAudioTV?.text = "${audioListData.size} Songs"
                    } else {
                        binding?.totalAudioTV?.text = "${audioListData.size} Song"
                    }

                    if (audioListData.isEmpty()) {
                        binding?.rlToolbarContainer?.visibility = View.GONE
                        binding?.rlNoSongsPresent?.visibility = View.VISIBLE
                        binding?.rlRvContainer?.visibility = View.GONE
                    } else {
                        binding?.rlToolbarContainer?.visibility = View.VISIBLE
                        binding?.rlNoSongsPresent?.visibility = View.GONE
                        binding?.rlRvContainer?.visibility = View.VISIBLE
                    }
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
        allSongsAdapter!!.isSearching = false
        AllSongsAdapter.isContextMenuEnabled = false
        //binding!!.rvAllSongs.setHasFixedSize(true)
        //allSongsAdapter.setHasStableIds(true)
        binding!!.rvFolderSongs.adapter = allSongsAdapter
        // binding!!.rvAllSongs.itemAnimator = null
        binding!!.rvFolderSongs.scrollToPosition(0)
        //binding?.rvFolderSongs?.alpha = 0.0f
    }

    private fun selectAllAudio() {
        binding?.selectAllAudios?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                allSongsAdapter!!.selectAllAudios()
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
                allSongsAdapter!!.unSelectAllAudios()
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
                        /*  Toast.makeText(
                              activity as Context,
                              "Sorry for inconvenience, feature is under development",
                              Toast.LENGTH_LONG
                          ).show()*/
                        /* Snackbar.make(
                             (activity as AppCompatActivity).window.decorView,
                             "Sorry for inconvenience, feature is under development",
                             Snackbar.LENGTH_LONG
                         ).show()*/

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

                        if (AllSongFragment.musicService?.mediaPlayer != null) {
                            AllSongFragment.musicService?.pausedByManually = true
                            val broadcastIntent =
                                Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
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
                            AllSongFragment.musicService?.pauseMedia()
                            AllSongFragment.musicService?.pausedByManually = true
                            AllSongFragment.musicService?.updateNotification(isAudioPlaying = false)
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

           /* if (AllSongFragment.musicService?.mediaPlayer != null) {
                AllSongFragment.musicService?.pausedByManually = true
                val broadcastIntent =
                    Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
                (activity as AppCompatActivity).sendBroadcast(
                    broadcastIntent
                )
                Log.d(
                    "SongThatWillBeDelete",
                    "deleteAudioFromDevice: New audio played "
                )
            }*/

            Toast.makeText(activity as Context, "Songs deleted", Toast.LENGTH_SHORT).show()
        }


        //dismiss()
    }

    // For api below 30
    @SuppressLint("SetTextI18n")
    private fun deleteAudioReqBelowApi30() {

        try {
            //Log.d("SongThatWillBeDelete", "deleteAudioFromDevice: path: $audioFile ")

            val alertDialog =
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

                            if (AllSongFragment.musicService?.mediaPlayer != null) {
                                AllSongFragment.musicService?.pausedByManually = true
                                val broadcastIntent =
                                    Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
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
                                AllSongFragment.musicService?.pauseMedia()
                                AllSongFragment.musicService?.pausedByManually = true
                                AllSongFragment.musicService?.updateNotification(isAudioPlaying = false)
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

               /* if (AllSongFragment.musicService?.mediaPlayer != null) {
                    AllSongFragment.musicService?.pausedByManually = true
                    val broadcastIntent =
                        Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
                    (activity as Context as AppCompatActivity).sendBroadcast(
                        broadcastIntent
                    )
                    Log.d(
                        "SongThatWillBeDelete",
                        "deleteAudioFromDevice: New audio played "
                    )
                }*/

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
        if (allSongsAdapter != null)
            allSongsAdapter!!.updateChanges(selectedPositionList)
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

    private fun playAudio(audioIndex: Int) {
        this.audioIndexPos = audioIndex
        //val loadQueueAudio = storage.loadQueueAudio()
        //loadQueueAudio[audioIndex].playingOrPause = 1
        //store audio to prefs
        storage.storeQueueAudio(audioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /* val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer) */
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
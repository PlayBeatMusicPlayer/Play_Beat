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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllAlbumsAdapter
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.adapter.ArtistsAlbumAdapter
import com.knesarcreation.playbeat.database.*
import com.knesarcreation.playbeat.databinding.FragmentArtistsTracksAndAlbumBinding
import com.knesarcreation.playbeat.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CopyOnWriteArrayList

class ArtistsTracksAndAlbumFragment : Fragment()/*, AllSongsAdapter.OnClickSongItem*/ {

    private var _binding: FragmentArtistsTracksAndAlbumBinding? = null
    private val binding get() = _binding
    private lateinit var viewModel: DataObservableClass
    private var artistsModel: ArtistsModel? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private val albumList = CopyOnWriteArrayList<AlbumModel>()

    //private var progressBar: CustomProgressDialog? = null
    private var listener: OnArtistAlbumItemClicked? = null
    private lateinit var storage: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private var allSongsAdapter: AllSongsAdapter? = null
    private lateinit var artistsAlbumAdapter: ArtistsAlbumAdapter
    private var allSongsAdapterMoreTracks: AllSongsAdapter? = null
    private var launchAlumData: Job? = null
    private var selectedSongsIdList = ArrayList<Long>()
    private var selectedAudioList = ArrayList<AllSongsModel>()
    private var selectedPositionList = ArrayList<Int>()
    private lateinit var textCountTV: TextView

    interface OnArtistAlbumItemClicked {
        fun openAlbumFromArtistFrag(album: String)
    }

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
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentArtistsTracksAndAlbumBinding.inflate(inflater, container, false)
        val view = binding?.root
        binding?.artisNameTVToolbar?.isSelected = true
        binding?.artisNameTV?.isSelected = true

        mViewModelClass =
            ViewModelProvider(this)[ViewModelClass::class.java]


        binding?.arrowBackIV?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.arrowBack?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        storage = StorageUtil(activity as Context)

        viewModel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        //progressBar = CustomProgressDialog(requireContext())
        //progressBar!!.show()
        // progressBar!!.setIsCancelable(true)
        // progressBar!!.setCanceledOnOutsideTouch(true)

        viewModel.artistsData.observe(viewLifecycleOwner, {
            convertGsonToAlbumModel(it)

            //binding?.rvAlbums?.setHasFixedSize(true)
            //binding?.rvAlbums?.setItemViewCacheSize(20)
            //binding?.rvTracks?.setItemViewCacheSize(20)

            binding?.artisNameTV?.text = artistsModel?.artistName
            binding?.artisNameTVToolbar?.text = artistsModel?.artistName
            binding?.artisNameTVToolbar2?.text = artistsModel?.artistName

            binding?.motionLayoutArtistAndAlbum?.visibility = View.VISIBLE
            binding?.rlMoreAudios?.visibility = View.GONE
            setUpAlbumAdapter()

            allSongsAdapter =
                AllSongsAdapter(
                    activity as Context,
                    AllSongsAdapter.OnClickListener { allSongModel, position ->
                        onClickAudio(allSongModel, position)
                    },
                    AllSongsAdapter.OnLongClickListener { allSongModel, position ->
                        if (allSongModel.isChecked) {
                            binding?.arrowBackIV?.visibility = View.GONE
                            binding?.rlContextMenu?.visibility = View.VISIBLE
                            binding?.selectAllAudios?.visibility = View.GONE
                            binding?.rlToolbar?.setBackgroundColor(
                                ContextCompat.getColor(
                                    activity as Context,
                                    R.color.app_theme_color
                                )
                            )
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
                                binding?.arrowBackIV?.visibility = View.VISIBLE
                                binding?.rlContextMenu?.visibility = View.INVISIBLE
                                binding?.rlToolbar?.setBackgroundColor(
                                    ContextCompat.getColor(
                                        activity as Context,
                                        R.color.transparent
                                    )
                                )
                                AllSongsAdapter.isContextMenuEnabled = false
                            }
                        }
                        viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled
                    },
                    false
                )
            allSongsAdapter!!.isSearching = false
            binding!!.rvTracks.adapter = allSongsAdapter
            binding!!.rvTracks.itemAnimator = null
            binding!!.rvTracks.scrollToPosition(0)

            getAudioAccordingAlbum(false)

        })

        binding?.playAll?.setOnClickListener {
            onClickAudio(audioList[0], 0)
        }

        binding?.seeAllTV?.setOnClickListener {
            binding?.motionLayoutArtistAndAlbum?.visibility = View.GONE
            binding?.rlMoreAudios?.visibility = View.VISIBLE
            if (AllSongsAdapter.isContextMenuEnabled) {
                binding?.selectAllAudios?.visibility = View.VISIBLE
            }
            setUpMoreTracksRV()
        }

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

    private fun disableContextMenu() {
        binding?.arrowBackIV?.visibility = View.VISIBLE
        binding?.rlContextMenu?.visibility = View.INVISIBLE
        binding?.rlToolbar?.setBackgroundColor(
            ContextCompat.getColor(
                activity as Context,
                R.color.transparent
            )
        )
        AllSongsAdapter.isContextMenuEnabled = false
        binding?.selectAllAudios?.isChecked = false
        if (allSongsAdapter != null)
            allSongsAdapter!!.updateChanges(selectedPositionList)

        if (allSongsAdapterMoreTracks != null)
            allSongsAdapterMoreTracks!!.updateChanges(selectedPositionList)

        selectedPositionList.clear()
        selectedSongsIdList.clear()
        selectedAudioList.clear()
        binding?.selectedAudiosTS!!.setText("0")
        viewModel.isContextMenuEnabled.value = false
    }

    private fun selectAllAudio() {
        binding?.selectAllAudios?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {

                allSongsAdapterMoreTracks!!.selectAllAudios()
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

                allSongsAdapterMoreTracks!!.unSelectAllAudios()
                allSongsAdapter!!.unSelectAllAudios()

                for ((position, audio) in audioList.withIndex()) {
                    selectedSongsIdList.remove(audio.songId)
                    selectedPositionList.remove(position)
                    selectedAudioList.remove(audio)
                }

                binding?.arrowBackIV?.visibility = View.VISIBLE
                binding?.rlContextMenu?.visibility = View.INVISIBLE
                binding?.rlToolbar?.setBackgroundColor(
                    ContextCompat.getColor(
                        activity as Context,
                        R.color.transparent
                    )
                )
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

            /*if (AllSongFragment.musicService?.mediaPlayer != null) {
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

                /*if (AllSongFragment.musicService?.mediaPlayer != null) {
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
                        /*mViewModelClass.deleteOneQueueAudio(
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
                    disableContextMenu()
                }
            }
    }

    private fun setUpMoreTracksRV() {
        allSongsAdapterMoreTracks = AllSongsAdapter(
            activity as Context,
            AllSongsAdapter.OnClickListener { allSongModel, position ->
                onClickAudio(allSongModel, position)
            }, AllSongsAdapter.OnLongClickListener { allSongModel, position ->
                if (allSongModel.isChecked) {
                    binding?.arrowBackIV?.visibility = View.GONE
                    binding?.rlContextMenu?.visibility = View.VISIBLE
                    binding?.selectAllAudios?.visibility = View.VISIBLE
                    binding?.rlToolbar?.setBackgroundColor(
                        ContextCompat.getColor(
                            activity as Context,
                            R.color.app_theme_color
                        )
                    )
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
                        binding?.arrowBackIV?.visibility = View.VISIBLE
                        binding?.rlContextMenu?.visibility = View.INVISIBLE
                        binding?.rlToolbar?.setBackgroundColor(
                            ContextCompat.getColor(
                                activity as Context,
                                R.color.transparent
                            )
                        )
                        AllSongsAdapter.isContextMenuEnabled = false
                    }
                }

                viewModel.isContextMenuEnabled.value = AllSongsAdapter.isContextMenuEnabled

            }, false
        )
        allSongsAdapterMoreTracks!!.isSearching = false
        binding!!.rvMoreTracks.adapter = allSongsAdapterMoreTracks
        binding!!.rvMoreTracks.itemAnimator = null
        binding!!.rvMoreTracks.scrollToPosition(0)

        getAudioAccordingAlbum(true)
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

    private fun setUpAlbumAdapter() {
        artistsAlbumAdapter = ArtistsAlbumAdapter(
            activity as Context,
            //albumList,
            object : AllAlbumsAdapter.OnAlbumClicked {
                override fun onClicked(albumModel: AlbumModel) {
                    val gson = Gson()
                    val album = gson.toJson(albumModel)
                    listener?.openAlbumFromArtistFrag(album)
                }
            }
        )
        binding?.rvAlbums?.adapter = artistsAlbumAdapter

    }

    private fun getAudioAccordingAlbum(moreTrackRV: Boolean) {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner) {
            if (launchAlumData != null && launchAlumData?.isActive!!) {
                launchAlumData?.cancel()
            }
            launchAlumData = lifecycleScope.launch(Dispatchers.IO) {
                val audioArtist: List<AllSongsModel> =
                    mViewModelClass.getAudioAccordingArtist(artistsModel?.artistName!!)
                getAlbumsAccordingToArtist(audioArtist)
                audioList.clear()
                val sortedList = audioArtist.sortedBy { allSongsModel -> allSongsModel.songName }
                audioList.addAll(sortedList)

                (activity as AppCompatActivity).runOnUiThread {
                    if (audioList.size >= 4) {
                        binding?.sepratorView1?.visibility = View.VISIBLE
                        binding?.seeAllTV?.visibility = View.VISIBLE
                    } else {
                        binding?.sepratorView1?.visibility = View.GONE
                        binding?.seeAllTV?.visibility = View.GONE
                    }

                    if (audioArtist.isEmpty()) {
                        if (!isHidden) {
                            Snackbar.make(
                                (activity as AppCompatActivity).window.decorView,
                                "Deleted empty artist",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            mViewModelClass.deleteArtist(artistsModel!!.id, lifecycleScope)
                            (activity as AppCompatActivity).onBackPressed()
                        }
                    }

                    if (moreTrackRV) {
                        allSongsAdapterMoreTracks!!.submitList(getCheckedAudioList(audioArtist).toMutableList())
                    } else {
                        allSongsAdapter!!.submitList(getCheckedAudioList(audioArtist).toMutableList())
                    }

                    Log.d(
                        "ArtistsAlbumFrag",
                        "getAudioAccordingAlbum: ${artistsModel?.artistName!!}  , audioAlbum : $audioList"
                    )
                    if (audioArtist.size == 1 && audioArtist.size == 1) {
                        binding?.totalSongsTV?.text =
                            "${audioArtist.size} Song"
                    } else {
                        binding?.totalSongsTV?.text =
                            "${audioArtist.size} Songs"
                    }
                }
            }
        }
    }

    private fun getAlbumsAccordingToArtist(audioArtist: List<AllSongsModel>) {
        albumList.clear()
        for (audio in audioArtist) {
            val albumModel = AlbumModel(
                audio.albumId,
                audio.albumName,
                audio.artistsName,
                audio.artUri,
                0,
                0
            )
            if (!albumList.contains(albumModel)) {
                albumList.add(albumModel)
            }
        }
        (activity as AppCompatActivity).runOnUiThread {
            artistsAlbumAdapter.submitList(albumList)
            // binding?.rvTracks?.visibility = View.VISIBLE
            //binding?.trackTextTV?.visibility = View.VISIBLE
        }
    }

    private fun onClickAudio(allSongModel: AllSongsModel, position: Int) {

        if (File(Uri.parse(allSongModel.data).path!!).exists()) {
            storage.saveIsShuffled(false)

            val prevPlayingAudioIndex = storage.loadAudioIndex()
            val audioList = storage.loadQueueAudio()
            var prevPlayingAudioModel: AllSongsModel? = null
            var restrictToUpdateAudio = false

            if (audioList.isNotEmpty()) {
                prevPlayingAudioModel = audioList[prevPlayingAudioIndex]
                restrictToUpdateAudio = allSongModel.songId == prevPlayingAudioModel.songId

                if (storage.getIsAudioPlayedFirstTime()) {
                    restrictToUpdateAudio = false
                }

                // restricting to update if clicked audio is same
                if (!restrictToUpdateAudio) {
                    mViewModelClass.deleteQueue(lifecycleScope)

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

            playAudio(position)

            // restricting to update if clicked audio is same
            if (!restrictToUpdateAudio) {
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
                    queueListModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
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
            }
        } else {
            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun convertGsonToAlbumModel(artistsDataString: String) {
        val gson = Gson()
        val type = object : TypeToken<ArtistsModel>() {}.type
        artistsModel = gson.fromJson(artistsDataString, type)
    }

    /* override fun onClick(allSongModel: AllSongsModel, position: Int) {
         storageUtil.saveIsShuffled(false)
         playAudio(position)
     }*/

    private fun playAudio(position: Int) {
        AudioPlayingFromCategory.audioPlayingFromAlbumORArtist = true

        storage.storeQueueAudio(audioList)
        storage.storeAudioIndex(position)

        //Send a broadcast to the service -> PLAY_NEW_AUDIO
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)

        (activity as Context).sendBroadcast(broadcastIntent)
        Log.d(
            "isAudioListSaved",
            "onAudioPlayed: serviceBound is active:  "
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as OnArtistAlbumItemClicked
        } catch (e: ClassCastException) {
            e.printStackTrace()
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
            bottomShadowIVArtistFrag = binding!!.bottomShadowIV,
            isArtistFrag = true,
            topViewIVArtistFrag = binding!!.topViewIV,
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            playlistBG = null,
            isPlaylistFrag = false,
            null,
            isSearchFrag = false,
            null,
            false
        ).settingSavedBackgroundTheme()
    }
}
package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AlbumModel
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentAlbumSongBinding
import com.knesarcreation.playbeat.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CopyOnWriteArrayList

class AlbumFragment : Fragment()/*, AlbumAdapter.OnAlbumSongClicked*//*, ServiceConnection*/ {

    private lateinit var viewModel: DataObservableClass
    private var _binding: FragmentAlbumSongBinding? = null
    private val binding get() = _binding
    private var albumData: AlbumModel? = null
    private var albumAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private var isAudioListSaved = false
    private lateinit var storage: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private var allSongsAdapter: AllSongsAdapter? = null
    private var launchAlumData: Job? = null
    private var selectedSongsIdList = ArrayList<Long>()
    private var selectedAudioList = ArrayList<AllSongsModel>()
    private var selectedPositionList = ArrayList<Int>()
    private lateinit var textCountTV: TextView

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
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAlbumSongBinding.inflate(inflater, container, false)
        val view = binding!!.root

        mViewModelClass =
            ViewModelProvider(this)[ViewModelClass::class.java]

        binding?.arrowBackIV!!.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }
        binding?.albumNameTV?.isSelected = true

        storage = StorageUtil(activity as Context)

        viewModel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.albumData.observe(viewLifecycleOwner, {
            convertGsonToAlbumModel(it)
            binding?.albumNameTV!!.text = albumData?.albumName
            //setting album art to IV
            Glide.with(activity as Context).load(albumData?.artUri).apply(
                RequestOptions.placeholderOf(
                    R.drawable.album_png
                )
            ).into(binding?.albumArtIV!!)

            //setting blurred image to Cover view
            if (albumData?.artUri != null) {
                val bitmap = UriToBitmapConverter.getBitmap(
                    (activity as Context).contentResolver,
                    albumData?.artUri!!.toUri()
                )

                val blurredImg: Bitmap = if (bitmap != null) {
                    BlurBuilder().blur(activity as Context, bitmap, 25f)
                } else {
                    //placeholder album art
                    val placeHolderBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.album_png)
                    BlurBuilder().blur(activity as Context, placeHolderBitmap, 25f)

                }

                Glide.with(activity as Context).asBitmap().load(blurredImg)
                    .into(binding?.blurredCoverIV!!)
            }

            // loadAlbumSongs()
            getAudioAccordingAlbum()

        })

        playAllAudioInAlbum()

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
        selectedPositionList.clear()
        selectedSongsIdList.clear()
        selectedAudioList.clear()
        binding?.selectedAudiosTS!!.setText("0")
        viewModel.isContextMenuEnabled.value = false
    }

    private fun selectAllAudio() {
        binding?.selectAllAudios?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                allSongsAdapter!!.selectAllAudios()
                for ((position, audio) in albumAudioList.withIndex()) {
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
                for ((position, audio) in albumAudioList.withIndex()) {
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
                        /* mViewModelClass.deleteOneQueueAudio(
                             audio.songId,
                             lifecycleScope
                         ) */
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

    private fun getAudioAccordingAlbum() {
        allSongsAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position)

                }, AllSongsAdapter.OnLongClickListener { allSongModel, position ->
                    if (allSongModel.isChecked) {
                        binding?.arrowBackIV?.visibility = View.GONE
                        binding?.rlContextMenu?.visibility = View.VISIBLE
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
        allSongsAdapter!!.isSearching = false
        AllSongsAdapter.isContextMenuEnabled = false
        binding!!.rvAlbumAudio.adapter = allSongsAdapter
        binding!!.rvAlbumAudio.itemAnimator = DefaultItemAnimator()
        binding!!.rvAlbumAudio.scrollToPosition(0)

        mViewModelClass.getAllSong().observe(viewLifecycleOwner) {
            if (launchAlumData != null && launchAlumData?.isActive!!) {
                launchAlumData?.cancel()
            }
            launchAlumData = lifecycleScope.launch(Dispatchers.IO) {
                val audioAlbum: List<AllSongsModel> =
                    mViewModelClass.getAudioAccordingAlbum(albumData?.albumName!!)
                // tempAudioAlbum = audioAlbum as ArrayList<AllSongsModel>

                albumAudioList.clear()
                val sortedList = audioAlbum.sortedBy { allSongsModel -> allSongsModel.songName }
                albumAudioList.addAll(sortedList)

                (activity as AppCompatActivity).runOnUiThread {
                    if (audioAlbum.isEmpty()) {
                        if (!isHidden) {
                            Snackbar.make(
                                (activity as AppCompatActivity).window.decorView,
                                "Deleted empty album",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            mViewModelClass.deleteAlbum(albumData!!.id, lifecycleScope)
                            (activity as AppCompatActivity).onBackPressed()
                        }
                    }
                    if (AllSongsAdapter.isContextMenuEnabled) {
                        allSongsAdapter!!.submitList(getCheckedAudioList(sortedList).toMutableList())
                    } else {
                        allSongsAdapter!!.submitList(audioAlbum.sortedBy { allSongsModel -> allSongsModel.songName })
                    }
                }

                //update songs count in album
                mViewModelClass.updateSongCount(
                    audioAlbum.size,
                    albumData?.albumId!!,
                    lifecycleScope
                )
            }
        }
    }

    private fun playAllAudioInAlbum() {
        binding?.playAlbum?.setOnClickListener {
            onClickAudio(albumAudioList[0], 0)
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

    private fun convertGsonToAlbumModel(albumDataString: String) {
        val gson = Gson()
        val type = object : TypeToken<AlbumModel>() {}.type
        albumData = gson.fromJson(albumDataString, type)
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
                for (audio in this.albumAudioList) {
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

    private fun playAudio(position: Int) {
        AudioPlayingFromCategory.audioPlayingFromAlbumORArtist = true

        storage.storeQueueAudio(albumAudioList)
        storage.storeAudioIndex(position)

        //Send a broadcast to the service -> PLAY_NEW_AUDIO
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)

        (activity as Context).sendBroadcast(broadcastIntent)
        Log.d(
            "isAudioListSaved",
            "onAudioPlayed:$isAudioListSaved ,serviceBound is active:  "
        )

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
            bottomShadowIVAlbumFrag = binding!!.bottomShadowIV,
            isAlbumFrag = true,
            topViewIV = binding!!.topViewIV,
            bottomShadowIVArtistFrag = null,
            isArtistFrag = false,
            topViewIVArtistFrag = null,
            parentViewArtistAndAlbumFrag = null,
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            playlistBG = null,
            isPlaylistFrag = false,
            searchFragBg = null,
            isSearchFrag = false,
            settingFragBg = null,
            isSettingFrag = false
        ).settingSavedBackgroundTheme()
    }

}
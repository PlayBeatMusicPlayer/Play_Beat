package com.knesarcreation.playbeat.fragment

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentAllSongBinding
import com.knesarcreation.playbeat.service.PlayBeatMusicService
import com.knesarcreation.playbeat.utils.CustomProgressDialog
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList


class AllSongFragment : Fragment(), ServiceConnection/*, AllSongsAdapter.OnClickSongItem */ {
    private lateinit var allSongsAdapter: AllSongsAdapter
    lateinit var progressBar: CustomProgressDialog
    private var _binding: FragmentAllSongBinding? = null
    private val binding get() = _binding

    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var tempAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private lateinit var storage: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private var isAnimated = false
    private var shuffledList = CopyOnWriteArrayList<AllSongsModel>()
    private var count = 0

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
        storage = StorageUtil(activity as AppCompatActivity)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        //loadAudio()

        startService()

        //set up recycler view
        refreshLayout()

        observeAudioData()

        sortAudios()

        shuffleAudio()

        return view!!

    }

    private fun observeAudioData() {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner, {
            if (it != null) {
                audioList.clear()
                tempAudioList.clear()
                tempAudioList.addAll(it)
                count++
                when (storage.getAudioSortedValue(StorageUtil.AUDIO_KEY)) {
                    "Name" -> {
                        val sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                        audioList.addAll(sortedList)
                        allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.songName })
                        binding?.sortAudioTV?.text = "Name"
                        Log.d("sortedListObserved", "observeAudioData:$sortedList ")
                    }
                    "Duration" -> {
                        val sortedList = it.sortedBy { allSongsModel -> allSongsModel.duration }
                        audioList.addAll(sortedList)
                        allSongsAdapter.submitList(sortedList/*it.sortedBy { allSongsModel -> allSongsModel.duration }*/)
                        binding?.sortAudioTV?.text = "Duration"
                    }
                    "DateAdded" -> {
                        val sortedList =
                            it.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
                        audioList.addAll(sortedList)
                        allSongsAdapter.submitList(sortedList/*it.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }*/)
                        binding?.sortAudioTV?.text = "Date Added"
                    }
                    "ArtistName" -> {
                        val sortedList =
                            it.sortedBy { allSongsModel -> allSongsModel.artistsName }
                        audioList.addAll(sortedList)
                        allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.artistsName })
                        binding?.sortAudioTV?.text = "Artist Name"
                    }
                    else -> {
                        storage.saveAudioSortingMethod(StorageUtil.AUDIO_KEY, "Name")
                        val sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                        audioList.addAll(sortedList)
                        allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.songName })
                        binding?.sortAudioTV?.text = "Name"
                    }
                }

                if (!isAnimated) {
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

            } else {
                binding?.totalAudioTV?.text = "0 Song"
            }
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

                    refreshLayout()
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

                    refreshLayout()
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
                    refreshLayout()
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
                    refreshLayout()
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

    private fun refreshLayout() {
        allSongsAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position, false)
                })
        allSongsAdapter.isSearching = false
        binding!!.rvAllSongs.adapter = allSongsAdapter
        binding!!.rvAllSongs.itemAnimator = null
        binding!!.rvAllSongs.scrollToPosition(0)
        binding?.rvAllSongs?.alpha = 0.0f
    }


    private fun startService() {
        if (musicService == null) {
            if (storage.getIsAudioPlayedFirstTime()) {
                // if app opened first time
                val audioList = storage.loadAudio()
                storage.storeQueueAudio(audioList)
                storage.storeAudioIndex(0) // since service is creating firstTime
            } else {
                //audioList = storage.loadAudio()
                audioIndexPos = storage.loadAudioIndex()

                //highlight the paused audio when app opens and service is closed
                val audio = storage.loadQueueAudio()
                mViewModelClass.updateSong(
                    audio[audioIndexPos].songId,
                    audio[audioIndexPos].songName,
                    0, /*pause*/
                    (context as AppCompatActivity).lifecycleScope
                )
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
        }

        if (!storage.getIsAudioPlayedFirstTime()) {
            val updatePlayer = Intent(Broadcast_UPDATE_MINI_PLAYER)
            (activity as Context).sendBroadcast(updatePlayer)
        }

    }

    private fun playAudio(audioIndex: Int) {
        this.audioIndexPos = audioIndex
        //store audio to prefs
        storage.storeQueueAudio(audioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as PlayBeatMusicService.LocalBinder
        musicService = binder.getService()
        //serviceBound = true
        Log.d("AllSongServicesBounded", "onServiceConnected: connected service")
        //controlAudio()
        // Toast.makeText(this, "Service Bound", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        musicService = null
        Toast.makeText(activity as Context, "null service", Toast.LENGTH_SHORT).show()
        //serviceBound = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyedActivity = true
    }


    private fun onClickAudio(allSongModel: AllSongsModel, position: Int, isShuffled: Boolean) {
        storage.saveIsShuffled(isShuffled)

        val prevPlayingAudioIndex = storage.loadAudioIndex()
        val audioList = storage.loadQueueAudio()
        val prevPlayingAudioModel = audioList[prevPlayingAudioIndex]

        var restrictToUpdateAudio = allSongModel.songId == prevPlayingAudioModel.songId

        /*Toast.makeText(context, "$position , ${this.audioList.indexOf(allSongModel)}", Toast.LENGTH_SHORT)
            .show()*/

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

        playAudio(this.audioList.indexOf(allSongModel))

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
                    audio.audioUri,
                    audio.artUri,
                    -1,
                    audio.dateAdded,
                    audio.isFavourite,
                    audio.favAudioAddedTime,
                    audio.mostPlayedCount
                )
                mViewModelClass.insertQueue(queueListModel, lifecycleScope)
            }

            mViewModelClass.updateQueueAudio(
                prevPlayingAudioModel.songId,
                prevPlayingAudioModel.songName,
                -1,
                (context as AppCompatActivity).lifecycleScope
            )

            mViewModelClass.updateQueueAudio(
                allSongModel.songId,
                allSongModel.songName,
                1,
                (context as AppCompatActivity).lifecycleScope
            )
        }

        // incrementMostPlayedCount(this.audioList, this.audioList.indexOf(allSongModel))
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
                audio.audioUri,
                audio.artUri,
                audio.dateAdded,
                audio.isFavourite,
                audio.favAudioAddedTime
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

            Log.d(
                "MostPlayedAudio1111",
                "onReceive:  ${allSongsModel.songName} , ${allSongsModel.mostPlayedCount} "
            )

        }
        storage.storeQueueAudio(list)
    }
}
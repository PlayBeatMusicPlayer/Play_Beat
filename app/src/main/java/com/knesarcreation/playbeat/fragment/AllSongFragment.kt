package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit


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

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        lifecycleScope.launch(Dispatchers.IO) {
            loadAudio()
        }

        //set up recycler view
        refreshLayout()

        observeAudioData()
        storage = StorageUtil(activity as AppCompatActivity)

        sortAudios()

        shuffleAudio()

        return view!!

    }

    private fun observeAudioData() {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner, {
            if (it != null) {
                audioList.clear()
                count++
                when (storage.getAudioSortedValue()) {
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
                        storage.saveAudioSortingMethod("Name")
                        val sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                        audioList.addAll(sortedList)
                        allSongsAdapter.submitList(it.sortedBy { allSongsModel -> allSongsModel.songName })
                        binding?.sortAudioTV?.text = "Name"
                    }
                }

                if (count == 3) {
                    if (it.isNotEmpty()) {
                        animateRecyclerView()
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
        when (storage.getAudioSortedValue()) {
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
            val bottomSheetSortByOptions = BottomSheetSortBy(activity as Context, "audio")
            bottomSheetSortByOptions.show(
                (context as AppCompatActivity).supportFragmentManager,
                "bottomSheetSortByOptions"
            )
            bottomSheetSortByOptions.listener = object : BottomSheetSortBy.OnSortingAudio {
                override fun byDate() {
                    storage.saveAudioSortingMethod("DateAdded")
                    val sortedByDateAdded =
                        audioList.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }

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
                    storage.saveAudioSortingMethod("Name")
                    val sortedBySongName =
                        audioList.sortedBy { allSongsModel -> allSongsModel.songName }

                    refreshLayout()
                    allSongsAdapter.submitList(sortedBySongName)
                    audioList.clear()
                    audioList.addAll(sortedBySongName)
                    animateRecyclerView()
                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Name"
                }

                override fun byDuration() {
                    storage.saveAudioSortingMethod("Duration")
                    val sortedByDuration =
                        audioList.sortedBy { allSongsModel -> allSongsModel.duration }
                    refreshLayout()
                    allSongsAdapter.submitList(sortedByDuration)
                    audioList.clear()
                    audioList.addAll(sortedByDuration)
                    animateRecyclerView()
                    bottomSheetSortByOptions.dismiss()
                    binding?.sortAudioTV?.text = "Duration"
                }

                override fun byArtistName() {
                    storage.saveAudioSortingMethod("ArtistName")
                    val sortedByArtistName =
                        audioList.sortedBy { allSongsModel -> allSongsModel.artistsName }
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

    @SuppressLint("Range")
    private fun loadAudio() {
        tempAudioList.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA, // path
            MediaStore.Audio.Media.DATE_ADDED
        )

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        val query =
            (activity as Context).contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            mViewModelClass.deleteSongs(lifecycleScope)

            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val data = cursor.getString(dataColumn)
                val dateAdded = cursor.getString(dateAddedColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                //val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                val artUri = Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                // getting audio uri
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val allSongsModel =
                    AllSongsModel(
                        id,
                        albumId,
                        name,
                        artist,
                        album,
                        size,
                        duration,
                        data,
                        contentUri.toString(),
                        artUri,
                        dateAdded,
                        false,
                        0L
                    )

                /*storage.loadAudioIndex()
                val playingAudio = storage.loadAudio()[]*/
                allSongsModel.playingOrPause = -1
                tempAudioList.add(allSongsModel)
            }

            cursor.close()

            var audio = CopyOnWriteArrayList<AllSongsModel>()
            if (!storage.getIsAudioPlayedFirstTime()) {
                audio = storage.loadAudio()
            }
            // update audio to DB
            var isFav = false
            var favAudioAddedTime = 0L
            //var currentPlayedAudioTime = 0L
            for (audioData in tempAudioList) {
                if (!storage.getIsAudioPlayedFirstTime()) {
                    for (savedAudioData in audio) {
                        if (savedAudioData.songId == audioData.songId) {
                            isFav = savedAudioData?.isFavourite!!
                            favAudioAddedTime = savedAudioData.favAudioAddedTime
                            audioData.currentPlayedAudioTime = savedAudioData.currentPlayedAudioTime
                            break
                        }
                    }
                }
                if (isFav) {
                    audioData.favAudioAddedTime = favAudioAddedTime
                }
                audioData.isFavourite = isFav
                //audioData.currentPlayedAudioTime = currentPlayedAudioTime
                mViewModelClass.insertAllSongs(audioData, lifecycleScope)

                // assigning isFav to false, favAudioAddedTime = 0 , for next iteration
                isFav = false
                favAudioAddedTime = 0L
                //currentPlayedAudioTime = 0L

            }
            startService()
        }
    }

    private fun startService() {
        if (musicService == null) {
            if (storage.getIsAudioPlayedFirstTime()) {
                // if app opened first time
                storage.storeAudio(tempAudioList)
                storage.storeAudioIndex(0) // since service is creating firstTime
            } else {
                //audioList = storage.loadAudio()
                audioIndexPos = storage.loadAudioIndex()

                // If any new songs added then, getting a new index of current playing audio
                /*  if (!storage.getIsAudioPlayedFirstTime()) {
                      try {
                          val currentPlayingAudio = storage.loadAudio()[audioIndexPos]
                          audioIndexPos = audioList.indexOf(currentPlayingAudio)

                          storage.storeAudioIndex(audioIndexPos)
                      } catch (e: ArrayIndexOutOfBoundsException) {
                          e.printStackTrace()
                      }
                  }
                  Log.d("audioIndexAllAudio", "startService: $audioIndexPos ")
  */
                //highlight the paused audio when app opens and service is closed
                val audio = storage.loadAudio()
                mViewModelClass.updateSong(
                    audio[audioIndexPos].songId,
                    audio[audioIndexPos].songName,
                    0, /*pause*/
                    (context as AppCompatActivity).lifecycleScope
                )

                /*  for (audioData in audio) {
                      if (audioData.isFavourite) {
                          mViewModelClass.updateFavouriteAudio(true, audioData.songId, lifecycleScope)
                      }
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
        }

        if (!storage.getIsAudioPlayedFirstTime()) {
            val updatePlayer = Intent(Broadcast_UPDATE_MINI_PLAYER)
            (activity as Context).sendBroadcast(updatePlayer)
        }

    }

    private fun playAudio(audioIndex: Int) {
        this.audioIndexPos = audioIndex
        //store audio to prefs
        storage.storeAudio(audioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        /* Toast.makeText(
             activity as Context,
             ".....${audioList[0]} , index: $audioIndex",
             Toast.LENGTH_SHORT
         ).show()*/
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
        Log.d("AllSongServicesBounded", "onServiceConnected: connected servic")
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
        val audioList = storage.loadAudio()
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
                    audio.favAudioAddedTime
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
    }

}
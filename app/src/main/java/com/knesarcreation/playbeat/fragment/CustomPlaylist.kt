package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
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
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    private var playListModel: PlaylistModel? = null
    private var newAudioArtIndex = 0
    private lateinit var newAudioAddedModel: AllSongsModel
    private val tempAudioList = ArrayList<AllSongsModel>()

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

                playListModel = convertStringTpPlaylistModel(it)
                binding!!.titleNameTV.text = "${playListModel!!.playlistName}"
                binding!!.artisNameTVToolbar.text = "${playListModel!!.playlistName}"

                setUpCustomPlaylistRecyclerAdapter()
                observerCustomPlaylistAudio()

                //getAudioArt(tempAudioList)

            }
        })

        binding?.arrowBackIV?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.arrowBack?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.playBtn?.setOnClickListener {
            onClickAudio(customPlaylist[0], 0)
        }

        binding?.sortedTextTV?.setOnClickListener {
            sortAudios()
        }

        return view
    }

    private fun sortAudios() {
        val bottomSheetSortByOptions =
            BottomSheetSortBy(activity as Context, "playlistAudios", playListModel?.playlistName!!)
        bottomSheetSortByOptions.show(
            (context as AppCompatActivity).supportFragmentManager,
            "bottomSheetSortByOptions"
        )

        bottomSheetSortByOptions.listener = object : BottomSheetSortBy.OnSortingAudio {
            override fun byName() {
                storage.saveAudioSortingMethod(playListModel?.playlistName!!, "Name")
                val sortedBySongName =
                    customPlaylist.sortedBy { allSongsModel -> allSongsModel.songName }

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
                storage.saveAudioSortingMethod(playListModel?.playlistName!!, "ArtistName")
                val sortedByArtistName =
                    customPlaylist.sortedBy { allSongsModel -> allSongsModel.artistsName }
                setUpCustomPlaylistRecyclerAdapter()
                binding?.rvCustomPlaylist?.alpha = 0.0f
                customPlaylistAdapter!!.submitList(sortedByArtistName)
                customPlaylist.clear()
                customPlaylist.addAll(sortedByArtistName)
                animateRecyclerView()
                binding?.sortedTextTV?.text = "Artist Name"
                bottomSheetSortByOptions.dismiss()
            }

            /*override fun defaultOrder() {
                storage.saveFavAudioSortingMethod("defaultOrder")
                val sortedByFavAddedDate =
                    customPlaylist.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime }

                setUpFavRecyclerAdapter()
                binding?.rvFavSongs?.alpha = 0.0f
                customPlaylistAdapter!!.submitList(sortedByFavAddedDate)
                customPlaylist.clear()
                customPlaylist.addAll(sortedByFavAddedDate)
                animateRecyclerView()
                bottomSheetSortByOptions.dismiss()
                binding?.sortedTextTV?.text = "Default"
            }*/

            override fun byDate() {
                storage.saveAudioSortingMethod(playListModel?.playlistName!!, "DateAdded")
                val sortedByDateAdded =
                    customPlaylist.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }

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
                })
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
                val playlistAudios = mViewModelClass.getPlaylistAudios(playListModel?.id!!)
                val songIdsListString: String = playlistAudios[0].songIds
                if (songIdsListString != "") {

                    val songIdsList = convertStringToList(songIdsListString)

                    val audio = mViewModelClass.getRangeOfPlaylistAudio(songIdsList)

                    if (audio.isNotEmpty()) {
                        customPlaylist.clear()
                        //customPlaylist.addAll(it)
                        //customPlaylistAdapter?.submitList(it)
                        //tempAudioList.clear()
                        tempAudioList.addAll(audio.sortedByDescending { allSongsModel ->
                            allSongsModel.songName
                        })


                        (activity as AppCompatActivity).runOnUiThread {

                            val sortedList: List<AllSongsModel>
                            when (storage.getAudioSortedValue(playListModel?.playlistName!!)) {
                                "Name" -> {
                                    sortedList =
                                        audio.sortedBy { allSongsModel -> allSongsModel.songName }
                                    customPlaylist.addAll(sortedList)
                                    customPlaylistAdapter!!.submitList(audio.sortedBy { allSongsModel -> allSongsModel.songName })
                                    binding?.sortedTextTV?.text = "Name"
                                }
                                "DateAdded" -> {
                                    sortedList =
                                        audio.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
                                    customPlaylist.addAll(sortedList)
                                    customPlaylistAdapter!!.submitList(audio.sortedByDescending { allSongsModel -> allSongsModel.dateAdded })
                                    binding?.sortedTextTV?.text = "Date Added"
                                }
                                "ArtistName" -> {
                                    sortedList =
                                        audio.sortedBy { allSongsModel -> allSongsModel.artistsName }
                                    customPlaylist.addAll(sortedList)
                                    customPlaylistAdapter!!.submitList(audio.sortedBy { allSongsModel -> allSongsModel.artistsName })
                                    binding?.sortedTextTV?.text = "Artist Name"
                                }
                                else -> {
                                    sortedList =
                                        audio.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime }
                                    customPlaylist.addAll(sortedList)
                                    customPlaylistAdapter!!.submitList(audio.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime })
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

                            if (tempAudioList.isNotEmpty()) {
                                binding?.rlNoSongsPresent?.visibility = View.GONE
                                binding?.motionLayoutPlayListAudios?.visibility =
                                    View.VISIBLE
                                binding?.noSongDescription?.visibility = View.GONE
                                val factory =
                                    DrawableCrossFadeFactory.Builder()
                                        .setCrossFadeEnabled(true)
                                        .build()
                                Glide.with(binding?.coverArtistImage!!)
                                    .load(customPlaylist[0].artUri)
                                    .transition(DrawableTransitionOptions.withCrossFade(factory))
                                    .apply(RequestOptions.placeholderOf(R.drawable.audio_icon_placeholder))
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

    private fun getAudioArt(
        tempAudioList: ArrayList<AllSongsModel>
    ) {

        if (tempAudioList.isNotEmpty()) {
            for ((index, audio) in customPlaylist.withIndex()) {
                for (prevAudio in tempAudioList) {
                    if (audio.songId != prevAudio.songId) {
                        newAudioArtIndex = index
                        newAudioAddedModel = audio
                        break
                    }
                }
            }
            if (customPlaylist.containsAll(tempAudioList)) {
                Log.d("audioListaaaaa", "getAudioArt: true ")
            } else {
                Log.d("audioListaaaaa", "getAudioArt: false ")
            }

            (activity as AppCompatActivity).runOnUiThread{
                Toast.makeText(
                    activity as Context,
                    "${newAudioAddedModel.songName}",
                    Toast.LENGTH_SHORT
                ).show()
                val indexOf = customPlaylist.indexOf(newAudioAddedModel)
                val factory =
                    DrawableCrossFadeFactory.Builder()
                        .setCrossFadeEnabled(true)
                        .build()
                Glide.with(binding?.coverArtistImage!!)
                    .load(customPlaylist[indexOf].artUri)
                    .transition(DrawableTransitionOptions.withCrossFade(factory))
                    .apply(RequestOptions.placeholderOf(R.drawable.audio_icon_placeholder))
                    .into(binding?.coverArtistImage!!)
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
        storage.saveIsShuffled(false)
        val prevPlayingAudioIndex = storage.loadAudioIndex()
        val prevQueueList = storage.loadQueueAudio()
        val prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]

        Log.d(
            "PlayListAudios111s",
            "onClickAudio: allSongModel $allSongModel ,  favAudioList $customPlaylist "
        )
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

        playAudio(customPlaylist.indexOf(allSongModel))

        // adding queue list to DB and show highlight of current audio
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
                audio.audioUri,
                audio.artUri,
                -1,
                audio.dateAdded,
                audio.isFavourite,
                audio.favAudioAddedTime,
                audio.mostPlayedCount
            )
            queueListModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
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

    private fun playAudio(audioIndex: Int) {
        this.currentPlayingAudioIndex = audioIndex
        //store audio to prefs

        storage.storeQueueAudio(customPlaylist)
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
}
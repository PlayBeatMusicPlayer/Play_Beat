package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentPlayListAudiosBinding
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList

class PlayListAudiosFragment : Fragment() {

    private var currentPlayingAudioIndex = 0
    private var _binding: FragmentPlayListAudiosBinding? = null
    private val binding get() = _binding
    private var allSongsAdapter: AllSongsAdapter? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var favAudioList = ArrayList<AllSongsModel>()
    private var isRvAnimated = false
    private lateinit var storage: StorageUtil

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

        setUpRecyclerView()
        observeFavouriteAudio()

        binding?.arrowBackIV?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.playBtn?.setOnClickListener {
            onClickAudio(favAudioList[0], 0)
        }

        binding?.sortIV?.setOnClickListener {
            sortAudios()
        }

        return view
    }

    private fun sortAudios() {
        when (storage.getFavAudioSortedValue()) {
            "defaultOrder" -> {
                binding?.sortedTextTV?.text = "Default"
            }
            "Name" -> {
                binding?.sortedTextTV?.text = "Name"
            }
            "DateAdded" -> {
                binding?.sortedTextTV?.text = "Date Added"
            }
            "ArtistName" -> {
                binding?.sortedTextTV?.text = "Artist Name"
            }
            else -> binding?.sortedTextTV?.text = "Default"
        }
        val bottomSheetSortByOptions = BottomSheetSortBy(activity as Context, "favourites")
        bottomSheetSortByOptions.show(
            (context as AppCompatActivity).supportFragmentManager,
            "bottomSheetSortByOptions"
        )
        bottomSheetSortByOptions.listener = object : BottomSheetSortBy.OnSortingAudio {
            override fun byName() {
                storage.saveFavAudioSortingMethod("Name")
                val sortedBySongName =
                    favAudioList.sortedBy { allSongsModel -> allSongsModel.songName }

                setUpRecyclerView()
                binding?.rvTracks?.alpha = 0.0f
                allSongsAdapter!!.submitList(sortedBySongName)
                favAudioList.clear()
                favAudioList.addAll(sortedBySongName)
                animateRecyclerView()
                bottomSheetSortByOptions.dismiss()
                binding?.sortedTextTV?.text = "Name"
            }

            override fun byArtistName() {
                storage.saveFavAudioSortingMethod("ArtistName")
                val sortedByArtistName =
                    favAudioList.sortedBy { allSongsModel -> allSongsModel.artistsName }
                setUpRecyclerView()
                binding?.rvTracks?.alpha = 0.0f
                allSongsAdapter!!.submitList(sortedByArtistName)
                favAudioList.clear()
                favAudioList.addAll(sortedByArtistName)
                animateRecyclerView()
                binding?.sortedTextTV?.text = "Artist Name"
                bottomSheetSortByOptions.dismiss()
            }

            override fun defaultOrder() {
                storage.saveFavAudioSortingMethod("defaultOrder")
                val sortedByFavAddedDate =
                    favAudioList.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime }

                setUpRecyclerView()
                binding?.rvTracks?.alpha = 0.0f
                allSongsAdapter!!.submitList(sortedByFavAddedDate)
                favAudioList.clear()
                favAudioList.addAll(sortedByFavAddedDate)
                animateRecyclerView()
                bottomSheetSortByOptions.dismiss()
                binding?.sortedTextTV?.text = "Default"
            }

            override fun byDate() {
                storage.saveFavAudioSortingMethod("DateAdded")
                val sortedByDateAdded =
                    favAudioList.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }

                setUpRecyclerView()
                binding?.rvTracks?.alpha = 0.0f
                allSongsAdapter!!.submitList(sortedByDateAdded)
                favAudioList.clear()
                favAudioList.addAll(sortedByDateAdded)
                animateRecyclerView()
                bottomSheetSortByOptions.dismiss()
                binding?.sortedTextTV?.text = "Date Added"
            }
        }
    }

    private fun setUpRecyclerView() {
        allSongsAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position)
                })
        allSongsAdapter!!.isSearching = false
        binding?.rvTracks?.adapter = allSongsAdapter
        binding!!.rvTracks.itemAnimator = null
        //binding?.rvTracks?.alpha = 0.0f
    }

    private fun onClickAudio(allSongModel: AllSongsModel, position: Int) {
        storage.saveIsShuffled(false)
        val prevPlayingAudioIndex = storage.loadAudioIndex()
        val prevQueueList = storage.loadAudio()
        val prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]

        // restricting to update if clicked audio is same

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

        playAudio(position)

        // adding queue list to DB and show highlight of current audio
        for (audio in this.favAudioList) {
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

    private fun playAudio(audioIndex: Int) {
        this.currentPlayingAudioIndex = audioIndex
        //store audio to prefs
        val searchList = CopyOnWriteArrayList<AllSongsModel>()
        searchList.addAll(favAudioList)
        storage.storeAudio(searchList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }

    private fun observeFavouriteAudio() {
        mViewModelClass.getFavouriteAudios().observe(viewLifecycleOwner) {
            if (it != null) {
                favAudioList.clear()

                val tempFavList = ArrayList<AllSongsModel>()
                tempFavList.addAll(it.sortedByDescending { allSongsModel ->
                    allSongsModel.favAudioAddedTime
                })

                //allSongsAdapter?.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime })

                when (storage.getFavAudioSortedValue()) {
                    "defaultOrder" -> {
                        val sortedList =
                            it.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime }
                        favAudioList.addAll(sortedList)
                        allSongsAdapter!!.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime })
                        binding?.sortedTextTV?.text = "Default"
                        Log.d("sortedListObserved", "observeAudioData:$sortedList ")
                    }
                    "Name" -> {
                        val sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                        favAudioList.addAll(sortedList)
                        allSongsAdapter!!.submitList(it.sortedBy { allSongsModel -> allSongsModel.songName })
                        binding?.sortedTextTV?.text = "Name"
                    }
                    "DateAdded" -> {
                        val sortedList =
                            it.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
                        favAudioList.addAll(sortedList)
                        allSongsAdapter!!.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.dateAdded })
                        binding?.sortedTextTV?.text = "Date Added"
                    }
                    "ArtistName" -> {
                        val sortedList =
                            it.sortedBy { allSongsModel -> allSongsModel.artistsName }
                        favAudioList.addAll(sortedList)
                        allSongsAdapter!!.submitList(it.sortedBy { allSongsModel -> allSongsModel.artistsName })
                        binding?.sortedTextTV?.text = "Artist Name"
                    }
                    else -> {
                        val sortedList =
                            it.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime }
                        favAudioList.addAll(sortedList)
                        allSongsAdapter!!.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.favAudioAddedTime })
                        binding?.sortedTextTV?.text = "Default"
                        Log.d("sortedListObserved", "observeAudioData:$sortedList ")
                    }
                }
                if (it.size >= 2) {
                    binding?.totalSongsTV?.text = "${it.size} Songs"
                } else {
                    binding?.totalSongsTV?.text = "${it.size} Song"
                }

                if (tempFavList.isNotEmpty()) {
                    Glide.with(binding?.coverArtistImage!!).load(tempFavList[0].artUri)
                        .into(binding?.coverArtistImage!!)
                } else {
                    // show No Fav layout todo
                }
            } else {
                binding?.totalSongsTV?.text = "0 Song"
            }
        }
    }

    private fun animateRecyclerView() {
        binding?.rvTracks!!.animate()
            .translationY(-10f)
            //translationYBy(30f)
            .alpha(1.0f)
            .setListener(null)
    }
}
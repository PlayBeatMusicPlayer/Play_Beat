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
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


class LastAddedAudioFragment : Fragment() {

    private var _binding: FragmentPlayListAudiosBinding? = null
    private val binding get() = _binding
    private var lastAddedSongsAdapter: AllSongsAdapter? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var lastAddedAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private lateinit var storage: StorageUtil
    private var currentPlayingAudioIndex = 0
    private lateinit var viewModel: DataObservableClass

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
                    "lastAdded" -> {
                        binding!!.rvFavSongs.visibility = View.GONE
                        binding!!.rvHistoryAdded.visibility = View.GONE
                        binding!!.rvLastPlayedAudio.visibility = View.VISIBLE
                        binding!!.sortIV.visibility = View.GONE
                        binding!!.sortedTextTV.visibility = View.GONE
                        binding!!.titleNameTV.text = "Last added"
                        binding!!.artisNameTVToolbar.text = "Last added"
                    }
                }
            }
        })

        setUpLastAddedRecyclerAdapter()
        observeLastAddedAudio()

        binding?.arrowBackIV?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.playBtn?.setOnClickListener {
            onClickAudio(lastAddedAudioList[0], 0)
        }

        return view

    }

    private fun setUpLastAddedRecyclerAdapter() {
        lastAddedSongsAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position)
                })
        lastAddedSongsAdapter!!.isSearching = false
        binding?.rvLastPlayedAudio?.adapter = lastAddedSongsAdapter
        binding!!.rvLastPlayedAudio.itemAnimator = null
    }


    private fun observeLastAddedAudio() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -2)
        mViewModelClass.getLastAddedAudio(cal.timeInMillis.toString()).observe(viewLifecycleOwner) {
            if (it != null) {
                Log.d("lastAddedAudio", "onHiddenChanged: $it")
                lastAddedAudioList.clear()
                val sortedByDescending =
                    it.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }
                lastAddedAudioList.addAll(sortedByDescending)
                lastAddedSongsAdapter!!.submitList(sortedByDescending)
                binding?.rvFavSongs?.scrollToPosition(0)

                if (it.size >= 2) {
                    binding?.totalSongsTV?.text = "${it.size} Songs"
                } else {
                    binding?.totalSongsTV?.text = "${it.size} Song"
                }

                if (lastAddedAudioList.isNotEmpty()) {
                    Glide.with(binding?.coverArtistImage!!).load(lastAddedAudioList[0].artUri)
                        .into(binding?.coverArtistImage!!)
                } else {
                    // show No Fav layout todo
                }
            }

        }


    }

    private fun onClickAudio(
        allSongModel: AllSongsModel,
        position: Int
    ) {
        storage.saveIsShuffled(false)
        val prevPlayingAudioIndex = storage.loadAudioIndex()
        val prevQueueList = storage.loadAudio()
        val prevPlayingAudioModel = prevQueueList[prevPlayingAudioIndex]

        Log.d(
            "lastAddedAudioList11",
            "onClickAudio: allSongModel $allSongModel ,  lastAddedAudioList $lastAddedAudioList "
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

        playAudio(lastAddedAudioList.indexOf(allSongModel))

        // adding queue list to DB and show highlight of current audio
        for (audio in lastAddedAudioList) {
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

        storage.storeAudio(lastAddedAudioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }


}
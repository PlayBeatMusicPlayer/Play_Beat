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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentPlayListAudiosBinding
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList

class MostPlayedFragment : Fragment() {

    private var currentPlayingAudioIndex = 0
    private var _binding: FragmentPlayListAudiosBinding? = null
    private val binding get() = _binding
    private var mostPlayedAudioAdapter: AllSongsAdapter? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var mostPlayedAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private lateinit var storage: StorageUtil
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
                    "mostPlayed" -> {
                        binding!!.rvFavSongs.visibility = View.GONE
                        binding!!.rvHistoryAdded.visibility = View.GONE
                        binding!!.rvLastPlayedAudio.visibility = View.GONE
                        binding!!.rvMostPlayed.visibility = View.VISIBLE
                        //binding!!.sortIV.visibility = View.GONE
                        binding!!.sortedTextTV.visibility = View.GONE
                        binding!!.rvCustomPlaylist.visibility = View.GONE
                        binding!!.titleNameTV.text = "Most played"
                        binding!!.artisNameTVToolbar.text = "Most played"
                    }
                }
            }
        })

        setUpMostPlayedAudioRecyclerAdapter()
        observeMostPlayedAudio()

        binding?.arrowBackIV?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.arrowBack?.setOnClickListener {
            (activity as AppCompatActivity).onBackPressed()
        }

        binding?.playBtn?.setOnClickListener {
            onClickAudio(mostPlayedAudioList[0], 0)
        }

        return view
    }


    private fun setUpMostPlayedAudioRecyclerAdapter() {
        mostPlayedAudioAdapter =
            AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position)
                })
        mostPlayedAudioAdapter!!.isSearching = false
        binding?.rvMostPlayed?.adapter = mostPlayedAudioAdapter
    }

    private fun observeMostPlayedAudio() {
        mViewModelClass.getMostPlayedAudio().observe(viewLifecycleOwner) {
            if (it != null) {
                mostPlayedAudioList.clear()

                mostPlayedAudioList.addAll(it.sortedByDescending { allSongsModel -> allSongsModel.mostPlayedCount })
                mostPlayedAudioAdapter!!.submitList(it.sortedByDescending { allSongsModel -> allSongsModel.mostPlayedCount })
                binding?.rvMostPlayed?.scrollToPosition(0)

                if (it.size >= 2) {
                    binding?.totalSongsTV?.text = "${mostPlayedAudioList.size} Songs"
                } else {
                    binding?.totalSongsTV?.text = "${mostPlayedAudioList.size} Song"
                }

                if (mostPlayedAudioList.isNotEmpty()) {
                    val factory =
                        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                    binding?.rlNoSongsPresent?.visibility = View.GONE
                    binding?.motionLayoutPlayListAudios?.visibility = View.VISIBLE
                    binding?.noSongDescription?.visibility = View.GONE
                    Glide.with(binding?.coverArtistImage!!).load(mostPlayedAudioList[0].artUri)
                        .transition(withCrossFade(factory))
                        .apply(
                            RequestOptions.placeholderOf(R.drawable.audio_icon_placeholder)
                                .centerCrop()
                        )
                        .into(binding?.coverArtistImage!!)
                } else {
                    // no audio present
                    binding?.rlNoSongsPresent?.visibility = View.VISIBLE
                    binding?.motionLayoutPlayListAudios?.visibility = View.GONE
                    binding?.noSongDescription?.visibility = View.GONE
                    binding?.image?.setImageResource(R.drawable.music_note_icon)
                }
            }
        }
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
            "onClickAudio: allSongModel $allSongModel ,  mostPlayedAudioList $mostPlayedAudioList "
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

        playAudio(mostPlayedAudioList.indexOf(allSongModel))

        // adding queue list to DB and show highlight of current audio
        for (audio in this.mostPlayedAudioList) {
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

        storage.storeQueueAudio(mostPlayedAudioList)
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

    }

    private fun animateRecyclerView() {
        binding?.rvMostPlayed!!.animate()
            .translationY(-10f)
            //translationYBy(30f)
            .alpha(1.0f)
            .setListener(null)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        /* if (!hidden) {
             if (!storage.getIsAudioPlayedFirstTime()) {
                 val audiosList = storage.loadQueueAudio()
                 if (audiosList.isNotEmpty()) {
                     for (audios in audiosList) {
                         if (audios.mostPlayedCount != 0) {
                             mViewModelClass.updateMostPlayedAudioCount(
                                 audios.songId,
                                 audios.mostPlayedCount,
                                 lifecycleScope
                             )
                             Log.d("MostPlayedAudio1111", "onReceive:  ${audios.songName} , ${audios.mostPlayedCount} ")
                         }
                     }
                 }
             }
         }*/
    }

}
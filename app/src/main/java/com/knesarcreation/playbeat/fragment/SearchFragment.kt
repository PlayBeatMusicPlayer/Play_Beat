package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.QueueListModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentSearchBinding
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var audioSearchList = ArrayList<AllSongsModel>()
    private var allSongsAdapter: AllSongsAdapter? = null
    private lateinit var storageUtil: StorageUtil
    private var currentPlayingAudioIndex = -1
    private lateinit var mViewModelClass: ViewModelClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
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
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val view = binding?.root

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        storageUtil = StorageUtil(activity as Context)
        // val list = storageUtil.loadAudio()
        //audioList.addAll(list)
        currentPlayingAudioIndex = storageUtil.loadAudioIndex()

        observeAudioData()
        binding?.searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true

            override fun onQueryTextChange(newText: String?): Boolean {
                audioSearchList.clear()
                if (newText != null) {
                    binding?.rvSearchList?.visibility = View.VISIBLE
                    binding?.searchLottie?.visibility = View.GONE
                    val userInput = newText.lowercase()
                    audioSearchList.clear()
                    for (audio in audioList) {
                        if (audio.songName.lowercase().contains(userInput)) {
                            if (!audioSearchList.contains(audio)) {
                                audioSearchList.add(audio)
                            }
                        }
                    }
                    allSongsAdapter = AllSongsAdapter(activity as Context,
                        AllSongsAdapter.OnClickListener { allSongModel, position ->
                            onClickAudio(allSongModel, position)
                        })
                    allSongsAdapter?.isSearching = true
                    allSongsAdapter?.queryText = userInput
                    binding?.rvSearchList?.adapter = allSongsAdapter
                    allSongsAdapter?.submitList(audioSearchList.sortedBy { allSongsModel -> allSongsModel.songName })
                }
                if (newText?.length == 0) {
                    audioSearchList.clear()
                    allSongsAdapter?.submitList(audioSearchList.sortedBy { allSongsModel -> allSongsModel.songName })
                    binding?.rvSearchList?.visibility = View.GONE
                    binding?.searchLottie?.visibility = View.VISIBLE
                }
                return true
            }
        })

        return view
    }

    private fun observeAudioData() {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner) {
            if (it != null) {
                audioList.clear()
                val sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                audioList.addAll(sortedList)

                val newSearchList = ArrayList<AllSongsModel>()
                if (audioSearchList.isNotEmpty()) {
                    for (searchAudio in audioSearchList) {
                        for (audio in audioList) {
                            if (searchAudio.songId == audio.songId) {
                                newSearchList.add(audio)
                            }
                        }
                    }
                    allSongsAdapter?.submitList(newSearchList.sortedBy { allSongsModel -> allSongsModel.songName })
                }
            }
        }
    }

    private fun onClickAudio(allSongModel: AllSongsModel, position: Int) {
        Toast.makeText(activity as Context, "$position", Toast.LENGTH_SHORT).show()

        storageUtil.saveIsShuffled(false)
        val prevPlayingAudioIndex = storageUtil.loadAudioIndex()
        val prevQueueList = storageUtil.loadAudio()
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
        for (audio in this.audioSearchList) {
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
        val searchList = CopyOnWriteArrayList<AllSongsModel>()
        searchList.addAll(audioSearchList)
        storageUtil.storeAudio(searchList)
        //Store the new audioIndex to SharedPreferences
        storageUtil.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            audioSearchList.clear()
            allSongsAdapter?.submitList(audioSearchList.sortedBy { allSongsModel -> allSongsModel.songName })
            binding?.rvSearchList?.visibility = View.GONE
            binding?.searchLottie?.visibility = View.VISIBLE
            //binding?.searchView?.setIconifiedByDefault(false);
//            binding?.searchView?.isFocusable = false
//            binding?.searchView?.isIconified = false
            //binding?.searchView?.isFocusedByDefault = false
        } else {
//            binding?.searchView?.setIconifiedByDefault(false);
            //binding?.searchView?.isFocusable = true
//            binding?.searchView?.isIconified = false
            //binding?.searchView?.requestFocusFromTouch()
            // binding?.searchView?.isFocusedByDefault = true
        }
    }

}
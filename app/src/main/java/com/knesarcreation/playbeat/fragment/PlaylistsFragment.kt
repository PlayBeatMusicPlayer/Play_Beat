package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.knesarcreation.playbeat.adapter.PlaylistAdapter
import com.knesarcreation.playbeat.database.PlaylistModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.FragmentPlaylistsBinding
import com.knesarcreation.playbeat.utils.StorageUtil

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding
    private var listener: OnPlayListCategoryClicked? = null
    private var playListAdapter: PlaylistAdapter? = null
    private lateinit var mViewModelClass: ViewModelClass
    private var playList = ArrayList<PlaylistModel>()
    private lateinit var storage: StorageUtil

    interface OnPlayListCategoryClicked {
        fun playlistCategory(category: String, mode: Int)
    }

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
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        val view = binding?.root

        storage = StorageUtil(activity as Context)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        binding?.favButton?.setOnClickListener {
            listener?.playlistCategory("fav", 0)
        }

        binding?.lastAddedBtn?.setOnClickListener {
            listener?.playlistCategory("lastAdded", 0)
        }

        binding?.historyBtn?.setOnClickListener {
            listener?.playlistCategory("history", 0)
        }

        binding?.mostPlayedBtn?.setOnClickListener {
            listener?.playlistCategory("mostPlayed", 0)
        }

        binding?.fabCreatePlaylist?.setOnClickListener {
            val bottomSheetCreatePlaylist = BottomSheetCreatePlaylist(activity as Context, "")
            bottomSheetCreatePlaylist.show(childFragmentManager, "bottomSheetCreatePlaylist")
        }

        setUpPlaylistRecyclerView()
        observerPlayList()
        sortPlayList()


        return view
    }

    private fun sortPlayList() {
        /*when (storage.getAudioSortedValue(StorageUtil.PLAYLIST_AUDIO_KEY)) {
            "Name" -> {
                binding?.sortPlaylistTV?.text = "Name"
            }
            "DateAdded" -> {
                binding?.sortPlaylistTV?.text = "Date Added"
            }

            else -> binding?.sortPlaylistTV?.text = "Name"
        }*/

        binding?.sortPlaylist?.setOnClickListener {
            val bottomSheetSortByOptions = BottomSheetSortBy(activity as Context, "playlist","")
            bottomSheetSortByOptions.show(
                (context as AppCompatActivity).supportFragmentManager,
                "bottomSheetSortByOptions"
            )
            bottomSheetSortByOptions.listener = object : BottomSheetSortBy.OnSortingAudio {
                override fun byName() {
                    storage.saveAudioSortingMethod(StorageUtil.PLAYLIST_KEY, "Name")
                    val sortedBySongName =
                        playList.sortedBy { playlistModel -> playlistModel.playlistName }

                    setUpPlaylistRecyclerView()
                    binding?.rvPlaylist?.alpha = 0.0f
                    playListAdapter!!.submitList(sortedBySongName)
                    playList.clear()
                    playList.addAll(sortedBySongName)
                    animateRecyclerView()
                    bottomSheetSortByOptions.dismiss()
                    binding?.sortPlaylistTV?.text = "Name"
                }


                override fun byDate() {
                    storage.saveAudioSortingMethod(StorageUtil.PLAYLIST_KEY, "DateAdded")
                    val sortedByDateAdded =
                        playList.sortedByDescending { allSongsModel -> allSongsModel.dateAdded }

                    setUpPlaylistRecyclerView()
                    binding?.rvPlaylist?.alpha = 0.0f
                    playListAdapter!!.submitList(sortedByDateAdded)
                    playList.clear()
                    playList.addAll(sortedByDateAdded)
                    animateRecyclerView()
                    bottomSheetSortByOptions.dismiss()
                    binding?.sortPlaylistTV?.text = "Date Added"
                }
            }
        }
    }

    private fun observerPlayList() {
        mViewModelClass.getAllPlaylists().observe(viewLifecycleOwner) {
            if (it != null) {
                playList.clear()

                val sortedList: List<PlaylistModel>
                when (storage.getAudioSortedValue(StorageUtil.PLAYLIST_KEY)) {

                    "Name" -> {
                        sortedList = it.sortedBy { playlistModel -> playlistModel.playlistName }
                        playList.addAll(sortedList)
                        playListAdapter!!.submitList(it.sortedBy { playlistModel -> playlistModel.playlistName })
                        binding?.sortPlaylistTV?.text = "Name"
                    }
                    "DateAdded" -> {
                        sortedList =
                            it.sortedByDescending { playlistModel -> playlistModel.dateAdded }
                        playList.addAll(sortedList)
                        playListAdapter!!.submitList(it.sortedByDescending { playlistModel -> playlistModel.dateAdded })
                        binding?.sortPlaylistTV?.text = "Date Added"
                    }

                    else -> {
                        sortedList = it.sortedBy { playlistModel -> playlistModel.playlistName }
                        playList.addAll(sortedList)
                        playListAdapter!!.submitList(it.sortedBy { playlistModel -> playlistModel.playlistName })
                        binding?.sortPlaylistTV?.text = "Name"
                        Log.d("sortedListObserved", "observeAudioData:$sortedList ")
                    }
                }

                if (playList.isNotEmpty()) {
                    binding?.noPlayList?.visibility = View.GONE
                    binding?.rlPlaylistContainer?.visibility = View.VISIBLE
                    binding?.sortPlaylist?.visibility = View.VISIBLE
                } else {
                    binding?.noPlayList?.visibility = View.VISIBLE
                    binding?.rlPlaylistContainer?.visibility = View.GONE
                    binding?.sortPlaylist?.visibility = View.GONE
                }
            }
        }
    }

    private fun setUpPlaylistRecyclerView() {
        playListAdapter = PlaylistAdapter(object : PlaylistAdapter.OnPlaylistClicked {
            override fun onClicked(playlistModel: PlaylistModel) {
                val gson = Gson()
                listener?.playlistCategory(gson.toJson(playlistModel), 1)
            }
        })
        binding?.rvPlaylist?.adapter = playListAdapter
    }

    private fun animateRecyclerView() {
        binding?.rvPlaylist!!.animate()
            .translationY(-10f)
            //translationYBy(30f)
            .alpha(1.0f)
            .setListener(null)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnPlayListCategoryClicked
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

}
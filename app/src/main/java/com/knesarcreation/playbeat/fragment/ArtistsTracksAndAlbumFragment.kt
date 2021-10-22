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
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.adapter.AllAlbumsAdapter
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.adapter.ArtistsAlbumAdapter
import com.knesarcreation.playbeat.database.*
import com.knesarcreation.playbeat.databinding.FragmentArtistsTracksAndAlbumBinding
import com.knesarcreation.playbeat.utils.AudioPlayingFromCategory
import com.knesarcreation.playbeat.utils.CustomProgressDialog
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class ArtistsTracksAndAlbumFragment : Fragment()/*, AllSongsAdapter.OnClickSongItem*/ {

    private var _binding: FragmentArtistsTracksAndAlbumBinding? = null
    private val binding get() = _binding
    private lateinit var viewmodel: DataObservableClass
    private var artistsModel: ArtistsModel? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private val albumList = CopyOnWriteArrayList<AlbumModel>()
    private var progressBar: CustomProgressDialog? = null
    private var listener: OnArtistAlbumItemClicked? = null
    private lateinit var storageUtil: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private lateinit var allSongsAdapter: AllSongsAdapter
    private lateinit var artistsAlbumAdapter: ArtistsAlbumAdapter
    private lateinit var allSongsAdapterMoreTracks: AllSongsAdapter
    private var launchAlumData: Job? = null
    private var isBackPressed = false

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

        storageUtil = StorageUtil(activity as Context)

        viewmodel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        progressBar = CustomProgressDialog(requireContext())
        progressBar!!.setMessage("Please wait...")
        progressBar!!.setIsCancelable(true)
        progressBar!!.setCanceledOnOutsideTouch(true)

        viewmodel.artistsData.observe(viewLifecycleOwner, {
            convertGsonToAlbumModel(it)

            //binding?.rvAlbums?.setHasFixedSize(true)
            //binding?.rvAlbums?.setItemViewCacheSize(20)
            //binding?.rvTracks?.setItemViewCacheSize(20)

            binding?.artisNameTV?.text = artistsModel?.artistName
            binding?.artisNameTVToolbar?.text = artistsModel?.artistName
            binding?.artisNameTVToolbar2?.text = artistsModel?.artistName

            isBackPressed = false

            binding?.motionLayoutArtistAndAlbum?.visibility = View.VISIBLE
            binding?.rlMoreAudios?.visibility = View.GONE
            setUpAlbumAdapter()

            allSongsAdapter =
                AllSongsAdapter(
                    activity as Context,
                    AllSongsAdapter.OnClickListener { allSongModel, position ->
                        onClickAudio(allSongModel, position)
                    },
                    AllSongsAdapter.OnLongClickListener { allSongModel, longClickSelectionEnable ->

                    },
                    false
                )
            allSongsAdapter.isSearching = false
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
            setUpMoreTracksRV()
        }

        return view
    }

    private fun setUpMoreTracksRV() {
        allSongsAdapterMoreTracks = AllSongsAdapter(
            activity as Context,
            AllSongsAdapter.OnClickListener { allSongModel, position ->
                onClickAudio(allSongModel, position)
            }, AllSongsAdapter.OnLongClickListener { allSongModel, longClickSelectionEnable ->

            }, false
        )
        allSongsAdapterMoreTracks.isSearching = false
        binding!!.rvMoreTracks.adapter = allSongsAdapterMoreTracks
        binding!!.rvMoreTracks.itemAnimator = null
        binding!!.rvMoreTracks.scrollToPosition(0)

        getAudioAccordingAlbum(true)
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
                        if (!isBackPressed) {
                            isBackPressed = true
                            (activity as AppCompatActivity).onBackPressed()
                        }
                    }
                    if (moreTrackRV) {
                        allSongsAdapterMoreTracks.submitList(audioArtist.sortedBy { allSongsModel -> allSongsModel.songName })
                    } else {
                        allSongsAdapter.submitList(audioArtist.sortedBy { allSongsModel -> allSongsModel.songName })
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
        storageUtil.saveIsShuffled(false)

        val prevPlayingAudioIndex = storageUtil.loadAudioIndex()
        val audioList = storageUtil.loadQueueAudio()
        var prevPlayingAudioModel: AllSongsModel? = null
        var restrictToUpdateAudio = false

        if (audioList.isNotEmpty()) {
            prevPlayingAudioModel = audioList[prevPlayingAudioIndex]
            restrictToUpdateAudio = allSongModel.songId == prevPlayingAudioModel.songId

            if (storageUtil.getIsAudioPlayedFirstTime()) {
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

        storageUtil.storeQueueAudio(audioList)
        storageUtil.storeAudioIndex(position)

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
}
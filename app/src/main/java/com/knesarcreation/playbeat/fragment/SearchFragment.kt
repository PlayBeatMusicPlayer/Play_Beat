package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.google.gson.Gson
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllAlbumsAdapter
import com.knesarcreation.playbeat.adapter.AllArtistsAdapter
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.database.*
import com.knesarcreation.playbeat.databinding.FragmentSearchBinding
import com.knesarcreation.playbeat.utils.SavedAppTheme
import com.knesarcreation.playbeat.utils.StorageUtil
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding
    private var onAlbumItemClickListener: AllAlbumsFragment.OnAlbumItemClicked? = null
    private var openArtistFragmentListener: AllArtistsFragment.OpenArtisFragment? = null
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var albumList = CopyOnWriteArrayList<AlbumModel>()
    private var artistList = CopyOnWriteArrayList<ArtistsModel>()
    private var audioSearchList = ArrayList<AllSongsModel>()
    private var albumSearchList = ArrayList<AlbumModel>()
    private var artistSearchList = ArrayList<ArtistsModel>()
    private var allSongsAdapter: AllSongsAdapter? = null
    private var allAlbumAdapter: AllAlbumsAdapter? = null
    private var allArtistsAdapter: AllArtistsAdapter? = null
    private lateinit var storageUtil: StorageUtil
    private var currentPlayingAudioIndex = -1
    private lateinit var mViewModelClass: ViewModelClass
    private var userInput = ""
    //private var doClearSearchResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
            duration = 200L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
            duration = 200L
        }

    }

    /* private fun initializeAddMob() {
         val adBanner = AdBanner(activity as Context, binding!!.adView_container)
         adBanner.initializeAddMob()
     }*/

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val view = binding?.root

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        binding?.noSearchResultTV?.visibility = View.GONE
        binding?.llNoSearchResult?.visibility = View.VISIBLE
        binding?.rvSearchList?.visibility = View.GONE
        binding?.llSearchResultCount?.visibility = View.GONE

        handleSearchFilterButtons()

        storageUtil = StorageUtil(activity as Context)
        // val list = storageUtil.loadAudio()
        //audioList.addAll(list)
        currentPlayingAudioIndex = storageUtil.loadAudioIndex()


        //initializeAddMob()

        observeAudioData()
        observerAlbumsData()
        observeAudioArtistData()

        binding?.searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true

            override fun onQueryTextChange(newText: String?): Boolean {
                // clear search result when navigate to next fragments
                //doClearSearchResult = true

                audioSearchList.clear()
                albumSearchList.clear()
                artistSearchList.clear()
                if (newText != null) {
                    // transition to start
                    if (binding?.rvSearchList!!.scrollState == 0)
                        binding?.motionLayoutSearchFrag?.jumpToState(R.id.start)

                    binding?.rvSearchList?.visibility = View.VISIBLE
                    binding?.llSearchResultCount?.visibility = View.VISIBLE
                    binding?.llNoSearchResult?.visibility = View.GONE
                    binding?.noSearchResultTV?.visibility = View.VISIBLE
                    userInput = newText.lowercase()
                    audioSearchList.clear()
                    albumSearchList.clear()
                    artistSearchList.clear()
                    for (audio in audioList) {
                        if (audio.songName.lowercase().contains(userInput)) {
                            if (!audioSearchList.contains(audio)) {
                                audioSearchList.add(audio)
                            }
                        }
                    }

                    for (album in albumList) {
                        if (album.albumName.lowercase().contains(userInput)) {
                            if (!albumSearchList.contains(album)) {
                                albumSearchList.add(album)
                            }
                        }
                    }

                    for (artist in artistList) {
                        if (artist.artistName.lowercase().contains(userInput)) {
                            if (!artistSearchList.contains(artist)) {
                                artistSearchList.add(artist)
                            }
                        }
                    }

                    when {
                        binding?.songsFilterButton?.isChecked!! -> {
                            showSongSearchedResult()
                        }
                        binding?.albumsFilterButton?.isChecked!! -> {
                            showAlbumSearchedResult()
                        }
                        binding?.artistsFilterButton?.isChecked!! -> {
                            showArtistSearchedResult()
                        }
                    }

                }
                if (newText?.length == 0) {
                    clearSearchResult()
                    binding?.llNoSearchResult?.visibility = View.VISIBLE
                    binding?.noSearchResultTV?.visibility = View.VISIBLE
                }
                return true
            }
        })

        return view
    }

    private fun handleSearchFilterButtons() {
        binding?.allFilterButton?.visibility = View.GONE

        //Default Songs filter btn will be checked
        binding?.songsFilterButton!!.isChecked = true
        binding?.songsFilterButton!!.strokeWidth = 0

        /*binding?.allFilterButton?.setOnClickListener {
            uncheckAllFilterButton()
            binding?.allFilterButton!!.isChecked = true
            binding?.allFilterButton!!.strokeWidth = 0
        }*/

        binding?.songsFilterButton?.setOnClickListener {
            //binding?.motionLayoutSearchFrag?.transitionToStart()
            binding?.motionLayoutSearchFrag?.jumpToState(R.id.start)

            uncheckAllFilterButton()
            clearSearchResult()
            binding?.songsFilterButton!!.isChecked = true
            binding?.songsFilterButton!!.strokeWidth = 0

            if (userInput.trim() != "") {
                showSongSearchedResult()
            }
        }

        binding?.albumsFilterButton?.setOnClickListener {
            //binding?.motionLayoutSearchFrag?.transitionToStart()
            binding?.motionLayoutSearchFrag?.jumpToState(R.id.start)

            uncheckAllFilterButton()
            clearSearchResult()
            binding?.albumsFilterButton!!.isChecked = true
            binding?.albumsFilterButton!!.strokeWidth = 0

            if (userInput.trim() != "") {
                showAlbumSearchedResult()
            }
        }

        binding?.artistsFilterButton?.setOnClickListener {
            // binding?.motionLayoutSearchFrag?.transitionToStart()
            binding?.motionLayoutSearchFrag?.jumpToState(R.id.start)

            uncheckAllFilterButton()
            clearSearchResult()
            binding?.artistsFilterButton!!.isChecked = true
            binding?.artistsFilterButton!!.strokeWidth = 0
            if (userInput.trim() != "") {
                showArtistSearchedResult()
            }
        }
    }

    private fun uncheckAllFilterButton() {
        for (i in 0..4) {
            binding?.allFilterButton!!.isChecked = false
            binding?.allFilterButton!!.strokeColor = ColorStateList.valueOf(Color.GRAY)
            binding?.allFilterButton!!.strokeWidth = 3

            binding?.songsFilterButton!!.isChecked = false
            binding?.songsFilterButton!!.strokeColor = ColorStateList.valueOf(Color.GRAY)
            binding?.songsFilterButton!!.strokeWidth = 3

            binding?.albumsFilterButton!!.isChecked = false
            binding?.albumsFilterButton!!.strokeColor = ColorStateList.valueOf(Color.GRAY)
            binding?.albumsFilterButton!!.strokeWidth = 3

            binding?.artistsFilterButton!!.isChecked = false
            binding?.artistsFilterButton!!.strokeColor = ColorStateList.valueOf(Color.GRAY)
            binding?.artistsFilterButton!!.strokeWidth = 3
        }
    }

    private fun showAlbumSearchedResult() {
        binding?.iconIV!!.setImageResource(R.drawable.ic_album_teal)
        binding?.titleTV!!.text = "${albumSearchList.size} Albums"

        if (albumSearchList.size > 0) {
            binding?.llSearchResultCount?.visibility = View.VISIBLE
            binding?.rvSearchList?.visibility = View.VISIBLE
            binding?.llNoSearchResult?.visibility = View.GONE
            allAlbumAdapter = AllAlbumsAdapter(
                activity as Context,
                /* albumList,*/
                object : AllAlbumsAdapter.OnAlbumClicked {
                    override fun onClicked(albumModel: AlbumModel) {
                        val gson = Gson()
                        val album = gson.toJson(albumModel)
                        // doClearSearchResult = false
                        onAlbumItemClickListener?.openAlbum(album, false)
                    }
                })
            binding?.rvSearchList!!.adapter = allAlbumAdapter
            allAlbumAdapter?.queryText = userInput
            allAlbumAdapter?.isSearching = true
            allAlbumAdapter?.submitList(albumSearchList.sortedBy { albumModel -> albumModel.albumName })
        } else {
            clearSearchResult()
            binding?.llNoSearchResult?.visibility = View.VISIBLE
            binding?.noSearchResultTV?.visibility = View.VISIBLE
        }
    }

    private fun showArtistSearchedResult() {
        binding?.iconIV!!.setImageResource(R.drawable.ic_artist_teal)
        binding?.titleTV!!.text = "${artistSearchList.size} Artists"
        if (artistSearchList.size > 0) {
            binding?.rvSearchList?.visibility = View.VISIBLE
            binding?.llSearchResultCount?.visibility = View.VISIBLE
            binding?.llNoSearchResult?.visibility = View.GONE
            allArtistsAdapter = AllArtistsAdapter(
                activity as Context,
                object : AllArtistsAdapter.OnArtistClicked {
                    override fun getArtistData(artistsModel: ArtistsModel) {
                        val gson = Gson()
                        val artistsData = gson.toJson(artistsModel)
                        openArtistFragmentListener?.onOpenArtistTrackAndAlbumFragment(
                            artistsData,
                            false
                        )
                    }
                }
            )
            allArtistsAdapter?.isSearching = true
            allArtistsAdapter?.queryText = userInput
            binding?.rvSearchList?.adapter = allArtistsAdapter
            allArtistsAdapter?.submitList(artistSearchList.sortedBy { artistModel -> artistModel.artistName })
        } else {
            clearSearchResult()
            binding?.llNoSearchResult?.visibility = View.VISIBLE
            binding?.noSearchResultTV?.visibility = View.VISIBLE
        }
    }

    private fun showSongSearchedResult() {
        binding?.iconIV!!.setImageResource(R.drawable.music_note_icon_teal)
        binding?.titleTV!!.text = "${audioSearchList.size} Songs"

        if (audioSearchList.size > 0) {
            //Toast.makeText(activity as Context, "${audioSearchList.size}", Toast.LENGTH_SHORT)
            //  .show()
            //Log.d("showSongSearchedResult", "showSongSearchedResult: ${audioSearchList.size}")
            binding?.llSearchResultCount?.visibility = View.VISIBLE
            binding?.rvSearchList?.visibility = View.VISIBLE
            binding?.llNoSearchResult?.visibility = View.GONE
            allSongsAdapter = AllSongsAdapter(
                activity as Context,
                AllSongsAdapter.OnClickListener { allSongModel, position ->
                    onClickAudio(allSongModel, position)
                },
                AllSongsAdapter.OnLongClickListener { _, _ ->

                },
                false
            )

            allSongsAdapter?.isSearching = true
            allSongsAdapter?.queryText = userInput
            binding?.rvSearchList!!.adapter = allSongsAdapter
            allSongsAdapter?.submitList(audioSearchList.sortedBy { allSongsModel -> allSongsModel.songName })
        } else {
            clearSearchResult()
            binding?.llNoSearchResult?.visibility = View.VISIBLE
            binding?.noSearchResultTV?.visibility = View.VISIBLE
        }
    }

    private fun clearSearchResult() {
        when {
            binding?.songsFilterButton?.isChecked!! -> {
                audioSearchList.clear()
                allSongsAdapter?.submitList(audioSearchList.sortedBy { allSongsModel -> allSongsModel.songName })
                binding?.rvSearchList?.visibility = View.GONE
                binding?.llSearchResultCount?.visibility = View.GONE
            }
            binding?.albumsFilterButton?.isChecked!! -> {
                albumSearchList.clear()
                allAlbumAdapter?.submitList(albumSearchList.sortedBy { albumModel -> albumModel.albumName })
                binding?.llSearchResultCount?.visibility = View.GONE
                binding?.rvSearchList?.visibility = View.GONE
            }
            binding?.artistsFilterButton?.isChecked!! -> {
                artistSearchList.clear()
                allArtistsAdapter?.submitList(artistSearchList.sortedBy { artistModel -> artistModel.artistName })
                binding?.llSearchResultCount?.visibility = View.GONE
                binding?.rvSearchList?.visibility = View.GONE
            }
        }
    }

    private fun observeAudioData() {
        mViewModelClass.getAllSong().observe(viewLifecycleOwner) {
            if (it != null) {
                audioList.clear()
                val sortedList = it.sortedBy { allSongsModel -> allSongsModel.songName }
                audioList.addAll(sortedList)

                //newSearchList is a local variable just for here
                val newSearchList = ArrayList<AllSongsModel>()
                /** doing this operation to see the changes in recycler view at real time
                1. make a new list
                2. check search result audio present in audio list  if yes add it in newSearchList and submit the list*/
                if (audioSearchList.isNotEmpty()) {
                    for (searchAudio in audioSearchList) {
                        for (audio in audioList) {
                            if (searchAudio.songId == audio.songId) {
                                newSearchList.add(audio)
                            }
                        }
                    }
                    //updating audioSearchList: its a global variable so we need to update this list also
                    audioSearchList.clear()
                    audioSearchList.addAll(newSearchList)

                    //submitting newSearchList for notifying adapter
                    /** NOTE: if we pass same audioSearchList then adapter will not notify , so here i am making newSearch list
                    and adding data to it from audioSearchList and submitting it to adapter */
                    allSongsAdapter?.submitList(newSearchList.sortedBy { allSongsModel -> allSongsModel.songName })
                }
            }
        }
    }

    private fun observerAlbumsData() {
        mViewModelClass.getAlbums().observe(viewLifecycleOwner) {
            if (it != null) {
                albumList.clear()
                val sortedByAlbumName = it.sortedBy { albumModel -> albumModel.albumName }
                albumList.addAll(sortedByAlbumName)
            }
        }
    }

    private fun observeAudioArtistData() {
        mViewModelClass.getAllArtists().observe(viewLifecycleOwner) {
            if (it != null) {
                artistList.clear()
                val sortedArtistList = it.sortedBy { artistsModel -> artistsModel.artistName }
                artistList.addAll(sortedArtistList)
            }
        }
    }

    private fun onClickAudio(allSongModel: AllSongsModel, position: Int) {
        // Toast.makeText(activity as Context, "$position", Toast.LENGTH_SHORT).show()
        if (File(Uri.parse(allSongModel.data).path!!).exists()) {
            storageUtil.saveIsShuffled(false)
            val prevPlayingAudioIndex = storageUtil.loadAudioIndex()
            val prevQueueList = storageUtil.loadQueueAudio()
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
        } else {
            Snackbar.make(
                (activity as AppCompatActivity).window.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
        }

    }

    private fun playAudio(audioIndex: Int) {
        this.currentPlayingAudioIndex = audioIndex
        //store audio to prefs
        val searchList = CopyOnWriteArrayList<AllSongsModel>()
        searchList.addAll(audioSearchList)
        storageUtil.storeQueueAudio(searchList)
        //Store the new audioIndex to SharedPreferences
        storageUtil.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        /*val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)*/
    }

    // @RequiresApi(Build.VERSION_CODES.O)
    //  override fun onHiddenChanged(hidden: Boolean) {
    //     super.onHiddenChanged(hidden)
    //     if (hidden) {
    //if (doClearSearchResult)
    // clearSearchResult()
    //}
    //}

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
            bottomShadowIVAlbumFrag = null,
            isAlbumFrag = false,
            topViewIV = null,
            bottomShadowIVArtistFrag = null,
            isArtistFrag = false,
            topViewIVArtistFrag = null,
            parentViewArtistAndAlbumFrag = null,
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            playlistBG = null,
            isPlaylistFrag = false,
            searchFragBg = binding!!.searchFragBG,
            isSearchFrag = true,
            settingFragBg = null,
            isSettingFrag = false,

            ).settingSavedBackgroundTheme()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onAlbumItemClickListener = context as AllAlbumsFragment.OnAlbumItemClicked
            openArtistFragmentListener = context as AllArtistsFragment.OpenArtisFragment
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }
}
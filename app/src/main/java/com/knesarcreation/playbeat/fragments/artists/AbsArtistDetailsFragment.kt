package com.knesarcreation.playbeat.fragments.artists

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spanned
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.material.transition.MaterialContainerTransform
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.EXTRA_ALBUM_ID
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.album.HorizontalAlbumAdapter
import com.knesarcreation.playbeat.adapter.song.SimpleSongAdapter
import com.knesarcreation.playbeat.databinding.FragmentArtistDetailsBinding
import com.knesarcreation.playbeat.dialogs.AddToPlaylistDialog
import com.knesarcreation.playbeat.extensions.*
import com.knesarcreation.playbeat.fragments.base.AbsMainActivityFragment
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.glide.SingleColorTarget
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.SortOrder
import com.knesarcreation.playbeat.interfaces.IAlbumClickListener
import com.knesarcreation.playbeat.interfaces.ICabCallback
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.model.Artist
import com.knesarcreation.playbeat.network.Result
import com.knesarcreation.playbeat.network.model.LastFmArtist
import com.knesarcreation.playbeat.repository.RealRepository
import com.knesarcreation.playbeat.util.*
import com.knesarcreation.playbeat.util.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import java.util.*

abstract class AbsArtistDetailsFragment : AbsMainActivityFragment(R.layout.fragment_artist_details),
    IAlbumClickListener, ICabHolder {
    private var _binding: FragmentArtistDetailsBinding? = null
    private val binding get() = _binding!!

    abstract val detailsViewModel: ArtistDetailsViewModel
    abstract val artistId: Long?
    abstract val artistName: String?
    private lateinit var artist: Artist
    private lateinit var songAdapter: SimpleSongAdapter
    private lateinit var albumAdapter: HorizontalAlbumAdapter
    private var forceDownload: Boolean = false
    private var lang: String? = null
    private var biography: Spanned? = null
    private lateinit var mView: View
    private val savedSongSortOrder: String
        get() = PreferenceUtil.artistDetailSongSortOrder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(surfaceColor())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentArtistDetailsBinding.bind(view)
        setHasOptionsMenu(true)
        mainActivity.addMusicServiceEventListener(detailsViewModel)
        mainActivity.setSupportActionBar(binding.toolbar)
        binding.toolbar.title = null
        binding.collapsingToolbar!!.transitionName = (artistId ?: artistName).toString()
        this.mView = view
        postponeEnterTransition()
        detailsViewModel.getArtist().observe(viewLifecycleOwner) {
            requireView().doOnPreDraw {
                startPostponedEnterTransition()
            }
            showArtist(it, view)
        }

        when (App.getContext().generalThemeValue) {
            ThemeMode.LIGHT -> binding.shadowUp!!.setImageResource(R.drawable.shadow_up_artist_ligth)

            ThemeMode.DARK -> binding.shadowUp!!.setImageResource(R.drawable.shadow_up_artist_dark)

            ThemeMode.BLACK -> binding.shadowUp!!.setImageResource(R.drawable.shadow_up_artist_just_black)

            ThemeMode.AUTO -> binding.shadowUp!!.setImageResource(R.drawable.shadow_up_artist_follow_system)
        }

        if (PreferenceUtil.materialYou) {
            binding.shadowUp!!.setImageResource(R.drawable.shadow_up_artist_material_you)
        }

        setupRecyclerView()

        binding.fragmentArtistContent.playAction.apply {
            setOnClickListener { MusicPlayerRemote.openQueue(artist.sortedSongs, 0, true) }
        }
        binding.fragmentArtistContent.shuffleAction.apply {
            setOnClickListener { MusicPlayerRemote.openAndShuffleQueue(artist.songs, true) }
        }

        binding.fragmentArtistContent.biographyText.setOnClickListener {
            if (binding.fragmentArtistContent.biographyText.maxLines == 4) {
                binding.fragmentArtistContent.biographyText.maxLines = Integer.MAX_VALUE
            } else {
                binding.fragmentArtistContent.biographyText.maxLines = 4
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!handleBackPress()) {
                remove()
                requireActivity().onBackPressed()
            }
        }
        setupSongSortButton()
        /*binding.appBarLayout?.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())*/
    }

    private fun setupRecyclerView() {
        albumAdapter = HorizontalAlbumAdapter(requireActivity(), ArrayList(), this, this)
        binding.fragmentArtistContent.albumRecyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = GridLayoutManager(this.context, 1, GridLayoutManager.HORIZONTAL, false)
            adapter = albumAdapter
        }
        songAdapter = SimpleSongAdapter(requireActivity(), ArrayList(), R.layout.item_song, this)
        binding.fragmentArtistContent.recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = LinearLayoutManager(this.context)
            adapter = songAdapter
        }
    }

    private fun showArtist(artist: Artist, view: View) {
        if (artist.songCount == 0) {
            findNavController().navigateUp()
            return
        }
        this.artist = artist
        loadArtistImage(artist)
        if (PlayBeatUtil.isAllowedToDownloadMetadata(requireContext())) {
            loadBiography(artist.name)
        }
        binding.collapsingToolbar!!.title = artist.name
        /* view.findViewById<com.knesarcreation.playbeat.views.BaselineGridTextView>(R.id.artistTitle).text =
             artist.name*/
        view.findViewById<com.knesarcreation.playbeat.views.BaselineGridTextView>(R.id.text).text =
            String.format(
                "%s • %s",
                MusicUtil.getArtistInfoString(requireContext(), artist),
                MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(artist.songs))
            )
        /*binding.artistTitle.text = artist.name*/
        /* binding.text.text = String.format(
             "%s • %s",
             MusicUtil.getArtistInfoString(requireContext(), artist),
             MusicUtil.getReadableDurationString(MusicUtil.getTotalDuration(artist.songs))
         )*/
        val songText = resources.getQuantityString(
            R.plurals.albumSongs,
            artist.songCount,
            artist.songCount
        )
        val albumText = resources.getQuantityString(
            R.plurals.albums,
            artist.songCount,
            artist.songCount
        )
        binding.fragmentArtistContent.songTitle.text = songText
        binding.fragmentArtistContent.albumTitle.text = albumText
        songAdapter.swapDataSet(artist.sortedSongs)
        albumAdapter.swapDataSet(artist.albums)
    }

    private fun loadBiography(
        name: String,
        lang: String? = Locale.getDefault().language
    ) {
        biography = null
        this.lang = lang
        detailsViewModel.getArtistInfo(name, lang, null)
            .observe(viewLifecycleOwner) {
                when (it) {
                    is Result.Loading -> println("Loading")
                    is Result.Error -> println("Error")
                    is Result.Success -> artistInfo(it.data)
                }
            }
    }

    private fun artistInfo(lastFmArtist: LastFmArtist?) {
        if (lastFmArtist != null && lastFmArtist.artist != null && lastFmArtist.artist.bio != null) {
            val bioContent = lastFmArtist.artist.bio.content
            if (bioContent != null && bioContent.trim { it <= ' ' }.isNotEmpty()) {
                binding.fragmentArtistContent.run {
                    biographyText.isVisible = true
                    biographyTitle.isVisible = true
                    biography = bioContent.parseAsHtml()
                    biographyText.text = biography
                    if (lastFmArtist.artist.stats.listeners.isNotEmpty()) {
                        listeners.show()
                        listenersLabel.show()
                        scrobbles.show()
                        scrobblesLabel.show()
                        listeners.text =
                            PlayBeatUtil.formatValue(lastFmArtist.artist.stats.listeners.toFloat())
                        scrobbles.text =
                            PlayBeatUtil.formatValue(lastFmArtist.artist.stats.playcount.toFloat())
                    }
                }
            }
        }

        // If the "lang" parameter is set and no biography is given, retry with default language
        if (biography == null && lang != null) {
            loadBiography(artist.name, null)
        }
    }

    private fun loadArtistImage(artist: Artist) {
        /*  GlideApp.with(requireContext()).asBitmapPalette().artistImageOptions(artist)
              .load(PlayBeatGlideExtension.getArtistModel(artist))
              .dontAnimate()
              .into(object : SingleColorTarget(binding.image) {
                  override fun onColorReady(color: Int) {
                      setColors(color)
                  }
              })*/

        GlideApp.with(requireContext()).asBitmapPalette().artistImageOptions(artist)
            .load(PlayBeatGlideExtension.getArtistModel(artist))
            .dontAnimate()
            .into(object : SingleColorTarget(binding.image) {
                override fun onColorReady(color: Int) {
                    setColors(color)
                }
            })
    }

    private fun setColors(color: Int) {
        if (_binding != null) {
            binding.fragmentArtistContent.shuffleAction.applyColor(color)
            binding.fragmentArtistContent.playAction.applyOutlineColor(color)
        }
    }

    override fun onAlbumClick(albumId: Long, view: View) {
        findNavController().navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to albumId),
            null,
            FragmentNavigatorExtras(
                view to albumId.toString()
            )
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return handleSortOrderMenuItem(item)
    }

    private fun handleSortOrderMenuItem(item: MenuItem): Boolean {
        val songs = artist.songs
        when (item.itemId) {
            android.R.id.home -> findNavController().navigateUp()
            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(songs)
                return true
            }
            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(songs)
                return true
            }
            R.id.action_add_to_playlist -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    val playlists = get<RealRepository>().fetchPlaylists()
                    withContext(Dispatchers.Main) {
                        AddToPlaylistDialog.create(playlists, songs)
                            .show(childFragmentManager, "ADD_PLAYLIST")
                    }
                }
                return true
            }
            R.id.action_set_artist_image -> {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.pick_from_local_storage)),
                    REQUEST_CODE_SELECT_IMAGE
                )
                return true
            }
            R.id.action_reset_artist_image -> {
                showToast(resources.getString(R.string.updating))
                CustomArtistImageUtil.getInstance(requireContext()).resetCustomArtistImage(artist)
                forceDownload = true
                return true
            }
        }
        return true
    }

    private fun setupSongSortButton() {
        binding.fragmentArtistContent.songSortOrder.setOnClickListener {
            PopupMenu(requireContext(), binding.fragmentArtistContent.songSortOrder).apply {
                inflate(R.menu.menu_artist_song_sort_order)
                setUpSortOrderMenu(menu)
                setOnMenuItemClickListener { menuItem ->
                    val sortOrder = when (menuItem.itemId) {
                        R.id.action_sort_order_title -> SortOrder.ArtistSongSortOrder.SONG_A_Z
                        R.id.action_sort_order_title_desc -> SortOrder.ArtistSongSortOrder.SONG_Z_A
                        R.id.action_sort_order_album -> SortOrder.ArtistSongSortOrder.SONG_ALBUM
                        R.id.action_sort_order_year -> SortOrder.ArtistSongSortOrder.SONG_YEAR
                        R.id.action_sort_order_song_duration -> SortOrder.ArtistSongSortOrder.SONG_DURATION
                        else -> {
                            throw IllegalArgumentException("invalid ${menuItem.title}")
                        }
                    }
                    menuItem.isChecked = true
                    setSaveSortOrder(sortOrder)
                    return@setOnMenuItemClickListener true
                }
                show()
            }
        }
    }

    private fun setSaveSortOrder(sortOrder: String) {
        PreferenceUtil.artistDetailSongSortOrder = sortOrder
        songAdapter.swapDataSet(artist.sortedSongs)
    }

    private fun setUpSortOrderMenu(sortOrder: Menu) {
        when (savedSongSortOrder) {
            SortOrder.ArtistSongSortOrder.SONG_A_Z -> sortOrder.findItem(R.id.action_sort_order_title).isChecked =
                true
            SortOrder.ArtistSongSortOrder.SONG_Z_A -> sortOrder.findItem(R.id.action_sort_order_title_desc).isChecked =
                true
            SortOrder.ArtistSongSortOrder.SONG_ALBUM ->
                sortOrder.findItem(R.id.action_sort_order_album).isChecked = true
            SortOrder.ArtistSongSortOrder.SONG_YEAR ->
                sortOrder.findItem(R.id.action_sort_order_year).isChecked = true
            SortOrder.ArtistSongSortOrder.SONG_DURATION ->
                sortOrder.findItem(R.id.action_sort_order_song_duration).isChecked = true
            else -> {
                throw IllegalArgumentException("invalid $savedSongSortOrder")
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SELECT_IMAGE -> if (resultCode == Activity.RESULT_OK) {
                data?.data?.let {
                    CustomArtistImageUtil.getInstance(requireContext())
                        .setCustomArtistImage(artist, it)
                }
            }
            else -> if (resultCode == Activity.RESULT_OK) {
                println("OK")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_artist_detail, menu)
    }


    private fun handleBackPress(): Boolean {
        cab?.let {
            if (it.isActive()) {
                it.destroy()
                return true
            }
        }
        return false
    }

    private var cab: AttachedCab? = null

    override fun openCab(menuRes: Int, callback: ICabCallback): AttachedCab {
        cab?.let {
            if (it.isActive()) {
                it.destroy()
            }
        }

        cab = createCab(R.id.toolbarCab) {
            menu(menuRes)
            closeDrawable(R.drawable.ic_close)
            backgroundColor(literal = PlayBeatColorUtil.shiftBackgroundColor(surfaceColor()))
            slideDown()
            onCreate { cab, menu ->
                binding.toolbar.visibility = View.GONE
                binding.toolbar.title = ""
                binding.collapsingToolbar!!.title = ""
                binding.artistTitle.visibility = View.VISIBLE
                mView.findViewById<com.knesarcreation.playbeat.views.BaselineGridTextView>(R.id.artistTitle).text =
                    artist.name
                callback.onCabCreated(cab, menu)
            }
            onSelection {
                callback.onCabItemClicked(it)
            }
            onDestroy {
                if (!it.isActive()) {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.title = ""
                    binding.collapsingToolbar!!.title = artist.name
                    binding.artistTitle.visibility = View.GONE
                }
                //binding.toolbar2!!.visibility = View.VISIBLE
                callback.onCabFinished(it)
            }

        }
        return cab as AttachedCab
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_CODE_SELECT_IMAGE = 9002
    }
}
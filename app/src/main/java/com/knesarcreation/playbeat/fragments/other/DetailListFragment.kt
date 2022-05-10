package com.knesarcreation.playbeat.fragments.other

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.*
import com.knesarcreation.playbeat.adapter.song.ShuffleButtonSongAdapter
import com.knesarcreation.playbeat.databinding.FragmentPlaylistDetailBinding
import com.knesarcreation.playbeat.db.toSong
import com.knesarcreation.playbeat.extensions.dipToPix
import com.knesarcreation.playbeat.extensions.generalThemeValue
import com.knesarcreation.playbeat.extensions.showToast
import com.knesarcreation.playbeat.extensions.surfaceColor
import com.knesarcreation.playbeat.fragments.base.AbsMainActivityFragment
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.glide.playlistSongsPreview.PlaylistSongsPreview
import com.knesarcreation.playbeat.interfaces.ICabCallback
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.util.PlayBeatColorUtil
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.theme.ThemeMode


class DetailListFragment : AbsMainActivityFragment(R.layout.fragment_playlist_detail),
    ICabHolder {
    private val args by navArgs<DetailListFragmentArgs>()
    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private var showClearHistoryOption = false
    private var priviousToolbarTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaylistDetailBinding.bind(view)
        when (args.type) {
            TOP_ARTISTS,
            RECENT_ARTISTS,
            TOP_ALBUMS,
            RECENT_ALBUMS,
            FAVOURITES -> {
                enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
                returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
            }
            else -> {
                enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
                returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
            }
        }

        mainActivity.setSupportActionBar(binding.toolbar)

        when (App.getContext().generalThemeValue) {
            ThemeMode.LIGHT -> binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_ligth)

            ThemeMode.DARK -> {
                if (PreferenceUtil.materialYou) {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_material_you)
                } else {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_dark)
                }
            }

            ThemeMode.BLACK -> binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_just_black) /*inhnaces dark*/

            ThemeMode.AUTO -> {
                if (PreferenceUtil.materialYou) {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_material_you)
                } else {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_follow_system)
                }
            }
        }

        binding.progressIndicator.hide()
        when (args.type) {
            FAVOURITES -> loadFavorite()
            HISTORY_PLAYLIST -> {
                loadHistory()
                showClearHistoryOption = true // Reference to onCreateOptionsMenu
            }
            LAST_ADDED_PLAYLIST -> lastAddedSongs()
            TOP_PLAYED_PLAYLIST -> topPlayed()
        }

        binding.recyclerView.adapter?.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                val height = dipToPix(52f)
                binding.recyclerView.updatePadding(bottom = height.toInt())
            }
        })
        /*binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())*/
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!handleBackPress()) {
                remove()
                findNavController().navigateUp()
            }
        }

        binding.playlistDetailsMore.visibility = View.GONE
    }

    private fun lastAddedSongs() {
        // binding.toolbar.setTitle(R.string.last_added)
        binding.collapsingToolbar.title = resources.getString(R.string.last_added)

        val songAdapter = ShuffleButtonSongAdapter(
            requireActivity(),
            mutableListOf(),
            R.layout.item_list, this
        )
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            scheduleLayoutAnimation()
        }
        libraryViewModel.recentSongs().observe(viewLifecycleOwner) { songs ->
            songAdapter.swapDataSet(songs)
            if (songs.isNotEmpty()) {
                binding.collapsingToolbar.visibility = View.VISIBLE
                binding.toolbarOutsideCollapsing.visibility = View.GONE
                binding.appBarLayout.fitsSystemWindows = true

                GlideApp.with(requireActivity())
                    .load(PlaylistSongsPreview(songs))
                    .playlistOptions()
                    .transition(PlayBeatGlideExtension.getDefaultTransition())
                    .into(binding.image)
            } else {
                mainActivity.setSupportActionBar(binding.toolbarOutsideCollapsing)
                binding.appBarLayout.fitsSystemWindows = true
                binding.toolbarOutsideCollapsing.fitsSystemWindows = true
                binding.collapsingToolbar.visibility = View.GONE
                binding.toolbarOutsideCollapsing.visibility = View.VISIBLE
                binding.toolbarOutsideCollapsing.title = resources.getString(R.string.last_added)
            }
        }
    }

    private fun topPlayed() {
        //binding.toolbar.setTitle(R.string.my_top_tracks)
        binding.collapsingToolbar.title = resources.getString(R.string.my_top_tracks)

        val songAdapter = ShuffleButtonSongAdapter(
            requireActivity(),
            mutableListOf(),
            R.layout.item_list, this
        )
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
        }
        libraryViewModel.playCountSongs().observe(viewLifecycleOwner) { songs ->
            songAdapter.swapDataSet(songs)
            binding.empty.isVisible = songs.isEmpty()
            if (songs.isNotEmpty()) {
                binding.collapsingToolbar.visibility = View.VISIBLE
                binding.toolbarOutsideCollapsing.visibility = View.GONE
                binding.appBarLayout.fitsSystemWindows = true
                //val iconColor = ATHUtil.resolveColor(activity as Context, R.attr.colorControlNormal)
                //val error = PlayBeatUtil.getTintedVectorDrawable(
                //    activity as Context, R.drawable.ic_file_music, iconColor
                //)

                /* GlideApp.with(requireContext())
                     .load(PlayBeatGlideExtension.getSongModel(songs[0]))
                     .error(error)
                     .placeholder(error)
                     .transition(PlayBeatGlideExtension.getDefaultTransition())
                     .into(binding.image)*/

                GlideApp.with(requireActivity())
                    .load(PlaylistSongsPreview(songs))
                    .playlistOptions()
                    .transition(PlayBeatGlideExtension.getDefaultTransition())
                    .into(binding.image)

            } else {
                mainActivity.setSupportActionBar(binding.toolbarOutsideCollapsing)
                binding.appBarLayout.fitsSystemWindows = true
                binding.toolbarOutsideCollapsing.fitsSystemWindows = true
                binding.collapsingToolbar.visibility = View.GONE
                binding.toolbarOutsideCollapsing.visibility = View.VISIBLE
                binding.toolbarOutsideCollapsing.title = resources.getString(R.string.my_top_tracks)
            }
        }
    }

    private fun loadHistory() {
        //binding.toolbar.setTitle(R.string.history)
        binding.collapsingToolbar.title = resources.getString(R.string.history)

        val songAdapter = ShuffleButtonSongAdapter(
            requireActivity(),
            mutableListOf(),
            R.layout.item_list, this
        )
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
        }

        libraryViewModel.observableHistorySongs().observe(viewLifecycleOwner) {
            songAdapter.swapDataSet(it)
            binding.empty.isVisible = it.isEmpty()

            if (it.isNotEmpty()) {
                binding.collapsingToolbar.visibility = View.VISIBLE
                binding.toolbarOutsideCollapsing.visibility = View.GONE
                binding.appBarLayout.fitsSystemWindows = true
                // val iconColor = ATHUtil.resolveColor(activity as Context, R.attr.colorControlNormal)
                // val error = PlayBeatUtil.getTintedVectorDrawable(
                //     activity as Context, R.drawable.ic_file_music, iconColor
                // )

                /* GlideApp.with(requireContext())
                     .load(PlayBeatGlideExtension.getSongModel(it[0]))
                     .error(error)
                     .placeholder(error)
                     .transition(PlayBeatGlideExtension.getDefaultTransition())
                     .into(binding.image)*/
                GlideApp.with(requireActivity())
                    .load(PlaylistSongsPreview(it))
                    .playlistOptions()
                    .transition(PlayBeatGlideExtension.getDefaultTransition())
                    .into(binding.image)

            } else {
                mainActivity.setSupportActionBar(binding.toolbarOutsideCollapsing)
                binding.appBarLayout.fitsSystemWindows = true
                binding.toolbarOutsideCollapsing.fitsSystemWindows = true
                binding.collapsingToolbar.visibility = View.GONE
                binding.toolbarOutsideCollapsing.visibility = View.VISIBLE
                binding.toolbarOutsideCollapsing.title = resources.getString(R.string.history)
            }
        }
    }

    private fun loadFavorite() {
        binding.toolbar.setTitle(R.string.favorites)
        try {
            val songAdapter = ShuffleButtonSongAdapter(
                requireActivity(),
                mutableListOf(),
                R.layout.item_list, this
            )

            binding.recyclerView.apply {
                adapter = songAdapter
                layoutManager = linearLayoutManager()
            }
            libraryViewModel.favorites().observe(viewLifecycleOwner) { songEntities ->
                val songs = songEntities.map { songEntity -> songEntity.toSong() }
                binding.empty.isVisible = songs.isEmpty()
                songAdapter.swapDataSet(songs)

                if (songs.isNotEmpty()) {
                    binding.collapsingToolbar.visibility = View.VISIBLE
                    binding.toolbarOutsideCollapsing.visibility = View.GONE
                    binding.appBarLayout.fitsSystemWindows = true

                    // val iconColor =
                    //    ATHUtil.resolveColor(activity as Context, R.attr.colorControlNormal)
                    // val error = PlayBeatUtil.getTintedVectorDrawable(
                    //   activity as Context, R.drawable.ic_file_music, iconColor
                    // )

                    /*GlideApp.with(requireContext())
                        .load(PlayBeatGlideExtension.getSongModel(songs[0]))
                        .error(error)
                        .placeholder(error)
                        .transition(PlayBeatGlideExtension.getDefaultTransition())
                        .into(binding.image)*/
                    GlideApp.with(requireActivity())
                        .load(PlaylistSongsPreview(songs))
                        .playlistOptions()
                        .transition(PlayBeatGlideExtension.getDefaultTransition())
                        .into(binding.image)


                } else {
                    mainActivity.setSupportActionBar(binding.toolbarOutsideCollapsing)
                    binding.appBarLayout.fitsSystemWindows = true
                    binding.toolbarOutsideCollapsing.fitsSystemWindows = true
                    binding.collapsingToolbar.visibility = View.GONE
                    binding.toolbarOutsideCollapsing.visibility = View.VISIBLE
                    binding.toolbarOutsideCollapsing.title = resources.getString(R.string.favorites)
                }

            }
        } catch (e: Exception) {
            showToast("You have no favourite playlist.")
            findNavController().popBackStack()
        }

    }


    private fun linearLayoutManager(): LinearLayoutManager =
        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

    private fun gridLayoutManager(): GridLayoutManager =
        GridLayoutManager(requireContext(), gridCount(), GridLayoutManager.VERTICAL, false)

    private fun gridCount(): Int {
        if (PlayBeatUtil.isTablet()) {
            return if (PlayBeatUtil.isLandscape()) 6 else 4
        }
        return if (PlayBeatUtil.isLandscape()) 4 else 2
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var cab: AttachedCab? = null

    private fun handleBackPress(): Boolean {
        cab?.let {
            if (it.isActive()) {
                it.destroy()
                return true
            }
        }
        return false
    }

    override fun openCab(menuRes: Int, callback: ICabCallback): AttachedCab {
        cab?.let {
            println("Cab")
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
                priviousToolbarTitle = binding.collapsingToolbar.title.toString()
                binding.collapsingToolbar.title = ""

                callback.onCabCreated(cab, menu)
            }
            onSelection {
                callback.onCabItemClicked(it)
            }
            onDestroy {
                if (!it.isActive()) {
                    binding.toolbar.visibility = View.VISIBLE
                    binding.toolbar.title = ""
                    binding.collapsingToolbar.title = priviousToolbarTitle
                }
                callback.onCabFinished(it)
            }
        }
        return cab as AttachedCab
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_clear_history, menu)
        if (showClearHistoryOption) {
            menu.findItem(R.id.action_clear_history).isVisible = true // Show Clear History option
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_clear_history -> {
                if (binding.recyclerView.adapter?.itemCount!! > 0) {
                    libraryViewModel.clearHistory()

                    val snackBar =
                        Snackbar.make(
                            binding.container,
                            getString(R.string.history_cleared),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.history_undo_button)) {
                                libraryViewModel.restoreHistory()
                            }
                            .setActionTextColor(Color.YELLOW)
                    val snackBarView = snackBar.view
                    snackBarView.translationY =
                        -(resources.getDimension(R.dimen.mini_player_height))
                    snackBar.show()
                }
            }
        }
        return false
    }
}

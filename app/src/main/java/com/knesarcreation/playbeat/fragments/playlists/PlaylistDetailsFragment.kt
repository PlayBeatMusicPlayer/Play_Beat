package com.knesarcreation.playbeat.fragments.playlists

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.material.transition.MaterialSharedAxis
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.animator.GeneralItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.song.OrderablePlaylistSongAdapter
import com.knesarcreation.playbeat.databinding.FragmentPlaylistDetailBinding
import com.knesarcreation.playbeat.db.PlaylistWithSongs
import com.knesarcreation.playbeat.db.toSongs
import com.knesarcreation.playbeat.extensions.dip
import com.knesarcreation.playbeat.extensions.generalThemeValue
import com.knesarcreation.playbeat.extensions.surfaceColor
import com.knesarcreation.playbeat.fragments.base.AbsMainActivityFragment
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.playlistPreview.PlaylistPreview
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.menu.BottomSheetPlaylistMenuHelper
import com.knesarcreation.playbeat.interfaces.ICabCallback
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.PlayBeatColorUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.theme.ThemeMode
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class PlaylistDetailsFragment : AbsMainActivityFragment(R.layout.fragment_playlist_detail),
    ICabHolder {
    private val arguments by navArgs<PlaylistDetailsFragmentArgs>()
    private val viewModel by viewModel<PlaylistDetailsViewModel> {
        parametersOf(arguments.extraPlaylist)
    }

    private var _binding: FragmentPlaylistDetailBinding? = null
    private val binding get() = _binding!!
    private var priviousToolbarTitle = ""

    private lateinit var playlist: PlaylistWithSongs
    private lateinit var playlistSongAdapter: OrderablePlaylistSongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaylistDetailBinding.bind(view)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).addTarget(view)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        setHasOptionsMenu(true)
        mainActivity.setSupportActionBar(binding.toolbar)
        binding.container.transitionName = "playlist"
        playlist = arguments.extraPlaylist
        binding.toolbar.title = playlist.playlistEntity.playlistName
        setUpRecyclerView()
        viewModel.getSongs().observe(viewLifecycleOwner) {
            // Log.d("PlaylistDetailsFrag", "onViewCreated: Songs present in playlist : ${it.toSongs()[0].title}")
            songs(it.toSongs())
        }
        viewModel.playlistExists().observe(viewLifecycleOwner) {
            if (!it) {
                findNavController().navigateUp()
            }
        }
        postponeEnterTransition()
        requireView().doOnPreDraw { startPostponedEnterTransition() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!handleBackPress()) {
                remove()
                requireActivity().onBackPressed()
            }
        }

        when (App.getContext().generalThemeValue) {
            ThemeMode.LIGHT -> binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_ligth)

            ThemeMode.DARK -> {
                if (PreferenceUtil.materialYou) {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_material_you)
                } else {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_dark)
                }
            }

            ThemeMode.BLACK -> binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_just_black)

            ThemeMode.AUTO -> {
                if (PreferenceUtil.materialYou) {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_material_you)
                } else {
                    binding.shadowUp.setImageResource(R.drawable.shadow_up_artist_follow_system)
                }
            }
        }

        binding.playlistDetailsMore.setOnClickListener {
            BottomSheetPlaylistMenuHelper(activity as Context).handleMenuClick(playlist)
        }
        /*binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())*/
    }

    private fun setUpRecyclerView() {
        playlistSongAdapter = OrderablePlaylistSongAdapter(
            playlist.playlistEntity,
            requireActivity(),
            ArrayList(),
            R.layout.item_queue,
            this
        )

        val dragDropManager = RecyclerViewDragDropManager()

        val wrappedAdapter: RecyclerView.Adapter<*> =
            dragDropManager.createWrappedAdapter(playlistSongAdapter)


        val animator: GeneralItemAnimator = DraggableItemAnimator()
        binding.recyclerView.itemAnimator = animator

        dragDropManager.attachRecyclerView(binding.recyclerView)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = wrappedAdapter
        }
        playlistSongAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkIsEmpty()
            }
        })
    }

   /* override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_playlist_detail, menu)
    }*/

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return PlaylistMenuHelper.handleMenuClick(requireActivity(), playlist, item)
    }*/

    private fun checkForPadding() {
        val itemCount: Int = playlistSongAdapter.itemCount
        if (itemCount > 0 && MusicPlayerRemote.playingQueue.isNotEmpty()) {
            binding.recyclerView.updatePadding(bottom = dip(R.dimen.mini_player_height))
        } else {
            binding.recyclerView.updatePadding(bottom = 0)
        }
    }

    private fun checkIsEmpty() {
        checkForPadding()
        binding.empty.isVisible = playlistSongAdapter.itemCount == 0
        binding.emptyText.isVisible = playlistSongAdapter.itemCount == 0
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onPause() {
        playlistSongAdapter.saveSongs(playlist.playlistEntity)
        super.onPause()
    }

    private fun showEmptyView() {
        binding.empty.isVisible = true
        binding.emptyText.isVisible = true
    }

    fun songs(songs: List<Song>) {
        binding.progressIndicator.hide()
        if (songs.isNotEmpty()) {
            binding.collapsingToolbar.visibility = View.VISIBLE
            binding.toolbarOutsideCollapsing.visibility = View.GONE
            binding.appBarLayout.fitsSystemWindows = true

            playlistSongAdapter.swapDataSet(songs)
            GlideApp.with(requireActivity())
                .load(PlaylistPreview(playlist))
                .playlistOptions()
                .into(binding.image)

        } else {
            showEmptyView()
            mainActivity.setSupportActionBar(binding.toolbarOutsideCollapsing)
            binding.appBarLayout.fitsSystemWindows = false
            binding.toolbarOutsideCollapsing.fitsSystemWindows = false
            binding.collapsingToolbar.visibility = View.GONE
            binding.toolbarOutsideCollapsing.visibility = View.VISIBLE
            binding.toolbarOutsideCollapsing.title = playlist.playlistEntity.playlistName
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

}
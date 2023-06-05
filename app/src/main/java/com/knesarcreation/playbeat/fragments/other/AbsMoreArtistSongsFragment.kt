package com.knesarcreation.playbeat.fragments.other

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.song.ShuffleButtonSongAdapter
import com.knesarcreation.playbeat.databinding.FragmentMoreSongsBinding
import com.knesarcreation.playbeat.extensions.dipToPix
import com.knesarcreation.playbeat.extensions.surfaceColor
import com.knesarcreation.playbeat.fragments.artists.ArtistDetailsViewModel
import com.knesarcreation.playbeat.fragments.base.AbsMainActivityFragment
import com.knesarcreation.playbeat.interfaces.ICabCallback
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.model.Artist
import com.knesarcreation.playbeat.util.PlayBeatColorUtil

abstract class AbsMoreArtistSongsFragment : AbsMainActivityFragment(R.layout.fragment_more_songs),
    ICabHolder {

    private var _binding: FragmentMoreSongsBinding? = null
    private val binding get() = _binding!!
    abstract val detailsViewModel: ArtistDetailsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMoreSongsBinding.bind(view)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)

        mainActivity.addMusicServiceEventListener(detailsViewModel)
        mainActivity.setSupportActionBar(binding.toolbar)

        detailsViewModel.getArtist().observe(viewLifecycleOwner) {

            loadData(it)

        }

        binding.recyclerView.adapter?.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                val height = dipToPix(52f)
                binding.recyclerView.updatePadding(bottom = height.toInt())
            }
        })
        /*binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())*/
        //postponeEnterTransition()
        //view.doOnPreDraw { startPostponedEnterTransition() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!handleBackPress()) {
                remove()
                findNavController().navigateUp()
            }
        }

        binding.playlistDetailsMore.visibility = View.GONE
    }

    private fun loadData(artist: Artist) {
        binding.toolbar.title = artist.name

        binding.progressIndicator.hide()
        if (artist.songCount == 0) {
            findNavController().navigateUp()
            return
        }

        val songAdapter = ShuffleButtonSongAdapter(
            requireActivity(),
            mutableListOf(),
            R.layout.item_list, this
        )
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
        }
        songAdapter.swapDataSet(artist.songs)
    }

    private fun linearLayoutManager(): LinearLayoutManager =
        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)


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
                callback.onCabCreated(cab, menu)
            }
            onSelection {
                callback.onCabItemClicked(it)
            }
            onDestroy {
                if (!it.isActive()) {
                    binding.toolbar.visibility = View.VISIBLE
                }
                callback.onCabFinished(it)
            }
        }
        return cab as AttachedCab
    }
}
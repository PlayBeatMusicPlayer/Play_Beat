package com.knesarcreation.playbeat.fragments.genres

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.EXTRA_GENRE
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.GenreAdapter
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.fragments.base.AbsRecyclerViewFragment
import com.knesarcreation.playbeat.interfaces.IGenreClickListener
import com.knesarcreation.playbeat.model.Genre
import com.knesarcreation.playbeat.util.PlayBeatUtil

class
GenresFragment : AbsRecyclerViewFragment<GenreAdapter, LinearLayoutManager>(),
    IGenreClickListener {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getGenre().observe(viewLifecycleOwner) {
            if (it.isNotEmpty())
                adapter?.swapDataSet(it)
            else
                adapter?.swapDataSet(listOf())
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            remove()
            requireActivity().onBackPressed()
        }
        binding.playlistContainer.playlistViewContainer.visibility = View.GONE
    }

    override fun createLayoutManager(): LinearLayoutManager {
        return if (PlayBeatUtil.isLandscape()) {
            GridLayoutManager(activity, 4)
        } else {
            GridLayoutManager(activity, 2)
        }
    }

    override fun createAdapter(): GenreAdapter {
        val dataSet = if (adapter == null) ArrayList() else adapter!!.dataSet
        return GenreAdapter(requireActivity(), dataSet, this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        /* menu.removeItem(R.id.action_grid_size)
         menu.removeItem(R.id.action_layout_type)*/
        menu.removeItem(R.id.action_sort_order)
        //menu.findItem(R.id.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        //Setting up cast button
        CastButtonFactory.setUpMediaRouteButton(requireContext(), menu, R.id.action_cast)
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Genres)
    }


    override val titleRes: Int
        get() = R.string.genres

    override val emptyMessage: Int
        get() = R.string.no_genres

    override val isShuffleVisible: Boolean
        get() = false

    companion object {
        @JvmField
        val TAG: String = GenresFragment::class.java.simpleName

        @JvmStatic
        fun newInstance(): GenresFragment {
            return GenresFragment()
        }
    }

    override fun onClickGenre(genre: Genre, view: View) {
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).addTarget(requireView())
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        findNavController().navigate(
            R.id.genreDetailsFragment,
            bundleOf(EXTRA_GENRE to genre),
            null,
            null
        )
    }
}

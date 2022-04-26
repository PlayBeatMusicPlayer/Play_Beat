package com.knesarcreation.playbeat.fragments.albums

import android.os.Bundle
import android.view.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.knesarcreation.playbeat.fragments.GridStyle
import com.knesarcreation.playbeat.fragments.bottomSheets.BottomSheetSort
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.gms.cast.framework.CastButtonFactory
import com.knesarcreation.playbeat.EXTRA_ALBUM_ID
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.album.AlbumAdapter
import com.knesarcreation.playbeat.extensions.surfaceColor
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.SortOrder.AlbumSortOrder
import com.knesarcreation.playbeat.interfaces.IAlbumClickListener
import com.knesarcreation.playbeat.interfaces.ICabCallback
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.PlayBeatColorUtil

class AlbumsFragment : AbsRecyclerViewCustomGridSizeFragment<AlbumAdapter, GridLayoutManager>(),
    IAlbumClickListener, ICabHolder {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getAlbums().observe(viewLifecycleOwner) {
            if (it.isNotEmpty())
                adapter?.swapDataSet(it)
            else
                adapter?.swapDataSet(listOf())
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (!handleBackPress()) {
                remove()
                requireActivity().onBackPressed()
            }
        }

        binding.playlistContainer.playlistViewContainer.visibility = View.GONE

    }

    override val titleRes: Int
        get() = R.string.albums

    override val emptyMessage: Int
        get() = R.string.no_albums

    override val isShuffleVisible: Boolean
        get() = true

    override fun onShuffleClicked() {
        libraryViewModel.getAlbums().value?.let {
            MusicPlayerRemote.setShuffleMode(MusicService.SHUFFLE_MODE_NONE)
            MusicPlayerRemote.openQueue(
                queue = it.shuffled().flatMap { album -> album.songs },
                startPosition = 0,
                startPlaying = true
            )
        }
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), getGridSize())
    }

    override fun createAdapter(): AlbumAdapter {
        val dataSet = if (adapter == null) ArrayList() else adapter!!.dataSet
        return AlbumAdapter(
            requireActivity(),
            dataSet,
            itemLayoutRes(),
            this,
            this
        )
    }

    override fun setGridSize(gridSize: Int) {
        layoutManager?.spanCount = gridSize
        adapter?.notifyDataSetChanged()
    }

    override fun loadSortOrder(): String {
        return PreferenceUtil.albumSortOrder
    }

    override fun saveSortOrder(sortOrder: String) {
        PreferenceUtil.albumSortOrder = sortOrder
    }

    override fun loadGridSize(): Int {
        return PreferenceUtil.albumGridSize
    }

    override fun saveGridSize(gridColumns: Int) {
        PreferenceUtil.albumGridSize = gridColumns
    }

    override fun loadGridSizeLand(): Int {
        return PreferenceUtil.albumGridSizeLand
    }

    override fun saveGridSizeLand(gridColumns: Int) {
        PreferenceUtil.albumGridSizeLand = gridColumns
    }

    override fun setSortOrder(sortOrder: String) {
        libraryViewModel.forceReload(ReloadType.Albums)
    }

    override fun loadLayoutRes(): Int {
        return PreferenceUtil.albumGridStyle.layoutResId
    }

    override fun saveLayoutRes(layoutRes: Int) {
        PreferenceUtil.albumGridStyle = GridStyle.values().first { gridStyle ->
            gridStyle.layoutResId == layoutRes
        }
    }

    companion object {
        fun newInstance(): AlbumsFragment {
            return AlbumsFragment()
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
        reenterTransition = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val gridSizeItem: MenuItem = menu.findItem(R.id.action_grid_size)
        if (PlayBeatUtil.isLandscape()) {
            gridSizeItem.setTitle(R.string.action_grid_size_land)
        }
        setUpGridSizeMenu(gridSizeItem.subMenu)
        /*  val layoutItem = menu.findItem(R.id.action_layout_type)
          setupLayoutMenu(layoutItem.subMenu)
          setUpSortOrderMenu(menu.findItem(R.id.action_sort_order).subMenu)*/
        //Setting up cast button
        CastButtonFactory.setUpMediaRouteButton(requireContext(), menu, R.id.action_cast)
    }

    private fun setUpSortOrderMenu(
        sortOrderMenu: SubMenu
    ) {
        val currentSortOrder: String? = getSortOrder()
        sortOrderMenu.clear()
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_asc,
            0,
            R.string.sort_order_a_z
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_A_Z)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_desc,
            1,
            R.string.sort_order_z_a
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_Z_A)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_artist,
            2,
            R.string.sort_order_album_artist
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_ARTIST)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_year,
            3,
            R.string.sort_order_year
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_YEAR)
        sortOrderMenu.add(
            0,
            R.id.action_album_sort_order_num_songs,
            4,
            R.string.sort_order_num_songs
        ).isChecked =
            currentSortOrder.equals(AlbumSortOrder.ALBUM_NUMBER_OF_SONGS)

        sortOrderMenu.setGroupCheckable(0, true, true)
    }

    private fun setUpGridSizeMenu(
        gridSizeMenu: SubMenu
    ) {
        when (getGridSize()) {
            1 -> gridSizeMenu.findItem(R.id.action_grid_size_1).isChecked =
                true
            2 -> gridSizeMenu.findItem(R.id.action_grid_size_2).isChecked = true
            3 -> gridSizeMenu.findItem(R.id.action_grid_size_3).isChecked = true
            4 -> gridSizeMenu.findItem(R.id.action_grid_size_4).isChecked = true
            5 -> gridSizeMenu.findItem(R.id.action_grid_size_5).isChecked = true
            6 -> gridSizeMenu.findItem(R.id.action_grid_size_6).isChecked = true
            7 -> gridSizeMenu.findItem(R.id.action_grid_size_7).isChecked = true
            8 -> gridSizeMenu.findItem(R.id.action_grid_size_8).isChecked = true
        }
        val gridSize: Int = maxGridSize
        if (gridSize < 8) {
            gridSizeMenu.findItem(R.id.action_grid_size_8).isVisible = false
        }
        if (gridSize < 7) {
            gridSizeMenu.findItem(R.id.action_grid_size_7).isVisible = false
        }
        if (gridSize < 6) {
            gridSizeMenu.findItem(R.id.action_grid_size_6).isVisible = false
        }
        if (gridSize < 5) {
            gridSizeMenu.findItem(R.id.action_grid_size_5).isVisible = false
        }
        if (gridSize < 4) {
            gridSizeMenu.findItem(R.id.action_grid_size_4).isVisible = false
        }
        if (gridSize < 3) {
            gridSizeMenu.findItem(R.id.action_grid_size_3).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (handleGridSizeMenuItem(item)) {
            return true
        }
        if (item.itemId == R.id.action_sort) {
            val bottomSheetSongsFragment = BottomSheetSort(getSortOrder(), 2 /*album sort*/)
            bottomSheetSongsFragment.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheets"
            )
            bottomSheetSongsFragment.listenerAlbum =
                object : BottomSheetSort.OnAlbumSortOptionListener {
                    override fun sortByAlbumName() {
                        val sortOrder = AlbumSortOrder.ALBUM_A_Z
                        if (sortOrder != PreferenceUtil.albumSortOrder) {
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(AlbumSortOrder.ALBUM_Z_A)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByAlbumArtist() {
                        val sortOrder = AlbumSortOrder.ALBUM_ARTIST
                        if (sortOrder != PreferenceUtil.albumSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByAlbumNoOfSongs() {
                        val sortOrder = AlbumSortOrder.ALBUM_NUMBER_OF_SONGS
                        if (sortOrder != PreferenceUtil.albumSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByAlbumYear() {
                        val sortOrder = AlbumSortOrder.ALBUM_YEAR
                        if (sortOrder != PreferenceUtil.albumSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun handleSortOrderMenuItem(
        item: MenuItem
    ): Boolean {
        val sortOrder: String = when (item.itemId) {
            R.id.action_album_sort_order_asc -> AlbumSortOrder.ALBUM_A_Z
            R.id.action_album_sort_order_desc -> AlbumSortOrder.ALBUM_Z_A
            R.id.action_album_sort_order_artist -> AlbumSortOrder.ALBUM_ARTIST
            R.id.action_album_sort_order_year -> AlbumSortOrder.ALBUM_YEAR
            R.id.action_album_sort_order_num_songs -> AlbumSortOrder.ALBUM_NUMBER_OF_SONGS
            else -> PreferenceUtil.albumSortOrder
        }
        if (sortOrder != PreferenceUtil.albumSortOrder) {
            item.isChecked = true
            setAndSaveSortOrder(sortOrder)
            return true
        }
        return false
    }

    /*private fun handleLayoutResType(
        item: MenuItem
    ): Boolean {
        val layoutRes = when (item.itemId) {
            R.id.action_layout_normal -> R.layout.item_grid
            R.id.action_layout_card -> R.layout.item_card
            R.id.action_layout_colored_card -> R.layout.item_card_color
            R.id.action_layout_circular -> R.layout.item_grid_circle
            R.id.action_layout_image -> R.layout.image
            R.id.action_layout_gradient_image -> R.layout.item_image_gradient
            else -> PreferenceUtil.albumGridStyle.layoutResId
        }
        if (layoutRes != PreferenceUtil.albumGridStyle.layoutResId) {
            item.isChecked = true
            setAndSaveLayoutRes(layoutRes)
            return true
        }
        return false
    }*/

    private fun handleGridSizeMenuItem(
        item: MenuItem
    ): Boolean {
        val gridSize = when (item.itemId) {
            R.id.action_grid_size_1 -> 1
            R.id.action_grid_size_2 -> 2
            R.id.action_grid_size_3 -> 3
            R.id.action_grid_size_4 -> 4
            R.id.action_grid_size_5 -> 5
            R.id.action_grid_size_6 -> 6
            R.id.action_grid_size_7 -> 7
            R.id.action_grid_size_8 -> 8
            else -> 0
        }
        if (gridSize > 0) {
            item.isChecked = true
            setAndSaveGridSize(gridSize)
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.forceReload(ReloadType.Albums)
    }

    override fun onPause() {
        super.onPause()
        if (cab.isActive()) {
            cab.destroy()
        }
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
        cab = createCab(R.id.toolbar_container) {
            menu(menuRes)
            closeDrawable(R.drawable.ic_close)
            backgroundColor(literal = PlayBeatColorUtil.shiftBackgroundColor(surfaceColor()))
            slideDown()
            onCreate { cab, menu -> callback.onCabCreated(cab, menu) }
            onSelection {
                callback.onCabItemClicked(it)
            }
            onDestroy { callback.onCabFinished(it) }
        }
        return cab as AttachedCab
    }
}

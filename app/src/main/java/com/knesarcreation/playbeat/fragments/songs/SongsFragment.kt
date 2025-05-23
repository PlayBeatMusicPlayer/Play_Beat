package com.knesarcreation.playbeat.fragments.songs

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.play.core.appupdate.AppUpdateManager
import com.knesarcreation.playbeat.BuildConfig
import com.knesarcreation.playbeat.INTERSTITIAL_SHUFFLE_BUTTON
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.song.SongAdapter
import com.knesarcreation.playbeat.ads.InterstitialAdHelperClass
import com.knesarcreation.playbeat.extensions.surfaceColor
import com.knesarcreation.playbeat.fragments.GridStyle
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.fragments.base.AbsRecyclerViewCustomGridSizeFragment
import com.knesarcreation.playbeat.fragments.bottomSheets.BottomSheetSort
import com.knesarcreation.playbeat.helper.SortOrder.SongSortOrder
import com.knesarcreation.playbeat.interfaces.ICabCallback
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.interfaces.IPlaybackStateChanged
import com.knesarcreation.playbeat.util.NavigationUtil
import com.knesarcreation.playbeat.util.PlayBeatColorUtil
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil

@SuppressLint("StaticFieldLeak")
var mInterstitialAdHelperClass: InterstitialAdHelperClass? = null

class SongsFragment : AbsRecyclerViewCustomGridSizeFragment<SongAdapter, GridLayoutManager>(),
    ICabHolder, IPlaybackStateChanged {

    private var appUpdateManager: AppUpdateManager? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as IPlaybackStateChanged.TempInter
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        if (mInterstitialAdHelperClass != null)
            mInterstitialAdHelperClass = null
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        libraryViewModel.getSongs().observe(viewLifecycleOwner) {
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

        if (BuildConfig.VERSION_CODE > PreferenceUtil.lastVersion && !BuildConfig.DEBUG) {
            NavigationUtil.gotoWhatNews(childFragmentManager)
        }

        mInterstitialAdHelperClass = InterstitialAdHelperClass(requireContext())
        mInterstitialAdHelperClass?.loadInterstitialAd(INTERSTITIAL_SHUFFLE_BUTTON)
    }

    override val titleRes: Int
        get() = R.string.songs

    override val emptyMessage: Int
        get() = R.string.no_songs

    override val isShuffleVisible: Boolean
        get() = true

    override fun onShuffleClicked() {
        libraryViewModel.shuffleSongs()
    }

    override fun createLayoutManager(): GridLayoutManager {
        return GridLayoutManager(requireActivity(), getGridSize())
    }

    override fun createAdapter(): SongAdapter {
        val dataSet = if (adapter == null) mutableListOf() else adapter!!.dataSet
        return SongAdapter(
            requireActivity(),
            dataSet,
            itemLayoutRes(),
            this
        )

    }

    override fun loadGridSize(): Int {
        return PreferenceUtil.songGridSize
    }

    override fun saveGridSize(gridColumns: Int) {
        PreferenceUtil.songGridSize = gridColumns
    }

    override fun loadGridSizeLand(): Int {
        return PreferenceUtil.songGridSizeLand
    }

    override fun saveGridSizeLand(gridColumns: Int) {
        PreferenceUtil.songGridSizeLand = gridColumns
    }

    override fun setGridSize(gridSize: Int) {
        adapter?.notifyDataSetChanged()
    }

    override fun loadSortOrder(): String {
        return PreferenceUtil.songSortOrder
    }

    override fun saveSortOrder(sortOrder: String) {
        PreferenceUtil.songSortOrder = sortOrder
    }

    @LayoutRes
    override fun loadLayoutRes(): Int {
        return PreferenceUtil.songGridStyle.layoutResId
    }

    override fun saveLayoutRes(@LayoutRes layoutRes: Int) {
        PreferenceUtil.songGridStyle = GridStyle.values().first { gridStyle ->
            gridStyle.layoutResId == layoutRes
        }
    }

    override fun setSortOrder(sortOrder: String) {
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val gridSizeItem: MenuItem = menu.findItem(R.id.action_grid_size)
        if (PlayBeatUtil.isLandscape()) {
            gridSizeItem.setTitle(R.string.action_grid_size_land)
        }
        gridSizeItem.subMenu?.let { setUpGridSizeMenu(it) }
        /*val layoutItem = menu.findItem(R.id.action_layout_type)
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
            R.id.action_song_sort_order_asc,
            0,
            R.string.sort_order_a_z
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_A_Z
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_desc,
            1,
            R.string.sort_order_z_a
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_Z_A
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_artist,
            2,
            R.string.sort_order_artist
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_ARTIST_ASC
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_album,
            3,
            R.string.sort_order_album
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_ALBUM_ASC
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_year,
            4,
            R.string.sort_order_year
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_YEAR_DESC
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_date,
            5,
            R.string.sort_order_date
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_DATE_DESC
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_date_modified,
            6,
            R.string.sort_order_date_modified
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_DATE_MODIFIED_DESC
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_composer,
            7,
            R.string.sort_order_composer
        ).isChecked =
            currentSortOrder == SongSortOrder.COMPOSER_ACS
        sortOrderMenu.add(
            0,
            R.id.action_song_sort_order_album_artist,
            8,
            R.string.album_artist
        ).isChecked =
            currentSortOrder == SongSortOrder.SONG_ALBUM_ARTIST_ASC

        sortOrderMenu.setGroupCheckable(0, true, true)
    }

    /*private fun setupLayoutMenu(
        subMenu: SubMenu
    ) {
        when (itemLayoutRes()) {
            R.layout.item_card -> subMenu.findItem(R.id.action_layout_card).isChecked = true
            R.layout.item_grid -> subMenu.findItem(R.id.action_layout_normal).isChecked = true
            R.layout.item_card_color ->
                subMenu.findItem(R.id.action_layout_colored_card).isChecked = true
            R.layout.item_grid_circle ->
                subMenu.findItem(R.id.action_layout_circular).isChecked = true
            R.layout.image -> subMenu.findItem(R.id.action_layout_image).isChecked = true
            R.layout.item_image_gradient ->
                subMenu.findItem(R.id.action_layout_gradient_image).isChecked = true
        }
    }*/

    private fun setUpGridSizeMenu(
        gridSizeMenu: SubMenu
    ) {
        when (getGridSize()) {
            1 -> gridSizeMenu.findItem(R.id.action_grid_size_1).isChecked = true
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
        /* if (handleLayoutResType(item)) {
             return true
         }
         if (handleSortOrderMenuItem(item)) {
             return true
         }*/
        if (item.itemId == R.id.action_sort) {
            val bottomSheetSongsFragment = BottomSheetSort(getSortOrder(), 1 /*sort songs*/)
            bottomSheetSongsFragment.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheets"
            )
            bottomSheetSongsFragment.listenerAudio =
                object : BottomSheetSort.OnAudioSortOptionListener {
                    override fun sortByName() {
                        val sortOrder = SongSortOrder.SONG_A_Z
                        if (sortOrder != PreferenceUtil.songSortOrder) {
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(SongSortOrder.SONG_Z_A)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByAlbumName() {
                        val sortOrder = SongSortOrder.SONG_ALBUM_ASC
                        if (sortOrder != PreferenceUtil.songSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(SongSortOrder.SONG_ALBUM_DESC)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByArtistName() {
                        val sortOrder = SongSortOrder.SONG_ARTIST_ASC
                        if (sortOrder != PreferenceUtil.songSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(SongSortOrder.SONG_ARTIST_DESC)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByDateAdded() {
                        val sortOrder = SongSortOrder.SONG_DATE_DESC
                        if (sortOrder != PreferenceUtil.songSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(SongSortOrder.SONG_DATE_ASC)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByDateModified() {
                        val sortOrder = SongSortOrder.SONG_DATE_MODIFIED_DESC
                        if (sortOrder != PreferenceUtil.songSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(SongSortOrder.SONG_DATE_MODIFIED_ASC)
                        }
                        bottomSheetSongsFragment.dismiss()
                    }

                    override fun sortByComposer() {
                        val sortOrder = SongSortOrder.COMPOSER_ACS
                        if (sortOrder != PreferenceUtil.songSortOrder) {
                            //item.isChecked = true
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(SongSortOrder.COMPOSER_DESC)
                        }
                        bottomSheetSongsFragment.dismiss()

                    }

                    override fun sortByYear() {
                        val sortOrder = SongSortOrder.SONG_YEAR_DESC
                        if (sortOrder != PreferenceUtil.songSortOrder) {
                            setAndSaveSortOrder(sortOrder)
                        } else {
                            setAndSaveSortOrder(SongSortOrder.SONG_YEAR_ASC)
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
            R.id.action_song_sort_order_asc -> SongSortOrder.SONG_A_Z
            R.id.action_song_sort_order_desc -> SongSortOrder.SONG_Z_A
            R.id.action_song_sort_order_artist -> SongSortOrder.SONG_ARTIST_ASC
            R.id.action_song_sort_order_album_artist -> SongSortOrder.SONG_ALBUM_ARTIST_ASC
            R.id.action_song_sort_order_album -> SongSortOrder.SONG_ALBUM_ASC
            R.id.action_song_sort_order_year -> SongSortOrder.SONG_YEAR_DESC
            R.id.action_song_sort_order_date -> SongSortOrder.SONG_DATE_DESC
            R.id.action_song_sort_order_composer -> SongSortOrder.COMPOSER_ACS
            R.id.action_song_sort_order_date_modified -> SongSortOrder.SONG_DATE_MODIFIED_DESC
            else -> PreferenceUtil.songSortOrder
        }
        if (sortOrder != PreferenceUtil.songSortOrder) {
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
            else -> PreferenceUtil.songGridStyle.layoutResId
        }
        if (layoutRes != PreferenceUtil.songGridStyle.layoutResId) {
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
        libraryViewModel.forceReload(ReloadType.Songs)
    }

    override fun onPause() {

        super.onPause()
        if (cab.isActive()) {
            cab.destroy()
        }
    }

    companion object {
        @JvmField
        var TAG: String = SongsFragment::class.java.simpleName
        var listener: IPlaybackStateChanged.TempInter? = null

        @JvmStatic
        fun newInstance(): SongsFragment {
            return SongsFragment()
        }
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

    override fun onStateChanged() {
        Log.d("OnStateChanged_MusicStateChanged", "onStateChanged: $listener ")
        listener?.mNotify()
    }
}

package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.adapter.ViewPagerAdapter
import com.knesarcreation.playbeat.databinding.HomeFragmentBinding
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.SavedAppTheme
import com.knesarcreation.playbeat.viewPager.CustomViewPager


class HomeFragment : Fragment()/*, AllSongFragment.OnContextMenuEnabled*/ {

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding
    private lateinit var mViewPager: CustomViewPager
    private lateinit var mTabLayout: TabLayout
    private var pagerAdapter: ViewPagerAdapter? = null
    private lateinit var viewModel: DataObservableClass

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
    ): View {
        // Inflate the layout for this fragment
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        val view = binding!!.root

        viewModel = activity?.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        } ?: throw Exception("Invalid Activity")

        mViewPager = _binding!!.mViewPager
        mTabLayout = _binding!!.tabLayout

        val list =
            listOf(AllSongFragment(), FoldersFragment(), AllAlbumsFragment(), AllArtistsFragment())
        pagerAdapter = ViewPagerAdapter(childFragmentManager, list)
        mTabLayout.setupWithViewPager(mViewPager)
        mViewPager.offscreenPageLimit = 4
        mViewPager.adapter = pagerAdapter

        viewModel.isContextMenuEnabled.observe(viewLifecycleOwner, {
            if (it != null) {

                mViewPager.disableScroll(it)
                for (i in 0..3)
                    (mTabLayout.getChildAt(0) as ViewGroup).getChildAt(i).isEnabled = !it

            }
        })


        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        viewModel.isContextMenuEnabled.removeObservers(this)
    }

    override fun onResume() {
        super.onResume()
        SavedAppTheme(
            activity as Context,
            binding!!.homeFragBackground,
            binding!!.tabLayout,
            null,
            isHomeFrag = true,
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
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            playlistBG = null,
            isPlaylistFrag = false,
             null,
            isSearchFrag = false,
            null,
            false
        ).settingSavedBackgroundTheme()
    }

    /*  override fun disabledViews(longClickSelectionEnable: Boolean) {

      }*/

}
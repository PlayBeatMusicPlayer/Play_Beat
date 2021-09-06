package com.knesarcreation.playbeat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.knesarcreation.playbeat.adapter.ViewPagerAdapter
import com.knesarcreation.playbeat.databinding.HomeFragmentBinding


class HomeFragment : Fragment() {

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding
    private lateinit var mViewPager: ViewPager
    private lateinit var mTabLayout: TabLayout
    private var pagerAdapter: ViewPagerAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        val view = binding!!.root

        mViewPager = _binding!!.mViewPager
        mTabLayout = _binding!!.tabLayout

        val list = listOf(AllSongFragment(), AlbumFragment(), ArtistsFragment())
        pagerAdapter = ViewPagerAdapter(childFragmentManager, list)
        mTabLayout.setupWithViewPager(mViewPager)
        mViewPager.offscreenPageLimit = 3
        mViewPager.adapter = pagerAdapter

        return view
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}
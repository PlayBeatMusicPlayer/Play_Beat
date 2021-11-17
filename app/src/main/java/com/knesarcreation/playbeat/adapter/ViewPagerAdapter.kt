package com.knesarcreation.playbeat.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter


class ViewPagerAdapter(
    fm: FragmentManager,
    var fragmentList: List<Fragment>,
) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getCount() = fragmentList.size

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                fragmentList[0]
            }
            1 -> {
                fragmentList[1]
            }
            2 -> {
                fragmentList[2]
            }
            else -> {
                fragmentList[3]
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> {
                "Songs"
            }
            1 -> {
                "Folders"
            }
            2 -> {
                "Albums"
            }
            else -> {
                "Artists"
            }
        }
    }
}
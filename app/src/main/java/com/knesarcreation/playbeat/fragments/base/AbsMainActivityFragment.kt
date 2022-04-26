package com.knesarcreation.playbeat.fragments.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import com.knesarcreation.playbeat.activities.MainActivity
import com.knesarcreation.playbeat.extensions.setTaskDescriptionColorAuto
import com.knesarcreation.playbeat.fragments.LibraryViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

abstract class AbsMainActivityFragment(@LayoutRes layout: Int) : AbsMusicServiceFragment(layout) {
    val libraryViewModel: LibraryViewModel by sharedViewModel()

    val mainActivity: MainActivity
        get() = activity as MainActivity

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        mainActivity.setTaskDescriptionColorAuto()
    }
}

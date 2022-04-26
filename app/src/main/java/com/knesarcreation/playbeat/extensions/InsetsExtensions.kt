package com.knesarcreation.playbeat.extensions

import androidx.core.view.WindowInsetsCompat
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil

fun WindowInsetsCompat?.safeGetBottomInsets(): Int {
    return if (PreferenceUtil.isFullScreenMode) {
        return 0
    } else {
        this?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom
            ?: PlayBeatUtil.getNavigationBarHeight()
    }
}

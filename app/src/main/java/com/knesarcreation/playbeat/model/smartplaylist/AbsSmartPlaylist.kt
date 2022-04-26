package com.knesarcreation.playbeat.model.smartplaylist

import androidx.annotation.DrawableRes
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.model.AbsCustomPlaylist

abstract class AbsSmartPlaylist(
    name: String,
    @DrawableRes val iconRes: Int = R.drawable.ic_queue_music
) : AbsCustomPlaylist(
    id = PlaylistIdGenerator(name, iconRes),
    name = name
)
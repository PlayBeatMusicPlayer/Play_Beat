package com.knesarcreation.playbeat.model.smartplaylist

import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.R
import kotlinx.parcelize.Parcelize

@Parcelize
class LastAddedPlaylist : AbsSmartPlaylist(
    name = App.getContext().getString(R.string.last_added),
    iconRes = R.drawable.ic_library_add
) {
    override fun songs(): List<Song> {
        return lastAddedRepository.recentSongs()
    }
}
package com.knesarcreation.playbeat.model.smartplaylist

import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.R
import kotlinx.parcelize.Parcelize

@Parcelize
class ShuffleAllPlaylist : AbsSmartPlaylist(
    name = App.getContext().getString(R.string.action_shuffle_all),
    iconRes = R.drawable.ic_shuffle
) {
    override fun songs(): List<Song> {
        return songRepository.songs()
    }
}
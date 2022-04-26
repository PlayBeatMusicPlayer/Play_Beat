package com.knesarcreation.playbeat.model.smartplaylist

import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.model.Song
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent

@Parcelize
class HistoryPlaylist : AbsSmartPlaylist(
    name = App.getContext().getString(R.string.history),
    iconRes = R.drawable.ic_history
), KoinComponent {

    override fun songs(): List<Song> {
        return topPlayedRepository.recentlyPlayedTracks()
    }
}
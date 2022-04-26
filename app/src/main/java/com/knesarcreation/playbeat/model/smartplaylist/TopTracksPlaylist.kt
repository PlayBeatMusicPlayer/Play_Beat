package com.knesarcreation.playbeat.model.smartplaylist

import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.R
import kotlinx.parcelize.Parcelize

@Parcelize
class TopTracksPlaylist : AbsSmartPlaylist(
    name = App.getContext().getString(R.string.my_top_tracks),
    iconRes = R.drawable.ic_trending_up
) {
    override fun songs(): List<Song> {
        return topPlayedRepository.topTracks()
    }
}
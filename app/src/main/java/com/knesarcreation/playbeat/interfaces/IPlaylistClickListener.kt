package com.knesarcreation.playbeat.interfaces

import android.view.View
import com.knesarcreation.playbeat.db.PlaylistWithSongs

interface IPlaylistClickListener {
    fun onPlaylistClick(playlistWithSongs: PlaylistWithSongs, view: View)
}
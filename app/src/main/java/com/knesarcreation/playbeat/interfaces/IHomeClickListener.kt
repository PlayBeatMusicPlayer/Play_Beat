package com.knesarcreation.playbeat.interfaces

import com.knesarcreation.playbeat.model.Album
import com.knesarcreation.playbeat.model.Artist
import com.knesarcreation.playbeat.model.Genre

interface IHomeClickListener {
    fun onAlbumClick(album: Album)

    fun onArtistClick(artist: Artist)

    fun onGenreClick(genre: Genre)
}
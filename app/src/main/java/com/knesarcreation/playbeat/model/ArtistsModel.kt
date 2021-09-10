package com.knesarcreation.playbeat.model

data class ArtistsModel(
    var id: Long,
    var artistName: String,
    var noOfAlbums: Int,
    var noOfTracks: Int,
    /*var albumId: Long,
    var albumName: String,
    var subArtistName: String*/
) {

    constructor() : this(0L, "", 0, 0/*, 0L, "", ""*/)
}
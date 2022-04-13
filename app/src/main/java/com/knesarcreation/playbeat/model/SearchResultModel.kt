package com.knesarcreation.playbeat.model

import com.knesarcreation.playbeat.database.AlbumModel
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ArtistsModel

data class SearchResultModel(
    var songList: ArrayList<AllSongsModel>,
    var albumList: ArrayList<AlbumModel>,
   /* var artistList: ArrayList<ArtistsModel>*/
)

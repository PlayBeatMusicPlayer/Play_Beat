package com.knesarcreation.playbeat.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insertPlaylist(playlistModel: PlaylistModel)

    @Query("Delete from playlist where id = :id")
    suspend fun deletePlaylist(id: Int)

    @Query("Select * from playlist")
    fun getPlaylist(): LiveData<List<PlaylistModel>>

    @Query("Update playlist set songIds = :audioList where id = :id")
    suspend fun updatePlaylist(audioList: String, id: Int)

    @Query("Update playlist set playlistName = :playlistName where id = :id")
    suspend fun renamePlaylist(playlistName: String, id: Int)

    @Query("Select * from playlist where id = :id")
    fun getPlaylistAudios(id: Int): List<PlaylistModel>
}
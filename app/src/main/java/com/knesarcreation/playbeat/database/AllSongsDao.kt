package com.knesarcreation.playbeat.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AllSongsDao {

    @Insert
    suspend fun insertSongs(allSongsModel: AllSongsModel)

    @Query("DELETE FROM allSongsModel")
    suspend fun deleteSongs()

    @Query("UPDATE allSongsModel set playingOrPause = :isPlayingOrPause where songId = :songId and songName =:songName")
    suspend fun updateSongs(songId: Long, songName: String, isPlayingOrPause: Int)

    @Query("SELECT * FROM allSongsModel")
    fun getAllSongs(): LiveData<List<AllSongsModel>>

}
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

    @Query("UPDATE allSongsModel set currentPlayedAudioTime = :currentTime where songId = :songId ")
    suspend fun updateCurrentPlayedTime(songId: Long, currentTime: Long)

    @Query("SELECT * FROM allSongsModel")
    fun getAllSongs(): LiveData<List<AllSongsModel>>

    /* @Query("SELECT * FROM allSongsModel where albumName =:albumName")
     fun getAudioAccordingAlbum(albumName: String): LiveData<List<AllSongsModel>>*/

    @Query("SELECT * FROM allSongsModel where albumName =:albumName")
    fun getAudioAccordingAlbum(albumName: String): List<AllSongsModel>

    @Query("SELECT * FROM allSongsModel where artistsName = :artistName")
    fun getAudioAccordingArtists(artistName: String): List<AllSongsModel>

    @Query("SELECT * FROM allSongsModel where isFavourite = :isFav")
    fun getFavouritesAudio(isFav: Boolean): LiveData<List<AllSongsModel>>

    @Query("SELECT * FROM allSongsModel where songId = :songId")
    fun getOneFavAudio(songId: Long): List<AllSongsModel>

    @Query("UPDATE allSongsModel set isFavourite = :isFav, favAudioAddedTime =:favAudioAddedTime where songId = :songId")
    suspend fun updateFavouriteAudio(isFav: Boolean, songId: Long, favAudioAddedTime: Long)

    @Query("SELECT * FROM allSongsModel where dateAdded > :targetDate")
    fun getLastAddedAudio(targetDate: String): LiveData<List<AllSongsModel>>

    @Query("SELECT * FROM allSongsModel where currentPlayedAudioTime != 0")
    fun getPrevPlayedAudio(): LiveData<List<AllSongsModel>>

}
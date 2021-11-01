package com.knesarcreation.playbeat.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AlbumDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(albumModel: AlbumModel)

    @Query("Delete from albumModel")
    suspend fun deleteAllAlbum()

    @Query("Delete from albumModel where id =:id")
    suspend fun deleteAlbum(id: Long)

    @Query("Select * from albumModel")
    fun getAlbums(): LiveData<List<AlbumModel>>

    @Query("Select * from albumModel where artistName = :artistName")
    fun getAlbumAccordingToArtist(artistName: String): LiveData<List<AlbumModel>>

    @Query("Select * from albumModel where albumName = :albumName")
    fun getOnAlbum(albumName: String): List<AlbumModel>

    @Query("update albumModel  set  albumName = :albumName , artistName = :artistName , artUri = :artUri where albumId = :albumId")
    suspend fun updateAlbumData(
        albumId: Long,
        albumName: String,
        artistName: String,
        artUri: String
    )

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAlbum(albumModel: AlbumModel)

    @Query("Update albumModel set songCount =:songCount where albumId = :albumId")
    suspend fun updateSongCount(songCount: Int, albumId: Long)
}
package com.knesarcreation.playbeat.database

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface ArtistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artistsModel: ArtistsModel)

    @Query("Delete from artistsModel where id =:id")
    suspend fun deleteArtist(id: Long)

    @Query("Select * from artistsModel")
    fun getAllArtists(): LiveData<List<ArtistsModel>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateArtist(artistsModel: ArtistsModel)
}
package com.knesarcreation.playbeat.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QueueListDao {
    @Insert
    suspend fun insertQueueAudio(queueListModel: QueueListModel)

    @Query("DELETE FROM queueList")
    suspend fun deleteQueue()

    @Query("UPDATE queueList set isPlayingOrPause = :isPlayingOrPause where songId = :songId and songName =:songName")
    suspend fun updateQueue(songId: Long, songName: String, isPlayingOrPause: Int)

   /* suspend fun updateAllQueueList(
        songId: Long,
        albumId: Long,
        songName: String,
        artistsName: String,
    )*/

    @Query("SELECT * FROM queueList")
    fun getQueueAudioList(): LiveData<List<QueueListModel>>
}
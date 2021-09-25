package com.knesarcreation.playbeat.database

import android.app.Application
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Repository(var application: Application) {

    private var allSongsDao: AllSongsDao
    private var queueListDao: QueueListDao

    init {
        val databaseClient = DatabaseClient.getInstance(application)
        allSongsDao = databaseClient?.allSongsDao()!!
        queueListDao = databaseClient.queueListDao()
    }

    fun insertAllSongs(
        allSongsModel: AllSongsModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.insertSongs(allSongsModel)
        }
    }

    fun deleteSongs(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { allSongsDao.deleteSongs() }
    }

    fun updateSong(
        songId: Long,
        songName: String,
        isPlayingOrPause: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.updateSongs(songId, songName, isPlayingOrPause)
        }
    }

    fun getSongList(): LiveData<List<AllSongsModel>> {
        return allSongsDao.getAllSongs()
    }

    /*fun getAudioAccordingAlbum(albumName: String): LiveData<List<AllSongsModel>> {
        return allSongsDao.getAudioAccordingAlbum(albumName)
    }*/

    suspend fun getAudioAccordingAlbum(albumName: String): List<AllSongsModel> {
        return withContext(Dispatchers.IO) {
            return@withContext allSongsDao.getAudioAccordingAlbum(albumName)
        }
    }

    suspend fun getAudioAccordingArtist(artistName: String): List<AllSongsModel> {
        return withContext(Dispatchers.IO) {
            return@withContext allSongsDao.getAudioAccordingArtists(artistName)
        }
    }

    fun insertQueueAudio(
        queueListModel: QueueListModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            queueListDao.insertQueueAudio(queueListModel)
        }
    }

    fun deleteQueue(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { queueListDao.deleteQueue() }
    }

    fun updateQueueAudio(
        songId: Long,
        songName: String,
        isPlayingOrPause: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            queueListDao.updateQueue(songId, songName, isPlayingOrPause)
        }
    }

    fun getAllQueueAudio(): LiveData<List<QueueListModel>> {
        return queueListDao.getQueueAudioList()
    }

}
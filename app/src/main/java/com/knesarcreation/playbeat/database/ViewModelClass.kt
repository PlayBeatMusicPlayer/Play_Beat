package com.knesarcreation.playbeat.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData

class ViewModelClass(mApplication: Application) : AndroidViewModel(mApplication) {

    private var repository: Repository = Repository(mApplication)

    fun getAllSong(): LiveData<List<AllSongsModel>> {
        return repository.getSongList()
    }

    fun getFavouriteAudios(): LiveData<List<AllSongsModel>> {
        return repository.getFavouriteAudios()
    }

    suspend fun getOneFavAudio(songId: Long): List<AllSongsModel> {
        return repository.getOneFavAudio(songId)
    }

    fun getLastAddedAudio(targetDate: String): LiveData<List<AllSongsModel>> {
        return repository.getLastAddedAudio(targetDate)
    }

    fun getPrevPlayedAudios(): LiveData<List<AllSongsModel>> {
        return repository.getPrevPlayedAudio()
    }

    fun getQueueFavouriteAudios(): LiveData<List<QueueListModel>> {
        return repository.getQueueFavouriteAudios()
    }

    /*fun getAudioAccordingAlbum(albumName: String): LiveData<List<AllSongsModel>> {
        return repository.getAudioAccordingAlbum(albumName)
    }*/

    suspend fun getAudioAccordingAlbum(albumName: String): List<AllSongsModel> {
        return repository.getAudioAccordingAlbum(albumName)
    }

    suspend fun getAudioAccordingArtist(artistName: String): List<AllSongsModel> {
        return repository.getAudioAccordingArtist(artistName)
    }

    fun deleteSongs(lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteSongs(lifecycleScope)
    }

    fun updateSong(
        songId: Long,
        songName: String,
        isPlayingOrPause: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateSong(songId, songName, isPlayingOrPause, lifecycleScope)
    }

    fun updateCurrentPlayedTime(
        songId: Long,
        currentTime: Long,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateCurrentPlayedTime(songId, currentTime, lifecycleScope)
    }

    fun updateFavouriteAudio(
        isFav: Boolean,
        songId: Long,
        favAudioAddedTime: Long,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateFavouriteAudio(isFav, songId, favAudioAddedTime, lifecycleScope)
    }

    fun updateQueueFavouriteAudio(
        isFav: Boolean,
        songId: Long,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateQueueFavouriteAudio(isFav, songId, lifecycleScope)
    }

    fun insertAllSongs(allSongsModel: AllSongsModel, lifecycleScope: LifecycleCoroutineScope) {
        repository.insertAllSongs(allSongsModel, lifecycleScope)
    }

    fun getQueueAudio(): LiveData<List<QueueListModel>> {
        return repository.getAllQueueAudio()
    }

    fun deleteQueue(lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteQueue(lifecycleScope)
    }

    fun updateQueueAudio(
        songId: Long,
        songName: String,
        isPlayingOrPause: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateQueueAudio(songId, songName, isPlayingOrPause, lifecycleScope)
    }

    fun insertQueue(queueListModel: QueueListModel, lifecycleScope: LifecycleCoroutineScope) {
        repository.insertQueueAudio(queueListModel, lifecycleScope)
    }
}
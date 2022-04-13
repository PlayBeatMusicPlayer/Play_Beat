package com.knesarcreation.playbeat.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    suspend fun getRangeOfPlaylistAudio(songIds: ArrayList<Long>): List<AllSongsModel> {
        return repository.getRangeOfPlaylistAudio(songIds)
    }

    fun getLastAddedAudio(targetDate: String): LiveData<List<AllSongsModel>> {
        return repository.getLastAddedAudio(targetDate)
    }

    fun getPrevPlayedAudios(): LiveData<List<AllSongsModel>> {
        return repository.getPrevPlayedAudio()
    }

    fun getMostPlayedAudio(): LiveData<List<AllSongsModel>> {
        return repository.getMostPlayedAudio()
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

    suspend fun getAudioAccordingToFolders(folderId: String): List<AllSongsModel> {
        return repository.getAudioAccordingToFolders(folderId)
    }

    /*fun deleteSongs(lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteSongs(lifecycleScope)
    }*/

    fun deleteOneSong(songId: Long, lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteOneSong(songId, lifecycleScope)
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

    fun updateMostPlayedAudioCount(
        songId: Long,
        count: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateMostPlayedAudioCount(songId, count, lifecycleScope)
    }

    fun updateAudioTagsInAllSongsModel(
        songId: Long,
        songName: String,
        albumName: String,
        artistName: String,
        artUri: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateAudioTags(songId, songName, albumName, artistName, artUri, lifecycleScope)
    }

    fun updateFolderInAudio(
        songId: Long,
        folderId: String,
        folderName: String,
        noOfSongs: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateFolderInAudio(songId, folderId, folderName, noOfSongs, lifecycleScope)
    }

    fun updateAlbumData(
        albumId: Long,
        albumName: String,
        artistName: String,
        artUri: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateAlbumData(albumId, albumName, artistName, artUri, lifecycleScope)
    }

    fun updateAlbum(
        albumModel: AlbumModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateAlbum(albumModel, lifecycleScope)
    }

    fun updateArtist(
        artistsModel: ArtistsModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        return repository.updateArtist(artistsModel, lifecycleScope)
    }

    fun updateSongCount(
        songCount: Int,
        albumId: Long,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.updateSongCount(songCount, albumId, lifecycleScope)
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

    fun deleteOneQueueAudio(songId: Long, lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteOneQueueAudio(songId, lifecycleScope)
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

    fun insertPlaylist(playlistModel: PlaylistModel, lifecycleScope: LifecycleCoroutineScope) {
        repository.insertPlaylist(playlistModel, lifecycleScope)
    }

    fun deletePlaylist(id: Int, lifecycleScope: LifecycleCoroutineScope) {
        repository.deletePlaylist(id, lifecycleScope)
    }

    fun updatePlaylist(audioList: String, id: Int, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.updatePlaylist(
                audioList,
                id,
                lifecycleScope
            )
        }
    }

    fun renamePlaylist(playlistName: String, id: Int, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.renamePlaylist(
                playlistName,
                id,
                lifecycleScope
            )
        }
    }

    fun getAllPlaylists(): LiveData<List<PlaylistModel>> {
        return repository.getAllPlaylist()
    }

    suspend fun getPlaylistAudios(id: Int): List<PlaylistModel> {
        return repository.getPlaylistAudios(id)
    }


    fun insertAlbum(
        albumModel: AlbumModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.insertAlbum(albumModel, lifecycleScope)
    }

    fun insertArtist(
        artistsModel: ArtistsModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        repository.insertArtist(artistsModel, lifecycleScope)
    }

    fun deleteAlbum(id: Long, lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteAlbum(id, lifecycleScope)
    }

    fun deleteArtist(id: Long, lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteArtist(id, lifecycleScope)
    }

    fun deleteAllAlbum(lifecycleScope: LifecycleCoroutineScope) {
        repository.deleteAllAlbum(lifecycleScope)
    }

    fun getAlbums(): LiveData<List<AlbumModel>> {
        return repository.getAlbums()
    }

    fun getAllArtists(): LiveData<List<ArtistsModel>> {
        return repository.getAllArtists()
    }

    fun getAlbumAccordingToArtist(artistName: String): LiveData<List<AlbumModel>> {
        return repository.getAlbumAccordingToArtist(artistName)
    }

    suspend fun getOnAlbum(albumName: String): List<AlbumModel> {
        return repository.getOnAlbum(albumName)
    }

}
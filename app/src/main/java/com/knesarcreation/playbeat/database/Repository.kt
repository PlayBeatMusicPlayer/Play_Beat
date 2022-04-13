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
    private var playlistDao: PlaylistDao
    private var albumDao: AlbumDao
    private var artistDao: ArtistDao

    init {
        val databaseClient = DatabaseClient.getInstance(application)
        allSongsDao = databaseClient?.allSongsDao()!!
        queueListDao = databaseClient.queueListDao()
        playlistDao = databaseClient.playlistDao()
        albumDao = databaseClient.albumDao()
        artistDao = databaseClient.artistDao()
    }

    fun insertAllSongs(
        allSongsModel: AllSongsModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.insertSongs(allSongsModel)
        }
    }

    fun insertAlbum(
        albumModel: AlbumModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            albumDao.insertAlbum(albumModel)
        }
    }

    fun insertArtist(
        artistsModel: ArtistsModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            artistDao.insertArtist(artistsModel)
        }
    }

    fun deleteAlbum(id: Long, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) {
            albumDao.deleteAlbum(id)
        }
    }

    fun deleteArtist(id: Long, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) {
            artistDao.deleteArtist(id)
        }
    }

    fun deleteAllAlbum(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) {
            albumDao.deleteAllAlbum()
        }
    }

    fun getAlbums(): LiveData<List<AlbumModel>> {
        return albumDao.getAlbums()
    }

    fun getAllArtists(): LiveData<List<ArtistsModel>> {
        return artistDao.getAllArtists()
    }

    fun getAlbumAccordingToArtist(artistName: String): LiveData<List<AlbumModel>> {
        return albumDao.getAlbumAccordingToArtist(artistName)
    }

    suspend fun getOnAlbum(albumName: String): List<AlbumModel> {
        return withContext(Dispatchers.IO) {
            return@withContext albumDao.getOnAlbum(albumName)
        }
    }

   /* fun deleteSongs(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { allSongsDao.deleteSongs() }
    }*/

    fun deleteOneSong(songId: Long, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { allSongsDao.deleteOneSong(songId) }
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

    fun updateCurrentPlayedTime(
        songId: Long,
        currentTime: Long,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.updateCurrentPlayedTime(songId, currentTime)
        }
    }

    fun updateMostPlayedAudioCount(
        songId: Long,
        count: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.updateMostPlayedAudioCount(songId, count)
        }
    }

    fun updateAudioTags(
        songId: Long,
        songName: String,
        albumName: String,
        artistName: String,
        artUri: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.updateAudioTags(songId, songName, albumName, artistName, artUri)
        }
    }

    fun updateFolderInAudio(
        songId: Long,
        folderId: String,
        folderName: String,
        noOfSongs: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.updateFolderInAudio(songId, folderId, folderName, noOfSongs)
        }
    }

    fun updateAlbumData(
        albumId: Long,
        albumName: String,
        artistName: String,
        artUri: String,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            albumDao.updateAlbumData(albumId, albumName, artistName, artUri)
        }
    }

    fun updateAlbum(
        albumModel: AlbumModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            albumDao.updateAlbum(albumModel)
        }
    }

    fun updateArtist(
        artistsModel: ArtistsModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            artistDao.updateArtist(artistsModel)
        }
    }

    fun updateSongCount(songCount: Int, albumId: Long, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) {
            albumDao.updateSongCount(songCount, albumId)
        }
    }

    fun updateFavouriteAudio(
        isFav: Boolean,
        songId: Long,
        favAudioAddedTime: Long,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            allSongsDao.updateFavouriteAudio(isFav, songId, favAudioAddedTime)
        }
    }

    fun updateQueueFavouriteAudio(
        isFav: Boolean,
        songId: Long,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            queueListDao.updateQueueFavouriteAudio(isFav, songId)
        }
    }

    fun getSongList(): LiveData<List<AllSongsModel>> {
        return allSongsDao.getAllSongs()
    }

    fun getFavouriteAudios(): LiveData<List<AllSongsModel>> {
        return allSongsDao.getFavouritesAudio(isFav = true)
    }

    suspend fun getRangeOfPlaylistAudio(songIds: ArrayList<Long>): List<AllSongsModel> {
        return withContext(Dispatchers.IO) {
            return@withContext allSongsDao.getRangeOfPlaylistAudio(songIds)
        }
    }

    suspend fun getOneFavAudio(songId: Long): List<AllSongsModel> {
        return withContext(Dispatchers.IO) {
            return@withContext allSongsDao.getFavOneAudio(songId)
        }
    }

    fun getLastAddedAudio(targetDate: String): LiveData<List<AllSongsModel>> {
        return allSongsDao.getLastAddedAudio(targetDate)
    }

    fun getPrevPlayedAudio(): LiveData<List<AllSongsModel>> {
        return allSongsDao.getPrevPlayedAudio()
    }

    fun getMostPlayedAudio(): LiveData<List<AllSongsModel>> {
        return allSongsDao.getMostPlayedAudio()
    }

    fun getQueueFavouriteAudios(): LiveData<List<QueueListModel>> {
        return queueListDao.getQueueFavouritesAudio(isFav = true)
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


    suspend fun getAudioAccordingToFolders(folderId: String): List<AllSongsModel> {
        return withContext(Dispatchers.IO) {
            return@withContext allSongsDao.getAudioAccordingToFolders(folderId)
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

    fun deleteOneQueueAudio(songId: Long, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { queueListDao.deleteOneQueueAudio(songId) }
    }

    fun updateQueueAudio(
        songId: Long,
        songName: String,
        isPlayingOrPause: Int,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            queueListDao.updateQueueAudio(songId, songName, isPlayingOrPause)
        }
    }

    fun getAllQueueAudio(): LiveData<List<QueueListModel>> {
        return queueListDao.getQueueAudioList()
    }

    fun insertPlaylist(
        playlistModel: PlaylistModel,
        lifecycleScope: LifecycleCoroutineScope
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            playlistDao.insertPlaylist(playlistModel)
        }
    }

    fun deletePlaylist(id: Int, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { playlistDao.deletePlaylist(id) }
    }

    fun updatePlaylist(audioList: String, id: Int, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { playlistDao.updatePlaylist(audioList, id) }
    }

    fun renamePlaylist(playlistName: String, id: Int, lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch(Dispatchers.IO) { playlistDao.renamePlaylist(playlistName, id) }
    }

    fun getAllPlaylist(): LiveData<List<PlaylistModel>> {
        return playlistDao.getPlaylist()
    }

    suspend fun getPlaylistAudios(id: Int): List<PlaylistModel> {
        return withContext(Dispatchers.IO) {
            return@withContext playlistDao.getPlaylistAudios(id)
        }
    }

}
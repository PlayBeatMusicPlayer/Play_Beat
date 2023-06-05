package com.knesarcreation.playbeat.fragments

import android.animation.ValueAnimator
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.lifecycle.*
import com.knesarcreation.playbeat.*
import com.knesarcreation.playbeat.activities.MainActivity
import com.knesarcreation.playbeat.ads.InterstitialAdHelperClass
import com.knesarcreation.playbeat.ads.NEXT_ADS_SHOW_TIME
import com.knesarcreation.playbeat.db.*
import com.knesarcreation.playbeat.fragments.ReloadType.*
import com.knesarcreation.playbeat.fragments.search.Filter
import com.knesarcreation.playbeat.fragments.songs.mInterstitialAdHelperClass
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.interfaces.IMusicServiceEventListener
import com.knesarcreation.playbeat.model.*
import com.knesarcreation.playbeat.repository.RealRepository
import com.knesarcreation.playbeat.util.DensityUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LibraryViewModel(
    private val repository: RealRepository,
) : ViewModel(), IMusicServiceEventListener {

    private val _paletteColor = MutableLiveData<Int>()
    private val home = MutableLiveData<List<Home>>()
    private val suggestions = MutableLiveData<List<Song>>()
    private val albums = MutableLiveData<List<Album>>()
    private val songs = MutableLiveData<List<Song>>()
    private val artists = MutableLiveData<List<Artist>>()
    private val playlists = MutableLiveData<List<PlaylistWithSongs>>()
    private val legacyPlaylists = MutableLiveData<List<Playlist>>()
    private val genres = MutableLiveData<List<Genre>>()
    private val searchResults = MutableLiveData<List<Any>>()
    private val fabMargin = MutableLiveData(0)
    private val songHistory = MutableLiveData<List<Song>>()
    private var previousSongHistory = ArrayList<HistoryEntity>()
    val paletteColor: LiveData<Int> = _paletteColor

    init {
        loadLibraryContent()
    }

    private fun loadLibraryContent() = viewModelScope.launch(IO) {
        fetchHomeSections()
        fetchSuggestions()
        fetchSongs()
        fetchAlbums()
        fetchArtists()
        fetchGenres()
        fetchPlaylists()
    }

    fun getSearchResult(): LiveData<List<Any>> = searchResults

    fun getSongs(): LiveData<List<Song>> {
        return songs
    }

    fun getAlbums(): LiveData<List<Album>> {
        return albums
    }

    fun getArtists(): LiveData<List<Artist>> {
        return artists
    }

    fun getPlaylists(): LiveData<List<PlaylistWithSongs>> {
        return playlists
    }

    fun getLegacyPlaylist(): LiveData<List<Playlist>> {
        return legacyPlaylists
    }

    fun getGenre(): LiveData<List<Genre>> {
        return genres
    }

    fun getHome(): LiveData<List<Home>> {
        return home
    }

    fun getSuggestions(): LiveData<List<Song>> {
        return suggestions
    }

    fun getFabMargin(): LiveData<Int> {
        return fabMargin
    }

    private suspend fun fetchSongs() {
        songs.postValue(repository.allSongs())
    }

    private suspend fun fetchAlbums() {
        albums.postValue(repository.fetchAlbums())

    }

    private suspend fun fetchArtists() {
        if (PreferenceUtil.albumArtistsOnly) {
            artists.postValue(repository.albumArtists())
        } else {
            artists.postValue(repository.fetchArtists())
        }
    }

    private suspend fun fetchPlaylists() {
        playlists.postValue(repository.fetchPlaylistWithSongs())
    }

    private fun fetchLegacyPlaylist() {
        viewModelScope.launch(IO) {
            legacyPlaylists.postValue(repository.fetchLegacyPlaylist())
        }
    }

    private suspend fun fetchGenres() {
        genres.postValue(repository.fetchGenres())
    }

    private suspend fun fetchHomeSections() {
        home.postValue(repository.homeSections())
    }

    private suspend fun fetchSuggestions() {
        suggestions.postValue(repository.suggestions())
    }

    fun search(query: String?, filter: Filter) =
        viewModelScope.launch(IO) {
            val result = repository.search(query, filter)
            searchResults.postValue(result)
        }

    fun forceReload(reloadType: ReloadType) = viewModelScope.launch(IO) {
        when (reloadType) {
            Songs -> fetchSongs()
            Albums -> fetchAlbums()
            Artists -> fetchArtists()
            HomeSections -> fetchHomeSections()
            Playlists -> fetchPlaylists()
            Genres -> fetchGenres()
            Suggestions -> fetchSuggestions()
        }
    }

    fun updateColor(newColor: Int) {
        _paletteColor.postValue(newColor)
    }

    override fun onMediaStoreChanged() {
        println("onMediaStoreChanged")
        loadLibraryContent()
    }

    override fun onServiceConnected() {
        println("onServiceConnected")
    }

    override fun onServiceDisconnected() {
        println("onServiceDisconnected")
    }

    override fun onQueueChanged() {
        println("onQueueChanged")
    }

    override fun onPlayingMetaChanged() {
        println("onPlayingMetaChanged")
    }

    override fun onPlayStateChanged() {
        println("onPlayStateChanged")
    }

    override fun onRepeatModeChanged() {
        println("onRepeatModeChanged")
    }

    override fun onShuffleModeChanged() {
        println("onShuffleModeChanged")
    }

    override fun onFavoriteStateChanged() {
        println("onFavoriteStateChanged")
    }

    fun shuffleSongs() = viewModelScope.launch(IO) {
        if ((System.currentTimeMillis() - InterstitialAdHelperClass.prevSeenAdsTime) / 1000 >= NEXT_ADS_SHOW_TIME) {
            mInterstitialAdHelperClass?.showInterstitial(
                INTERSTITIAL_SHUFFLE_BUTTON,
                SHUFFLE_BUTTON,
                repository.allSongs(),
                0
            )
        } else {
            val songs = repository.allSongs()
            MusicPlayerRemote.openAndShuffleQueue(
                songs,
                true
            )
        }
    }

    fun renameRoomPlaylist(playListId: Long, name: String) = viewModelScope.launch(IO) {
        repository.renameRoomPlaylist(playListId, name)
    }

    fun deleteSongsInPlaylist(songs: List<SongEntity>) {
        viewModelScope.launch(IO) {
            repository.deleteSongsInPlaylist(songs)
            forceReload(Playlists)
        }
    }

    fun deleteSongsFromPlaylist(playlists: List<PlaylistEntity>) = viewModelScope.launch(IO) {
        repository.deletePlaylistSongs(playlists)
    }

    fun deleteRoomPlaylist(playlists: List<PlaylistEntity>) = viewModelScope.launch(IO) {
        repository.deleteRoomPlaylist(playlists)
    }

    fun albumById(id: Long) = repository.albumById(id)
    suspend fun artistById(id: Long) = repository.artistById(id)
    suspend fun favoritePlaylist() = repository.favoritePlaylist()
    suspend fun isFavoriteSong(song: SongEntity) = repository.isFavoriteSong(song)
    suspend fun isSongFavorite(songId: Long) = repository.isSongFavorite(songId)
    suspend fun insertSongs(songs: List<SongEntity>) = repository.insertSongs(songs)
    suspend fun removeSongFromPlaylist(songEntity: SongEntity) =
        repository.removeSongFromPlaylist(songEntity)

    private suspend fun checkPlaylistExists(playlistName: String): List<PlaylistEntity> =
        repository.checkPlaylistExists(playlistName)

    private suspend fun createPlaylist(playlistEntity: PlaylistEntity): Long =
        repository.createPlaylist(playlistEntity)

    fun importPlaylists() = viewModelScope.launch(IO) {
        val playlists = repository.fetchLegacyPlaylist()
        playlists.forEach { playlist ->
            val playlistEntity = repository.checkPlaylistExists(playlist.name).firstOrNull()
            if (playlistEntity != null) {
                val songEntities = playlist.getSongs().map {
                    it.toSongEntity(playlistEntity.playListId)
                }
                repository.insertSongs(songEntities)
            } else {
                if (playlist != Playlist.empty) {
                    val playListId = createPlaylist(PlaylistEntity(playlistName = playlist.name))
                    val songEntities = playlist.getSongs().map {
                        it.toSongEntity(playListId)
                    }
                    repository.insertSongs(songEntities)
                }
            }
            forceReload(Playlists)
        }
    }

    fun deleteTracks(songs: List<Song>) = viewModelScope.launch(IO) {
        repository.deleteSongs(songs)
        fetchPlaylists()
        loadLibraryContent()
    }

    fun recentSongs(): LiveData<List<Song>> = liveData {
        emit(repository.recentSongs())
    }

    fun playCountSongs(): LiveData<List<Song>> = liveData {
        val songs = repository.playCountSongs().map {
            it.toSong()
        }
        emit(songs)
        // Cleaning up deleted or moved songs
        withContext(IO) {
            songs.forEach { song ->
                if (!File(song.data).exists() || song.id == -1L) {
                    repository.deleteSongInPlayCount(song.toPlayCount())
                }
            }
            emit(repository.playCountSongs().map {
                it.toSong()
            })
        }
    }

    fun artists(type: Int): LiveData<List<Artist>> = liveData {
        when (type) {
            TOP_ARTISTS -> emit(repository.topArtists())
            RECENT_ARTISTS -> {
                emit(repository.recentArtists())
            }
        }
    }

    fun albums(type: Int): LiveData<List<Album>> = liveData {
        when (type) {
            TOP_ALBUMS -> emit(repository.topAlbums())
            RECENT_ALBUMS -> {
                emit(repository.recentAlbums())
            }
        }
    }

    fun artist(artistId: Long): LiveData<Artist> = liveData {
        emit(repository.artistById(artistId))
    }

    fun fetchContributors(): LiveData<List<Contributor>> = liveData {
        emit(repository.contributor())
    }

    fun observableHistorySongs(): LiveData<List<Song>> {
        val songs = repository.historySong().map {
            it.toSong()
        }
        songHistory.value = songs
        // Cleaning up deleted or moved songs
        viewModelScope.launch {
            songs.forEach { song ->
                if (!File(song.data).exists() || song.id == -1L) {
                    repository.deleteSongInHistory(song.id)
                }
            }
        }
        songHistory.value = repository.historySong().map {
            it.toSong()
        }
        return songHistory
    }

    fun clearHistory() {
        viewModelScope.launch(IO) {
            previousSongHistory = repository.historySong() as ArrayList<HistoryEntity>

            repository.clearSongHistory()
        }
        songHistory.value = emptyList()
    }


    fun restoreHistory() {
        viewModelScope.launch(IO) {
            if (previousSongHistory.isNotEmpty()) {
                val history = ArrayList<Song>()
                for (song in previousSongHistory) {
                    repository.addSongToHistory(song.toSong())
                    history.add(song.toSong())
                }
                songHistory.postValue(history)
            }
        }
    }

    fun favorites() = repository.favorites()

    fun clearSearchResult() {
        viewModelScope.launch {
            searchResults.postValue(emptyList())
        }
    }

    fun addToPlaylist(playlistName: String, songs: List<Song>) {
        viewModelScope.launch(IO) {
            val playlists = checkPlaylistExists(playlistName)
            if (playlists.isEmpty()) {
                val playlistId: Long =
                    createPlaylist(PlaylistEntity(playlistName = playlistName))
                insertSongs(songs.map { it.toSongEntity(playlistId) })
                withContext(Main) {
                    Toast.makeText(
                        App.getContext(),
                        App.getContext()
                            .getString(R.string.playlist_created_sucessfully, playlistName),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val playlist = playlists.firstOrNull()
                if (playlist != null) {
                    insertSongs(songs.map {
                        it.toSongEntity(playListId = playlist.playListId)
                    })
                }
            }
            forceReload(Playlists)
            withContext(Main) {
                Toast.makeText(
                    App.getContext(), App.getContext().getString(
                        R.string.added_song_count_to_playlist,
                        songs.size,
                        playlistName
                    ), Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun setFabMargin(bottomMargin: Int) {
        val currentValue = DensityUtil.dip2px(App.getContext(), 16F) +
                bottomMargin
        ValueAnimator.ofInt(fabMargin.value!!, currentValue).apply {
            addUpdateListener {
                fabMargin.postValue(
                    (it.animatedValue as Int)
                )
            }
            doOnEnd {
                fabMargin.postValue(currentValue)
            }
            start()
        }
    }
}

enum class ReloadType {
    Songs,
    Albums,
    Artists,
    HomeSections,
    Playlists,
    Genres,
    Suggestions
}

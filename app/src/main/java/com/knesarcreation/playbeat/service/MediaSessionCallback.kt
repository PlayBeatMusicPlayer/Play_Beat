package com.knesarcreation.playbeat.service

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.knesarcreation.playbeat.helper.ShuffleHelper.makeShuffleList
import com.knesarcreation.playbeat.model.Album
import com.knesarcreation.playbeat.model.Artist
import com.knesarcreation.playbeat.model.Playlist
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.repository.*
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.service.MusicService.Companion.CYCLE_REPEAT
import com.knesarcreation.playbeat.service.MusicService.Companion.TOGGLE_FAVORITE
import com.knesarcreation.playbeat.service.MusicService.Companion.TOGGLE_SHUFFLE
import com.knesarcreation.playbeat.auto.AutoMediaIDHelper
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.MusicPlayerRemote.cycleRepeatMode
import com.knesarcreation.playbeat.repository.TopPlayedRepository
import com.knesarcreation.playbeat.util.MusicUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Created by hemanths on 2019-08-01.
 */

class MediaSessionCallback(
    private val context: Context,
    private val musicService: MusicService
) : MediaSessionCompat.Callback(), KoinComponent {

    private val songRepository by inject<SongRepository>()
    private val albumRepository by inject<AlbumRepository>()
    private val artistRepository by inject<ArtistRepository>()
    private val genreRepository by inject<GenreRepository>()
    private val playlistRepository by inject<PlaylistRepository>()
    private val topPlayedRepository by inject<TopPlayedRepository>()

    override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
        super.onPlayFromMediaId(mediaId, extras)
        val musicId = AutoMediaIDHelper.extractMusicID(mediaId!!)
        println(musicId)
        val itemId = musicId?.toLong() ?: -1
        val songs: ArrayList<Song> = ArrayList()
        when (val category = AutoMediaIDHelper.extractCategory(mediaId)) {
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM -> {
                val album: Album = albumRepository.album(itemId)
                songs.addAll(album.songs)
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST -> {
                val artist: Artist = artistRepository.artist(itemId)
                songs.addAll(artist.songs)
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM_ARTIST -> {
                val artist: Artist =
                    artistRepository.albumArtist(albumRepository.album(itemId).albumArtist!!)
                songs.addAll(artist.songs)
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST -> {
                val playlist: Playlist = playlistRepository.playlist(itemId)
                songs.addAll(playlist.getSongs())
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE -> {
                songs.addAll(genreRepository.songs(itemId))
                musicService.openQueue(songs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SHUFFLE -> {
                val allSongs: ArrayList<Song> = songRepository.songs() as ArrayList<Song>
                makeShuffleList(allSongs, -1)
                musicService.openQueue(allSongs, 0, true)
            }
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY,
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SUGGESTIONS,
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS,
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE -> {
                val tracks: List<Song> = when (category) {
                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY -> topPlayedRepository.recentlyPlayedTracks()
                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SUGGESTIONS -> topPlayedRepository.recentlyPlayedTracks()
                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS -> topPlayedRepository.recentlyPlayedTracks()
                    else -> musicService.playingQueue
                }
                songs.addAll(tracks)
                var songIndex = MusicUtil.indexOfSongInList(tracks, itemId)
                if (songIndex == -1) {
                    songIndex = 0
                }
                musicService.openQueue(songs, songIndex, true)
            }
            else -> {
            }
        }
        musicService.play()
    }

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        val songs = ArrayList<Song>()
        if (query.isNullOrEmpty()) {
            // The user provided generic string e.g. 'Play music'
            // Build appropriate playlist queue
            songs.addAll(songRepository.songs())
        } else {
            // Build a queue based on songs that match "query" or "extras" param
            val mediaFocus: String? = extras?.getString(MediaStore.EXTRA_MEDIA_FOCUS)
            if (mediaFocus == MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE) {
                val artistQuery = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                if (artistQuery != null) {
                    artistRepository.artists(artistQuery).forEach {
                        songs.addAll(it.songs)
                    }
                }
            } else if (mediaFocus == MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE) {
                val albumQuery = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                if (albumQuery != null) {
                    albumRepository.albums(albumQuery).forEach {
                        songs.addAll(it.songs)
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            // No focus found, search by query for song title
            query?.also {
                songs.addAll(songRepository.songs(it))
            }
        }

        musicService.openQueue(songs, 0, true)

        musicService.play()
    }

    override fun onPlay() {
        super.onPlay()
        musicService.play()
    }

    override fun onPause() {
        super.onPause()
        musicService.pause()
        Log.d("SleepTimeAudioPausedMediaSession", "pause:  True")

    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        musicService.playNextSong(true)
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        musicService.back(true)
    }

    override fun onStop() {
        super.onStop()
        musicService.quit()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        musicService.seek(pos.toInt())
    }

    override fun onCustomAction(action: String, extras: Bundle?) {
        when (action) {
            CYCLE_REPEAT -> {
                cycleRepeatMode()
                musicService.updateMediaSessionPlaybackState()
            }

            TOGGLE_SHUFFLE -> {
                musicService.toggleShuffle()
                musicService.updateMediaSessionPlaybackState()
            }
            TOGGLE_FAVORITE -> {
                MusicUtil.toggleFavorite(context, MusicPlayerRemote.currentSong)
                musicService.updateMediaSessionPlaybackState()
            }
            else -> {
                println("Unsupported action: $action")
            }
        }
    }

    private fun checkAndStartPlaying(songs: ArrayList<Song>, itemId: Long) {
        var songIndex = MusicUtil.indexOfSongInList(songs, itemId)
        if (songIndex == -1) {
            songIndex = 0
        }
        openQueue(songs, songIndex)
    }

    private fun openQueue(songs: ArrayList<Song>, index: Int, startPlaying: Boolean = true) {
        MusicPlayerRemote.openQueue(songs, index, startPlaying)
    }
}
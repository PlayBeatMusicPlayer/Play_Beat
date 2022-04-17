package com.knesarcreation.playbeat.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.knesarcreation.playbeat.activity.ActivityBottomBarFragmentContainer
import com.knesarcreation.playbeat.database.AlbumModel
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ArtistsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.model.FolderModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class LoadAllAudios(var context: Context, val fromSplash: Boolean) {
    private var albumList = ArrayList<AlbumModel>()
    private var artistsList = ArrayList<ArtistsModel>()
    private var queueAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private var mViewModelClass: ViewModelClass =
        ViewModelProvider(context as AppCompatActivity)[ViewModelClass::class.java]

    private var storage = StorageUtil(context)

    fun loadAudio(isFilteredAudio: Boolean) {
        queueAudioList.clear()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA, // path
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.YEAR,
        )

        var duration = storage.getFilterAudioDuration()

        if (storage.getIsAudioPlayedFirstTime()) {
            duration = 20L
        }

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs =
            arrayOf(TimeUnit.MILLISECONDS.convert(duration, TimeUnit.SECONDS).toString())

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val query = context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        (context as AppCompatActivity).lifecycleScope.launch(Dispatchers.IO) {
            queryAudio(query, isFilteredAudio)
        }
    }

    private fun queryAudio(query: Cursor?, isFilteredAudio: Boolean) {
        query.use { cursor ->
            // Cache column indices.
            val idColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            //mViewModelClass.deleteSongs(lifecycleScope)

            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val data = cursor.getString(dataColumn)
                val dateAdded = cursor.getString(dateAddedColumn)
                val artistId = cursor.getLong(artistIdColumn)
                val displayName = cursor.getString(displayNameColumn)
                val year = cursor.getInt(yearColumn)
                //val contentType = cursor.getString(contentTypeColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                //val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                val artUri = Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                // getting audio uri
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                /* var thumbnail: Bitmap? = null
                 if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                     try {
                         thumbnail =
                             applicationContext.contentResolver.loadThumbnail(
                                 contentUri, Size(640, 480), null
                             )
                     } catch (e: java.lang.Exception) {
                         e.printStackTrace()
                     }
                 }*/

                try {
                    val allSongsModel = if (displayName != null) {

                        AllSongsModel(
                            id,
                            albumId,
                            name.trim(),
                            artist.trim(),
                            album.trim(),
                            size,
                            duration,
                            data,
                            contentUri.toString(),
                            artUri,
                            dateAdded,
                            false,
                            0L,
                            artistId,
                            displayName,
                            "",
                            year,
                            "",
                            "",
                            0
                        )
                    } else {
                        AllSongsModel(
                            id,
                            albumId,
                            name.trim(),
                            artist.trim(),
                            album.trim(),
                            size,
                            duration,
                            data,
                            contentUri.toString(),
                            artUri,
                            dateAdded,
                            false,
                            0L,
                            artistId,
                            "",
                            "",
                            year,
                            "",
                            "",
                            0
                        )
                    }

                    /*storage.loadAudioIndex()
             val playingAudio = storage.loadAudio()[]*/
                    allSongsModel.playingOrPause = -1
                    queueAudioList.add(allSongsModel)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val audioCount = storage.getAudioCount()
            //Toast.makeText(this, "${audioCount} , ${query!!.count}", Toast.LENGTH_SHORT).show()
            if (audioCount == 0) {
                storage.saveAudioCount(query?.count!!)
                storage.storeAudio(queueAudioList)
                // insert all audios to DB
                for (audioModel in queueAudioList) {
                    mViewModelClass.insertAllSongs(
                        audioModel,
                        (context as AppCompatActivity).lifecycleScope
                    )
                }
                // insert all album
                loadAlbum()
                loadArtists()
                //addFolderInAudio()
                //loadAudioArtThumbnail()
            } else {
                if (audioCount < query!!.count) {
                    mViewModelClass.deleteAllAlbum((context as AppCompatActivity).lifecycleScope)
                    storage.saveAudioCount(query.count)

                    /* val oldQueueAudio = storage.loadQueueAudio()
                     val currentPlayingAudioIndex = storage.loadAudioIndex()
                     val currentPlayingAudio = oldQueueAudio[currentPlayingAudioIndex]*/

                    // if any new audio added then update DB with new audio
                    val storedAudioList = storage.loadAudio()
                    var isNewAudioFound = false
                    for (newAudio in queueAudioList) {
                        for (audio in storedAudioList) {
                            if (newAudio.songId == audio.songId) {
                                isNewAudioFound = false
                                break
                            } else {
                                isNewAudioFound = true
                            }
                        }
                        if (isNewAudioFound) {
                            mViewModelClass.insertAllSongs(
                                newAudio,
                                (context as AppCompatActivity).lifecycleScope
                            )
                        }
                    }

                    loadAlbum()
                    loadArtists()
                    //addFolderInAudio()
                    //loadAudioArtThumbnail()
                    storage.storeAudio(queueAudioList)
                } else if (audioCount > query.count) {

                    // if any audio deleted then update DB
                    val storedAudioList = storage.loadAudio()
                    val loadQueueAudio = storage.loadQueueAudio()
                    var audioIndex = storage.loadAudioIndex()
                    if (audioIndex == -1) {
                        audioIndex = 0
                        storage.storeAudioIndex(audioIndex)
                    }
                    if (loadQueueAudio.isNotEmpty()) {
                        val currentPlayingAudio = loadQueueAudio[audioIndex]
                        var isAudioFound = false

                        val currentPlayingAudioData = currentPlayingAudio.data
                        val currentPlayingAudioFile =
                            File(Uri.parse(currentPlayingAudioData).path!!)
                        if (currentPlayingAudioFile.exists()) {
                            mViewModelClass.deleteAllAlbum((context as AppCompatActivity).lifecycleScope)
                        }

                        var isPlayingAudioFilterdOut = true
                        for (audio in storedAudioList) {
                            for (queriedAudio in queueAudioList) {
                                if (queriedAudio.songId == audio.songId) {
                                    isAudioFound = true
                                    break
                                } else {
                                    if (currentPlayingAudio.songId == queriedAudio.songId) {
                                        isPlayingAudioFilterdOut = false
                                    }
                                    isAudioFound = false
                                }
                            }
                            if (!isAudioFound) {
                                if (currentPlayingAudioFile.exists()) {
                                    // if current playing audio file deleted then this code will not execute
                                    // this code will only execute if current playing audio file is not deleted
                                    Log.d(
                                        "isAudioWasPlaying",
                                        "queryAudio:${isPlayingAudioFilterdOut} "
                                    )
                                    /*if (audio.playingOrPause == 1 || audio.playingOrPause == 0) {
                                    isAudioWasPlaying = true
                                }*/

                                    loadQueueAudio.remove(audio)
                                    mViewModelClass.deleteOneSong(
                                        audio.songId,
                                        (context as AppCompatActivity).lifecycleScope
                                    )
                                }
                            }
                        }

                        if (isPlayingAudioFilterdOut) {
                            if (isFilteredAudio) {
                                audioIndex = 0
                                storage.storeAudioIndex(audioIndex)
                                storage.storeQueueAudio(loadQueueAudio)
                                storage.storeAudio(queueAudioList)

                                if (AllSongFragment.musicService?.mediaPlayer != null) {
                                    AllSongFragment.musicService?.pausedByManually = true
                                    val broadcastIntent =
                                        Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
                                    (context as AppCompatActivity).sendBroadcast(
                                        broadcastIntent
                                    )

                                    Log.d(
                                        "SongThatWillBeDelete",
                                        "deleteAudioFromDevice: New audio played "
                                    )
                                }

                                if (loadQueueAudio.isNotEmpty()) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        AllSongFragment.musicService?.pauseMedia()
                                        AllSongFragment.musicService?.pausedByManually = true
                                        AllSongFragment.musicService?.updateNotification(
                                            isAudioPlaying = false
                                        )
                                        ///binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_play_audio)
                                        // highlight pause audio with pause anim
                                        mViewModelClass.updateSong(
                                            currentPlayingAudio.songId,
                                            currentPlayingAudio.songName,
                                            0,
                                            (context as AppCompatActivity).lifecycleScope
                                        )
                                        mViewModelClass.updateQueueAudio(
                                            currentPlayingAudio.songId,
                                            currentPlayingAudio.songName,
                                            0,
                                            (context as AppCompatActivity).lifecycleScope
                                        )
                                    }, 500)
                                }
                            }
                        }


                        // loadAudioArtThumbnail()

                        //get a new index when any audio is deleted
                        if (currentPlayingAudioFile.exists()) {
                            // if audio is not filtered from durations then this code will execute
                            if (!isPlayingAudioFilterdOut) {
                                val newCurrentPlayingAudioIndex =
                                    loadQueueAudio.indexOf(currentPlayingAudio)
                                storage.storeAudioIndex(newCurrentPlayingAudioIndex)
                            }

                            //save query count only when current playing audio not deleted
                            storage.storeQueueAudio(loadQueueAudio)
                            storage.storeAudio(queueAudioList)
                            storage.saveAudioCount(query.count)

                            loadAlbum()
                            loadArtists()
                        }
                    } else {
                        mViewModelClass.deleteAllAlbum((context as AppCompatActivity).lifecycleScope)
                        var isAudioFound = false

                        for (audio in storedAudioList) {
                            for (queriedAudio in queueAudioList) {
                                if (queriedAudio.songId == audio.songId) {
                                    isAudioFound = true
                                    break
                                } else {
                                    isAudioFound = false
                                }
                            }
                            if (!isAudioFound) {
                                loadQueueAudio.remove(audio)
                                mViewModelClass.deleteOneSong(
                                    audio.songId,
                                    (context as AppCompatActivity).lifecycleScope
                                )
                            }
                        }

                        // no audio was playing save zero index
                        audioIndex = 0
                        storage.storeAudioIndex(audioIndex)
                        storage.storeQueueAudio(loadQueueAudio)
                        storage.storeAudio(queueAudioList)
                        storage.saveAudioCount(query.count)

                        loadAlbum()
                        loadArtists()
                    }
                }
            }

            cursor.close()

            getFoldersFromAudio()

            if (fromSplash) {
                Handler(Looper.getMainLooper()).postDelayed({
                    context.startActivity(
                        Intent(
                            context,
                            ActivityBottomBarFragmentContainer::class.java
                        )
                    )
                    (context as AppCompatActivity).finish()
                }, 1000)
            }
        }
    }

    private fun getFoldersFromAudio() {
        //creating audio paths/uris list
        val pathsList: ArrayList<String> = ArrayList()
        pathsList.clear()
        for (i in 0 until queueAudioList.size) {
            val folderName: String =
                File(queueAudioList[i].data).parentFile!!.name
            val folderId: String =
                File(queueAudioList[i].data).parentFile!!.parent!!
            pathsList.add("$folderId/$folderName")
        }

        //generating folder names from audio paths/uris
        val folderList: MutableList<FolderModel> = ArrayList()
        folderList.clear()
        for (i in 0 until queueAudioList.size) {
            val folderName: String = File(queueAudioList[i].data).parentFile!!.name
            val folderId: String = File(queueAudioList[i].data).parentFile!!.parent!!
            val count: Int = Collections.frequency(pathsList, "$folderId/$folderName")
            /* var folderRoot1 = ""
             val folderRoot: String = if (queriedAudioList[i].data.contains("emulated")) {
                 "emulated"
             } else {
                 "storage"
             }
             if (i > 0) {
                 folderRoot1 = if (queriedAudioList[i - 1].data.contains("emulated")) {
                     "emulated"
                 } else {
                     "storage"
                 }
             }*/

            mViewModelClass.updateFolderInAudio(
                queueAudioList[i].songId,
                "${folderId}/$folderName",
                folderName,
                count,
                (context as AppCompatActivity).lifecycleScope
            )

            /* if (i == 0) {
                 *//*val model = FolderModel(
                    folderId,
                    folderName,
                    count
                )
                folderList.add(model)*//*
                mViewModelClass.updateFolderInAudio(
                    queriedAudioList[i].songId,
                    "${folderId}/$folderName",
                    folderName,
                    count,
                    lifecycleScope
                )

            } else if (folderName == File(queriedAudioList[i - 1].data).parentFile!!
                    .name && folderRoot == folderRoot1
            ) {
                //exclude
            } else {
                *//* val model = FolderModel(
                     folderId,
                     folderName,
                     count
                 )
                 folderList.add(model)*//*
                mViewModelClass.updateFolderInAudio(
                    queriedAudioList[i].songId,
                    "${folderId}/$folderName",
                    folderName,
                    count,
                    lifecycleScope
                )
            }*/
        }

        Log.d("folderListSplash", "getFoldersFromAudio: $folderList ")
    }

    private fun loadAlbum() {
        albumList.clear()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            /*MediaStore.Audio.Albums.ALBUM_ID,*/
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.LAST_YEAR,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
        )

        // Show only audios that are at least 1 minutes in duration.
        //val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        // val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display audios in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        val query: Cursor? = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            //val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val lastYearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.LAST_YEAR)
            val noOfSongsColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                //val albumId = cursor.getLong(albumIdColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)
                val lastYear = cursor.getInt(lastYearColumn)
                val noOfSongs = cursor.getInt(noOfSongsColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                //val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                val artUri = Uri.withAppendedPath(sArtworkUri, id.toString()).toString()

                val audioAlbumModel =
                    AlbumModel(
                        id,
                        album.trim(),
                        artist.trim(),
                        artUri,
                        lastYear,
                        noOfSongs
                    )

                if (!albumList.contains(audioAlbumModel)) {
                    albumList.add(audioAlbumModel)
                }
            }
            cursor.close()
            for (album in albumList) {
                mViewModelClass.insertAlbum(album, (context as AppCompatActivity).lifecycleScope)
            }
        }
    }

    private fun loadArtists() {
        artistsList.clear()
        val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            //MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            //MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        // Show only audios that are at least 1 minutes in duration.
        //val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        // val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display audios in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        val query =
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )


        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistsIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            /*val noOfAlbumsColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val noOfTracksColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)*/


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val artists = cursor.getString(artistsIdColumn)
                //val noOfAlbum = cursor.getInt(noOfAlbumsColumn)
                //val noOfTracks = cursor.getInt(noOfTracksColumn)

                //getting album art uri
                //var bitmap: Bitmap? = null
                //val sArtworkUri = Uri
                //   .parse("content://media/external/audio/albumart")
                //val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                // val artUri = Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()

                val artistModel =
                    ArtistsModel(
                        id,
                        artists.trim(),
                        //noOfAlbum,
                        //noOfTracks,
                    )
                artistsList.add(artistModel)
                mViewModelClass.insertArtist(
                    artistModel,
                    (context as AppCompatActivity).lifecycleScope
                )
            }
            cursor.close()
            /*for (artistModel in artistsList) {
                mViewModelClass.insertArtist(artistModel, lifecycleScope)
            }*/
        }
    }

}
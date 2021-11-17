package com.knesarcreation.playbeat.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.knesarcreation.playbeat.BuildConfig
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AlbumModel
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ArtistsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.model.AudioArtBitmapModel
import com.knesarcreation.playbeat.model.FolderModel
import com.knesarcreation.playbeat.utils.MakeStatusBarTransparent
import com.knesarcreation.playbeat.utils.StorageUtil
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private var mPermRequest: ActivityResultLauncher<String>? = null
    private var mReqPermForManageAllFiles: ActivityResultLauncher<Intent>? = null
    private lateinit var mViewModelClass: ViewModelClass
    private lateinit var storage: StorageUtil
    private var queriedAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private val albumList = ArrayList<AlbumModel>()
    private val artistsList = ArrayList<ArtistsModel>()
    private val audioArtBitmapList = ArrayList<AudioArtBitmapModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen)

        storage = StorageUtil(this)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        MakeStatusBarTransparent().transparent(this)
        requestStoragePermission()

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadAudio()
            } else {
                showPermissionAlert()
            }
        } else {*/
        mPermRequest!!.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        //}

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mPermRequest!!.launch(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            mPermRequest!!.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }*/

    }

    private fun requestStoragePermission() {
        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             mReqPermForManageAllFiles = registerForActivityResult(
                 ActivityResultContracts.StartActivityForResult()
             ) {
                 // for android 11 and above
                 if (Environment.isExternalStorageManager()) {
                     // Permission granted. Now resume workflow.
                     loadAudio()
                 } else {
                     showPermissionAlert()
                 }
             }
         } else {*/
        // for android 10 and below
        mPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    // do stuff if permission granted
                    loadAudio()

                } else {
                    val permAlert = AlertDialog.Builder(this)
                    permAlert.setMessage("Storage permission is required to read Media Files. Please grant permission to proceed further.")
                    permAlert.setPositiveButton("Allow") { dialog, _ ->
                        mPermRequest!!.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        dialog.dismiss()
                    }
                    permAlert.setNegativeButton("Dismiss") { dialog, _ ->
                        Toast.makeText(
                            this,
                            "Permission is required to access media files",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        finish()
                        dialog.dismiss()
                    }
                    permAlert.setCancelable(false)
                    permAlert.show()

                }
            }
        //}
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionAlert() {
        val permAlert = AlertDialog.Builder(this)
        permAlert.setMessage("Play beat required external storage permission to manage audio files. Please grant permission to proceed further.")
        permAlert.setPositiveButton("Grant") { dialog, _ ->
            val intent = Intent(
                ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
            )

            mReqPermForManageAllFiles?.launch(intent)
            dialog.dismiss()
        }
        permAlert.setNegativeButton("Dismiss") { dialog, _ ->
            Toast.makeText(
                this,
                "Permission is required to access media files",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
            dialog.dismiss()
        }
        permAlert.setCancelable(false)
        permAlert.show()
    }

    private fun loadAudio() {
        queriedAudioList.clear()

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

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val query = contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        queryAudio(query)
    }

    private fun queryAudio(query: Cursor?) {
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
                    queriedAudioList.add(allSongsModel)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val audioCount = storage.getAudioCount()
            if (audioCount == 0) {
                storage.saveAudioCount(query?.count!!)
                storage.storeAudio(queriedAudioList)
                // insert all audios to DB
                for (audioModel in queriedAudioList) {
                    mViewModelClass.insertAllSongs(audioModel, lifecycleScope)
                }
                // insert all album
                loadAlbum()
                loadArtists()
                //addFolderInAudio()
                //loadAudioArtThumbnail()
            } else {
                if (audioCount < query!!.count) {
                    mViewModelClass.deleteAllAlbum(lifecycleScope)
                    storage.saveAudioCount(query.count)

                    /* val oldQueueAudio = storage.loadQueueAudio()
                     val currentPlayingAudioIndex = storage.loadAudioIndex()
                     val currentPlayingAudio = oldQueueAudio[currentPlayingAudioIndex]*/

                    // if any new audio added then update DB with new audio
                    val storedAudioList = storage.loadAudio()
                    var isNewAudioFound = false
                    for (newAudio in queriedAudioList) {
                        for (audio in storedAudioList) {
                            if (newAudio.songId == audio.songId) {
                                isNewAudioFound = false
                                break
                            } else {
                                isNewAudioFound = true
                            }
                        }
                        if (isNewAudioFound) {
                            mViewModelClass.insertAllSongs(newAudio, lifecycleScope)
                        }
                    }

                    loadAlbum()
                    loadArtists()
                    //addFolderInAudio()
                    //loadAudioArtThumbnail()
                    storage.storeAudio(queriedAudioList)
                }
                else if (audioCount > query.count) {

                    // if any audio deleted then update DB
                    val storedAudioList = storage.loadAudio()
                    val loadQueueAudio = storage.loadQueueAudio()
                    val audioIndex = storage.loadAudioIndex()
                    val currentPlayingAudio = loadQueueAudio[audioIndex]
                    var isAudioFound = false

                    val data = currentPlayingAudio.data
                    val file = File(Uri.parse(data).path!!)
                    if (file.exists()) {
                        mViewModelClass.deleteAllAlbum(lifecycleScope)
                    }

                    for (audio in storedAudioList) {
                        for (queriedAudio in queriedAudioList) {
                            if (queriedAudio.songId == audio.songId) {
                                isAudioFound = true
                                break
                            } else {
                                isAudioFound = false
                            }
                        }
                        if (!isAudioFound) {
                            if (file.exists()) {
                                // if current playing audio file deleted then this code will not execute
                                // this code will only execute if current playing audio file is not deleted
                                loadQueueAudio.remove(audio)
                                mViewModelClass.deleteOneSong(audio.songId, lifecycleScope)
                            }
                        }
                    }

                    // loadAudioArtThumbnail()
                    //get a new index when any audio is deleted
                    if (file.exists()) {
                        val newCurrentPlayingAudioIndex =
                            loadQueueAudio.indexOf(currentPlayingAudio)
                        storage.storeAudioIndex(newCurrentPlayingAudioIndex)
                        //save query count only when current playing audio not deleted
                        storage.storeQueueAudio(loadQueueAudio)
                        storage.storeAudio(queriedAudioList)
                        storage.saveAudioCount(query.count)

                        loadAlbum()
                        loadArtists()
                        //  addFolderInAudio()
                    }
                }
            }

            cursor.close()

            getFoldersFromAudio()

            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, ActivityBottomBarFragmentContainer::class.java))
                finish()
            }, 1000)
        }
    }

    private fun getFoldersFromAudio() {
        //creating audio paths/uris list
        val pathsList: ArrayList<String> = ArrayList()
        pathsList.clear()
        for (i in 0 until queriedAudioList.size) {
            val folderName: String =
                File(queriedAudioList[i].data).parentFile!!.name
            val folderId: String =
                File(queriedAudioList[i].data).parentFile!!.parent!!
            pathsList.add("$folderId/$folderName")
        }

        //generating folder names from audio paths/uris
        val folderList: MutableList<FolderModel> = ArrayList()
        folderList.clear()
        for (i in 0 until queriedAudioList.size) {
            val folderName: String = File(queriedAudioList[i].data).parentFile!!.name
            val folderId: String = File(queriedAudioList[i].data).parentFile!!.parent!!
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
                queriedAudioList[i].songId,
                "${folderId}/$folderName",
                folderName,
                count,
                lifecycleScope
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

    /* private fun loadAudioArtThumbnail() {
         audioArtBitmapList.clear()
         var thumbnail: Bitmap? = null
         for (audio in queriedAudioList) {
             if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                 try {
                     thumbnail =
                         applicationContext.contentResolver.loadThumbnail(
                             Uri.parse(audio.contentUri), Size(640, 480), null
                         )
                     audioArtBitmapList.add(
                         AudioArtBitmapModel(
                             audio.songId,
                             audio.songName,
                             thumbnail
                         )
                     )
                 } catch (e: java.lang.Exception) {
                     e.printStackTrace()
                 }
             }
         }
         storage.storeAudioArtBitmapImage(audioArtBitmapList)
     }*/

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

        val query: Cursor? = contentResolver.query(
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
                mViewModelClass.insertAlbum(album, lifecycleScope)
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
            contentResolver.query(
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
                mViewModelClass.insertArtist(artistModel, lifecycleScope)
            }
            cursor.close()
            /*for (artistModel in artistsList) {
                mViewModelClass.insertArtist(artistModel, lifecycleScope)
            }*/
        }
    }

}
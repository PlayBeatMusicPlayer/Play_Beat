package com.knesarcreation.playbeat.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentUris
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.utils.MakeStatusBarTransparent
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private var mPermRequest: ActivityResultLauncher<String>? = null
    private lateinit var mViewModelClass: ViewModelClass
    private lateinit var storage: StorageUtil
    private var queriedAudioList = CopyOnWriteArrayList<AllSongsModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen)

        storage = StorageUtil(this)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        MakeStatusBarTransparent().transparent(this)
        requestStoragePermission()

        mPermRequest!!.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    }

    private fun requestStoragePermission() {
        mPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    // do stuff if permission granted
                    loadAudio()

                } else {
                    val permAlert = AlertDialog.Builder(this)
                    permAlert.setMessage("Storage permission is required to access Media Files")
                    permAlert.setPositiveButton("Allow") { dialog, _ ->
                        mPermRequest!!.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
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
            MediaStore.Audio.Media.DATE_ADDED
        )


        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

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

                val allSongsModel =
                    AllSongsModel(
                        id,
                        albumId,
                        name,
                        artist,
                        album,
                        size,
                        duration,
                        data,
                        contentUri.toString(),
                        artUri,
                        dateAdded,
                        false,
                        0L
                    )

                /*storage.loadAudioIndex()
                val playingAudio = storage.loadAudio()[]*/
                allSongsModel.playingOrPause = -1
                queriedAudioList.add(allSongsModel)
            }

            val audioCount = storage.getAudioCount()
            if (audioCount == 0) {
                storage.saveAudioCount(query?.count!!)
                storage.storeAudio(queriedAudioList)
                // insert all audios to DB
                for (audioModel in queriedAudioList) {
                    mViewModelClass.insertAllSongs(audioModel, lifecycleScope)
                }
            } else {
                if (audioCount < query!!.count) {
                    storage.saveAudioCount(query.count)
                    // if any new audio added then update DB with new audio
                    val storedAudioList = storage.loadAudio()
                    var isNewAudioFound = false
                    for (queriedAudio in queriedAudioList) {
                        for (audio in storedAudioList) {
                            if (queriedAudio.songId == audio.songId) {
                                isNewAudioFound = false
                                break
                            } else {
                                isNewAudioFound = true
                            }
                        }
                        if (isNewAudioFound) {
                            mViewModelClass.insertAllSongs(queriedAudio, lifecycleScope)
                        }
                    }
                    storage.storeAudio(queriedAudioList)
                } else if (audioCount > query.count) {
                    storage.saveAudioCount(query.count)
                    // if any audio deleted then update DB
                    val storedAudioList = storage.loadAudio()
                    var isAudioFound = false
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
                            mViewModelClass.deleteOneSong(audio.songId, lifecycleScope)
                        }
                    }
                    storage.storeAudio(queriedAudioList)
                }
            }

            cursor.close()

            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, ActivityBottomBarFragmentContainer::class.java))
                finish()
            }, 1000)
        }
    }
}
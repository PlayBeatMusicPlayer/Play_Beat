package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.databinding.FragmentAllSongBinding
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.service.PlayBeatMusicService
import com.knesarcreation.playbeat.utils.AudioPlayingFromCategory
import com.knesarcreation.playbeat.utils.CustomProgressDialog
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit


class AllSongFragment : Fragment(), ServiceConnection, AllSongsAdapter.OnClickSongItem {
    private lateinit var allSongsAdapter: AllSongsAdapter
    lateinit var progressBar: CustomProgressDialog
    private var _binding: FragmentAllSongBinding? = null
    private val binding get() = _binding

    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()

    companion object {
        const val Broadcast_PLAY_NEW_AUDIO = "com.knesarcreation.playbeat.utils.PlayNewAudio"
        const val Broadcast_BOTTOM_UPDATE_PLAYER_UI =
            "com.knesarcreation.playbeat.utils.UpdatePlayerUi"
        const val READ_STORAGE_PERMISSION = 101
        var musicService: PlayBeatMusicService? = null
        //var serviceBound = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAllSongBinding.inflate(inflater, container, false)
        val view = binding?.root

        //checkPermission(activity as Context)

        lifecycleScope.launch(Dispatchers.IO) {
            loadAudio()
        }

        return view!!

    }

    @SuppressLint("Range")
    private fun loadAudio() {
        audioList.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA, // path
        )

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        val query =
            (activity as Context).contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)


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
                        albumId,
                        name,
                        artist,
                        album,
                        size,
                        duration,
                        data,
                        contentUri.toString(),
                        artUri
                    )
                audioList.add(allSongsModel)
            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {
                allSongsAdapter =
                    AllSongsAdapter(activity as Context, audioList, this)
                binding!!.rvAllSongs.adapter = allSongsAdapter
                cursor.close()
                // progressBar.dismiss()
            }
        }

        // after loading audio start the service
        startService()
    }

    private fun startService() {
        if (musicService == null) {
            val storage = StorageUtil(activity as AppCompatActivity)
            storage.storeAudio(audioList)
            storage.storeAudioIndex(0) // since service is creating firstTime
            Log.d(
                "AlbumFragment.musicService",
                "playAudio: its null... service created : Service is  null"
            )
            val playerIntent = Intent(activity as Context, PlayBeatMusicService::class.java)
            (activity as AppCompatActivity).startService(playerIntent)
            (activity as AppCompatActivity).bindService(
                playerIntent,
                this,
                Context.BIND_AUTO_CREATE
            )
            Log.d(
                "AlbumFragment.musicService",
                "playAudio: its null... service created : Service is  null"
            )
        }
        val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)
    }

    private fun playAudio(audioIndex: Int) {
        this.audioIndexPos = audioIndex
        val storage = StorageUtil(activity as Context)
        if (AudioPlayingFromCategory.audioPlayingFromAlbumORArtist) {
            storage.storeAudio(audioList)
            AudioPlayingFromCategory.audioPlayingFromAlbumORArtist = false
        }
        //Store the new audioIndex to SharedPreferences
        storage.storeAudioIndex(audioIndex)

        //Service is active send broadcast
        val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
        (activity as AppCompatActivity).sendBroadcast(broadcastIntent)

        val updatePlayer = Intent(Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        (activity as Context).sendBroadcast(updatePlayer)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as PlayBeatMusicService.LocalBinder
        musicService = binder.getService()
        //serviceBound = true
        Log.d("AllSongServicesBounded", "onServiceConnected: connected servic")
        //controlAudio()
        // Toast.makeText(this, "Service Bound", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        musicService = null
        Toast.makeText(activity as Context, "null serivce", Toast.LENGTH_SHORT).show()
        //serviceBound = false
    }


    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        playAudio(position)
    }


    override fun onDestroy() {
        super.onDestroy()
        isDestroyedActivity = true
    }


    /* override fun onRequestPermissionsResult(
         requestCode: Int,
         permissions: Array<out String>,
         grantResults: IntArray
     ) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)
         when (requestCode) {
             READ_STORAGE_PERMISSION -> {
                 if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                     if (ContextCompat.checkSelfPermission(
                             activity as Context,
                             android.Manifest.permission.READ_EXTERNAL_STORAGE
                         ) == PackageManager.PERMISSION_GRANTED
                     ) {
                         lifecycleScope.launch(Dispatchers.IO) {
                             loadAudio()
                         }
                     }
                 } else {
                     Toast.makeText(
                         activity as Context,
                         "Permission is required to show music",
                         Toast.LENGTH_SHORT
                     )
                         .show()
                     (context as AppCompatActivity).finish()
                 }
             }
         }
     }*/
}
package com.knesarcreation.playbeat.activity

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.provider.MediaStore.Audio
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.databinding.ActivityAllSongsBinding
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.receiver.MediaButtonIntentReceiver
import com.knesarcreation.playbeat.service.PlayBeatMusicService
import com.knesarcreation.playbeat.service.PlayBeatMusicService.LocalBinder
import com.knesarcreation.playbeat.utils.CustomProgressDialog
import com.knesarcreation.playbeat.utils.PlaybackStatus
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.content.ComponentName

import android.media.AudioManager





class PlayerActivity : AppCompatActivity(), ServiceConnection, AllSongsAdapter.OnClickSongItem {

    private var musicService: PlayBeatMusicService? = null
    private lateinit var allSongsAdapter: AllSongsAdapter
    var serviceBound = false
    lateinit var progressBar: CustomProgressDialog
    lateinit var binding: ActivityAllSongsBinding

    companion object {
        var audioList = ArrayList<AllSongsModel>()
        const val Broadcast_PLAY_NEW_AUDIO = " com.knesarcreation.playbeat.utils.PlayNewAudio"
        const val Broadcast_UPDATE_PLAYER_UI = " com.knesarcreation.playbeat.utils.UpdatePlayerUi"
    }

    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllSongsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = CustomProgressDialog(this)
        progressBar.setMessage("Fetching Songs..")
        progressBar.setCanceledOnOutsideTouch(false)
        progressBar.show()

        registerUpdatePlayerUI()

        lifecycleScope.launch(Dispatchers.IO) {
            loadAudio()
        }

    }

//    private fun registerMediaBtn() {
//        val filter = IntentFilter(Intent.ACTION_MEDIA_BUTTON)
//        val receiver = MediaButtonIntentReceiver()
//        filter.priority = 1000000
//        regisM(receiver, filter)
//
//    }

    @SuppressLint("Range")
    private fun loadAudio() {
        audioList.clear()
        val collection = Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            Audio.Media._ID,
            Audio.Media.DISPLAY_NAME,
            Audio.Media.DURATION,
            Audio.Media.SIZE,
            Audio.Media.ALBUM,
            Audio.Media.ARTIST,
            Audio.Media.ALBUM_ID,
            Audio.Media.DATA, // path
        )

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${Audio.Media.DISPLAY_NAME} ASC"

        val query =
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(Audio.Media.DATA)


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
                    Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val allSongsModel =
                    AllSongsModel(
                        albumId,
                        null,
                        name,
                        artist,
                        album,
                        size,
                        duration,
                        data,
                        contentUri,
                        artUri
                    )
                audioList.add(allSongsModel)
            }

            // Stuff that updates the UI
            runOnUiThread {
                allSongsAdapter = AllSongsAdapter(this, audioList, this)
                binding.rvAllSongs.adapter = allSongsAdapter
                cursor.close()
                progressBar.dismiss()
            }
        }

    }

    private fun playAudio(activeAudio: AllSongsModel?, audioIndex: Int) {
        this.audioIndexPos = audioIndex
        //Check is service is active
        if (!serviceBound) {
            val storage = StorageUtil(applicationContext)
            // storage.storeAudio(audioList)
            storage.storeAudioIndex(audioIndex)

            val playerIntent = Intent(this, PlayBeatMusicService::class.java)
            //playerIntent.putExtra("audioPosition", audioIndex)
            startService(playerIntent)
            bindService(playerIntent, this, Context.BIND_AUTO_CREATE)

        } else {
            //Store the new audioIndex to SharedPreferences
            val storage = StorageUtil(applicationContext)
            storage.storeAudioIndex(audioIndex)

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
        }

        updatePlayingMusic(audioIndex)

    }


    private fun controlAudio() {
        binding.skipNextAudio.setOnClickListener {
            val audioIndexPos = getSongFromPos()
            val storage = StorageUtil(applicationContext)
            storage.storeAudioIndex(audioIndexPos)
            val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            updatePlayingMusic(audioIndexPos)
//            Toast.makeText(this, "Skipped Next", Toast.LENGTH_SHORT).show()
        }

        binding.playPauseIV.setOnClickListener {
            if (musicService?.mediaPlayer!!.isPlaying) {
                musicService?.pauseMedia()
                binding.playPauseIV.setImageResource(R.drawable.ic_play_audio)
            } else if (!musicService?.mediaPlayer!!.isPlaying) {
                musicService?.playMedia()
                binding.playPauseIV.setImageResource(R.drawable.ic_pause_audio)
                musicService?.updateMetaData()
                musicService?.buildNotification(
                    PlaybackStatus.PLAYING,
                    PlaybackStatus.UN_FAVOURITE,
                    1f
                )
            }
        }
    }

    private fun getSongFromPos(): Int {
        return if (audioIndexPos == audioList.size - 1) {
            //if last in playlist
            //audioIndexPos = 0
            //audioList[audioIndexPos]
            0
        } else {
            //get next in playlist
            /*audioList[++audioIndexPos]*/
            ++audioIndexPos
        }
    }

    private fun updatePlayingMusic(audioIndex: Int) {
        binding.songNameTV.text = audioList[audioIndex].songName
        binding.artistOrAlbumNameTV.text = audioList[audioIndex].artistsName
        Glide.with(this).load(audioList[audioIndexPos].artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.music_note_1).centerCrop())
            .into(binding.albumArtIV)
        if (musicService?.mediaPlayer != null) {
            if (musicService?.mediaPlayer!!.isPlaying) {
                binding.playPauseIV.setImageResource(R.drawable.ic_pause_audio)
            } else {
                binding.playPauseIV.setImageResource(R.drawable.ic_play_audio)
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as LocalBinder
        musicService = binder.getService()
        serviceBound = true
        controlAudio()
//        Toast.makeText(this, "Service Bound", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        serviceBound = false
    }


    override fun onSaveInstanceState(
        savedInstanceState: Bundle,
        outPersistentState: PersistableBundle
    ) {
        savedInstanceState.putBoolean("ServiceState", serviceBound)
        super.onSaveInstanceState(savedInstanceState, outPersistentState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        serviceBound = savedInstanceState.getBoolean("ServiceState")
        super.onRestoreInstanceState(savedInstanceState)
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        if (serviceBound) {
//            unbindService(this)
//            //service is active
//            musicService?.stopSelf()
////            unregisterReceiver(updatePlayerUI)
//        }
//    }

    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        playAudio(allSongModel, position)
    }

    private val updatePlayerUI: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            //Get the new media index form SharedPreferences
            audioIndexPos = StorageUtil(applicationContext).loadAudioIndex()
//            Toast.makeText(this@PlayerActivity, "$audioIndexPos", Toast.LENGTH_SHORT).show()
            //UpdatePlayer UI
            if (!isDestroyedActivity)
                updatePlayingMusic(audioIndexPos)
        }
    }

    private fun registerUpdatePlayerUI() {
        //Register playNewMedia receiver
        val filter = IntentFilter(Broadcast_UPDATE_PLAYER_UI)
        registerReceiver(updatePlayerUI, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyedActivity = true
    }

}
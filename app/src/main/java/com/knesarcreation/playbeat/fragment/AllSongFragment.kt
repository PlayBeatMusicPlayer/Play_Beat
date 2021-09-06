package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.knesarcreation.playbeat.activity.PlayerActivity
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.databinding.FragmentAllSongBinding
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.service.PlayBeatMusicService
import com.knesarcreation.playbeat.utils.CustomProgressDialog
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AllSongFragment : Fragment(), ServiceConnection, AllSongsAdapter.OnClickSongItem {
    private lateinit var allSongsAdapter: AllSongsAdapter
    var serviceBound = false
    lateinit var progressBar: CustomProgressDialog
    private var _binding: FragmentAllSongBinding? = null
    private val binding get() = _binding

    private var audioIndexPos = -1
    private var isDestroyedActivity = false

    companion object {
        var audioList = ArrayList<AllSongsModel>()
        const val Broadcast_PLAY_NEW_AUDIO = " com.knesarcreation.playbeat.utils.PlayNewAudio"
        const val Broadcast_UPDATE_PLAYER_UI = " com.knesarcreation.playbeat.utils.UpdatePlayerUi"
         var musicService: PlayBeatMusicService? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentAllSongBinding.inflate(inflater, container, false)
        val view = binding?.root

        lifecycleScope.launch(Dispatchers.IO) {
            loadAudio()
        }

        return view!!

    }

    override fun onResume() {
        super.onResume()
    }

    @SuppressLint("Range")
    private fun loadAudio() {
        PlayerActivity.audioList.clear()
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
                PlayerActivity.audioList.add(allSongsModel)
            }

            // Stuff that updates the UI
            (activity as AppCompatActivity).runOnUiThread {
                allSongsAdapter =
                    AllSongsAdapter(activity as Context, PlayerActivity.audioList, this)
                binding!!.rvAllSongs.adapter = allSongsAdapter
                cursor.close()
//                progressBar.dismiss()
            }
        }

    }

    private fun playAudio(activeAudio: AllSongsModel?, audioIndex: Int) {
        this.audioIndexPos = audioIndex
        //Check is service is active
        if (!serviceBound) {
            val storage = StorageUtil(activity as AppCompatActivity)
            // storage.storeAudio(audioList)
            storage.storeAudioIndex(audioIndex)

            val playerIntent = Intent(activity as Context, PlayBeatMusicService::class.java)
            //playerIntent.putExtra("audioPosition", audioIndex)
            (activity as AppCompatActivity).startService(playerIntent)
            (activity as AppCompatActivity).bindService(
                playerIntent,
                this,
                Context.BIND_AUTO_CREATE
            )

        } else {
            //Store the new audioIndex to SharedPreferences
            val storage = StorageUtil(activity as Context)
            storage.storeAudioIndex(audioIndex)

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            val broadcastIntent = Intent(Broadcast_PLAY_NEW_AUDIO)
            (activity as AppCompatActivity).sendBroadcast(broadcastIntent)
        }

        //updatePlayingMusic(audioIndex)

    }


    /*  private fun controlAudio() {
          binding!!.skipNextAudio.setOnClickListener {
              val audioIndexPos = getSongFromPos()
              val storage = StorageUtil(activity as AppCompatActivity)
              storage.storeAudioIndex(audioIndexPos)
              val broadcastIntent = Intent(PlayerActivity.Broadcast_PLAY_NEW_AUDIO)
              (activity as AppCompatActivity).sendBroadcast(broadcastIntent)
              updatePlayingMusic(audioIndexPos)
              // Toast.makeText(this, "Skipped Next", Toast.LENGTH_SHORT).show()
          }

          binding!!.playPauseIV.setOnClickListener {
              if (musicService?.mediaPlayer!!.isPlaying) {
                  musicService?.pauseMedia()
                  binding!!.playPauseIV.setImageResource(R.drawable.ic_play_audio)
              } else if (!musicService?.mediaPlayer!!.isPlaying) {
                  musicService?.playMedia()
                  binding!!.playPauseIV.setImageResource(R.drawable.ic_pause_audio)
                  musicService?.updateMetaData()
                  musicService?.buildNotification(
                      PlaybackStatus.PLAYING,
                      PlaybackStatus.UN_FAVOURITE,
                      1f
                  )
              }
          }
      }*/

    /*   private fun updatePlayingMusic(audioIndex: Int) {
           binding!!.songNameTV.text = PlayerActivity.audioList[audioIndex].songName
           binding!!.artistOrAlbumNameTV.text = PlayerActivity.audioList[audioIndex].artistsName
           Glide.with(this).load(PlayerActivity.audioList[audioIndexPos].artUri)
               .apply(RequestOptions.placeholderOf(R.drawable.music_note_1).centerCrop())
               .into(binding!!.albumArtIV)
           if (musicService?.mediaPlayer != null) {
               if (musicService?.mediaPlayer!!.isPlaying) {
                   binding!!.playPauseIV.setImageResource(R.drawable.ic_pause_audio)
               } else {
                   binding!!.playPauseIV.setImageResource(R.drawable.ic_play_audio)
               }
           }
       }*/

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        val binder = service as PlayBeatMusicService.LocalBinder
        musicService = binder.getService()
        serviceBound = true
        //controlAudio()
        // Toast.makeText(this, "Service Bound", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        serviceBound = false
    }

    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        playAudio(allSongModel, position)
        val updatePlayer = Intent(Broadcast_UPDATE_PLAYER_UI)
        (activity as AppCompatActivity).sendBroadcast(updatePlayer)
    }


    override fun onDestroy() {
        super.onDestroy()
        isDestroyedActivity = true
    }

}
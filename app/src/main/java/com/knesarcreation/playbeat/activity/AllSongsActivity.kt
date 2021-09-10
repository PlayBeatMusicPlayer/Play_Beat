/*
package com.knesarcreation.playbeat.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.MediaStore
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.knesarcreation.playbeat.ActionPlay
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.fragment.BottomSheetPlayingSong
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.receiver.MediaButtonIntentReceiver
import com.knesarcreation.playbeat.service.MusicPlayerService
import com.knesarcreation.playbeat.utils.ApplicationChannel.Companion.ACTION_NEXT
import com.knesarcreation.playbeat.utils.ApplicationChannel.Companion.ACTION_PLAY_PAUSE
import com.knesarcreation.playbeat.utils.ApplicationChannel.Companion.ACTION_PREV
import com.knesarcreation.playbeat.utils.ApplicationChannel.Companion.CHANNEL_ID
import com.knesarcreation.playbeat.utils.CustomProgressDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.floor

class AllSongsActivity : AppCompatActivity(), AllSongsAdapter.OnClickSongItem,
    BottomSheetPlayingSong.OnControlSongFromBottomSheet, ActionPlay, ServiceConnection {
    private lateinit var arrowBackIV: ImageView
    private lateinit var albumArtIV: ImageView
    private lateinit var backwardIv: ImageView
    private lateinit var playPauseIV: ImageView
    private lateinit var nextBtnClciked: ImageView
    private lateinit var rvAllSongs: RecyclerView
    private lateinit var rlPlayingSong: RelativeLayout
    private lateinit var allSongsAdapter: AllSongsAdapter
    private lateinit var songNameTV: TextView
    private lateinit var artistOrAlbumNameTV: TextView
    private lateinit var startDurationTV: TextView
    private lateinit var endDurationTV: TextView
    private lateinit var mSeekBar: SeekBar
    private lateinit var openBottomSheetView: View
    lateinit var progressBar: CustomProgressDialog
    private var elapsedRunningSong: CountDownTimer? = null
    private var totalDurationInMillis = 0L
    private var clickedSongPos = -1
    private lateinit var bottomSheetPlayingSong: BottomSheetPlayingSong
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var controller: MediaControllerCompat

    companion object {
        const val READ_STORAGE_PERMISSION = 101
        const val MEDIA_SESSION_TAG = "AUDIO"
        const val CONTENT_INTENT_REQ_CODE = 102
        const val PREV_NEXT_PLAY_PAUSE_REQ_CODE = 103
        const val NOTIFY_ID = 110
        var musicPlayerService: MusicPlayerService? = null
        var isLoop = false
        var isShuffled = false
        var random = 0
        var backStackedSongs = ArrayList<Int>()
        val allSongList = CopyOnWriteArrayList<AllSongsModel>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_songs)

        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_TAG)
        controller = mediaSession.controller
        initialization()
        songNameTV.isSelected = true

        rvAllSongs.setHasFixedSize(true)
        rvAllSongs.isNestedScrollingEnabled = true

        progressBar = CustomProgressDialog(this)
        progressBar.setMessage("Fetching Songs..")
        progressBar.setCanceledOnOutsideTouch(false)
        progressBar.show()

        arrowBackIV.setOnClickListener {
            onBackPressed()
        }

        openBottomSheetView.setOnClickListener {
            bottomSheetPlayingSong =
                BottomSheetPlayingSong(
                    this,
                    allSongList,
                    clickedSongPos,
                    */
/* millisLeft,*//*

                    this
                )
            bottomSheetPlayingSong.show(supportFragmentManager, "bottomSheetPlayingSong")
        }

        val intent = Intent(this, MusicPlayerService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)

        val mService = Intent(this, MusicPlayerService::class.java)
        mService.putExtra("MusicPosition", clickedSongPos)
        startService(mService)
        checkPermission()
        controlMusic()

    }

    private fun initialization() {
        arrowBackIV = findViewById(R.id.arrowBackIV)
        rvAllSongs = findViewById(R.id.rvAllSongs)
        rlPlayingSong = findViewById(R.id.rlPlayingSong)
        songNameTV = findViewById(R.id.songNameTV)
        artistOrAlbumNameTV = findViewById(R.id.artistOrAlbumNameTV)
        backwardIv = findViewById(R.id.skipPrevAudio)
        playPauseIV = findViewById(R.id.playPauseIV)
        nextBtnClciked = findViewById(R.id.skipNextAudio)
        mSeekBar = findViewById(R.id.seekBar)
        endDurationTV = findViewById(R.id.endTimeTV)
        startDurationTV = findViewById(R.id.startTimeTV)
        albumArtIV = findViewById(R.id.albumArtIV)
        openBottomSheetView = findViewById(R.id.openBottomSheetView)
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_STORAGE_PERMISSION
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_STORAGE_PERMISSION
                )
            }
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                getMusic()
            }
        }
    }

    @SuppressLint("Range")
    private fun getMusic() {
        allSongList.clear()
        */
/* val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             MediaStore.Audio.Media.getContentUri(
                 MediaStore.VOLUME_EXTERNAL
             )
         } else {
             MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
         }*//*

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_FRAGMENT,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA, // path
        )

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(20, TimeUnit.SECONDS).toString())

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        val query =
            contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)

        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val artistsColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_FRAGMENT)
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
//                var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                val artUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString()
//                val artUri = Uri.withAppendedPath(sArtworkUri, albumId.toString()).toString()
//                try {
                */
/*if (Build.VERSION.SDK_INT < 28) {*//*

//                    bitmap = MediaStore.Images.Media.getBitmap(
//                        contentResolver, albumArtUri
//                    )
//                    bitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
                */
/*} else {
                    val source =
                        ImageDecoder.createSource(this.contentResolver, albumArtUri)
                    bitmap = ImageDecoder.decodeBitmap(source)
                }*//*


//                } catch (e: FileNotFoundException) {
//                    e.printStackTrace()
//                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.music_note_1)
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }

                // getting audio uri
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                */
/* var albumArt: ByteArray? = null
                 try {
                     albumArt = SongAlbumArt.get(data)
                 } catch (e: Exception) {
                     Log.d("SongAlbumArt", "getMusic:${e.message} ")
                 }*//*


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
                allSongList.add(allSongsModel)
            }
            runOnUiThread {
                // Stuff that updates the UI
                allSongsAdapter = AllSongsAdapter(this, allSongList, this)
                rvAllSongs.adapter = allSongsAdapter
                cursor.close()
                progressBar.dismiss()
            }

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            getMusic()
                        }
                    }
                } else {
                    Toast.makeText(this, "Permission is required to show music", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        }
    }

    override fun onClick(allSongModel: AllSongsModel, position: Int) {
        this.clickedSongPos = position
        if (musicPlayerService == null) {
            showNotification(R.drawable.ic_noti_play_circle)
            playSong(position)
        } else if (musicPlayerService != null) {
            musicPlayerService?.stop()
            musicPlayerService?.release()
            showNotification(R.drawable.ic_noti_pause_circle)
            playSong(position)
        }
    }

    private fun playSong(position: Int) {
        Log.d("SongPosition", "playSong:$position ")
        val allSongModel = allSongList[position]
        musicPlayerService?.createMediaPlayer(position, this)
//        mMediaPlayer = MediaPlayer.create(this, allSongModel.uri)
        musicPlayerService?.start()
        //since media is playing
        playPauseIV.setImageResource(R.drawable.ic_pause)

        //media details
        mSeekBar.progress = 0
        mSeekBar.max = ((allSongModel.duration.toDouble() / 1000).toInt())

        totalDurationInMillis = allSongModel.duration.toLong()
        songNameTV.text = allSongModel.songName
        artistOrAlbumNameTV.text = allSongModel.artistsName

//        val albumArt = SongAlbumArt.get((allSongModel.path))
//        val albumArt = allSongModel.albumArt
//        if (albumArt != null) {
//            Glide.with(this).load(albumArt).centerCrop().into(albumArtIV)
//        } else {
//            Glide.with(this).load(R.drawable.music_note_1).into(albumArtIV)
//        }

        val songDuration = millisToMinutesAndSeconds(allSongModel.duration)
        endDurationTV.text = songDuration
        runningSongCountDownTime(allSongModel.duration)
    }

    private fun controlMusic() {
        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(progress: SeekBar?) {}

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                if (musicPlayerService != null */
/*&& fromUser*//*
) {
                    musicPlayerService?.seekTo((seekbar?.progress!! * 1000))
                    runningSongCountDownTime((totalDurationInMillis - (seekbar?.progress!! * 1000)).toInt())
                }
            }
        })

        playPauseIV.setOnClickListener {
            Log.d(
                "Service454554",
                "controlMusic: $musicPlayerService , ${musicPlayerService?.isPlaying()} ${musicPlayerService?.currentPosition()!!}"
            )

            if (musicPlayerService != null) {
                if (musicPlayerService?.isPlaying() == true) {
                    showNotification(R.drawable.ic_noti_play_circle)
                    musicPlayerService?.pause()
                    playPauseIV.setImageResource(R.drawable.ic_play_audio)
                    elapsedRunningSong?.cancel()
                } else {
                    showNotification(R.drawable.ic_noti_pause_circle)
                    musicPlayerService?.start()
                    playPauseIV.setImageResource(R.drawable.ic_pause)
                    //resume song
                    runningSongCountDownTime(totalDurationInMillis.toInt() - musicPlayerService?.currentPosition()!!)
                }
            }
        }

        nextBtnClciked.setOnClickListener {
            if (musicPlayerService != null) {
                if (!isShuffled) {
                    incrementSongByOne()
                } else {
                    //if shuffled enabled
                    random = Random().nextInt(allSongList.size)
                    clickedSongPos = random
                    backStackedSongs.add(clickedSongPos)
                }
                musicPlayerService?.stop()
                showNotification(R.drawable.ic_noti_pause_circle)
                playSong(clickedSongPos)
            } else {
                showNotification(R.drawable.ic_noti_pause_circle)
                playSong(clickedSongPos)
            }
        }
    }

    private fun incrementSongByOne() {
        if (clickedSongPos != allSongList.size - 1) {
            clickedSongPos++
        } else {
            clickedSongPos = -1
            clickedSongPos++
        }
    }

    private fun millisToMinutesAndSeconds(millis: Int): String {
        val minutes = floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds "
    }

    private fun runningSongCountDownTime(durationInMillis: Int) {
        if (elapsedRunningSong != null) {
            elapsedRunningSong?.cancel()
        }
        elapsedRunningSong = object : CountDownTimer(durationInMillis.toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                */
/* millisLeft = millisUntilFinished*//*

                val elapsedTime = (totalDurationInMillis - millisUntilFinished)
                val millisToMinutesAndSeconds = millisToMinutesAndSeconds(elapsedTime.toInt())
                startDurationTV.text = millisToMinutesAndSeconds

                val maxSeekProgress = totalDurationInMillis / 1000
                val seekProgress = musicPlayerService?.currentPosition()!! / 1000
                */
/* ((elapsedTime.toDouble() / totalDurationInMillis.toDouble()) * maxSeekProgress)*//*


                Log.d(
                    "elapsedTime",
                    "onTick: elapsedTime: $elapsedTime totalDurationInMillis $durationInMillis  seekProgress: $seekProgress"
                )

                mSeekBar.progress = seekProgress

                if (mSeekBar.progress in (maxSeekProgress - 1)..maxSeekProgress) {
                    Log.d(
                        "MaxSeekAllSongsPage",
                        "onTick: maxSeek: $maxSeekProgress,${mSeekBar.progress} "
                    )
                    //for getting random value at last tick of count
                    random = Random().nextInt(allSongList.size)
                    backStackedSongs.add(clickedSongPos)
                }

                // time and seek bar will not run if song is in paused state
                if (musicPlayerService != null) {
                    if (musicPlayerService?.isPlaying() == false) {
                        elapsedRunningSong?.cancel()
                    }
                }
            }

            override fun onFinish() {
                if (!isLoop && !isShuffled) {
                    // normal running player
                    incrementSongByOne()
                    playSong(clickedSongPos)
                } else {
                    if (isShuffled) {
                        // shuffled
                        Log.d("ActivityRandom", "onFinish:$random ")
                        if (musicPlayerService != null) {
                            //getting random songs and playing it
                            playSong(random)
                            clickedSongPos = random
                        }
                    } else if (isLoop) {
                        // looped one song
                        if (musicPlayerService != null) {
                            playSong(clickedSongPos)
                        }
                    }
                }
            }
        }
        elapsedRunningSong?.start()
    }

    override fun onSeekChangeListener(seekBar: SeekBar) {
        if (musicPlayerService != null */
/*&& fromUser*//*
) {
            musicPlayerService?.seekTo((seekBar.progress * 1000))
            runningSongCountDownTime((totalDurationInMillis - (seekBar.progress * 1000)).toInt())
        }
    }

    override fun onNextBtnClicked(clickedSongPos: Int) {
        if (musicPlayerService != null) {
            if (isShuffled) {
                this.clickedSongPos = clickedSongPos
            } else {
                this.clickedSongPos++
            }
            musicPlayerService?.stop()
            musicPlayerService?.release()
            playSong(clickedSongPos)
        } else {
            playSong(clickedSongPos)
        }
    }

    override fun onPrevBtnClicked(clickedSongPos: Int) {
        if (musicPlayerService != null) {
            if (isShuffled) {
                this.clickedSongPos = clickedSongPos
            } else {
                this.clickedSongPos--
            }
            musicPlayerService?.stop()
            musicPlayerService?.release()
            playSong(clickedSongPos)
        } else {
            playSong(clickedSongPos)
        }
    }

    override fun onPauseOrPlayClick() {
        if (musicPlayerService != null) {
            if (musicPlayerService?.isPlaying() == true) {
                musicPlayerService?.pause()
                playPauseIV.setImageResource(R.drawable.ic_play_audio)
                elapsedRunningSong?.cancel()
            } else {
                musicPlayerService?.start()
                playPauseIV.setImageResource(R.drawable.ic_pause)
                //resume song
                runningSongCountDownTime(totalDurationInMillis.toInt() - musicPlayerService?.currentPosition()!!)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    override fun playPauseClicked() {

    }

    override fun playPrevClicked() {
    }

    override fun playNextClicked() {

    }

    private fun showNotification(playPauseBtn: Int) {
        val intent = Intent(this, AllSongsActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            CONTENT_INTENT_REQ_CODE,
            intent,
            0
        )

        val prevIntent = Intent(this, MediaButtonIntentReceiver::class.java)
            .setAction(ACTION_PREV)
        val prevPendingIntent = PendingIntent.getBroadcast(
            this,
            PREV_NEXT_PLAY_PAUSE_REQ_CODE,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextIntent = Intent(this, MediaButtonIntentReceiver::class.java)
            .setAction(ACTION_NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(
            this,
            PREV_NEXT_PLAY_PAUSE_REQ_CODE,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIntent = Intent(this, MediaButtonIntentReceiver::class.java)
            .setAction(ACTION_PLAY_PAUSE)
        val playPausePendingIntent = PendingIntent.getBroadcast(
            this,
            PREV_NEXT_PLAY_PAUSE_REQ_CODE,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (clickedSongPos != -1) {
//            val albumArt: ByteArray? = allSongList[clickedSongPos].albumArt
//            val notificationThumbnail: Bitmap = if (albumArt != null) {
//                BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
//            } else {
//                BitmapFactory.decodeResource(resources, R.drawable.music_note_1)
//            }

            val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(controller.sessionToken)

            val mSession = android.media.session.MediaSession(this, "Media")
            mSession.isActive = true
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_play_audio)
                    //.setLargeIcon(notificationThumbnail)
                    .setContentTitle(allSongList[clickedSongPos].songName)
                    .setContentText(allSongList[clickedSongPos].artistsName)
                    .addAction(R.drawable.ic_noti_skip_prev, "Previous", prevPendingIntent)
                    .addAction(playPauseBtn, "PlayPause", playPausePendingIntent)
                    .addAction(R.drawable.ic_noti_skip_next, "Next", nextPendingIntent)
                    .setStyle(Notification.MediaStyle().setMediaSession(mSession.sessionToken))
//                      .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(contentIntent)
                    .build()
            } else {
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_play_audio)
                    //.setLargeIcon(notificationThumbnail)
                    .setContentTitle(allSongList[clickedSongPos].songName)
                    .setContentText(allSongList[clickedSongPos].artistsName)
                    .addAction(R.drawable.ic_noti_skip_prev, "Previous", prevPendingIntent)
                    .addAction(playPauseBtn, "PlayPause", playPausePendingIntent)
                    .addAction(R.drawable.ic_noti_skip_next, "Next", nextPendingIntent)
                    .setStyle(
                        androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(controller.sessionToken)
                            .setShowActionsInCompactView(0, 1, 2)
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(contentIntent)
                    .build()
            }


//            }


            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFY_ID, notification)


        }
    }

    override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
        val mBinder = service as (MusicPlayerService.MyBinder)
        musicPlayerService = mBinder.getService()
        Toast.makeText(this, "Connected $musicPlayerService", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicPlayerService = null
    }
}*/

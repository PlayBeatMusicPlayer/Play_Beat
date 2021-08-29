package com.knesarcreation.playbeat.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.AllSongsAdapter
import com.knesarcreation.playbeat.fragment.BottomSheetPlayingSong
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.utils.CustomProgressDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.floor

class AllSongsActivity : AppCompatActivity(), AllSongsAdapter.OnClickSongItem {
    private lateinit var arrowBackIV: ImageView
    private lateinit var albumArtIV: ImageView
    private lateinit var backwardIv: ImageView
    private lateinit var playPauseIV: ImageView
    private lateinit var forwardIV: ImageView
    private lateinit var rvAllSongs: RecyclerView
    private lateinit var rlPlayingSong: RelativeLayout
    private lateinit var allSongsAdapter: AllSongsAdapter
    private lateinit var songNameTV: TextView
    private lateinit var artistOrAlbumNameTV: TextView
    private lateinit var startDurationTV: TextView
    private lateinit var endDurationTV: TextView
    private lateinit var mSeekBar: SeekBar
    private lateinit var openBottomSheetView: View
    private val allSongList = ArrayList<AllSongsModel>()
    lateinit var progressBar: CustomProgressDialog
    private var mMediaPlayer: MediaPlayer? = null
    private var elapsedRunningSong: CountDownTimer? = null
    private var millisLeft = 0L
    private var totalDurationInMillis = 0L
    private var clickedSongPos = 0
    private lateinit var bottomSheetPlayingSong: BottomSheetPlayingSong

    companion object {
        const val READ_STORAGE_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_songs)

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
            bottomSheetPlayingSong = BottomSheetPlayingSong(allSongList, clickedSongPos)
            bottomSheetPlayingSong.show(supportFragmentManager, "bottomSheetPlayingSong")
        }

        checkPermission()
        controlMusic()
    }

    private fun initialization() {
        arrowBackIV = findViewById(R.id.arrowBackIV)
        rvAllSongs = findViewById(R.id.rvAllSongs)
        rlPlayingSong = findViewById(R.id.rlPlayingSong)
        songNameTV = findViewById(R.id.songNameTV)
        artistOrAlbumNameTV = findViewById(R.id.artistOrAlbumNameTV)
        backwardIv = findViewById(R.id.backwardIv)
        playPauseIV = findViewById(R.id.playPauseIV)
        forwardIV = findViewById(R.id.forwardIV)
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

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )

        // Show only audios that are at least 1 minutes in duration.
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES).toString())

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
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)


            while (cursor.moveToNext()) {
                //Get values of columns of a given audio
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val album = cursor.getString(albumColumn)
                val artist = cursor.getString(artistsColumn)
                val albumId = cursor.getLong(albumIdColumn)

                //getting album art uri
                var bitmap: Bitmap? = null
                val sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart")
                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)

                try {
                    /*if (Build.VERSION.SDK_INT < 28) {*/
                    bitmap = MediaStore.Images.Media.getBitmap(
                        contentResolver, albumArtUri
                    )
                    bitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, true)
                    /*} else {
                        val source =
                            ImageDecoder.createSource(this.contentResolver, albumArtUri)
                        bitmap = ImageDecoder.decodeBitmap(source)
                    }*/

                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.music_note_1)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                // getting audio uri
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val allSongsModel =
                    AllSongsModel(albumId, contentUri, name, artist, album, size, duration, bitmap)
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
        if (mMediaPlayer == null) {
            playSong(position)
        } else if (mMediaPlayer != null) {
            mMediaPlayer?.stop()
            playSong(position)
        }
    }

    private fun playSong(position: Int) {
        Log.d("SongPosition", "playSong:$position ")
        val allSongModel = allSongList[position]
        mMediaPlayer = MediaPlayer.create(this, allSongModel.uri)
        mMediaPlayer?.start()
        //since media is playing
        playPauseIV.setImageResource(R.drawable.ic_pause)

        //media details
        mSeekBar.progress = 0
        mSeekBar.max = ((allSongModel.duration.toDouble() / 1000).toInt())

        totalDurationInMillis = allSongModel.duration.toLong()
        songNameTV.text = allSongModel.songName
        artistOrAlbumNameTV.text = allSongModel.artistsName
        Glide.with(this).asBitmap().load(allSongModel.bitmap).into(albumArtIV)
        val songDuration = millisToMinutesAndSeconds(allSongModel.duration)
        endDurationTV.text = songDuration
        runningSongCountDownTime(allSongModel.duration)
    }

    private fun controlMusic() {
        playPauseIV.setOnClickListener {
            if (mMediaPlayer != null) {
                if (mMediaPlayer?.isPlaying == true) {
                    mMediaPlayer?.pause()
                    playPauseIV.setImageResource(R.drawable.ic_play)
                    elapsedRunningSong?.cancel()
                } else {
                    mMediaPlayer?.start()
                    playPauseIV.setImageResource(R.drawable.ic_pause)
                    //resume song
                    runningSongCountDownTime(millisLeft.toInt())
                }
            }
        }

        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(progress: SeekBar?) {}

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                if (mMediaPlayer != null /*&& fromUser*/) {
                    mMediaPlayer?.seekTo((seekbar?.progress!! * 1000))
                    runningSongCountDownTime((totalDurationInMillis - (seekbar?.progress!! * 1000)).toInt())
                }
            }
        })

        forwardIV.setOnClickListener {
            if (mMediaPlayer != null) {
                mMediaPlayer?.stop()
                clickedSongPos++
                playSong(clickedSongPos)
            } else {
                playSong(clickedSongPos)
            }
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
                millisLeft = millisUntilFinished
                val elapsedTime = (totalDurationInMillis - millisUntilFinished)
                val millisToMinutesAndSeconds = millisToMinutesAndSeconds(elapsedTime.toInt())
                startDurationTV.text = millisToMinutesAndSeconds

                val maxSeekProgress = totalDurationInMillis / 1000
                val seekProgress =
                    ((elapsedTime.toDouble() / totalDurationInMillis.toDouble()) * maxSeekProgress)

                Log.d(
                    "elapsedTime",
                    "onTick: elapsedTime: $elapsedTime totalDurationInMillis $durationInMillis  seekProgress: $seekProgress"
                )

                mSeekBar.progress = seekProgress.toInt()
            }

            override fun onFinish() {
//                playPauseIV.setImageResource(R.drawable.ic_play)
                if (clickedSongPos == (allSongList.size - 1)) clickedSongPos =
                    0 else clickedSongPos++
                if (mMediaPlayer != null)
                    playSong(clickedSongPos)
            }
        }
        elapsedRunningSong?.start()
    }
}
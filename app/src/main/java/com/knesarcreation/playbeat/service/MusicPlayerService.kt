/*
package com.knesarcreation.playbeat.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.knesarcreation.playbeat.ActionPlay
import com.knesarcreation.playbeat.activity.AllSongsActivity
import com.knesarcreation.playbeat.database.AllSongsModel

class MusicPlayerService : Service() {
    private val mBinder = MyBinder()
    private var mMediaPlayer: MediaPlayer? = null
    private var allSongList = ArrayList<AllSongsModel>()
    var uri: Uri? = null
    var position: Int = -1
    lateinit var ap: ActionPlay

    override fun onBind(p0: Intent?): IBinder {
        Log.d("OnBind", "onBind: Called")
        return mBinder
    }

    class MyBinder : Binder() {
        fun getService(): MusicPlayerService {
            return MusicPlayerService()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        allSongList = AllSongsActivity.allSongList
        val musicPosition = intent?.getIntExtra("MusicPosition", -1)
        if (musicPosition != -1) {
            playMedia(musicPosition)
        }
        return START_STICKY
    }

    private fun playMedia(musicPosition: Int?) {
        allSongList = AllSongsActivity.allSongList
        if (musicPosition != null) {
            position = musicPosition
        }
        mMediaPlayer?.stop()
        mMediaPlayer?.release()
        if (allSongList.isNotEmpty()) {
            Toast.makeText(applicationContext, "Started form on Start command", Toast.LENGTH_SHORT)
                .show()
            createMediaPlayer(position, applicationContext)
            mMediaPlayer?.start()
        }
    }


    fun start() {
        mMediaPlayer?.start()
    }

    fun pause() {
        mMediaPlayer?.pause()
    }

    fun isPlaying(): Boolean {
        return mMediaPlayer?.isPlaying ?: false
    }

    fun stop() {
        mMediaPlayer?.stop()
    }

    fun release() {
        mMediaPlayer?.release()
    }

    fun seekTo(position: Int) {
        mMediaPlayer?.seekTo(position)
    }

    fun currentPosition(): Int {
        return mMediaPlayer?.currentPosition ?: 0
    }

    fun createMediaPlayer(position: Int, context: Context) {
        val allSongModel = AllSongsActivity.allSongList[position]
        uri = Uri.parse(allSongModel.audioUri)
        mMediaPlayer = MediaPlayer.create(context, uri)
    }

}*/

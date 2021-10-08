package com.knesarcreation.playbeat.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.android.material.button.MaterialButton
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.utils.ApplicationChannel.Companion.CHANNEL_ID
import com.knesarcreation.playbeat.utils.PlaybackStatus
import com.knesarcreation.playbeat.utils.StorageUtil
import com.knesarcreation.playbeat.utils.UriToBitmapConverter
import java.util.concurrent.CopyOnWriteArrayList


class PlayBeatMusicService : Service(), AudioManager.OnAudioFocusChangeListener {

    // Binder given to clients
    private val iBinder = LocalBinder()
    private var audioManager: AudioManager? = null
    var mediaPlayer: MediaPlayer? = null

    //path to the audio file
    private var mediaFile: String? = null

    //Used to pause/resume MediaPlayer
    private var resumePosition = -1

    //Handle incoming phone calls
    private var ongoingCall = false
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    //List of available Audio files
    private var audioList: CopyOnWriteArrayList<AllSongsModel>? = null
    private var audioIndex = -1
    private var activeAudio //an object of the currently playing audio
            : AllSongsModel? = null

    //MediaSession
    private var mediaSessionManager: android.media.session.MediaSessionManager? = null
    private var mediaSession: MediaSessionCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    //AudioPlayer notification ID
    private val NOTIFICATION_ID = 101
    var pausedByManually = false
    private var isTaskRemoved = false
    private var sleepCountDownTimer: CountDownTimer? = null
    var isSleepTimeRunning = false
    private var initAudioOnPressResumeBtn = false

    companion object {
        const val ACTION_PLAY = "com.knesarcreation.playbeat.service.ACTION_PLAY"
        const val ACTION_PAUSE = "com.knesarcreation.playbeat.service.ACTION_PAUSE"
        const val ACTION_PREVIOUS = "com.knesarcreation.playbeat.service.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.knesarcreation.playbeat.service.ACTION_NEXT"
        const val ACTION_STOP = "com.knesarcreation.playbeat.service.ACTION_STOP"
        const val ACTION_FAVOURITE = "com.knesarcreation.playbeat.service.ACTION_FAVOURITE"
        const val ACTION_UN_FAVOURITE = "com.knesarcreation.playbeat.service.ACTION_UNFAVOURITE"
        const val OPEN_CONTENT = "com.knesarcreation.playbeat.service.OPEN_CONTENT"
    }

    fun updateNotification(isAudioPlaying: Boolean) {
        val storageUtil = StorageUtil(applicationContext)
        val loadAudio = storageUtil.loadQueueAudio()
        activeAudio = loadAudio[audioIndex]
        if (activeAudio?.isFavourite!!) {
            if (isAudioPlaying)
                buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.FAVOURITE, 1f)
            else
                buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.FAVOURITE, 0f)

        } else {
            if (isAudioPlaying)
                buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
            else
                buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Perform one-time setup procedures

        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener()
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver()
        //Listen for new Audio to play -- BroadcastReceiver
        registerPlayNewAudio()

        //register on click notification receiver
        // registerContentReceiver()
        //registerMediaBtn()

        resumePosition = StorageUtil(applicationContext).getAudioResumePos()

        // initialize audio when app reOpens and user click on playPause btn
        initAudioOnPressResumeBtn = true

    }

    override fun onBind(intent: Intent?): IBinder {
        return iBinder
    }

    private fun initMediaPlayer() {
        //Set up MediaPlayer event listeners

        //mediaPlayer?.setOnBufferingUpdateListener(this)
        //mediaPlayer?.setOnInfoListener(this)
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer?.reset()

        mediaPlayer = MediaPlayer.create(applicationContext, activeAudio?.audioUri!!.toUri())

        mediaPlayer?.setOnCompletionListener {
            if (!it?.isPlaying!!) {
                val isRepeatAudio = StorageUtil(applicationContext).getIsRepeatAudio()
                if (isRepeatAudio) {
                    stopMedia()
                    //reset mediaPlayer
                    mediaPlayer!!.reset()
                    initMediaPlayer()
                } else {
                    skipToNext()
                }
                //update meta data of notification and build it
                updateMetaData()
                updateNotification(true)
                // Toast.makeText(applicationContext, "Next", Toast.LENGTH_SHORT).show()
            }
        }

        mediaPlayer?.setOnPreparedListener {
            playMedia()
            // Toast.makeText(this, "Played", Toast.LENGTH_SHORT).show()
        }

        mediaPlayer?.setOnErrorListener { _, what, extra ->
            when (what) {
                MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(
                    "MediaPlayer Error",
                    "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
                )
                MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.d(
                    "MediaPlayer Error",
                    "MEDIA ERROR SERVER DIED $extra"
                )
                MediaPlayer.MEDIA_ERROR_UNKNOWN -> Log.d(
                    "MediaPlayer Error",
                    "MEDIA ERROR UNKNOWN $extra"
                )
            }
            return@setOnErrorListener false
        }

        mediaPlayer?.setOnSeekCompleteListener {}
    }

    //Handle incoming phone calls
    private fun callStateListener() {
        // Get the telephony manager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        //Starting listening for PhoneState changes
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK, TelephonyManager.CALL_STATE_RINGING -> if (mediaPlayer != null) {
                        pauseMedia()
                        //buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
                        updateNotification(false)
                        ongoingCall = true
                    }
                    TelephonyManager.CALL_STATE_IDLE ->
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false
                                if (!pausedByManually) {
                                    // if user manually paused, so don't resume audio when call ends or went on idle mode
                                    // else call this resumeMedia() method
                                    resumeMedia()
                                    /*buildNotification(
                                        PlaybackStatus.PLAYING,
                                        PlaybackStatus.UN_FAVOURITE,
                                        1f
                                    )*/
                                    updateNotification(true)
                                }
                            }
                        }
                }
            }
        }
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager?.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            //Load data from SharedPreferences
            val storage = StorageUtil(applicationContext)
            audioList = storage.loadQueueAudio()
            audioIndex = storage.loadAudioIndex()

            Log.d("audioIndexService", "onStartCommand:  $audioIndex")

            if (audioIndex != -1 && audioIndex < audioList!!.size) {
                //index is in a valid range
                activeAudio = audioList!![audioIndex]
            } else {
                stopSelf()
            }

        } catch (e: Exception) {
            stopSelf()
        }

        //Request audio focus
        //if (!requestAudioFocus()) {
        //Could not gain focus
        //   stopSelf()
        //}

        if (mediaSessionManager == null) {
            try {
                mediaPlayer = MediaPlayer()
                initMediaSession()
                //initMediaPlayer()
            } catch (e: RemoteException) {
                e.printStackTrace()
                stopSelf()
            }
            //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
            //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent)
        return START_STICKY
    }

    private fun playMedia() {
        if (mediaPlayer == null) return
        if (!mediaPlayer?.isPlaying!!) {
            mediaPlayer?.start()
            /** Update UI of [AllSongFragment] */
            val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
            sendBroadcast(updatePlayer)
        }
    }

    fun stopMedia() {
        if (mediaPlayer == null) return
        if (mediaPlayer?.isPlaying!!) {
            mediaPlayer?.stop()
        }
    }

    fun pauseMedia() {
        if (mediaPlayer != null) {
            if (mediaPlayer?.isPlaying!!) {
                mediaPlayer?.pause()
                resumePosition = mediaPlayer?.currentPosition!!
                /** Update UI of [AllSongFragment] */
                val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
                sendBroadcast(updatePlayer)
            }
        }
    }

    fun resumeMedia() {
        if (mediaPlayer == null) return
        if (!mediaPlayer?.isPlaying!!) {
            if (initAudioOnPressResumeBtn) {
                initAudioOnPressResumeBtn = false
                initMediaPlayer()
            }
            Toast.makeText(applicationContext, "initAudio: $initAudioOnPressResumeBtn", Toast.LENGTH_SHORT).show()
            mediaPlayer?.seekTo(resumePosition)
            mediaPlayer?.start()

            requestAudioFocus()

            /** Update UI of [AllSongFragment] */
            val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
            sendBroadcast(updatePlayer)

        }
    }

    private fun skipToNext() {
        val storageUtil = StorageUtil(applicationContext)
        audioList!!.clear()
        audioList = storageUtil.loadQueueAudio()
        audioIndex = storageUtil.loadAudioIndex()
        val tempIndex: Int = audioIndex
        if (audioIndex == audioList!!.size - 1) {
            //if last in playlist
            audioIndex = 0
            activeAudio = audioList!![audioIndex]
        } else {
            //get next in playlist
            activeAudio = audioList!![++audioIndex]
        }

        //Toast.makeText(applicationContext, "$audioIndex", Toast.LENGTH_SHORT).show()
        //Update stored index
        storageUtil.storeAudioIndex(audioIndex)
        stopMedia()
        //reset mediaPlayer
        mediaPlayer!!.reset()
        initMediaPlayer()

        /** Update UI of [AllSongFragment] */
        val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        val bundle = Bundle()
        bundle.putInt("index", tempIndex)
        updatePlayer.putExtras(bundle)
        sendBroadcast(updatePlayer)
    }

    private fun skipToPrevious() {
        val storageUtil = StorageUtil(applicationContext)
        audioIndex = storageUtil.loadAudioIndex()
        val tempIndex: Int = audioIndex
        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList!!.size - 1
            activeAudio = audioList!![audioIndex]
        } else {
            //get previous in playlist
            activeAudio = audioList!![--audioIndex]
        }

        //Update stored index
        storageUtil.storeAudioIndex(audioIndex)
        stopMedia()
        //reset mediaPlayer
        mediaPlayer!!.reset()
        initMediaPlayer()

        /** Update UI of [AllSongFragment] */
        val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        val bundle = Bundle()
        bundle.putInt("index", tempIndex)
        updatePlayer.putExtras(bundle)
        sendBroadcast(updatePlayer)
    }

    fun onSeekTo(position: Long) {
        resumePosition = position.toInt()
        mediaPlayer?.seekTo(resumePosition)
        if (mediaPlayer?.isPlaying!!) {
            //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
            updateNotification(true)
        } else {
            updateNotification(false)
            //buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
        }
    }

    override fun onAudioFocusChange(focusState: Int) {
        //Invoked when the audio focus of the system is updated.
        when (focusState) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // resume playback
                if (!pausedByManually) {
                    if (mediaPlayer == null) initMediaPlayer() else if (!mediaPlayer!!.isPlaying) {
                        resumeMedia()
                        //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
                        updateNotification(true)
                    }
                    mediaPlayer!!.setVolume(1.0f, 1.0f)
                }
                Log.d("MediaServiceAUDIOFOCUS_GAIN", "onAudioFocusChange: AUDIOFOCUS_GAIN ")
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer!!.isPlaying) {
                    pauseMedia()
                    //buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
                    updateNotification(false)
                }
                //mediaPlayer!!.release()
                //mediaPlayer = null
                Log.d("MediaServiceAUDIOFOCUS_LOSS", "onAudioFocusChange: AUDIOFOCUS_LOSS ")

            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer!!.isPlaying) {
                    pauseMedia()
                    //buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
                    updateNotification(false)
                }
                Log.d(
                    "MediaServiceAUDIOFOCUS_LOSS_TRANSIENT",
                    "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT "
                )

            }


            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.setVolume(0.1f, 0.1f)
                Log.d(
                    "MediaServiceAUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK",
                    "onAudioFocusChange: AUDIOFOCUS_LOSS_TRANSIENT "
                )

            }

        }
    }

    fun buildNotification(
        playbackStatus: PlaybackStatus,
        favUnFavStatus: PlaybackStatus,
        playbackSpeed: Float
    ) {
        var playPauseActionIcon = R.drawable.ic_noti_pause_circle //needs to be initialized
        var favUnFavActionIcon = R.drawable.ic_pink_outlined_heart //needs to be initialized

        var playPauseAction: PendingIntent? = null
        var favUnFavAction: PendingIntent? = null


        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            playPauseActionIcon = R.drawable.ic_noti_pause_circle
            //create the pause action
            playPauseAction = playbackAction(1)
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            playPauseActionIcon = R.drawable.ic_noti_play_circle
            //create the play action
            playPauseAction = playbackAction(0)
        }

        if (favUnFavStatus == PlaybackStatus.FAVOURITE) {
            favUnFavActionIcon = R.drawable.ic_noti_fav
            favUnFavAction = playbackAction(4)
        } else if (favUnFavStatus == PlaybackStatus.UN_FAVOURITE) {
            favUnFavActionIcon = R.drawable.ic_pink_outlined_heart
            favUnFavAction = playbackAction(5)
        }

        val largeIcon: Bitmap =
            UriToBitmapConverter.getBitmap(contentResolver, activeAudio?.artUri!!.toUri())
                ?: BitmapFactory.decodeResource(
                    resources,
                    R.drawable.music_note_notification
                )

        // Create a new Notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setShowWhen(false)
            // Set the Notification style
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle() // Attach our MediaSession token
                    .setMediaSession(mediaSession!!.sessionToken) // Show our playback controls in the compact notification view.
                    .setShowActionsInCompactView(1, 2)
            )
            .setLargeIcon(largeIcon)
            .setSmallIcon(R.drawable.play_beat_logo) // Set Notification content information
            .setContentText(activeAudio!!.artistsName)
            .setContentTitle(activeAudio!!.albumName)
            .setContentInfo(activeAudio!!.songName) // Add playback actions
            .addAction(R.drawable.ic_noti_skip_prev, "previous", playbackAction(3))
            .addAction(playPauseActionIcon, "pause", playPauseAction)
            .addAction(
                R.drawable.ic_noti_skip_next,
                "next",
                playbackAction(2)
            ).setOnlyAlertOnce(true).setAutoCancel(false)
            .addAction(favUnFavActionIcon, "favourite", favUnFavAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        //.setProgress(mediaPlayer?.duration!!, mediaPlayer?.currentPosition!!, false)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        if (mediaPlayer != null) {
            mediaSession?.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        mediaPlayer?.currentPosition!!.toLong(),
                        playbackSpeed
                    )
                    .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                    .build()
            )
        }

        /*   (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(
               NOTIFICATION_ID,
               notificationBuilder.build()
           )*/
    }

    private fun removeNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun playbackAction(actionNumber: Int): PendingIntent? {
        val playbackAction = Intent(this, PlayBeatMusicService::class.java)
        when (actionNumber) {
            0 -> {
                // Play
                playbackAction.action = ACTION_PLAY
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            1 -> {
                // Pause
                playbackAction.action = ACTION_PAUSE
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            2 -> {
                // Next track
                playbackAction.action = ACTION_NEXT
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            3 -> {
                // Previous track
                playbackAction.action = ACTION_PREVIOUS
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            4 -> {
                //Favourite media
                playbackAction.action = ACTION_FAVOURITE
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            5 -> {
                // un favourite media
                playbackAction.action = ACTION_UN_FAVOURITE
                return PendingIntent.getService(this, actionNumber, playbackAction, 0)
            }
            else -> {
            }
        }
        return null
    }

    private fun handleIncomingActions(playbackAction: Intent?) {
        if (playbackAction == null || playbackAction.action == null) return
        val actionString = playbackAction.action
        when {
            actionString.equals(ACTION_PLAY, ignoreCase = true) -> {
                transportControls!!.play()
            }
            actionString.equals(ACTION_PAUSE, ignoreCase = true) -> {
                transportControls!!.pause()
            }
            actionString.equals(ACTION_NEXT, ignoreCase = true) -> {
                transportControls!!.skipToNext()
            }
            actionString.equals(
                ACTION_PREVIOUS,
                ignoreCase = true
            ) -> {
                transportControls!!.skipToPrevious()
            }
            actionString.equals(ACTION_STOP, ignoreCase = true) -> {
                transportControls!!.stop()
            }
            actionString.equals(ACTION_FAVOURITE, ignoreCase = true) -> {
                transportControls!!.setRating(RatingCompat.newHeartRating(true))
            }
            actionString.equals(ACTION_UN_FAVOURITE, ignoreCase = true) -> {
                transportControls!!.setRating(RatingCompat.newHeartRating(false))
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager?.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun removeAudioFocus(): Boolean {
        val audioFocus = audioManager?.abandonAudioFocus(this) ?: false
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioFocus
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlayBeatMusicService {
            return this@PlayBeatMusicService
        }
    }

    @SuppressLint("ServiceCast")
    @Throws(RemoteException::class)
    private fun initMediaSession() {
        if (mediaSessionManager != null) return  //mediaSessionManager exists
        mediaSessionManager =
            getSystemService(MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
        // Create a new MediaSession
        mediaSession = MediaSessionCompat(applicationContext, "AudioPlayer")
        //Get MediaSessions transport controls
        transportControls = mediaSession?.controller?.transportControls
        //set MediaSession -> ready to receive media commands
        mediaSession?.isActive = true
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession?.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)

        //Set mediaSession's MetaData
        updateMetaData()

        // Attach Callback to receive MediaSession updates
        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent?): Boolean {

                val intentAction: String = mediaButtonIntent!!.action!!
                if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                    val event: KeyEvent =
                        mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)!!

                    val keyCode: Int = event.keyCode
                    val action: Int = event.action
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (mediaPlayer != null) {
                            if (mediaPlayer?.isPlaying!!) {
                                pauseMedia()
                                /*buildNotification(
                                    PlaybackStatus.PAUSED,
                                    PlaybackStatus.UN_FAVOURITE,
                                    0f
                                )*/
                                updateNotification(false)
                            } else {
                                resumeMedia()
                                /*buildNotification(
                                    PlaybackStatus.PLAYING,
                                    PlaybackStatus.UN_FAVOURITE,
                                    1f
                                )*/
                                updateNotification(true)
                            }
                        }
                        return true

                    }


                    if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        resumePosition = 0
                        skipToNext()
                        updateMetaData()
                        /* buildNotification(
                             PlaybackStatus.PLAYING,
                             PlaybackStatus.UN_FAVOURITE,
                             1f
                         )*/
                        updateNotification(true)
                        Log.d("skipToNextEarphone", "onMediaButtonEvent: run ")
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                        resumePosition = 0
                        skipToPrevious()
                        updateMetaData()
                        /* buildNotification(
                             PlaybackStatus.PLAYING,
                             PlaybackStatus.UN_FAVOURITE,
                             1f
                         )*/
                        updateNotification(true)
                        return true
                    }
                }
                return false

            }

            // Implement callbacks
            override fun onPlay() {
                super.onPlay()
                pausedByManually = false
                resumeMedia()
                /* */
                /** Update UI of [AllSongFragment] *//*
                val updatePlayer = Intent(AllSongFragment.Broadcast_BOTTOM_UPDATE_PLAYER_UI)
                sendBroadcast(updatePlayer)*/
                //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
                updateNotification(true)
            }

            override fun onPause() {
                super.onPause()
                pausedByManually = true
                pauseMedia()
                /** Update UI of [AllSongFragment] *//*
                val updatePlayer = Intent(AllSongFragment.Broadcast_BOTTOM_UPDATE_PLAYER_UI)
                sendBroadcast(updatePlayer)*/
                //buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
                updateNotification(false)
                if (isTaskRemoved)
                    stopServiceIfTaskRemoved()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                pausedByManually = false
                skipToNext()

                /* */
                /** Update UI of [AllSongFragment] *//*
                val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
                sendBroadcast(updatePlayer)*/

                //update meta data of notification and build it
                updateMetaData()
                // buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
                updateNotification(true)

            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                pausedByManually = false
                skipToPrevious()

                /** Update UI of [AllSongFragment] *//*
                val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
                sendBroadcast(updatePlayer)*/

                //update meta data of notification and build it
                updateMetaData()
                //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
                updateNotification(true)
            }


            override fun onStop() {
                super.onStop()
                pausedByManually = false
                removeNotification()
                //Stop the service
                stopSelf()
            }

            override fun onSeekTo(position: Long) {
                resumePosition = position.toInt()
                mediaPlayer?.seekTo(resumePosition)
                if (mediaPlayer?.isPlaying!!) {
                    //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
                    updateNotification(true)
                } else {
                    //buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
                    updateNotification(false)
                }
                super.onSeekTo(position)
            }

            override fun onSetRating(rating: RatingCompat?) {
                super.onSetRating(rating)
                val bundle = Bundle()
                bundle.putBoolean("hasSetFavFromNoti", true)
                if (rating?.hasHeart()!!) {
                    if (mediaPlayer?.isPlaying!!) {
                        buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
                    } else {
                        buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
                    }
                    Toast.makeText(applicationContext, "Removed Favourite", Toast.LENGTH_SHORT)
                        .show()
                    bundle.putBoolean("isFav", false)
                } else {
                    bundle.putBoolean("isFav", true)
                    if (mediaPlayer?.isPlaying!!) {
                        buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.FAVOURITE, 1f)
                    } else {
                        buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.FAVOURITE, 0f)
                    }
                    Toast.makeText(applicationContext, "Added Favourite", Toast.LENGTH_SHORT)
                        .show()
                }
                val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
                updatePlayer.putExtras(bundle)
                sendBroadcast(updatePlayer)
            }
        })
    }

    fun updateMetaData() {
        val albumArt =
            UriToBitmapConverter.getBitmap(contentResolver, activeAudio?.artUri!!.toUri())
                ?: BitmapFactory.decodeResource(resources, R.drawable.music_note_notification)

        // Update the current metadata
        mediaSession!!.setMetadata(
            MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio!!.artistsName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio!!.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio!!.songName)
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    activeAudio!!.duration.toLong()
                )
                .build()
        )
    }

    private fun updateMiniPlayerWithBundle() {
        /** Update UI of [AllSongFragment] */
        val updatePlayer = Intent(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        val bundle = Bundle()
        bundle.putBoolean("pausedFromNoti", true)
        updatePlayer.putExtras(bundle)
        sendBroadcast(updatePlayer)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        isTaskRemoved = true
        stopServiceIfTaskRemoved()
    }

    private fun stopServiceIfTaskRemoved() {
        if (!mediaPlayer?.isPlaying!!) {
            //store audio resume pos

            if (mediaPlayer?.currentPosition == 0) {
                // if user didn't resume audio then medial player current pos will we zero
                // at that time save the RESUME POSITION which we will get from PREFS
                StorageUtil(applicationContext).storeAudioResumePos(resumePosition)
                Toast.makeText(this, "$resumePosition", Toast.LENGTH_SHORT).show()
            } else {
                StorageUtil(applicationContext).storeAudioResumePos(mediaPlayer?.currentPosition!!)
            }

            StorageUtil(applicationContext).storeLastAudioMaxSeekProg(mediaPlayer?.duration!!)

            Toast.makeText(applicationContext, "Destroyed", Toast.LENGTH_SHORT).show()
            Log.d("PlayBeatServiceDestroyed", "onDestroy: Destroyed")
            if (mediaPlayer != null) {
                stopMedia()
                mediaPlayer!!.release()
            }
            removeAudioFocus()

            //Disable the PhoneStateListener
            if (phoneStateListener != null) {
                telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            }

            removeNotification()
            stopForeground(true)

            //unregister BroadcastReceivers
            unregisterReceiver(becomingNoisyReceiver)
            unregisterReceiver(playNewAudio)
            //unregisterReceiver(openContent)

            if (sleepCountDownTimer != null) {
                sleepCountDownTimer?.cancel()
                StorageUtil(applicationContext).saveSleepTime(0)
            }

            //clear cached playlist
            //StorageUtil(applicationContext).clearCachedAudioPlaylist()
            // mediaPlayer = null
            stopSelf()
            AllSongFragment.musicService = null
            //stopService(Intent(this, PlayBeatMusicService::class.java))
        }
    }

    // Broad cast receivers .......................................................
    private val playNewAudio: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val storageUtil = StorageUtil(applicationContext)

            initAudioOnPressResumeBtn = false

            audioList = storageUtil.loadQueueAudio()
            //Get the new media index form SharedPreferences
            audioIndex = storageUtil.loadAudioIndex()
            if (audioIndex != -1 && audioIndex < audioList!!.size) {
                //index is in a valid range
                activeAudio = audioList!![audioIndex]
            } else {
                stopSelf()
            }

            pausedByManually = false
            requestAudioFocus()

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio

            if (mediaPlayer != null && mediaPlayer?.isPlaying!!) {
                stopMedia()
                //mediaPlayer!!.reset()
            }

            initMediaPlayer()
            updateMetaData()
            //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
            //buildNotification(PlaybackStatus.PLAYING, PlaybackStatus.UN_FAVOURITE, 1f)
            updateNotification(true)
        }
    }

    private fun registerPlayNewAudio() {
        //Register playNewMedia receiver
        val filter = IntentFilter(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
        registerReceiver(playNewAudio, filter)
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia()
            //buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
            updateNotification(false)
        }
    }

    private fun registerBecomingNoisyReceiver() {
        //register after getting audio focus
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(becomingNoisyReceiver, intentFilter)
    }

    /* private val openContent: BroadcastReceiver = object : BroadcastReceiver() {
         override fun onReceive(context: Context?, intent: Intent?) {

         }
     }

     private fun registerContentReceiver() {
         val intentFiler = IntentFilter(OPEN_CONTENT)
         registerReceiver(openContent, intentFiler)
     }*/

    fun stopSleepTimer(sleepTimerTV: TextView, sleepTimeIV: ImageView) {
        if (sleepCountDownTimer != null) {
            sleepCountDownTimer?.cancel()
            isSleepTimeRunning = false
            StorageUtil(applicationContext).saveSleepTime(0)
            sleepTimeIV.visibility = View.VISIBLE
            sleepTimerTV.visibility = View.GONE
        }
    }

    fun startSleepTimeCountDown(
        countingTimeTV: TextView,
        rlEndTimeCountDown: RelativeLayout?,
        endAudioTimeBtn: MaterialButton?,
        sleepTimeInMin: Long,
        sleepTimeIV: ImageView,
        sleepTimerTV: TextView
    ) {
        if (sleepCountDownTimer != null) {
            sleepCountDownTimer?.cancel()
        }

        sleepCountDownTimer = object : CountDownTimer(sleepTimeInMin, 1000) {
            override fun onTick(millis: Long) {
                isSleepTimeRunning = true
                countingTimeTV.text = millisToMinutesAndSeconds(millis)
                sleepTimerTV.text = millisToMinutesAndSeconds(millis)
                sleepTimeIV.visibility = View.GONE
                sleepTimerTV.visibility = View.VISIBLE

                Log.d(
                    "sleepTimeCounter", "onTick: sleep time: ${(sleepTimeInMin)}" +
                            "millis: $millis ,Diff: ${(sleepTimeInMin) - millis}"
                )
                StorageUtil(applicationContext).saveSleepTime(millis)

            }

            override fun onFinish() {
                if (mediaPlayer != null) {
                    pauseMedia()
                    pausedByManually = false
                    /* buildNotification(
                         PlaybackStatus.PAUSED,
                         PlaybackStatus.UN_FAVOURITE,
                         0f
                     )*/
                    updateNotification(false)
                    isSleepTimeRunning = false
                    StorageUtil(applicationContext).saveSleepTime(0)
                    rlEndTimeCountDown?.visibility = View.GONE
                    endAudioTimeBtn?.visibility = View.VISIBLE
                    sleepTimeIV.visibility = View.VISIBLE
                    sleepTimerTV.visibility = View.GONE
                }
            }
        }.start()
    }

    fun startSleepTimeCountDown(
        sleepTimeInMin: Long,
        sleepTimeIV: ImageView,
        sleepTimerTV: TextView
    ) {
        if (sleepCountDownTimer != null) {
            sleepCountDownTimer?.cancel()
        }

        sleepCountDownTimer = object : CountDownTimer(sleepTimeInMin, 1000) {
            override fun onTick(millis: Long) {
                isSleepTimeRunning = true
                //countingTimeTV.text = millisToMinutesAndSeconds(millis)
                sleepTimerTV.text = millisToMinutesAndSeconds(millis)
                sleepTimeIV.visibility = View.GONE
                sleepTimerTV.visibility = View.VISIBLE

                Log.d(
                    "sleepTimeCounter", "onTick: sleep time: ${(sleepTimeInMin)}" +
                            "millis: $millis ,Diff: ${(sleepTimeInMin) - millis}"
                )
                StorageUtil(applicationContext).saveSleepTime(millis)

            }

            override fun onFinish() {
                if (mediaPlayer != null) {
                    pauseMedia()
                    pausedByManually = false
                    /* buildNotification(
                         PlaybackStatus.PAUSED,
                         PlaybackStatus.UN_FAVOURITE,
                         0f
                     )*/
                    updateNotification(false)
                    isSleepTimeRunning = false
                    StorageUtil(applicationContext).saveSleepTime(0)
                    //rlEndTimeCountDown?.visibility = View.GONE
                    //endAudioTimeBtn?.visibility = View.VISIBLE
                    sleepTimeIV.visibility = View.VISIBLE
                    sleepTimerTV.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun millisToMinutesAndSeconds(millis: Long): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds.toInt() == 60) "${(minutes.toInt() + 1)} : 00" else "${minutes.toInt()} : ${if (seconds < 10) "0" else ""}$seconds "
    }
}
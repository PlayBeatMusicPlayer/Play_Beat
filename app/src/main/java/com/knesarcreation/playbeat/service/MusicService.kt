package com.knesarcreation.playbeat.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.*
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.PowerManager.WakeLock
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.media.AudioAttributesCompat
import androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver.handleIntent
import androidx.preference.PreferenceManager
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.*
import com.knesarcreation.playbeat.appwidgets.*
import com.knesarcreation.playbeat.auto.AutoMediaIDHelper
import com.knesarcreation.playbeat.auto.AutoMusicProvider
import com.knesarcreation.playbeat.equailizer.Settings
import com.knesarcreation.playbeat.fragments.songs.SongsFragment
import com.knesarcreation.playbeat.glide.BlurTransformation
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension.getSongModel
import com.knesarcreation.playbeat.helper.MusicPlayerRemote.isCasting
import com.knesarcreation.playbeat.helper.ShuffleHelper.makeShuffleList
import com.knesarcreation.playbeat.interfaces.IPlaybackStateChanged
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.model.Song.Companion.emptySong
import com.knesarcreation.playbeat.model.smartplaylist.AbsSmartPlaylist
import com.knesarcreation.playbeat.providers.HistoryStore
import com.knesarcreation.playbeat.providers.MusicPlaybackQueueStore
import com.knesarcreation.playbeat.providers.SongPlayCountStore
import com.knesarcreation.playbeat.service.AudioFader.Companion.startFadeAnimator
import com.knesarcreation.playbeat.service.notification.PlayingNotification
import com.knesarcreation.playbeat.service.notification.PlayingNotificationClassic
import com.knesarcreation.playbeat.service.notification.PlayingNotificationImpl24
import com.knesarcreation.playbeat.service.playback.Playback
import com.knesarcreation.playbeat.util.MusicUtil.getMediaStoreAlbumCoverUri
import com.knesarcreation.playbeat.util.MusicUtil.getSongFileUri
import com.knesarcreation.playbeat.util.MusicUtil.isFavorite
import com.knesarcreation.playbeat.util.MusicUtil.toggleFavorite
import com.knesarcreation.playbeat.util.PackageValidator
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.PreferenceUtil.crossFadeDuration
import com.knesarcreation.playbeat.util.PreferenceUtil.isAlbumArtOnLockScreen
import com.knesarcreation.playbeat.util.PreferenceUtil.isBluetoothSpeaker
import com.knesarcreation.playbeat.util.PreferenceUtil.isBlurredAlbumArt
import com.knesarcreation.playbeat.util.PreferenceUtil.isClassicNotification
import com.knesarcreation.playbeat.util.PreferenceUtil.isHeadsetPlugged
import com.knesarcreation.playbeat.util.PreferenceUtil.isPauseOnZeroVolume
import com.knesarcreation.playbeat.util.PreferenceUtil.playbackSpeed
import com.knesarcreation.playbeat.util.PreferenceUtil.registerOnSharedPreferenceChangedListener
import com.knesarcreation.playbeat.util.PreferenceUtil.unregisterOnSharedPreferenceChangedListener
import com.knesarcreation.playbeat.volume.AudioVolumeObserver
import com.knesarcreation.playbeat.volume.OnAudioVolumeChangedListener
import org.koin.java.KoinJavaComponent.get
import java.util.*

/**
 * @author Karim Abou Zeid (kabouzeid), Andrew Neal. Modified by Prathamesh More
 */
class MusicService : MediaBrowserServiceCompat(),
    OnSharedPreferenceChangeListener, Playback.PlaybackCallbacks, OnAudioVolumeChangedListener {
    private val musicBind: IBinder = MusicBinder()

    //Equalizer
    var mEqualizer: Equalizer? = null
    var bassBoost: BassBoost? = null
    var presetReverb: PresetReverb? = null

    @JvmField
    var nextPosition = -1

    @JvmField
    var pendingQuit = false

    var playbackStateChangedListener: IPlaybackStateChanged? = null

    @JvmField
    var playback: Playback? = null
    private var mPackageValidator: PackageValidator? = null
    private val mMusicProvider = get<AutoMusicProvider>(AutoMusicProvider::class.java)
    private var trackEndedByCrossfade = false

    @JvmField
    var position = -1
    private val appWidgetBig = AppWidgetBig.instance
    private val appWidgetCard = AppWidgetCard.instance
    private val appWidgetClassic = AppWidgetClassic.instance
    private val appWidgetSmall = AppWidgetSmall.instance
    private val appWidgetText = AppWidgetText.instance
    private val appWidgetMd3 = AppWidgetMD3.instance
    private val appWidgetCircle = AppWidgetCircle.instance
    private val widgetIntentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.getStringExtra(EXTRA_APP_WIDGET_NAME)
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (command != null) {
                when (command) {
                    AppWidgetClassic.NAME -> {
                        appWidgetClassic.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetSmall.NAME -> {
                        appWidgetSmall.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetBig.NAME -> {
                        appWidgetBig.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetCard.NAME -> {
                        appWidgetCard.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetText.NAME -> {
                        appWidgetText.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetMD3.NAME -> {
                        appWidgetMd3.performUpdate(this@MusicService, ids)
                    }

                    AppWidgetCircle.NAME -> {
                        appWidgetCircle.performUpdate(this@MusicService, ids)
                    }
                }
            }
        }
    }
    private var audioManager: AudioManager? = null
        get() {
            if (field == null) {
                field = getSystemService()
            }
            return field
        }

    private val becomingNoisyReceiverIntentFilter =
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var becomingNoisyReceiverRegistered = false
    private val bluetoothConnectedIntentFilter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
    private var bluetoothConnectedRegistered = false
    private val headsetReceiverIntentFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
    private var headsetReceiverRegistered = false
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var mediaStoreObserver: ContentObserver
    private var musicPlayerHandlerThread: HandlerThread? = null
    private var notHandledMetaChangedForCurrentTrack = false
    private var originalPlayingQueue = ArrayList<Song>()

    @JvmField
    var playingQueue = ArrayList<Song>()
    var isPausedByTransientLossOfFocus = false
    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null
                && intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY
            ) {
                pause()
                Log.d("SleepTimeAudioPaused", "pause:  Become noisy")
            }
        }
    }
    private var playerHandler: PlaybackHandler? = null
    private val audioFocusListener = OnAudioFocusChangeListener { focusChange ->
        playerHandler?.obtainMessage(FOCUS_CHANGE, focusChange, 0)?.sendToTarget()
    }
    private var playingNotification: PlayingNotification? = null

    private val updateFavoriteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            playingNotification?.updateFavorite(currentSong) { startForegroundOrNotify() }
            appWidgetCircle.notifyChange(this@MusicService, FAVORITE_STATE_CHANGED)
        }
    }

    /*private val lockScreenReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isLockScreen && isPlaying) {
                val lockIntent = Intent(context, LockScreenActivity::class.java)
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(lockIntent)
            }
        }
    }*/
    private var queueSaveHandler: QueueSaveHandler? = null
    private var queueSaveHandlerThread: HandlerThread? = null
    private var queuesRestored = false

    var repeatMode = 0
        private set(value) {
            when (value) {
                REPEAT_MODE_NONE, REPEAT_MODE_ALL, REPEAT_MODE_THIS -> {
                    field = value
                    PreferenceManager.getDefaultSharedPreferences(this).edit {
                        putInt(SAVED_REPEAT_MODE, value)
                    }
                    prepareNext()
                    handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
                }
            }
        }

    @JvmField
    var shuffleMode = 0
    private val songPlayCountHelper = SongPlayCountHelper()

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null) {
                if (BluetoothDevice.ACTION_ACL_CONNECTED == action && isBluetoothSpeaker) {
                    if (audioManager!!.isBluetoothA2dpOn) {
                        play()
                    }
                }
            }
        }
    }

    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            when (state) {
                TelephonyManager.CALL_STATE_IDLE ->                             // Not in call: Play music
                    play()

                TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                    Log.d("SleepTimeAudioPaused", "pause:  Phone state")
                    pause()
                }                           // A call is dialing, active or on hold

                else -> {}
            }
            super.onCallStateChanged(state, incomingNumber)
        }
    }

    private val headsetReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null) {
                if (Intent.ACTION_HEADSET_PLUG == action) {
                    when (intent.getIntExtra("state", -1)) {
                        0 -> {
                            pause()
                            Log.d("SleepTimeAudioPausedMediaSession", "pause:  headset")
                        }

                        1 -> play()
                    }
                }
            }
        }
    }
    private var throttledSeekHandler: ThrottledSeekHandler? = null
    private var uiThreadHandler: Handler? = null
    private var wakeLock: WakeLock? = null
    private var notificationManager: NotificationManager? = null
    private var isForeground = false
    override fun onCreate() {
        super.onCreate()
        val telephonyManager = getSystemService<TelephonyManager>()
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        val powerManager = getSystemService<PowerManager>()
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
        }
        wakeLock?.setReferenceCounted(false)
        musicPlayerHandlerThread = HandlerThread("PlaybackHandler")
        musicPlayerHandlerThread?.start()
        playerHandler = PlaybackHandler(this, musicPlayerHandlerThread!!.looper)

        // Set MultiPlayer when crossfade duration is 0 i.e. off
        playback = if (crossFadeDuration == 0) {
            MultiPlayer(this)
        } else {
            CrossFadePlayer(this)
        }
        playback?.setCallbacks(this)
        setupMediaSession()

        playbackStateChangedListener = SongsFragment.newInstance()
        // queue saving needs to run on a separate thread so that it doesn't block the playback handler
        // events
        queueSaveHandlerThread =
            HandlerThread("QueueSaveHandler", Process.THREAD_PRIORITY_BACKGROUND)
        queueSaveHandlerThread?.start()
        queueSaveHandler = QueueSaveHandler(this, queueSaveHandlerThread!!.looper)
        uiThreadHandler = Handler()
        registerReceiver(widgetIntentReceiver, IntentFilter(APP_WIDGET_UPDATE))
        registerReceiver(updateFavoriteReceiver, IntentFilter(FAVORITE_STATE_CHANGED))
        sessionToken = mediaSession?.sessionToken
        notificationManager = getSystemService()
        initNotification()
        mediaStoreObserver = MediaStoreObserver(this, playerHandler!!)
        throttledSeekHandler = ThrottledSeekHandler(this, playerHandler!!)
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Albums.INTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Artists.INTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Genres.INTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        contentResolver
            .registerContentObserver(
                MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI, true, mediaStoreObserver
            )
        val audioVolumeObserver = AudioVolumeObserver(this)
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this)
        registerOnSharedPreferenceChangedListener(this)
        restoreState()
        sendBroadcast(Intent("com.knesarcreation.playbeat.PLAY_BEAT_SERVICE_CREATED"))
        registerHeadsetEvents()
        registerBluetoothConnected()
        //mPackageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
        mMusicProvider.setMusicService(this)
    }

    override fun onDestroy() {
        unregisterReceiver(widgetIntentReceiver)
        unregisterReceiver(updateFavoriteReceiver)
        if (becomingNoisyReceiverRegistered) {
            unregisterReceiver(becomingNoisyReceiver)
            becomingNoisyReceiverRegistered = false
        }
        if (headsetReceiverRegistered) {
            unregisterReceiver(headsetReceiver)
            headsetReceiverRegistered = false
        }
        if (bluetoothConnectedRegistered) {
            unregisterReceiver(bluetoothReceiver)
            bluetoothConnectedRegistered = false
        }
        mediaSession?.isActive = false
        PreferenceUtil.sleepTime = 0
        PreferenceUtil.isSleepTimeEnable = false
        Log.d("ServiceDestroyed", "onDestroy: on service destroyed ")
        quit()
        releaseResources()
        contentResolver.unregisterContentObserver(mediaStoreObserver)
        unregisterOnSharedPreferenceChangedListener(this)
        wakeLock?.release()
        sendBroadcast(Intent("com.knesarcreation.playbeat.PLAY_BEAT_SERVICE_DESTROYED"))
    }

    private fun acquireWakeLock(milli: Long) {
        wakeLock?.acquire(milli)
    }

    var pausedByZeroVolume = false
    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        if (isPauseOnZeroVolume) {
            if (isPlaying && currentVolume < 1) {
                pause()
                Log.d("SleepTimeAudioPausedMediaSession", "pause:  audio volume changed")
                println("Paused")
                pausedByZeroVolume = true
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                println("Played")
                play()
                pausedByZeroVolume = false
            }
        }
    }

    fun addSong(position: Int, song: Song) {
        playingQueue.add(position, song)
        originalPlayingQueue.add(position, song)
        notifyChange(QUEUE_CHANGED)
    }

    fun addSong(song: Song) {
        playingQueue.add(song)
        originalPlayingQueue.add(song)
        notifyChange(QUEUE_CHANGED)
    }

    fun addSongs(position: Int, songs: List<Song>?) {
        playingQueue.addAll(position, songs!!)
        originalPlayingQueue.addAll(position, songs)
        notifyChange(QUEUE_CHANGED)
    }

    fun addSongs(songs: List<Song>?) {
        playingQueue.addAll(songs!!)
        originalPlayingQueue.addAll(songs)
        notifyChange(QUEUE_CHANGED)
    }

    fun back(force: Boolean) {
        if (songProgressMillis > 2000) {
            seek(0)
        } else {
            playPreviousSong(force)
        }
    }

    fun clearQueue() {
        playingQueue.clear()
        originalPlayingQueue.clear()
        setPosition(-1)
        notifyChange(QUEUE_CHANGED)
    }

    fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            REPEAT_MODE_NONE -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_THIS
            else -> REPEAT_MODE_NONE
        }
    }

    val audioSessionId: Int
        get() = if (playback != null) {
            playback!!.audioSessionId
        } else -1
    val currentSong: Song
        get() = getSongAt(getPosition())
    val nextSong: Song?
        get() = if (isLastTrack && repeatMode == REPEAT_MODE_NONE) {
            null
        } else {
            getSongAt(getNextPosition(false))
        }

    private fun getNextPosition(force: Boolean): Int {
        var position = getPosition() + 1
        when (repeatMode) {
            REPEAT_MODE_ALL -> if (isLastTrack) {
                position = 0
            }

            REPEAT_MODE_THIS -> if (force) {
                if (isLastTrack) {
                    position = 0
                }
            } else {
                position -= 1
            }

            REPEAT_MODE_NONE -> if (isLastTrack) {
                position -= 1
            }

            else -> if (isLastTrack) {
                position -= 1
            }
        }
        return position
    }

    private fun getPlayingQueue(): List<Song> {
        return playingQueue
    }

    private fun getPosition(): Int {
        return position
    }

    private fun setPosition(position: Int) {
        // handle this on the handlers thread to avoid blocking the ui thread
        playerHandler?.removeMessages(SET_POSITION)
        playerHandler?.obtainMessage(SET_POSITION, position, 0)?.sendToTarget()
    }

    private fun getPreviousPosition(force: Boolean): Int {
        var newPosition = getPosition() - 1
        when (repeatMode) {
            REPEAT_MODE_ALL -> if (newPosition < 0) {
                newPosition = playingQueue.size - 1
            }

            REPEAT_MODE_THIS -> if (force) {
                if (newPosition < 0) {
                    newPosition = playingQueue.size - 1
                }
            } else {
                newPosition = getPosition()
            }

            REPEAT_MODE_NONE -> if (newPosition < 0) {
                newPosition = 0
            }

            else -> if (newPosition < 0) {
                newPosition = 0
            }
        }
        return newPosition
    }

    fun getQueueDurationMillis(position: Int): Long {
        var duration: Long = 0
        for (i in position + 1 until playingQueue.size) {
            duration += playingQueue[i].duration
        }
        return duration
    }

    private fun getShuffleMode(): Int {
        return shuffleMode
    }

    fun setShuffleMode(shuffleMode: Int) {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putInt(SAVED_SHUFFLE_MODE, shuffleMode)
        }
        when (shuffleMode) {
            SHUFFLE_MODE_SHUFFLE -> {
                this.shuffleMode = shuffleMode
                makeShuffleList(playingQueue, getPosition())
                position = 0
            }

            SHUFFLE_MODE_NONE -> {
                this.shuffleMode = shuffleMode
                val currentSongId = Objects.requireNonNull(currentSong).id
                playingQueue = ArrayList(originalPlayingQueue)
                var newPosition = 0
                for (song in playingQueue) {
                    if (song.id == currentSongId) {
                        newPosition = playingQueue.indexOf(song)
                    }
                }
                position = newPosition
            }
        }
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
        notifyChange(QUEUE_CHANGED)
        runOnUiThread {
            playbackStateChangedListener?.onStateChanged()
        }
    }

    private fun getSongAt(position: Int): Song {
        return if ((position >= 0) && (position < playingQueue.size)) {
            playingQueue[position]
        } else {
            emptySong
        }
    }

    val songDurationMillis: Int
        get() = if (playback != null) {
            playback!!.duration()
        } else -1
    val songProgressMillis: Int
        get() = if (playback != null) {
            playback!!.position()
        } else -1

    fun handleAndSendChangeInternal(what: String) {
        handleChangeInternal(what)
        sendChangeInternal(what)
    }

    private fun initNotification() {
        playingNotification = if (VERSION.SDK_INT >= VERSION_CODES.N
            && !isClassicNotification
        ) {
            PlayingNotificationImpl24.from(this, notificationManager!!, mediaSession!!)
        } else {
            PlayingNotificationClassic.from(this, notificationManager!!)
        }
    }

    val isLastTrack: Boolean
        get() = getPosition() == playingQueue.size - 1
    val isPlaying: Boolean
        get() = playback != null && playback!!.isPlaying

    fun moveSong(from: Int, to: Int) {
        if (from == to) {
            return
        }
        val currentPosition = getPosition()
        val songToMove = playingQueue.removeAt(from)
        playingQueue.add(to, songToMove)
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            val tmpSong = originalPlayingQueue.removeAt(from)
            originalPlayingQueue.add(to, tmpSong)
        }
        when {
            currentPosition in to until from -> {
                position = currentPosition + 1
            }

            currentPosition in (from + 1)..to -> {
                position = currentPosition - 1
            }

            from == currentPosition -> {
                position = to
            }
        }
        notifyChange(QUEUE_CHANGED)
    }

    fun notifyChange(what: String) {
        handleAndSendChangeInternal(what)
        sendPublicIntent(what)
    }

    override fun onBind(intent: Intent): IBinder {
        // For Android auto, need to call super, or onGetRoot won't be called.
        // return if ("android.media.browse.MediaBrowserService" == intent.action) {
        //     super.onBind(intent)!!
        // } else musicBind
        return musicBind
    }

    //  override fun onGetRoot(
    //      clientPackageName: String,
    //      clientUid: Int,
    //      rootHints: Bundle?
    //  ): BrowserRoot {


    // Check origin to ensure we're not allowing any arbitrary app to browse app contents
//        return if (!mPackageValidator!!.isKnownCaller(clientPackageName, clientUid)) {
//            // Request from an untrusted package: return an empty browser root
//            BrowserRoot(AutoMediaIDHelper.MEDIA_ID_EMPTY_ROOT, null)
//        } else {
//            /**
//             * By default return the browsable root. Treat the EXTRA_RECENT flag as a special case
//             * and return the recent root instead.
//             */
//            var isRecentRequest = false
//            if (rootHints != null) {
//                isRecentRequest =
//                    rootHints.getBoolean(BrowserRoot.EXTRA_RECENT)
//            }
//            val browserRootPath = if (isRecentRequest) {
//                AutoMediaIDHelper.RECENT_ROOT
//            } else {
//                AutoMediaIDHelper.MEDIA_ID_ROOT
//            }
//            BrowserRoot(browserRootPath, null)
//        }
    // }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == AutoMediaIDHelper.RECENT_ROOT) {
            val song = currentSong
            val mediaItem = MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(song.id.toString())
                    .setTitle(song.title)
                    .setSubtitle(song.artistName)
                    .setIconUri(getMediaStoreAlbumCoverUri(song.albumId))
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
            result.sendResult(listOf(mediaItem))
        } else {
            result.sendResult(mMusicProvider.getChildren(parentId, resources))
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?, key: String?
    ) {
        when (key) {
            CROSS_FADE_DURATION -> {
                val progress = songProgressMillis
                val wasPlaying = isPlaying
                /* Switch to MultiPlayer if Crossfade duration is 0 and
                Playback is not an instance of MultiPlayer */if (playback != null) playback?.setCrossFadeDuration(
                    crossFadeDuration
                )
                if (playback !is MultiPlayer && crossFadeDuration == 0) {
                    if (playback != null) {
                        playback?.release()
                    }
                    playback = null
                    playback = MultiPlayer(this)
                    playback?.setCallbacks(this)
                    if (openTrackAndPrepareNextAt(position)) {
                        seek(progress)
                        if (wasPlaying) {
                            play()
                        }
                    }
                } else if (playback !is CrossFadePlayer && crossFadeDuration > 0) {
                    if (playback != null) {
                        playback?.release()
                    }
                    playback = null
                    playback = CrossFadePlayer(this)
                    playback?.setCallbacks(this)
                    if (openTrackAndPrepareNextAt(position)) {
                        seek(progress)
                        if (wasPlaying) {
                            play()
                        }
                    }
                }
            }

            ALBUM_ART_ON_LOCK_SCREEN, BLURRED_ALBUM_ART -> updateMediaSessionMetaData()
            COLORED_NOTIFICATION -> {
                playingNotification?.updateMetadata(currentSong) {
                    playingNotification?.setPlaying(isPlaying)
                    startForegroundOrNotify()
                }
            }

            CLASSIC_NOTIFICATION -> {
                updateNotification()
                playingNotification?.updateMetadata(currentSong) {
                    playingNotification?.setPlaying(isPlaying)
                    startForegroundOrNotify()
                }
            }

            PLAYBACK_SPEED -> updateMediaSessionPlaybackState()
            TOGGLE_HEADSET -> registerHeadsetEvents()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null) {
            handleIntent(mediaSession, intent)
            restoreQueuesAndPositionIfNecessary()
            if (playback != null) {
                try {
                    mEqualizer = Equalizer(1000, audioSessionId)
                    bassBoost = BassBoost(1000, audioSessionId)
                    presetReverb = PresetReverb(1000, audioSessionId)
                } catch (e: Exception) {
                    Log.d("EqualizerFailedToInit", "onStartCommand: ${e.message} ")
                }
            }
            when (intent.action) {
                ACTION_TOGGLE_PAUSE -> if (isPlaying) {
                    Log.d("SleepTimeAudioPausedMediaSession", "pause:  ACTION_TOGGLE_PAUSE")

                    pause()
                } else {
                    play()
                }

                ACTION_PAUSE -> {
                    pause()
                    Log.d("SleepTimeAudioPausedMediaSession", "pause:  ACTION_PAUSE")

                }

                ACTION_PLAY -> play()
                ACTION_PLAY_PLAYLIST -> playFromPlaylist(intent)
                ACTION_REWIND -> back(true)
                ACTION_SKIP -> playNextSong(true)
                ACTION_STOP, ACTION_QUIT -> {
                    pendingQuit = false
                    quit()
                    Log.d("SleepTimeAudioPaused", "pause:  ACTION_QUIT")
                    PreferenceUtil.isSleepTimeEnable = false
                    PreferenceUtil.sleepTime = 0
                }

                ACTION_PENDING_QUIT -> pendingQuit = true
                TOGGLE_FAVORITE -> toggleFavorite(applicationContext, currentSong)
            }
        }
        return START_NOT_STICKY
    }

    override fun onTrackEnded() {
        acquireWakeLock(30000)
        playerHandler?.sendEmptyMessage(TRACK_ENDED)
    }

    override fun onTrackEndedWithCrossfade() {
        trackEndedByCrossfade = true
        acquireWakeLock(30000)
        playerHandler?.sendEmptyMessage(TRACK_ENDED)
    }

    override fun onTrackWentToNext() {
        playerHandler?.sendEmptyMessage(TRACK_WENT_TO_NEXT)
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (!isPlaying) {
            stopSelf()
        }
        return true
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return null
    }

    fun openQueue(
        playingQueue: List<Song>?,
        startPosition: Int,
        startPlaying: Boolean
    ) {
        if (playingQueue != null && playingQueue.isNotEmpty()
            && startPosition >= 0 && startPosition < playingQueue.size
        ) {
            // it is important to copy the playing queue here first as we might add/remove songs later
            originalPlayingQueue = ArrayList(playingQueue)
            this.playingQueue = ArrayList(originalPlayingQueue)
            var position = startPosition
            if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                makeShuffleList(this.playingQueue, startPosition)
                position = 0
            }
            if (startPlaying) {
                playSongAt(position)
            } else {
                setPosition(position)
            }
            notifyChange(QUEUE_CHANGED)
        }
    }

    @Synchronized
    fun openTrackAndPrepareNextAt(position: Int): Boolean {
        this.position = position
        val prepared = openCurrent()
        if (prepared) {
            prepareNextImpl()
        }
        notifyChange(META_CHANGED)
        notHandledMetaChangedForCurrentTrack = false
        return prepared
    }

    fun pause() {
        Log.i(TAG, "Paused")
        isPausedByTransientLossOfFocus = false
        if (playback != null && playback!!.isPlaying) {
            startFadeAnimator(playback!!, false) {

                //Code to run when Animator Ends
                playback?.pause()
                notifyChange(PLAY_STATE_CHANGED)
                runOnUiThread {
                    playbackStateChangedListener?.onStateChanged()
                }
                Log.d("SleepTimeAudioPaused", "pause:  True")
            }
        }
    }

    fun forcePause() {
        isPausedByTransientLossOfFocus = false
        if (playback != null && playback!!.isPlaying) {
            playback?.pause()
            notifyChange(PLAY_STATE_CHANGED)
        }
    }

    @Synchronized
    fun play() {
        if (requestFocus()) {
            if (playback != null && !playback!!.isPlaying) {

                try {
                    Settings().applyEqualizer(applicationContext)
                } catch (e: Exception) {
                    Log.d("ApplyEqualizerThrows", "play: ${e.message} ")
                }

                if (!playback!!.isInitialized) {
                    playSongAt(getPosition())
                } else {
                    //Don't Start playing when it's casting
                    if (isCasting) {
                        return
                    }
                    startFadeAnimator(playback!!, true) {

                        // Code when Animator Ends
                        if (!becomingNoisyReceiverRegistered) {
                            registerReceiver(
                                becomingNoisyReceiver,
                                becomingNoisyReceiverIntentFilter
                            )
                            becomingNoisyReceiverRegistered = true
                        }
                        if (notHandledMetaChangedForCurrentTrack) {
                            handleChangeInternal(META_CHANGED)
                            notHandledMetaChangedForCurrentTrack = false
                        }

                        // fixes a bug where the volume would stay ducked because the
                        // AudioManager.AUDIOFOCUS_GAIN event is not sent
                        playerHandler?.removeMessages(DUCK)
                        playerHandler?.sendEmptyMessage(UNDUCK)
                    }
                    //Start Playback with Animator
                    playback?.start()
                    notifyChange(PLAY_STATE_CHANGED)
                    runOnUiThread {
                        playbackStateChangedListener!!.onStateChanged()
                    }
                }
            }
        } else {
            Toast.makeText(
                this, resources.getString(R.string.audio_focus_denied), Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    fun playNextSong(force: Boolean) {
        playSongAt(getNextPosition(force))
    }

    fun playPreviousSong(force: Boolean) {
        playSongAt(getPreviousPosition(force))
    }

    fun playSongAt(position: Int) {
        // handle this on the handlers thread to avoid blocking the ui thread
        playerHandler?.removeMessages(PLAY_SONG)
        playerHandler?.obtainMessage(PLAY_SONG, position, 0)?.sendToTarget()
    }

    fun playSongAtImpl(position: Int) {
        if (!trackEndedByCrossfade) {
            // This is only imp if we are using crossfade
            if (playback is CrossFadePlayer) {
                (playback as CrossFadePlayer).sourceChangedByUser()
            }
        } else {
            trackEndedByCrossfade = false
        }
        if (openTrackAndPrepareNextAt(position)) {
            play()
        } else {
            Toast.makeText(this, resources.getString(R.string.unplayable_file), Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun playSongs(songs: ArrayList<Song>?, shuffleMode: Int) {
        if (songs != null && songs.isNotEmpty()) {
            if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                val startPosition = Random().nextInt(songs.size)
                openQueue(songs, startPosition, false)
                setShuffleMode(shuffleMode)
            } else {
                openQueue(songs, 0, false)
            }
            play()
        } else {
            Toast.makeText(applicationContext, R.string.playlist_is_empty, Toast.LENGTH_LONG).show()
        }
    }

    @Synchronized
    fun prepareNextImpl() {
        try {
            val nextPosition = getNextPosition(false)
            playback?.setNextDataSource(getTrackUri(getSongAt(nextPosition)))
            this.nextPosition = nextPosition
        } catch (ignored: Exception) {
        }
    }

    fun quit() {
        pause()
        Log.d("SleepTimeAudioPaused", "pause:  quit")
        stopForeground(true)
        notificationManager?.cancel(PlayingNotification.NOTIFICATION_ID)
        closeAudioEffectSession()
        audioManager?.abandonAudioFocus(audioFocusListener)
        stopSelf()
    }

    fun releaseWakeLock() {
        if (wakeLock!!.isHeld) {
            wakeLock?.release()
        }
    }

    fun removeSong(position: Int) {
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            playingQueue.removeAt(position)
            originalPlayingQueue.removeAt(position)
        } else {
            originalPlayingQueue.remove(playingQueue.removeAt(position))
        }
        rePosition(position)
        notifyChange(QUEUE_CHANGED)
    }

    private fun removeSongImpl(song: Song) {
        val deletePosition = playingQueue.indexOf(song)
        if (deletePosition != -1) {
            playingQueue.removeAt(deletePosition)
            rePosition(deletePosition)
        }

        val originalDeletePosition = originalPlayingQueue.indexOf(song)
        if (originalDeletePosition != -1) {
            originalPlayingQueue.removeAt(originalDeletePosition)
            rePosition(originalDeletePosition)
        }
    }

    fun removeSong(song: Song) {
        removeSongImpl(song)
        notifyChange(QUEUE_CHANGED)
    }

    fun removeSongs(songs: List<Song>) {
        for (song in songs) {
            removeSongImpl(song)
        }
        notifyChange(QUEUE_CHANGED)
    }

    private fun rePosition(deletedPosition: Int) {
        val currentPosition = getPosition()
        if (deletedPosition < currentPosition) {
            position = currentPosition - 1
        } else if (deletedPosition == currentPosition) {
            if (playingQueue.size > deletedPosition) {
                setPosition(position)
            } else {
                setPosition(position - 1)
            }
        }
    }

    @Synchronized
    fun restoreQueuesAndPositionIfNecessary() {
        if (!queuesRestored && playingQueue.isEmpty()) {
            val restoredQueue = MusicPlaybackQueueStore.getInstance(this).savedPlayingQueue
            val restoredOriginalQueue =
                MusicPlaybackQueueStore.getInstance(this).savedOriginalPlayingQueue
            val restoredPosition = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                SAVED_POSITION, -1
            )
            val restoredPositionInTrack =
                PreferenceManager.getDefaultSharedPreferences(this).getInt(
                    SAVED_POSITION_IN_TRACK, -1
                )
            if (restoredQueue.size > 0 && restoredQueue.size == restoredOriginalQueue.size && restoredPosition != -1) {
                originalPlayingQueue = ArrayList(restoredOriginalQueue)
                playingQueue = ArrayList(restoredQueue)
                position = restoredPosition
                openCurrent()
                prepareNext()
                if (restoredPositionInTrack > 0) {
                    seek(restoredPositionInTrack)
                }
                notHandledMetaChangedForCurrentTrack = true
                sendChangeInternal(META_CHANGED)
                sendChangeInternal(QUEUE_CHANGED)
            }
        }
        queuesRestored = true
    }

    fun runOnUiThread(runnable: Runnable?) {
        uiThreadHandler?.post(runnable!!)
    }

    fun savePositionInTrack() {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putInt(SAVED_POSITION_IN_TRACK, songProgressMillis)
        }
    }

    fun saveQueuesImpl() {
        MusicPlaybackQueueStore.getInstance(this).saveQueues(playingQueue, originalPlayingQueue)
    }

    fun saveState() {
        saveQueues()
        savePosition()
        savePositionInTrack()
    }

    @Synchronized
    fun seek(millis: Int): Int {
        return try {
            var newPosition = 0
            if (playback != null) {
                newPosition = playback!!.seek(millis)
            }
            throttledSeekHandler?.notifySeek()
            newPosition
        } catch (e: Exception) {
            -1
        }
    }

    // to let other apps know whats playing. i.E. last.fm (scrobbling) or musixmatch
    fun sendPublicIntent(what: String) {
        val intent = Intent(what.replace(PLAY_BEAT_PACKAGE_NAME, MUSIC_PACKAGE_NAME))
        val song = currentSong
        intent.putExtra("id", song.id)
        intent.putExtra("artist", song.artistName)
        intent.putExtra("album", song.albumName)
        intent.putExtra("track", song.title)
        intent.putExtra("duration", song.duration)
        intent.putExtra("position", songProgressMillis.toLong())
        intent.putExtra("playing", isPlaying)
        intent.putExtra("scrobbling_source", PLAY_BEAT_PACKAGE_NAME)
        sendStickyBroadcast(intent)
    }

    fun toggleShuffle() {
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            setShuffleMode(SHUFFLE_MODE_SHUFFLE)
        } else {
            setShuffleMode(SHUFFLE_MODE_NONE)
        }
    }

    fun updateMediaSessionPlaybackState() {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(MEDIA_SESSION_ACTIONS)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                songProgressMillis.toLong(),
                playbackSpeed
            )
        setCustomAction(stateBuilder)
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun updateNotification() {
        if (playingNotification != null && currentSong.id != -1L) {
            stopForegroundAndNotification()
            initNotification()
        }
    }

    fun updateMediaSessionMetaData() {
        Log.i(TAG, "onResourceReady: ")
        val song = currentSong
        if (song.id == -1L) {
            mediaSession?.setMetadata(null)
            return
        }
        val metaData = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (getPosition() + 1).toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.year.toLong())
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null)
        metaData.putLong(
            MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,
            playingQueue.size.toLong()
        )
        if (isAlbumArtOnLockScreen) {
            val screenSize = PlayBeatUtil.getScreenSize(this@MusicService)
            val request: RequestBuilder<Bitmap> =
                GlideApp.with(this@MusicService)
                    .asBitmap()
                    .songCoverOptions(song)
                    .load(getSongModel(song))
            if (isBlurredAlbumArt) {
                request.transform(BlurTransformation.Builder(this@MusicService).build())
            }
            runOnUiThread {
                request.into(object : SimpleTarget<Bitmap?>(screenSize.x, screenSize.y) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        mediaSession?.setMetadata(metaData.build())
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap?>?
                    ) {
                        metaData.putBitmap(
                            MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                            copy(resource)
                        )
                        mediaSession?.setMetadata(metaData.build())
                    }
                })
            }
        } else {
            mediaSession?.setMetadata(metaData.build())
        }
    }

    private fun closeAudioEffectSession() {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        if (playback != null) {
            audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playback!!.audioSessionId)
        }
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        sendBroadcast(audioEffectsIntent)
    }

    private fun handleChangeInternal(what: String) {
        when (what) {
            PLAY_STATE_CHANGED -> {
                updateMediaSessionPlaybackState()
                val isPlaying = isPlaying
                if (!isPlaying && songProgressMillis > 0) {
                    savePositionInTrack()
                }
                songPlayCountHelper.notifyPlayStateChanged(isPlaying)
                playingNotification?.setPlaying(isPlaying)
                startForegroundOrNotify()
            }

            FAVORITE_STATE_CHANGED -> {
                playingNotification?.updateFavorite(currentSong) {
                    startForegroundOrNotify()
                }

                updateMediaSessionMetaData()
                updateMediaSessionPlaybackState()
                savePosition()
                savePositionInTrack()
                val currentSong = currentSong
                HistoryStore.getInstance(this).addSongId(currentSong.id)
                if (songPlayCountHelper.shouldBumpPlayCount()) {
                    SongPlayCountStore.getInstance(this).bumpPlayCount(songPlayCountHelper.song.id)
                }
                songPlayCountHelper.notifySongChanged(currentSong)
            }

            META_CHANGED -> {
                playingNotification?.updateMetadata(currentSong) { startForegroundOrNotify() }
                playingNotification?.updateFavorite(currentSong) { startForegroundOrNotify() }
                updateMediaSessionMetaData()
                updateMediaSessionPlaybackState()
                savePosition()
                savePositionInTrack()
                val currentSong = currentSong
                HistoryStore.getInstance(this).addSongId(currentSong.id)
                if (songPlayCountHelper.shouldBumpPlayCount()) {
                    SongPlayCountStore.getInstance(this).bumpPlayCount(songPlayCountHelper.song.id)
                }
                songPlayCountHelper.notifySongChanged(currentSong)
            }

            QUEUE_CHANGED -> {
                updateMediaSessionMetaData() // because playing queue size might have changed
                saveState()
                if (playingQueue.size > 0) {
                    prepareNext()
                } else {
                    stopForegroundAndNotification()
                }
            }
        }
    }

    private fun startForegroundOrNotify() {
        if (playingNotification != null && currentSong.id != -1L) {
            if (!VersionUtils.hasS()) {
                if (isForeground && !isPlaying) {
                    // This makes the notification dismissible
                    // We can't call stopForeground(false) on A12 though, which may result in crashes
                    // when we call startForeground after that e.g. when Alarm goes off

                    stopForeground(false)
                    isForeground = false
                }
            }
            if (!isForeground) {
                // Specify that this is a media service, if supported.
                if (VersionUtils.hasQ()) {
                    startForeground(
                        PlayingNotification.NOTIFICATION_ID, playingNotification!!.build(),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(
                        PlayingNotification.NOTIFICATION_ID,
                        playingNotification!!.build()
                    )
                }
                isForeground = true
            } else {
                // If we are already in foreground just update the notification
                notificationManager?.notify(
                    PlayingNotification.NOTIFICATION_ID, playingNotification!!.build()
                )
            }
        }
    }

    private fun stopForegroundAndNotification() {
        stopForeground(true)
        notificationManager?.cancel(PlayingNotification.NOTIFICATION_ID)
        isForeground = false
    }

    @Synchronized
    private fun openCurrent(): Boolean {
        return try {
            if (playback != null) {
                return playback!!.setDataSource(
                    getTrackUri(
                        Objects.requireNonNull(
                            currentSong
                        )
                    )
                )
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun playFromPlaylist(intent: Intent) {
        val playlist: AbsSmartPlaylist? = intent.getParcelableExtra(INTENT_EXTRA_PLAYLIST)
        val shuffleMode = intent.getIntExtra(INTENT_EXTRA_SHUFFLE_MODE, getShuffleMode())
        if (playlist != null) {
            val playlistSongs = playlist.songs()
            if (playlistSongs.isNotEmpty()) {
                if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                    val startPosition = Random().nextInt(playlistSongs.size)
                    openQueue(playlistSongs, startPosition, true)
                    setShuffleMode(shuffleMode)
                } else {
                    openQueue(playlistSongs, 0, true)
                }
            } else {
                Toast.makeText(applicationContext, R.string.playlist_is_empty, Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            Toast.makeText(applicationContext, R.string.playlist_is_empty, Toast.LENGTH_LONG).show()
        }
    }

    private fun prepareNext() {
        playerHandler?.removeMessages(PREPARE_NEXT)
        playerHandler?.obtainMessage(PREPARE_NEXT)?.sendToTarget()
    }

    private fun registerBluetoothConnected() {
        Log.i(TAG, "registerBluetoothConnected: ")
        if (!bluetoothConnectedRegistered) {
            registerReceiver(bluetoothReceiver, bluetoothConnectedIntentFilter)
            bluetoothConnectedRegistered = true
        }
    }

    private fun registerHeadsetEvents() {
        if (!headsetReceiverRegistered && isHeadsetPlugged) {
            registerReceiver(headsetReceiver, headsetReceiverIntentFilter)
            headsetReceiverRegistered = true
        }
    }

    private fun releaseResources() {
        playerHandler?.removeCallbacksAndMessages(null)
        musicPlayerHandlerThread?.quitSafely()
        queueSaveHandler?.removeCallbacksAndMessages(null)
        queueSaveHandlerThread?.quitSafely()
        if (playback != null) {
            playback?.release()
        }
        playback = null
        mediaSession?.release()
    }

    private fun requestFocus(): Boolean {
        return AudioManagerCompat.requestAudioFocus(
            audioManager!!,
            AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setAudioAttributes(
                    AudioAttributesCompat.Builder().setContentType(CONTENT_TYPE_MUSIC).build()
                ).build()
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun restoreState() {
        shuffleMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(
            SAVED_SHUFFLE_MODE, 0
        )
        repeatMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(
            SAVED_REPEAT_MODE, 0
        )
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED)
        handleAndSendChangeInternal(REPEAT_MODE_CHANGED)
        playerHandler?.removeMessages(RESTORE_QUEUES)
        playerHandler?.sendEmptyMessage(RESTORE_QUEUES)
    }

    private fun savePosition() {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putInt(SAVED_POSITION, getPosition())
        }
    }

    private fun saveQueues() {
        queueSaveHandler?.removeMessages(SAVE_QUEUES)
        queueSaveHandler?.sendEmptyMessage(SAVE_QUEUES)
    }

    private fun sendChangeInternal(what: String) {
        sendBroadcast(Intent(what))
        appWidgetBig.notifyChange(this, what)
        appWidgetClassic.notifyChange(this, what)
        appWidgetSmall.notifyChange(this, what)
        appWidgetCard.notifyChange(this, what)
        appWidgetText.notifyChange(this, what)
        appWidgetMd3.notifyChange(this, what)
        appWidgetCircle.notifyChange(this, what)
    }

    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
        var repeatIcon = R.drawable.ic_repeat // REPEAT_MODE_NONE
        if (repeatMode == REPEAT_MODE_THIS) {
            repeatIcon = R.drawable.ic_repeat_one
        } else if (repeatMode == REPEAT_MODE_ALL) {
            repeatIcon = R.drawable.ic_repeat_white_circle
        }
        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                CYCLE_REPEAT, getString(R.string.action_cycle_repeat), repeatIcon
            )
                .build()
        )
        val shuffleIcon =
            if (getShuffleMode() == SHUFFLE_MODE_NONE) R.drawable.ic_shuffle_off_circled else R.drawable.ic_shuffle_on_circled
        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                TOGGLE_SHUFFLE, getString(R.string.action_toggle_shuffle), shuffleIcon
            )
                .build()
        )
        val favoriteIcon = if (isFavorite(
                applicationContext,
                currentSong
            )
        ) R.drawable.ic_favorite else R.drawable.ic_favorite_border
        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                TOGGLE_FAVORITE, getString(R.string.action_toggle_favorite), favoriteIcon
            )
                .build()
        )
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(
            this,
            "PlayBeat"
        )
        val mediasessionCallback = MediaSessionCallback(applicationContext, this)
        mediaSession?.setCallback(mediasessionCallback)
        mediaSession?.isActive = true
    }

    private fun makeTimerPendingIntent(flag: Int): PendingIntent? {
        return PendingIntent.getService(
            applicationContext,
            0,
            Intent(applicationContext, MusicService::class.java).setAction(ACTION_QUIT),
            flag or if (VersionUtils.hasMarshmallow())
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }


    companion object {
        val TAG: String = MusicService::class.java.simpleName
        const val PLAY_BEAT_PACKAGE_NAME = "com.knesarcreation.playbeat"
        const val MUSIC_PACKAGE_NAME = "com.android.music"
        const val ACTION_TOGGLE_PAUSE = "$PLAY_BEAT_PACKAGE_NAME.togglepause"
        const val ACTION_PLAY = "$PLAY_BEAT_PACKAGE_NAME.play"
        const val ACTION_PLAY_PLAYLIST = "$PLAY_BEAT_PACKAGE_NAME.play.playlist"
        const val ACTION_PAUSE = "$PLAY_BEAT_PACKAGE_NAME.pause"
        const val ACTION_STOP = "$PLAY_BEAT_PACKAGE_NAME.stop"
        const val ACTION_SKIP = "$PLAY_BEAT_PACKAGE_NAME.skip"
        const val ACTION_REWIND = "$PLAY_BEAT_PACKAGE_NAME.rewind"
        const val ACTION_QUIT = "$PLAY_BEAT_PACKAGE_NAME.quitservice"
        const val ACTION_PENDING_QUIT = "$PLAY_BEAT_PACKAGE_NAME.pendingquitservice"
        const val INTENT_EXTRA_PLAYLIST = PLAY_BEAT_PACKAGE_NAME + "intentextra.playlist"
        const val INTENT_EXTRA_SHUFFLE_MODE = "$PLAY_BEAT_PACKAGE_NAME.intentextra.shufflemode"
        const val APP_WIDGET_UPDATE = "$PLAY_BEAT_PACKAGE_NAME.appreciate"
        const val EXTRA_APP_WIDGET_NAME = PLAY_BEAT_PACKAGE_NAME + "app_widget_name"

        // Do not change these three strings as it will break support with other apps (e.g. last.fm
        // scrobbling)
        const val META_CHANGED = "$PLAY_BEAT_PACKAGE_NAME.metachanged"
        const val QUEUE_CHANGED = "$PLAY_BEAT_PACKAGE_NAME.queuechanged"
        const val PLAY_STATE_CHANGED = "$PLAY_BEAT_PACKAGE_NAME.playstatechanged"
        const val FAVORITE_STATE_CHANGED = "$PLAY_BEAT_PACKAGE_NAME.favoritestatechanged"
        const val REPEAT_MODE_CHANGED = "$PLAY_BEAT_PACKAGE_NAME.repeatmodechanged"
        const val SHUFFLE_MODE_CHANGED = "$PLAY_BEAT_PACKAGE_NAME.shufflemodechanged"
        const val MEDIA_STORE_CHANGED = "$PLAY_BEAT_PACKAGE_NAME.mediastorechanged"
        const val CYCLE_REPEAT = "$PLAY_BEAT_PACKAGE_NAME.cyclerepeat"
        const val TOGGLE_SHUFFLE = "$PLAY_BEAT_PACKAGE_NAME.toggleshuffle"
        const val TOGGLE_FAVORITE = "$PLAY_BEAT_PACKAGE_NAME.togglefavorite"
        const val SAVED_POSITION = "POSITION"
        const val SAVED_POSITION_IN_TRACK = "POSITION_IN_TRACK"
        const val SAVED_SHUFFLE_MODE = "SHUFFLE_MODE"
        const val SAVED_REPEAT_MODE = "REPEAT_MODE"
        const val RELEASE_WAKELOCK = 0
        const val TRACK_ENDED = 1
        const val TRACK_WENT_TO_NEXT = 2
        const val PLAY_SONG = 3
        const val PREPARE_NEXT = 4
        const val SET_POSITION = 5
        const val FOCUS_CHANGE = 6
        const val DUCK = 7
        const val UNDUCK = 8
        const val RESTORE_QUEUES = 9
        const val SHUFFLE_MODE_NONE = 0
        const val SHUFFLE_MODE_SHUFFLE = 1
        const val REPEAT_MODE_NONE = 0
        const val REPEAT_MODE_ALL = 1
        const val REPEAT_MODE_THIS = 2
        const val SAVE_QUEUES = 0
        private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO)

        private fun copy(bitmap: Bitmap): Bitmap? {
            var config = bitmap.config
            if (config == null) {
                config = Bitmap.Config.RGB_565
            }
            return try {
                bitmap.copy(config, false)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                null
            }
        }

        private fun getTrackUri(song: Song): String {
            return getSongFileUri(song.id).toString()
        }
    }
}
package com.knesarcreation.playbeat.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.contains
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.knesarcreation.playbeat.*
import com.knesarcreation.playbeat.activities.base.AbsCastActivity
import com.knesarcreation.playbeat.ads.InterstitialAdHelperClass
import com.knesarcreation.playbeat.databinding.SlidingMusicPanelLayoutBinding
import com.knesarcreation.playbeat.extensions.*
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.SearchQueryHelper.getSongs
import com.knesarcreation.playbeat.interfaces.IScrollHelper
import com.knesarcreation.playbeat.model.CategoryInfo
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.network.CheckNetworkConnectionLive
import com.knesarcreation.playbeat.repository.PlaylistSongsLoader
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.util.AppRater
import com.knesarcreation.playbeat.util.FirebaseRemote
import com.knesarcreation.playbeat.util.PreferenceUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get


@SuppressLint("StaticFieldLeak")
var mInterstitialAdHelper: InterstitialAdHelperClass? = null

class MainActivity : AbsCastActivity(), OnSharedPreferenceChangeListener {

    companion object {
        const val TAG = "MainActivity"
        const val EXPAND_PANEL = "expand_panel"
        //var mCountDownTimer: CountDownTimer? = null
        //var isCountDownFinished = true
    }

    private lateinit var checkNetworkConnectionLive: CheckNetworkConnectionLive
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var firebaseRemote: FirebaseRemote

    override fun createContentView(): SlidingMusicPanelLayoutBinding {
        return wrapSlidingMusicPanel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setTaskDescriptionColorAuto()
        hideStatusBar()
        updateTabs()
        AppRater.appLaunched(this)

        analytics = FirebaseAnalytics.getInstance(this)

        firebasAnalyticsLogEvents(
            "AppStarted", "App Is Started", "appStarted"
        )

        firebaseRemote = object : FirebaseRemote(this) {
            override fun onFetchComplete(updated: Boolean) {
                Log.d("onFetchComplete", "onFetchComplete:$updated ")
            }
        }
        firebaseRemote.fetchRemoteConfig()

        val freeGamesUrl: String? = intent.extras?.getString(EXTRAS_FREE_GAMES)
        val quizUrl: String? = intent.extras?.getString(EXTRAS_QUIZ)

        if (freeGamesUrl != null) {
            openCustomTab(CustomTabsIntent.Builder().build(), Uri.parse(freeGamesUrl))
        }
        if (quizUrl != null) {
            openCustomTab(CustomTabsIntent.Builder().build(), Uri.parse(quizUrl))
        }

        // checkConnectivity Live
        mInterstitialAdHelper = InterstitialAdHelperClass(this@MainActivity)
        checkNetworkConnectionLive = CheckNetworkConnectionLive(application)
        checkNetworkConnectionLive.observe(this) { isConnected ->
            if (isConnected) {
                Log.d("CheckNetworkConnectionLive", "onCreate:  Connected to internet.")
                // mInterstitialAdHelper?.loadInterstitialAd(
                //     INTERSTITIAL_SONG_CLICK
                // )
            } else {
                Log.d("CheckNetworkConnectionLive", "onCreate:  No Internet Connection")
            }
        }

        InterstitialAdHelperClass.prevSeenAdsTime = ((System.currentTimeMillis() / 1000) - 25)


        MobileAds.initialize(this) {}

        setupNavigationController()
        if (!hasPermissions()) {
            Toast.makeText(
                this,
                "Please allow the required permission to continue.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController(R.id.fragment_container).navigate(R.id.permissionFragment)
        }

        //startCountdown()
    }

    private fun setupNavigationController() {
        val navController = findNavController(R.id.fragment_container)
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.main_graph)

        val categoryInfo: CategoryInfo = PreferenceUtil.libraryCategory.first { it.visible }
        if (categoryInfo.visible) {
            if (!navGraph.contains(PreferenceUtil.lastTab)) PreferenceUtil.lastTab =
                categoryInfo.category.id
            navGraph.setStartDestination(if (PreferenceUtil.rememberLastTab) {
                PreferenceUtil.lastTab.let {
                    if (it == 0) {
                        categoryInfo.category.id
                    } else {
                        it
                    }
                }
            } else categoryInfo.category.id)
        }
        navController.graph = navGraph
        bottomNavigationView.setupWithNavController(navController)
        // Scroll Fragment to top
        bottomNavigationView.setOnItemReselectedListener {
            currentFragment(R.id.fragment_container).apply {
                if (this is IScrollHelper) {
                    scrollToTop()
                }
            }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == navGraph.startDestinationId) {
                currentFragment(R.id.fragment_container)?.enterTransition = null
            }
            when (destination.id) {
                /*  R.id.action_home,*/ R.id.action_song, R.id.action_album, R.id.action_artist, R.id.action_folder, R.id.action_playlist -> {
                // Save the last tab
                if (PreferenceUtil.rememberLastTab) {
                    saveTab(destination.id)
                }
                // Show Bottom Navigation Bar
                setBottomNavVisibility(visible = true, animate = true)
            }
                R.id.playing_queue_fragment -> {
                    setBottomNavVisibility(visible = false, hideBottomSheet = true)
                }
                else -> setBottomNavVisibility(
                    visible = false, animate = true
                ) // Hide Bottom Navigation Bar
            }
        }
    }

    private fun saveTab(id: Int) {
        PreferenceUtil.lastTab = id
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.fragment_container).navigateUp()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val expand = intent?.extra<Boolean>(EXPAND_PANEL)?.value ?: false
        if (expand && PreferenceUtil.isExpandPanel) {
            fromNotification = true
            slidingPanel.bringToFront()
            expandPanel()
            intent?.removeExtra(EXPAND_PANEL)
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mInterstitialAdHelper != null) mInterstitialAdHelper = null
        /* if (mCountDownTimer != null) {
             mCountDownTimer?.cancel()
             mCountDownTimer = null
         }*/
        PreferenceUtil.unregisterOnSharedPreferenceChangedListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == GENERAL_THEME || key == MATERIAL_YOU || key == WALLPAPER_ACCENT || key == BLACK_THEME || key == ADAPTIVE_COLOR_APP ||/* key == USER_NAME ||*/
            key == TOGGLE_FULL_SCREEN || key == TOGGLE_VOLUME || key == ROUND_CORNERS || key == CAROUSEL_EFFECT || key == NOW_PLAYING_SCREEN_ID || key == TOGGLE_GENRE || key == BANNER_IMAGE_PATH || key == PROFILE_IMAGE_PATH || key == CIRCULAR_ALBUM_ART || key == KEEP_SCREEN_ON || key == TOGGLE_SEPARATE_LINE || key == TOGGLE_HOME_BANNER || key == TOGGLE_ADD_CONTROLS || key == ALBUM_COVER_STYLE || key == HOME_ARTIST_GRID_STYLE || key == ALBUM_COVER_TRANSFORM || key == DESATURATED_COLOR || key == EXTRA_SONG_INFO || key == TAB_TEXT_MODE || key == LANGUAGE_NAME || key == LIBRARY_CATEGORIES || key == CUSTOM_FONT || key == APPBAR_MODE || key == CIRCLE_PLAY_BUTTON
        ) {
            postRecreate()
        }
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        intent ?: return
        handlePlaybackIntent(intent)
    }

    private fun handlePlaybackIntent(intent: Intent) {
        lifecycleScope.launch(IO) {
            val uri: Uri? = intent.data
            val mimeType: String? = intent.type
            var handled = false
            if (intent.action != null && intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
                val songs: List<Song> = getSongs(intent.extras!!)
                if (MusicPlayerRemote.shuffleMode == MusicService.SHUFFLE_MODE_SHUFFLE) {
                    MusicPlayerRemote.openAndShuffleQueue(songs, true)
                } else {
                    MusicPlayerRemote.openQueue(songs, 0, true)
                }
                handled = true
            }
            if (uri != null && uri.toString().isNotEmpty()) {
                MusicPlayerRemote.playFromUri(uri)
                handled = true
            } else if (MediaStore.Audio.Playlists.CONTENT_TYPE == mimeType) {
                val id = parseLongFromIntent(intent, "playlistId", "playlist")
                if (id >= 0L) {
                    val position: Int = intent.getIntExtra("position", 0)
                    val songs: List<Song> = PlaylistSongsLoader.getPlaylistSongList(get(), id)
                    MusicPlayerRemote.openQueue(songs, position, true)
                    handled = true
                }
            } else if (MediaStore.Audio.Albums.CONTENT_TYPE == mimeType) {
                val id = parseLongFromIntent(intent, "albumId", "album")
                if (id >= 0L) {
                    val position: Int = intent.getIntExtra("position", 0)
                    val songs = libraryViewModel.albumById(id).songs
                    MusicPlayerRemote.openQueue(
                        songs, position, true
                    )
                    handled = true
                }
            } else if (MediaStore.Audio.Artists.CONTENT_TYPE == mimeType) {
                val id = parseLongFromIntent(intent, "artistId", "artist")
                if (id >= 0L) {
                    val position: Int = intent.getIntExtra("position", 0)
                    val songs: List<Song> = libraryViewModel.artistById(id).songs
                    MusicPlayerRemote.openQueue(
                        songs, position, true
                    )
                    handled = true
                }
            }
            if (handled) {
                setIntent(Intent())
            }
        }
    }

    private fun parseLongFromIntent(
        intent: Intent, longKey: String, stringKey: String
    ): Long {
        var id = intent.getLongExtra(longKey, -1)
        if (id < 0) {
            val idString = intent.getStringExtra(stringKey)
            if (idString != null) {
                try {
                    id = idString.toLong()
                } catch (e: NumberFormatException) {
                    println(e.message)
                }
            }
        }
        return id
    }

    private fun firebasAnalyticsLogEvents(
        paramsKey: String, paramsValue: String, logEventName: String
    ) {
        val params = Bundle()
        params.putString(
            paramsKey, paramsValue
        )
        analytics.logEvent(logEventName, params)
    }

    /* private fun startCountdown() {
         if (mCountDownTimer != null) {
             mCountDownTimer?.cancel()
         }

         mCountDownTimer = object : CountDownTimer(20 * 1000*//*30 sec*//*, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                var millis = 0L
                if (millisUntilFinished > 1000) {
                    millis = millisUntilFinished / 1000
                }
                Log.d(TAG, "onTick: $millis")
                isCountDownFinished = false
            }

            override fun onFinish() {
                isCountDownFinished = true
            }
        }

        //mCountDownTimer?.start()
    }*/
}



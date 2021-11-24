package com.knesarcreation.playbeat.activity

import android.annotation.SuppressLint
import android.content.*
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.database.CursorWindow
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.gms.cast.framework.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.ActivityBottomBarFragmentBinding
import com.knesarcreation.playbeat.fragment.*
import com.knesarcreation.playbeat.utils.*
import me.ibrahimsn.lib.OnItemSelectedListener
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


class ActivityBottomBarFragmentContainer : AppCompatActivity()/*, ServiceConnection*/,
    AllAlbumsFragment.OnAlbumItemClicked, AllArtistsFragment.OpenArtisFragment,
    ArtistsTracksAndAlbumFragment.OnArtistAlbumItemClicked,
    PlaylistsFragment.OnPlayListCategoryClicked,
    FoldersFragment.OnFolderItemOpened {

    private lateinit var binding: ActivityBottomBarFragmentBinding
    private var homeFragment = HomeFragment()
    private var playlistsFragment = PlaylistsFragment()
    private var settingFragment = SettingFragment()
    private var albumSongFragment = AlbumFragment()
    private var searchFragment = SearchFragment()
    private var favouriteAudiosFragment = FavouriteAudiosFragment()
    private var lastAddedAudioFragment = LastAddedAudioFragment()
    private var historyAudiosFragment = HistoryAudiosFragment()
    private var mostPlayedFragment = MostPlayedFragment()
    private var artistsTracksAndAlbumFragment = ArtistsTracksAndAlbumFragment()
    private var foldersSpecificSongsFrag = FoldersSpecificSongs()
    private var customPlaylist = CustomPlaylist()
    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var viewModel: DataObservableClass
    private var queueAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private var isAlbumFragOpened = false
    private var isArtistsFragOpened = false
    private var isPlaylstCategoryOpened = false
    private var isFolderFragOpened = false
    private var isAlbumOpenedFromArtisFrag = false
    private var isNotiBuild = false
    private var runnableAudioProgress: Runnable? = null
    private var newState: Int = 0
    private lateinit var nowPlayingBottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private var audioRunnable: Runnable? = null
    private var shuffledList = CopyOnWriteArrayList<AllSongsModel>()
    private var isShuffled = false
    private var isPlayPauseClicked = false

    //private var mCastSession: CastSession? = null
    //private var mSessionManagerListener: SessionManagerListener<CastSession>? = null
    // private var mCastContext: CastContext? = null
    private lateinit var storage: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass
    private var isContextMenuEnabled = false
    private var isOpenFromNoti = false
    private var queueListBottomSheet: BottomSheetAudioQueueList? = null
    private var appUpdateManager: AppUpdateManager? = null
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null

    //private var isFavAudioObserved = false
    //private var launchFavAudioJob: Job? = null
    private var _playlistCategory = ""
    private val RC_APP_UPDATE = 11


    @SuppressLint("DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomBarFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.isAccessible = true
            field.set(null, 100 * 1024 * 1024) //the 100MB is the new size
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        MakeStatusBarTransparent().transparent(this)
        storage = StorageUtil(this)

        getAppUpdate()
        //setupCastListener()
        // setting cast context
        //mCastContext = CastContext.getSharedInstance(this)
        //mCastSession = mCastContext!!.sessionManager.currentCastSession

        binding.bottomSheet.songNameTV.isSelected = true
        binding.bottomSheet.songTextTV.isSelected = true
        binding.bottomBar.onItemSelectedListener = onItemSelectedListener

        viewModel = this.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        }

        viewModel.isContextMenuEnabled.observe(this, {
            if (it != null) {
                isContextMenuEnabled = it
                nowPlayingBottomSheetBehavior.isDraggable = !it
            }
        })

        if (savedInstanceState == null) {
            addFragmentsToFragmentContainer()
        } else {
            // if activity recreates then instead of adding new fragment get the old instances of fragment and assign it to all the fragments
            // this helps to prevent creation of duplicates fragment
            getOldFragInstances()
        }

        setBottomBarFragmentState(FragmentState.HOME).commit()

        registerUpdatePlayerUI()
        controlAudio()
        handleSeekBarChangeListener()
        //handlePlayPauseNextPrev()
        setupNowPlayingBottomSheet()
        shuffledAudioList()
        openAudioQueueList()
        manageRepeatAudioList()
        openSleepTimerSheet()

        binding.bottomSheet.seekBar.setOnClickListener { }

        //set resume progress in audio progress bar and seek bar
        binding.bottomSheet.seekBar.max =
            (storage.getLastAudioMaxSeekProg().toDouble() / 1000).toInt()
        binding.bottomSheet.seekBar.progress =
            (storage.getAudioResumePos().toDouble() / 1000).toInt()

        binding.bottomSheet.startTimeTV.text =
            millisToMinutesAndSeconds(storage.getAudioResumePos())

        showSleepTimerIfEnable()

        likeOrUnLikeAudio()

        binding.bottomSheet.moreOptionIV.setOnClickListener {
            val loadQueueAudioList = storage.loadQueueAudio()
            val bottomSheetMoreOptions =
                BottomSheetAudioMoreOptions(
                    this,
                    loadQueueAudioList[audioIndexPos],
                    true
                )
            bottomSheetMoreOptions.show(supportFragmentManager, "bottomSheetMoreOptions")
        }

    }

    private fun getAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(this)

        installStateUpdatedListener =
            object : InstallStateUpdatedListener {
                override fun onStateUpdate(state: InstallState) {
                    if (state.installStatus() == InstallStatus.DOWNLOADED) {
                        //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                        popupSnackbarForCompleteUpdate()
                    } else if (state.installStatus() == InstallStatus.INSTALLED) {
                        if (appUpdateManager != null) {
                            appUpdateManager!!.unregisterListener(this)
                        }
                    } else {
                        Log.i(
                            "appUpdateManager",
                            "InstallStateUpdatedListener: state: " + state.installStatus()
                        )
                    }
                }
            }

        appUpdateManager!!.registerListener(installStateUpdatedListener!!)
        checkUpdate()
    }

    private fun checkUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager!!.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        Log.d("AppUpdate", "Checking for updates")
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                // Request the update.
                Log.d("AppUpdate", "Update available")
                // Toast.makeText(this, "Update available", Toast.LENGTH_SHORT).show()
                try {
                    appUpdateManager!!.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE /*AppUpdateType.IMMEDIATE*/,
                        this,
                        RC_APP_UPDATE
                    )
                } catch (e: SendIntentException) {
                    e.printStackTrace()
                }

            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                popupSnackbarForCompleteUpdate();
            } else {
                Log.d("AppUpdate", "No Update available")
                // Toast.makeText(this, "No update available", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(
            findViewById(R.id.coordinatorLayout_main),
            "New app is ready!",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Install") {
            if (appUpdateManager != null) {
                appUpdateManager!!.completeUpdate()
            }
        }
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.teal_200))
        snackbar.anchorView = binding.bottomBar
        snackbar.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e("InAppUpate", "onActivityResult: app download failed")
            }
        }
    }

    private fun addFragmentsToFragmentContainer() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, homeFragment, "homeFragment")
            .add(R.id.fragmentContainer, playlistsFragment, "playlistsFragment")
            .add(R.id.fragmentContainer, settingFragment, "settingFragment")
            .add(R.id.fragmentContainer, albumSongFragment, "albumSongFragment")
            .add(
                R.id.fragmentContainer,
                artistsTracksAndAlbumFragment,
                "artistsTracksAndAlbumFragment"
            )
            .add(R.id.fragmentContainer, searchFragment, "searchFragment")
            .add(R.id.fragmentContainer, favouriteAudiosFragment, "favouriteAudiosFragment")
            .add(R.id.fragmentContainer, lastAddedAudioFragment, "lastAddedAudioFragment")
            .add(R.id.fragmentContainer, historyAudiosFragment, "historyAudiosFragment")
            .add(R.id.fragmentContainer, mostPlayedFragment, "mostPlayedFragment")
            .add(R.id.fragmentContainer, customPlaylist, "customPlaylist")
            .add(R.id.fragmentContainer, foldersSpecificSongsFrag, "foldersSpecificSongs")
            .commit()
        Log.d("ActivitySavedInstanceState", "onCreate: null instance ")
    }

    private fun getOldFragInstances() {
        val oldHomeFragInstance =
            supportFragmentManager.findFragmentByTag("homeFragment") as HomeFragment
        val oldPlayListFragInstance =
            supportFragmentManager.findFragmentByTag("playlistsFragment") as PlaylistsFragment
        val oldSettingFragInstance =
            supportFragmentManager.findFragmentByTag("settingFragment") as SettingFragment
        val oldAlbumFragInstance =
            supportFragmentManager.findFragmentByTag("albumSongFragment") as AlbumFragment
        val oldArtistsTracksAndAlbumFragInstance =
            supportFragmentManager.findFragmentByTag("artistsTracksAndAlbumFragment") as ArtistsTracksAndAlbumFragment
        val oldSearchFragInstance =
            supportFragmentManager.findFragmentByTag("searchFragment") as SearchFragment
        val oldFavouriteAudiosFragInstance =
            supportFragmentManager.findFragmentByTag("favouriteAudiosFragment") as FavouriteAudiosFragment
        val oldLastAddedAudioFragInstance =
            supportFragmentManager.findFragmentByTag("lastAddedAudioFragment") as LastAddedAudioFragment
        val oldHistoryAudiosFragmentInstance =
            supportFragmentManager.findFragmentByTag("historyAudiosFragment") as HistoryAudiosFragment
        val oldMostPlayedFragmentInstance =
            supportFragmentManager.findFragmentByTag("mostPlayedFragment") as MostPlayedFragment
        val oldCustomPlaylistInstance =
            supportFragmentManager.findFragmentByTag("customPlaylist") as CustomPlaylist
        val foldersSpecificSongs =
            supportFragmentManager.findFragmentByTag("foldersSpecificSongs") as FoldersSpecificSongs

        homeFragment = oldHomeFragInstance
        playlistsFragment = oldPlayListFragInstance
        settingFragment = oldSettingFragInstance
        albumSongFragment = oldAlbumFragInstance
        artistsTracksAndAlbumFragment = oldArtistsTracksAndAlbumFragInstance
        searchFragment = oldSearchFragInstance
        favouriteAudiosFragment = oldFavouriteAudiosFragInstance
        lastAddedAudioFragment = oldLastAddedAudioFragInstance
        historyAudiosFragment = oldHistoryAudiosFragmentInstance
        mostPlayedFragment = oldMostPlayedFragmentInstance
        customPlaylist = oldCustomPlaylistInstance
        foldersSpecificSongsFrag = foldersSpecificSongs
    }

    private fun likeOrUnLikeAudio() {
        binding.bottomSheet.likedAudioIV.setOnClickListener {
            val isFav = !storage.loadQueueAudio()[audioIndexPos].isFavourite
            if (isFav) {
                binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_fill)
                showLikedAudioAnim()
            } else {
                binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_break)
                showLikedAudioAnim()
            }

            if (AllSongFragment.musicService?.mediaPlayer != null) {
                if (isFav) {
                    if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PLAYING,
                            PlaybackStatus.FAVOURITE,
                            1f
                        )
                    } else {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PAUSED,
                            PlaybackStatus.FAVOURITE,
                            0f
                        )
                    }
                } else {
                    if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PLAYING,
                            PlaybackStatus.UN_FAVOURITE,
                            1f
                        )
                    } else {
                        AllSongFragment.musicService?.buildNotification(
                            PlaybackStatus.PAUSED,
                            PlaybackStatus.UN_FAVOURITE,
                            0f
                        )
                    }
                }
            }
            addAudioToFavourites(isFav)
        }
    }

    private fun addAudioToFavourites(isFav: Boolean) {
        val favAudioAddedTime = System.currentTimeMillis()
        mViewModelClass.updateFavouriteAudio(
            isFav,
            queueAudioList[audioIndexPos].songId,
            favAudioAddedTime,
            lifecycleScope,
        )

        val list = CopyOnWriteArrayList<AllSongsModel>()
        for (audio in queueAudioList) {
            if (audio.songId == queueAudioList[audioIndexPos].songId) {
                val allSongsModel = AllSongsModel(
                    audio.songId,
                    audio.albumId,
                    audio.songName,
                    audio.artistsName,
                    audio.albumName,
                    audio.size,
                    audio.duration,
                    audio.data,
                    audio.contentUri,
                    audio.artUri,
                    audio.dateAdded,
                    isFav,
                    favAudioAddedTime,
                    audio.artistId,
                    audio.displayName,
                    audio.contentType,
                    audio.year,
                    audio.folderId,
                    audio.folderName,
                    audio.noOfSongs
                )
                allSongsModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
                allSongsModel.playingOrPause = audio.playingOrPause
                list.add(allSongsModel)
            } else {
                val allSongsModel = AllSongsModel(
                    audio.songId,
                    audio.albumId,
                    audio.songName,
                    audio.artistsName,
                    audio.albumName,
                    audio.size,
                    audio.duration,
                    audio.data,
                    audio.contentUri,
                    audio.artUri,
                    audio.dateAdded,
                    audio.isFavourite,
                    audio.favAudioAddedTime,
                    audio.artistId,
                    audio.displayName,
                    audio.contentType,
                    audio.year,
                    audio.folderId,
                    audio.folderName,
                    audio.noOfSongs
                )
                allSongsModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
                allSongsModel.playingOrPause = audio.playingOrPause
                list.add(allSongsModel)
            }
        }
        storage.storeQueueAudio(list)
    }

    private fun showSleepTimerIfEnable() {
        val sleepTime: Long = storage.getSleepTime()
        if (sleepTime != 0L) {
            AllSongFragment.musicService?.startSleepTimeCountDown(
                sleepTime,
                binding.bottomSheet.sleepTimeIV,
                binding.bottomSheet.sleepTimerTV
            )
        }
    }

    private fun openSleepTimerSheet() {
        binding.bottomSheet.rlSleepTimer.setOnClickListener {
            val bottomSheetSleepTimer = BottomSheetSleepTimer(
                this,
                binding.bottomSheet.sleepTimerTV,
                binding.bottomSheet.sleepTimeIV
            )
            bottomSheetSleepTimer.show(supportFragmentManager, "bottomSheetSleepTimer")
        }

    }

    private fun manageRepeatAudioList() {
        if (storage.getIsRepeatAudio()) {
            binding.bottomSheet.loopOneSong.setImageResource(R.drawable.repeat_one_on_24)
        } else {
            binding.bottomSheet.loopOneSong.setImageResource(R.drawable.ic_repeat_24)
        }

        binding.bottomSheet.loopOneSong.setOnClickListener {
            if (!storage.getIsRepeatAudio()) {
                //repeat current audio
                Snackbar.make(window.decorView, "Repeat current audio", Snackbar.LENGTH_LONG).show()
                binding.bottomSheet.loopOneSong.setImageResource(R.drawable.repeat_one_on_24)
                storage.saveIsRepeatAudio(true)
            } else {
                //repeat audio list
                Snackbar.make(window.decorView, "Repeat audio list", Snackbar.LENGTH_LONG).show()
                binding.bottomSheet.loopOneSong.setImageResource(R.drawable.ic_repeat_24)
                storage.saveIsRepeatAudio(false)
            }
        }
    }

    private fun openAudioQueueList() {
        binding.bottomSheet.queueAudioIV.setOnClickListener {
            queueListBottomSheet = BottomSheetAudioQueueList(this)
            queueListBottomSheet!!.show(supportFragmentManager, "queueListBottomSheet")

            queueListBottomSheet!!.listener =
                object : BottomSheetAudioQueueList.OnRepeatAudioListener {
                    override fun onRepeatIconClicked() {
                        //Toast.makeText(this@ActivityBottomBarFragmentContainer, "clicked", Toast.LENGTH_SHORT).show()
                        manageRepeatAudioList()
                    }
                }
        }
    }

    private fun setupNowPlayingBottomSheet() {
        // val screenInches = GetScreenSizes(this).getScreenInches()
        /* when {
             screenInches < 5.5 -> {
                 val rlCoverArtLayoutParams =
                     binding.bottomSheet.rlCoverArt.layoutParams as RelativeLayout.LayoutParams
                 rlCoverArtLayoutParams.height = ConvertDpToPx.dpToPx(300, this)
                 binding.bottomSheet.rlCoverArt.layoutParams = rlCoverArtLayoutParams
             }

             screenInches > 5.6 -> {
                 val rlCoverArtLayoutParams =
                     binding.bottomSheet.rlCoverArt.layoutParams as RelativeLayout.LayoutParams
                 rlCoverArtLayoutParams.height = ConvertDpToPx.dpToPx(400, this)
                 binding.bottomSheet.rlCoverArt.layoutParams = rlCoverArtLayoutParams
             }
         }*/


        nowPlayingBottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheet.nowPlayingBottomSheetContainer)
        newState = BottomSheetBehavior.STATE_COLLAPSED

        nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_DRAGGING
        nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        if (!storage.getIsAudioPlayedFirstTime() && queueAudioList.isNotEmpty()) {
            nowPlayingBottomSheetBehavior.peekHeight =
                ConvertDpToPx.dpToPx(115, this@ActivityBottomBarFragmentContainer)
            binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE

            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin =
                ConvertDpToPx.dpToPx(115, this@ActivityBottomBarFragmentContainer)
            binding.fragmentContainer.layoutParams = layoutParams
            nowPlayingBottomSheetBehavior.isDraggable = true
        } else {
            // app open first time
            nowPlayingBottomSheetBehavior.isDraggable = false
            binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.GONE
        }

        binding.bottomSheet.closeSheetIV.setOnClickListener {
            nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        binding.bottomSheet.rlMiniPlayerBottomsheet.setOnClickListener {
            if (!isContextMenuEnabled) {
                nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.GONE
            }
        }

        nowPlayingBottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                this@ActivityBottomBarFragmentContainer.newState = newState
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.bottomBar.visibility = View.GONE
                    binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.GONE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.bottomBar.visibility = View.VISIBLE
                    binding.bottomBar.alpha = 1F
                    binding.bottomSheet.rlNowPlayingBottomSheet.alpha = 0F
                    if (queueAudioList.isNotEmpty()) {
                        binding.bottomSheet.rlMiniPlayerBottomsheet.alpha = 1F
                        binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d("NowPlayingSheetSlide1", "onSlide: $slideOffset ")
                binding.bottomBar.alpha = (0.4 - slideOffset).toFloat()
                binding.bottomSheet.rlMiniPlayerBottomsheet.alpha =
                    (0.4 - slideOffset).toFloat()
                binding.bottomSheet.rlNowPlayingBottomSheet.alpha = slideOffset

                if (isPlaylstCategoryOpened || isAlbumFragOpened || isArtistsFragOpened || isAlbumOpenedFromArtisFrag || isFolderFragOpened) {
                    if (slideOffset >= 0.2f) {
                        binding.bottomSheet.nowPlayingBottomSheetContainer.animate()
                            .translationY(0f).duration = 200
                    } else {
                        binding.bottomSheet.nowPlayingBottomSheetContainer.animate()
                            .translationY(binding.bottomBar.height.toFloat() - 15f).duration =
                            200
                    }
                }
            }
        })

    }

    private fun updateMiniPlayer(audioIndex: Int) {
        binding.bottomSheet.songNameTV.text = queueAudioList[audioIndex].songName
        binding.bottomSheet.artistOrAlbumNameTV.text = queueAudioList[audioIndex].artistsName

        Glide.with(this).load(queueAudioList[audioIndexPos].artUri)
            .apply(
                RequestOptions.placeholderOf(R.drawable.music_note_icon).centerCrop()
            )
            .into(binding.bottomSheet.albumArtIV)

        if (AllSongFragment.musicService?.mediaPlayer != null) {
            if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                // highlight the pause audio and  pause anim
                mViewModelClass.updateSong(
                    queueAudioList[audioIndex].songId,
                    queueAudioList[audioIndex].songName,
                    1,
                    lifecycleScope
                )
                mViewModelClass.updateQueueAudio(
                    queueAudioList[audioIndex].songId,
                    queueAudioList[audioIndex].songName,
                    1,
                    lifecycleScope
                )
                binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_pause_audio)
            } else {
                // highlight the playing audio
                mViewModelClass.updateSong(
                    queueAudioList[audioIndex].songId,
                    queueAudioList[audioIndex].songName,
                    0,
                    lifecycleScope
                )
                mViewModelClass.updateQueueAudio(
                    queueAudioList[audioIndex].songId,
                    queueAudioList[audioIndex].songName,
                    0,
                    lifecycleScope
                )
                binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_play_audio)
            }
        }
    }

    private fun controlAudio() {
        binding.bottomSheet.skipNextAudioMP.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            queueAudioList = storage.loadQueueAudio()
            // remove previous audio highlight

            checkAudioExistWhenSkipNext(audioIndexPos)

        }

        binding.bottomSheet.playPauseMiniPlayer.setOnClickListener {
            if (AllSongFragment.musicService != null) {
                isPlayPauseClicked = true
                updateMPAndEPPlayPauseBtn()
            }
        }

        //expandedPlayingDialog controllers
        binding.bottomSheet.playPauseExpandedPlayer.setOnClickListener {
            //handleStartTimeAndSeekBar()
            isPlayPauseClicked = true
            updateMPAndEPPlayPauseBtn()
        }

        binding.bottomSheet.skipNextAudioEP.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            queueAudioList = storage.loadQueueAudio()
            // remove previous audio highlight
            /* mViewModelClass.updateSong(
                 queueAudioList[audioIndexPos].songId,
                 queueAudioList[audioIndexPos].songName,
                 -1, // default
                 lifecycleScope
             )
             mViewModelClass.updateQueueAudio(
                 queueAudioList[audioIndexPos].songId,
                 queueAudioList[audioIndexPos].songName,
                 -1, // default
                 lifecycleScope
             )

             incrementPosByOne(audioIndexPos)
             storage.storeAudioIndex(audioIndexPos)
             AllSongFragment.musicService?.pausedByManually = false
             val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
             sendBroadcast(broadcastIntent)*/
            checkAudioExistWhenSkipNext(audioIndexPos)

            binding.bottomSheet.skipNextAudioEP.setImageResource(R.drawable.avd_music_next)
            val animatedVectorDrawable =
                binding.bottomSheet.skipNextAudioEP.drawable as AnimatedVectorDrawable
            animatedVectorDrawable.start()
            binding.bottomSheet.playPauseExpandedPlayer.isSelected = true
        }

        binding.bottomSheet.skipPrevAudioEP.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            queueAudioList = storage.loadQueueAudio()
            // remove previous audio highlight

            checkAudioExistWhenSkipPrev(audioIndexPos)

        }
    }

    private fun checkAudioExistWhenSkipNext(prevAudioIndex: Int)/*: Boolean*/ {

        Log.d("checkAudioExistWhenSkipNext", "checkAudioExistWhenSkipNext:$prevAudioIndex ")
        val newAudioIndexPos = if (prevAudioIndex != queueAudioList.size - 1) {
            prevAudioIndex + 1
        } else {
            0
        }
        //val newAudioIndexPos = prevAudioIndex + 1
        val audioPath = queueAudioList[newAudioIndexPos].data

        Log.d("CheckFileExist", "checkAudioExist: $newAudioIndexPos  ,  $audioPath")

        if (File(Uri.parse(audioPath).path!!).exists()) {
            Log.d("CheckFileExist", "checkAudioExist: exist ")
            var currentPlayingAudioIndex = storage.loadAudioIndex()
            if (currentPlayingAudioIndex == -1) {
                currentPlayingAudioIndex = 0
                storage.storeAudioIndex(currentPlayingAudioIndex)
            }
            mViewModelClass.updateSong(
                queueAudioList[currentPlayingAudioIndex].songId,
                queueAudioList[currentPlayingAudioIndex].songName,
                -1, // default
                lifecycleScope
            )

            mViewModelClass.updateQueueAudio(
                queueAudioList[currentPlayingAudioIndex].songId,
                queueAudioList[currentPlayingAudioIndex].songName,
                -1, // default
                lifecycleScope
            )

            incrementPosByOne(prevAudioIndex)
            storage.storeAudioIndex(audioIndexPos)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            updateMiniPlayer(audioIndexPos)
        } else {
            Snackbar.make(
                window!!.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
            if (audioIndexPos == queueAudioList.size - 1) {
                audioIndexPos = 0
            }

            checkAudioExistWhenSkipNext(audioIndexPos++)

        }
    }

    private fun checkAudioExistWhenSkipPrev(prevAudioIndex: Int) {
        val newAudioIndexPos: Int = if (prevAudioIndex != 0) {
            prevAudioIndex - 1
        } else {
            queueAudioList.size - 1
        }

        val audioPath = queueAudioList[newAudioIndexPos].data

        Log.d("CheckFileExist", "checkAudioExist: $newAudioIndexPos  ,  $audioPath")

        if (File(Uri.parse(audioPath).path!!).exists()) {
            val currentPlayingAudioIndex = storage.loadAudioIndex()
            mViewModelClass.updateSong(
                queueAudioList[currentPlayingAudioIndex].songId,
                queueAudioList[currentPlayingAudioIndex].songName,
                -1, // default
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                queueAudioList[currentPlayingAudioIndex].songId,
                queueAudioList[currentPlayingAudioIndex].songName,
                -1, // default
                lifecycleScope
            )

            decrementPosByOne(prevAudioIndex)
            storage.storeAudioIndex(audioIndexPos)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            //updateViews(audioIndexPos)
            //updateAudioController() // seek bar, start time, end time, play pause btn
            binding.bottomSheet.skipPrevAudioEP.setImageResource(R.drawable.avd_music_previous)
            val animatedVectorDrawable =
                binding.bottomSheet.skipPrevAudioEP.drawable as AnimatedVectorDrawable
            animatedVectorDrawable.start()
            binding.bottomSheet.playPauseExpandedPlayer.isSelected = true
        } else {
            Snackbar.make(
                window!!.decorView,
                "File doesn't exists", Snackbar.LENGTH_LONG
            ).show()
            checkAudioExistWhenSkipPrev(audioIndexPos--)
        }

    }

    private fun updateMPAndEPPlayPauseBtn() {
        if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
            // pause through button
            AllSongFragment.musicService?.pauseMedia()
            AllSongFragment.musicService?.pausedByManually = true
            AllSongFragment.musicService?.updateNotification(isAudioPlaying = false)
            binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_play_audio)
            // highlight pause audio with pause anim
            mViewModelClass.updateSong(
                queueAudioList[audioIndexPos].songId,
                queueAudioList[audioIndexPos].songName,
                0,
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                queueAudioList[audioIndexPos].songId,
                queueAudioList[audioIndexPos].songName,
                0,
                lifecycleScope
            )
        } else if (!AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
            // resume through button
            if (audioIndexPos == -1) {
                audioIndexPos = 0
                storage.storeAudioIndex(audioIndexPos)
            }
            AllSongFragment.musicService?.resumeMedia()
            AllSongFragment.musicService?.pausedByManually = false
            binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_pause_audio)
            AllSongFragment.musicService?.updateMetaData()
            AllSongFragment.musicService?.updateNotification(true)
            // calling two times for the first time
            if (!isNotiBuild) {
                isNotiBuild = true
                AllSongFragment.musicService?.updateNotification(true)
            }
            // highlight the playing audio
            mViewModelClass.updateSong(
                queueAudioList[audioIndexPos].songId,
                queueAudioList[audioIndexPos].songName,
                1,
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                queueAudioList[audioIndexPos].songId,
                queueAudioList[audioIndexPos].songName,
                1,
                lifecycleScope
            )
        }

        if (AllSongFragment.musicService?.mediaPlayer != null) {
            binding.bottomSheet.playPauseExpandedPlayer.isSelected =
                AllSongFragment.musicService?.mediaPlayer?.isPlaying!!
        }
        // update progress bar
        //binding.bottomSheet.audioProgress.max = audioList[audioIndexPos].duration
        // AllSongFragment.musicService?.mediaPlayer?.duration!!
        handleAudioProgressBar()
        handleStartTimeAndSeekBar()

    }

    private fun handleAudioProgressBar() {
        if (runnableAudioProgress != null) {
            handler.removeCallbacks(runnableAudioProgress!!)
        }
        runnableAudioProgress = object : Runnable {
            override fun run() {
                if (AllSongFragment.musicService != null) {
                    try {
                        if (AllSongFragment.musicService?.mediaPlayer != null) {
                            binding.bottomSheet.audioProgress.progress =
                                AllSongFragment.musicService?.mediaPlayer?.currentPosition!!
                            handler.postDelayed(this, 1000)
                        }
                    } catch (e: Exception) {
                        handler.removeCallbacks(runnableAudioProgress!!)
                    }

                }
            }

        }
        runOnUiThread(runnableAudioProgress)
    }

    private fun setBottomBarFragmentState(state: FragmentState): FragmentTransaction {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val transaction = supportFragmentManager.beginTransaction()
        //transaction.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
        when (state) {
            FragmentState.HOME -> {
                when {
                    isAlbumFragOpened -> {
                        transaction.hide(homeFragment)
                        transaction.hide(playlistsFragment)
                        transaction.hide(settingFragment)
                        transaction.hide(artistsTracksAndAlbumFragment)
                        transaction.hide(searchFragment)
                        transaction.hide(favouriteAudiosFragment)
                        transaction.hide(lastAddedAudioFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.hide(mostPlayedFragment)
                        transaction.hide(customPlaylist)
                        transaction.show(albumSongFragment)
                        transaction.hide(foldersSpecificSongsFrag)
                    }
                    isArtistsFragOpened || isAlbumOpenedFromArtisFrag -> {
                        transaction.hide(homeFragment)
                        transaction.hide(playlistsFragment)
                        transaction.hide(settingFragment)
                        transaction.show(artistsTracksAndAlbumFragment)
                        transaction.hide(albumSongFragment)
                        transaction.hide(searchFragment)
                        transaction.hide(favouriteAudiosFragment)
                        transaction.hide(lastAddedAudioFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.hide(customPlaylist)
                        transaction.hide(mostPlayedFragment)
                        transaction.hide(foldersSpecificSongsFrag)
                    }
                    else -> {
                        transaction.show(homeFragment)
                        transaction.hide(playlistsFragment)
                        transaction.hide(settingFragment)
                        transaction.hide(albumSongFragment)
                        transaction.hide(artistsTracksAndAlbumFragment)
                        transaction.hide(searchFragment)
                        transaction.hide(favouriteAudiosFragment)
                        transaction.hide(lastAddedAudioFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.hide(customPlaylist)
                        transaction.hide(mostPlayedFragment)
                        transaction.hide(foldersSpecificSongsFrag)
                    }
                }
                binding.bottomBar.itemActiveIndex = 0
            }
            FragmentState.PLAYLIST -> {
                transaction.hide(homeFragment)
                transaction.show(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                transaction.hide(searchFragment)
                transaction.hide(favouriteAudiosFragment)
                transaction.hide(lastAddedAudioFragment)
                transaction.hide(historyAudiosFragment)
                transaction.hide(mostPlayedFragment)
                transaction.hide(customPlaylist)
                transaction.hide(foldersSpecificSongsFrag)
                binding.bottomBar.itemActiveIndex = 1
            }
            FragmentState.SEARCH -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                transaction.show(searchFragment)
                transaction.hide(favouriteAudiosFragment)
                transaction.hide(lastAddedAudioFragment)
                transaction.hide(historyAudiosFragment)
                transaction.hide(mostPlayedFragment)
                transaction.hide(customPlaylist)
                transaction.hide(foldersSpecificSongsFrag)
                binding.bottomBar.itemActiveIndex = 2
            }
            FragmentState.SETTING -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.show(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                transaction.hide(searchFragment)
                transaction.hide(favouriteAudiosFragment)
                transaction.hide(lastAddedAudioFragment)
                transaction.hide(historyAudiosFragment)
                transaction.hide(mostPlayedFragment)
                transaction.hide(customPlaylist)
                transaction.hide(foldersSpecificSongsFrag)
                binding.bottomBar.itemActiveIndex = 3
            }
            FragmentState.ALBUM_FRAGMENT -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.show(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                transaction.hide(searchFragment)
                transaction.hide(favouriteAudiosFragment)
                transaction.hide(lastAddedAudioFragment)
                transaction.hide(historyAudiosFragment)
                transaction.hide(customPlaylist)
                transaction.hide(mostPlayedFragment)
                transaction.hide(foldersSpecificSongsFrag)

                isAlbumFragOpened = !isAlbumOpenedFromArtisFrag
                isArtistsFragOpened = false
            }
            FragmentState.ARTIST_TRACK_ALBUM_FRAGMENT -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.show(artistsTracksAndAlbumFragment)
                transaction.hide(searchFragment)
                transaction.hide(favouriteAudiosFragment)
                transaction.hide(lastAddedAudioFragment)
                transaction.hide(historyAudiosFragment)
                transaction.hide(mostPlayedFragment)
                transaction.hide(customPlaylist)
                transaction.hide(foldersSpecificSongsFrag)

                isArtistsFragOpened = true
                isAlbumFragOpened = false
            }
            FragmentState.PLAYLIST_CATEGORY -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                transaction.hide(searchFragment)
                transaction.hide(foldersSpecificSongsFrag)
                when (_playlistCategory) {
                    "fav" -> {
                        transaction.hide(lastAddedAudioFragment)
                        transaction.show(favouriteAudiosFragment)
                        transaction.hide(mostPlayedFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.hide(customPlaylist)
                    }
                    "lastAdded" -> {
                        transaction.hide(favouriteAudiosFragment)
                        transaction.show(lastAddedAudioFragment)
                        transaction.hide(mostPlayedFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.hide(customPlaylist)
                    }
                    "history" -> {
                        transaction.hide(favouriteAudiosFragment)
                        transaction.hide(lastAddedAudioFragment)
                        transaction.hide(mostPlayedFragment)
                        transaction.hide(customPlaylist)
                        transaction.show(historyAudiosFragment)
                    }
                    "mostPlayed" -> {
                        transaction.hide(favouriteAudiosFragment)
                        transaction.hide(lastAddedAudioFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.hide(customPlaylist)
                        transaction.show(mostPlayedFragment)
                    }
                    "customPlaylist" -> {
                        transaction.hide(favouriteAudiosFragment)
                        transaction.hide(lastAddedAudioFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.show(customPlaylist)
                        transaction.hide(mostPlayedFragment)
                    }
                }
                isPlaylstCategoryOpened = true
            }
            FragmentState.FOLDER_SPECIFIC_AUDIO -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                transaction.hide(searchFragment)
                transaction.hide(favouriteAudiosFragment)
                transaction.hide(lastAddedAudioFragment)
                transaction.hide(historyAudiosFragment)
                transaction.hide(customPlaylist)
                transaction.hide(mostPlayedFragment)
                transaction.show(foldersSpecificSongsFrag)
                isFolderFragOpened = true
            }
        }
        return transaction
    }

    private val onItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelect(pos: Int): Boolean {
            when (pos) {
                0 -> {
                    if (!isContextMenuEnabled) {
                        setBottomBarFragmentState(FragmentState.HOME).commit()
                        return true
                    } else {
                        binding.bottomBar.itemActiveIndex = 0
                    }
                }
                1 -> {
                    if (!isContextMenuEnabled) {
                        setBottomBarFragmentState(FragmentState.PLAYLIST).commit()
                        return true
                    } else {
                        binding.bottomBar.itemActiveIndex = 0
                    }
                }
                2 -> {
                    if (!isContextMenuEnabled) {
                        setBottomBarFragmentState(FragmentState.SEARCH).commit()
                        return true
                    } else {
                        binding.bottomBar.itemActiveIndex = 0
                    }
                }
                3 -> {
                    if (!isContextMenuEnabled) {

                        setBottomBarFragmentState(FragmentState.SETTING).commit()
                        return true
                    } else {
                        binding.bottomBar.itemActiveIndex = 0
                    }
                }
            }
            return false
        }
    }

    internal enum class FragmentState {
        HOME,
        PLAYLIST,
        SETTING,
        ALBUM_FRAGMENT,
        ARTIST_TRACK_ALBUM_FRAGMENT,
        SEARCH,
        PLAYLIST_CATEGORY,
        FOLDER_SPECIFIC_AUDIO,
    }

    override fun onBackPressed() {
        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
            if (supportFragmentManager.backStackEntryCount > 0 || !homeFragment.isHidden) {
                if (isContextMenuEnabled) {
                    viewModel.onBackPressed.value = true
                } else {
                    super.onBackPressed()
                }
            } else {
                if (isAlbumFragOpened) {
                    if (!playlistsFragment.isHidden || !settingFragment.isHidden) {
                        setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
                        binding.bottomBar.itemActiveIndex = 0
                    } else {
                        if (isContextMenuEnabled) {
                            viewModel.onBackPressed.value = true
                        } else {
                            animateBottomBarToVisible()
                            isAlbumFragOpened = false
                            setBottomBarFragmentState(FragmentState.HOME).commit()
                            binding.bottomBar.itemActiveIndex = 0
                        }
                    }
                } else if (isFolderFragOpened) {
                    /* if (!playlistsFragment.isHidden || !settingFragment.isHidden) {
                         setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
                         binding.bottomBar.itemActiveIndex = 0
                     } else {*/
                    if (isContextMenuEnabled) {
                        viewModel.onBackPressed.value = true
                    } else {
                        animateBottomBarToVisible()
                        isFolderFragOpened = false
                        setBottomBarFragmentState(FragmentState.HOME).commit()
                        binding.bottomBar.itemActiveIndex = 0
                    }
                    //}
                } else if (isArtistsFragOpened || isAlbumOpenedFromArtisFrag) {
                    if (!playlistsFragment.isHidden /* if open*/ || !settingFragment.isHidden /*if open*/) {
                        setBottomBarFragmentState(FragmentState.ARTIST_TRACK_ALBUM_FRAGMENT).commit()
                        binding.bottomBar.itemActiveIndex = 0
                    } else {
                        if (isContextMenuEnabled) {
                            viewModel.onBackPressed.value = true
                        } else {
                            if (isArtistsFragOpened)
                                animateBottomBarToVisible()

                            isArtistsFragOpened =
                                isAlbumOpenedFromArtisFrag
                            // if  isAlbumOpenedFromArtisFrag is open then
                            // this value will be assign to this -> isArtistsFragOpened while on back pressed

                            setBottomBarFragmentState(FragmentState.HOME).commit()
                            binding.bottomBar.itemActiveIndex = 0
                            isAlbumOpenedFromArtisFrag = false
                        }

                    }
                } else if (isPlaylstCategoryOpened) {
                    if (!playlistsFragment.isHidden /* if open*/ || !settingFragment.isHidden /*if open*/) {

                        setBottomBarFragmentState(FragmentState.PLAYLIST_CATEGORY).commit()
                        binding.bottomBar.itemActiveIndex = 1

                    } else {
                        if (isContextMenuEnabled) {
                            viewModel.onBackPressed.value = true
                        } else {
                            animateBottomBarToVisible()

                            isPlaylstCategoryOpened = false
                            setBottomBarFragmentState(FragmentState.PLAYLIST).commit()
                            binding.bottomBar.itemActiveIndex = 1
                        }

                        //isAlbumOpenedFromArtisFrag = false
                    }
                } else {
                    setBottomBarFragmentState(FragmentState.HOME).commit()
                    binding.bottomBar.itemActiveIndex = 0
                }

            }
        } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
            nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //handler.removeCallbacks(runnableAudioProgress!!)
        isDestroyedActivity = true
    }

    override fun openAlbum(album: String) {
        viewModel.albumData.value = album
        animateBottomBarToGone()
        setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
    }

    override fun folderOpen(audioFiles: String) {
        viewModel.folderData.value = audioFiles
        animateBottomBarToGone()
        setBottomBarFragmentState(FragmentState.FOLDER_SPECIFIC_AUDIO).commit()
    }

    override fun onOpenArtistTrackAndAlbumFragment(artistsData: String) {
        viewModel.artistsData.value = artistsData
        animateBottomBarToGone()
        setBottomBarFragmentState(FragmentState.ARTIST_TRACK_ALBUM_FRAGMENT).commit()
    }

    override fun openAlbumFromArtistFrag(album: String) {
        viewModel.albumData.value = album
        isAlbumOpenedFromArtisFrag = true

        // animateBottomBarToGone()

        setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
    }

    override fun playlistCategory(category: String, mode: Int) {
        _playlistCategory = if (mode == 1) {
            viewModel.customPlaylistData.value = category
            "customPlaylist"
        } else {
            viewModel.playlistCategory.value = category
            category
        }

        animateBottomBarToGone()

        setBottomBarFragmentState(FragmentState.PLAYLIST_CATEGORY).commit()
    }

    private fun animateMiniPlayerToGone() {
        binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.GONE
        val bottomMarginOfFrag =
            if (isPlaylstCategoryOpened || isAlbumFragOpened || isArtistsFragOpened || isAlbumOpenedFromArtisFrag || isFolderFragOpened) {
                if (binding.bottomSheet.rlMiniPlayerBottomsheet.isVisible) {
                    60
                } else {
                    0
                }
                // 65
            } else {
                if (binding.bottomSheet.rlMiniPlayerBottomsheet.isVisible) {
                    115
                } else {
                    55
                }
            }

        val layoutParams =
            binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.bottomMargin =
            ConvertDpToPx.dpToPx(bottomMarginOfFrag, this@ActivityBottomBarFragmentContainer)
        binding.fragmentContainer.layoutParams = layoutParams

        binding.bottomSheet.rlMiniPlayerBottomsheet.animate()
            .translationY(binding.bottomBar.height.toFloat())
            .alpha(0.0f).duration = 200

        nowPlayingBottomSheetBehavior.peekHeight =
            ConvertDpToPx.dpToPx(
                0,
                this@ActivityBottomBarFragmentContainer
            )

    }

    private fun animateMiniPlayerToVisible() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.bottomSheet.rlMiniPlayerBottomsheet.animate()
                .translationY(0f)
                .alpha(1f).duration = 200

            binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE

            nowPlayingBottomSheetBehavior.peekHeight =
                ConvertDpToPx.dpToPx(
                    115,
                    this@ActivityBottomBarFragmentContainer
                )

            val bottomMarginOfFrag =
                if (isPlaylstCategoryOpened || isAlbumFragOpened || isArtistsFragOpened || isAlbumOpenedFromArtisFrag || isFolderFragOpened) {
                    if (binding.bottomSheet.rlMiniPlayerBottomsheet.isVisible) {
                        60
                    } else {
                        0
                    }
                } else {
                    if (binding.bottomSheet.rlMiniPlayerBottomsheet.isVisible) {
                        115
                    } else {
                        55
                    }
                }

            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin =
                ConvertDpToPx.dpToPx(bottomMarginOfFrag, this@ActivityBottomBarFragmentContainer)
            binding.fragmentContainer.layoutParams = layoutParams
        }, 500)

    }

    private fun animateBottomBarToGone() {
        binding.bottomBar.animate()
            .translationY(binding.bottomBar.height.toFloat())
            .alpha(0.0f).duration = 200

        binding.bottomSheet.nowPlayingBottomSheetContainer.animate()
            .translationY(binding.bottomBar.height.toFloat() - 15f).duration = 200

        if (binding.bottomSheet.rlMiniPlayerBottomsheet.isVisible) {
            //Toast.makeText(applicationContext, "visible", Toast.LENGTH_SHORT).show()
            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin =
                ConvertDpToPx.dpToPx(50, this@ActivityBottomBarFragmentContainer)
            binding.fragmentContainer.layoutParams = layoutParams
        } else {
            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin =
                ConvertDpToPx.dpToPx(0, this@ActivityBottomBarFragmentContainer)
            binding.fragmentContainer.layoutParams = layoutParams
        }
    }

    private fun animateBottomBarToVisible() {
        binding.bottomBar.animate()
            .translationY(0f)
            .alpha(1f).duration = 200

        binding.bottomSheet.nowPlayingBottomSheetContainer.animate()
            .translationY(0f).duration = 200

        if (binding.bottomSheet.rlMiniPlayerBottomsheet.isVisible) {
            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin =
                ConvertDpToPx.dpToPx(115, this@ActivityBottomBarFragmentContainer)
            binding.fragmentContainer.layoutParams = layoutParams
        } else {
            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin =
                ConvertDpToPx.dpToPx(55, this@ActivityBottomBarFragmentContainer)
            binding.fragmentContainer.layoutParams = layoutParams
        }
    }

    /** Managing Now playing bottom sheet........................................................*/
    private fun updateExpandedPlayerViews(audioIndexPos: Int) {
        if (audioIndexPos != -1) {
            val allSongsModel = queueAudioList[audioIndexPos]
            binding.bottomSheet.songTextTV.text = allSongsModel.songName
            binding.bottomSheet.artistsNameTV.text = allSongsModel.artistsName
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

            val albumArt = UriToBitmapConverter.getBitmap(
                contentResolver!!,
                allSongsModel.artUri.toUri()
            )

            //setting player background
            if (albumArt != null) {
                binding.bottomSheet.nowPlayingNoAlbumArt.visibility = View.GONE
                binding.bottomSheet.nowPlayingAlbumArtIV.visibility = View.VISIBLE

                Glide.with(this).load(allSongsModel.artUri).transition(
                    withCrossFade(
                        factory
                    )
                )
                    .into(binding.bottomSheet.nowPlayingAlbumArtIV)
                Palette.from(albumArt).generate {
                    val bottomSwatch = it?.darkMutedSwatch
                    val topSwatch = it?.darkMutedSwatch

                    if (bottomSwatch != null && topSwatch != null) {
                        binding.bottomSheet.albumArtBottomGradient.setBackgroundResource(R.drawable.gradient_background_bottom_shadow_default)
                        binding.bottomSheet.bottomBackground.setBackgroundResource(R.drawable.app_theme_default_background)
                        //binding.bottomSheet.topBackground.setBackgroundResource(R.drawable.app_theme_default_background)
                        //binding.bottomSheet.albumArtTopGradient.setBackgroundResource(R.drawable.gradient_background_top_shadow)

                        val gradientDrawableBottomTop = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(bottomSwatch.rgb, 0x00000000)
                        )

                        /*val gradientDrawableTopBottom = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(topSwatch.rgb, 0x00000000)
                        )*/

                        Glide.with(this).load(gradientDrawableBottomTop)
                            .transition(withCrossFade(factory))
                            .into(binding.bottomSheet.albumArtBottomGradient)

                        /*Glide.with(this).load(gradientDrawableTopBottom)
                            .transition(withCrossFade(factory))
                            .into(binding.bottomSheet.albumArtTopGradient)*/

                        //Glide.with(this).load(gradientDrawableTopBottom)
                        //   .transition(withCrossFade(factory))
                        //  .into(binding.bottomSheet.albumArtTopGradient)

                        // parent bottom background
                        val gradientDrawableParentBottom = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(bottomSwatch.rgb, bottomSwatch.rgb)
                        )

                        // parent top background
                        // val gradientDrawableParentTop = GradientDrawable(
                        //   GradientDrawable.Orientation.TOP_BOTTOM,
                        //     intArrayOf(topSwatch.rgb, topSwatch.rgb)
                        // )

                        Glide.with(this).load(gradientDrawableParentBottom)
                            .transition(withCrossFade(factory))
                            .into(binding.bottomSheet.bottomBackground)

                        /*Glide.with(this).load(gradientDrawableParentTop)
                            .transition(withCrossFade(factory))
                            .into(binding.bottomSheet.topBackground)*/
//                        binding.bottomSheet.bottomBackground.background = gradientDrawableParent

                    } else {
                        defaultBackgroundOfExpandedPlayer()
                    }

                }

            } else {
                binding.bottomSheet.nowPlayingNoAlbumArt.visibility = View.VISIBLE
                binding.bottomSheet.nowPlayingAlbumArtIV.visibility = View.GONE

                // val originalBitmap = BitmapFactory.decodeResource(
                //     resources,
                //     R.drawable.music_note_icon
                // )

                defaultBackgroundOfExpandedPlayer()

//                Palette.from(originalBitmap).generate {
//                    val swatch = it?.dominantSwatch
//                    if (swatch != null) {
//                        binding.bottomSheet.albumArtBottomGradient.setBackgroundResource(R.drawable.gradient_background_bottom_shadow)
//                        binding.bottomSheet.bottomBackground.setBackgroundResource(R.drawable.app_theme_background_drawable)
//                        binding.bottomSheet.albumArtTopGradient.setBackgroundResource(R.drawable.gradient_background_top_shadow)
//
//                        val gradientDrawableBottomTop = GradientDrawable(
//                            GradientDrawable.Orientation.BOTTOM_TOP,
//                            intArrayOf(swatch.rgb, 0x00000000)
//                        )
//                        val gradientDrawableTopBottom = GradientDrawable(
//                            GradientDrawable.Orientation.TOP_BOTTOM,
//                            intArrayOf(swatch.rgb, swatch.population)
//                        )
//                        binding.bottomSheet.albumArtBottomGradient.background =
//                            gradientDrawableBottomTop
//                        binding.bottomSheet.albumArtTopGradient.background =
//                            gradientDrawableTopBottom
//
//                         parent background
//                        val gradientDrawableParent = GradientDrawable(
//                            GradientDrawable.Orientation.BOTTOM_TOP,
//                            intArrayOf(swatch.rgb, swatch.rgb)
//                        )
//                        binding.bottomSheet.bottomBackground.background = gradientDrawableParent
//                    }
//                }
            }
        }
    }

    private fun defaultBackgroundOfExpandedPlayer() {
        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

        Glide.with(this).load(R.drawable.music_note_icon)
            .transition(withCrossFade(factory))
            .into(binding.bottomSheet.nowPlayingNoAlbumArt)

        Glide.with(this).load(R.drawable.purple_background_bottom_gradient)
            .transition(withCrossFade(factory))
            .into(binding.bottomSheet.albumArtBottomGradient)

        Glide.with(this).load(R.drawable.purple_solid_background)
            .transition(withCrossFade(factory))
            .into(binding.bottomSheet.bottomBackground)

        /*  Glide.with(this).load(R.drawable.purple_background_top_gradient)
              .transition(withCrossFade(factory))
              .into(binding.bottomSheet.albumArtTopGradient)

          Glide.with(this).load(R.drawable.purple_solid_background)
              .transition(withCrossFade(factory))
              .into(binding.bottomSheet.topBackground)*/
    }

    private fun updateAudioController() {
        binding.bottomSheet.seekBar.max =
            (queueAudioList[audioIndexPos].duration.toDouble() / 1000).toInt()
        val audioDuration =
            millisToMinutesAndSeconds(queueAudioList[audioIndexPos].duration)
        binding.bottomSheet.endTimeTV.text = audioDuration

        handleStartTimeAndSeekBar()

        // change state of play pause
        if (AllSongFragment.musicService != null) {
            if (AllSongFragment.musicService?.mediaPlayer != null) {
                binding.bottomSheet.playPauseExpandedPlayer.isSelected =
                    AllSongFragment.musicService?.mediaPlayer?.isPlaying!!
            }
        }
    }

    private fun handleStartTimeAndSeekBar() {
        if (audioRunnable != null) {
            handler.removeCallbacks(audioRunnable!!)
        }
        audioRunnable = object : Runnable {
            override fun run() {
                if (AllSongFragment.musicService != null) {
                    if (AllSongFragment.musicService?.mediaPlayer != null) {
                        try {
                            binding.bottomSheet.startTimeTV.text =
                                millisToMinutesAndSeconds(AllSongFragment.musicService?.mediaPlayer?.currentPosition!!)
                            /* ObjectAnimator.ofInt(
                                 binding.bottomSheet.seekBar,
                                 "progress",
                                 (AllSongFragment.musicService?.mediaPlayer?.currentPosition!!.toDouble() / 1000).toInt()
                             ).setDuration(300).start()*/
                            binding.bottomSheet.seekBar.progress =
                                (AllSongFragment.musicService?.mediaPlayer?.currentPosition!!.toDouble() / 1000).toInt()
                            handler.postDelayed(this, 1000)
                        } catch (e: Exception) {
                            handler.removeCallbacks(audioRunnable!!)
                        }

                    }
                }
            }
        }
        runOnUiThread(audioRunnable)
    }

    private fun handleSeekBarChangeListener() {
        binding.bottomSheet.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    /*ObjectAnimator.ofInt(binding.bottomSheet.seekBar, "progress", progress)
                        .setDuration(300).start()*/
                    binding.bottomSheet.seekBar.progress = progress
                    binding.bottomSheet.startTimeTV.text =
                        millisToMinutesAndSeconds(progress * 1000)
                    if (audioRunnable != null)
                        handler.removeCallbacks(audioRunnable!!)
                }
            }

            override fun onStartTrackingTouch(seekbar: SeekBar?) {}

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                if (audioRunnable != null) {
                    if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!)
                        handler.postDelayed(audioRunnable!!, 1000)
                    AllSongFragment.musicService?.onSeekTo((seekbar?.progress!! * 1000).toLong())
                }
            }
        })
    }

    private fun millisToMinutesAndSeconds(millis: Int): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds "
    }

    private fun incrementPosByOne(newAudioIndexPos: Int) {
        audioIndexPos = newAudioIndexPos/* storage.loadAudioIndex()*/
        if (audioIndexPos != queueAudioList.size - 1) {
            ++audioIndexPos
        } else {
            audioIndexPos = -1
            ++audioIndexPos
        }
    }
    /* private fun getSongFromPos(): Int {
         return if (audioIndexPos == audioList.size - 1) {
             //if last in playlist
             0
         } else {
             //get next in playlist
             ++audioIndexPos
         }
     }*/

    private fun decrementPosByOne(prevAudioIndex: Int) {
        audioIndexPos = prevAudioIndex
        if (audioIndexPos != 0) {
            --audioIndexPos
        } else {
            audioIndexPos = queueAudioList.size
            --audioIndexPos
        }
//        Toast.makeText(this, "$audioIndexPos  size: ${audioList.size - 1}", Toast.LENGTH_SHORT)
//            .show()
    }

    private fun shuffledAudioList() {
        binding.bottomSheet.shuffleSongIV.setOnClickListener {
            isShuffled = storage.getIsShuffled()
            if (!isShuffled) {
                shuffledList.clear()
                storage.saveIsShuffled(true)
                //adding current playing audio at first position
                val allSongsModel = AllSongsModel(
                    queueAudioList[audioIndexPos].songId,
                    queueAudioList[audioIndexPos].albumId,
                    queueAudioList[audioIndexPos].songName,
                    queueAudioList[audioIndexPos].artistsName,
                    queueAudioList[audioIndexPos].albumName,
                    queueAudioList[audioIndexPos].size,
                    queueAudioList[audioIndexPos].duration,
                    queueAudioList[audioIndexPos].data,
                    queueAudioList[audioIndexPos].contentUri,
                    queueAudioList[audioIndexPos].artUri,
                    queueAudioList[audioIndexPos].dateAdded,
                    queueAudioList[audioIndexPos].isFavourite,
                    queueAudioList[audioIndexPos].favAudioAddedTime,
                    queueAudioList[audioIndexPos].artistId,
                    queueAudioList[audioIndexPos].displayName,
                    queueAudioList[audioIndexPos].contentType,
                    queueAudioList[audioIndexPos].year,
                    queueAudioList[audioIndexPos].folderId,
                    queueAudioList[audioIndexPos].folderName,
                    queueAudioList[audioIndexPos].noOfSongs
                )
                allSongsModel.currentPlayedAudioTime =
                    queueAudioList[audioIndexPos].currentPlayedAudioTime
                allSongsModel.playingOrPause = queueAudioList[audioIndexPos].playingOrPause
                shuffledList.add(
                    0, allSongsModel
                )
                //remove current playing audio list after adding
                queueAudioList.removeAt(audioIndexPos)
                //shuffle the list and add all data to Shuffled list
                shuffledList.addAll(queueAudioList.shuffled())
                queueAudioList.clear()
                queueAudioList.addAll(shuffledList)
                binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle_on_24)
                Log.d("ShuffledAudioList", "onCreateView: $shuffledList")
                storage.storeQueueAudio(queueAudioList)
                audioIndexPos = 0
                //If user shuffled audio, assigning audio index to zero since zero index audio is already playing : when user will click next button it will play index 1 audio.
                storage.storeAudioIndex(audioIndexPos)

            } else {
                storage.saveIsShuffled(false)
                //val index = if (audioIndexPos == -1) 0 else audioIndexPos
                val currentPlayingAudio = queueAudioList[audioIndexPos]
                Log.d("currentPlayingAudio", "shuffledAudioList: $currentPlayingAudio")
                val sortedList: List<AllSongsModel> =
                    queueAudioList.sortedBy { allSongsModel -> allSongsModel.songName }
                Log.d(
                    "ShuffledaudioList",
                    "onCreateView: $sortedList"
                )
                val currentAudioIndexInSortedList = sortedList.indexOf(currentPlayingAudio)
                Log.d(
                    "currentAudioIndexInSortedList",
                    "shuffledAudioList: $currentAudioIndexInSortedList"
                )

                audioIndexPos = currentAudioIndexInSortedList
                shuffledList.clear()
                queueAudioList.clear()
                queueAudioList.addAll(sortedList)
                storage.storeQueueAudio(queueAudioList)
                storage.storeAudioIndex(audioIndexPos)
                binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle)
                //Toast.makeText(this, "$audioIndexPos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val updatePlayerUI: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            // if (intent.action == AllSongFragment.Broadcast_UPDATE_MINI_PLAYER) {
            isShuffled = storage.getIsShuffled()

            try {
                queueAudioList = storage.loadQueueAudio()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //Get the current playing media index form SharedPreferences
            audioIndexPos = storage.loadAudioIndex()

            val bundle = intent.extras
            Log.d("audioList", "onReceive: $queueAudioList")

            //show the mini player
            if (storage.getIsAudioPlayedFirstTime() || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                if (queueAudioList.isNotEmpty()) {
                    storage.saveIsAudioPlayedFirstTime(false)
                    binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE
                    animateMiniPlayerToVisible()
                    nowPlayingBottomSheetBehavior.isDraggable = !isContextMenuEnabled
                }
            }

            /*  Toast.makeText(
                  this@ActivityBottomBarFragmentContainer,
                  "audioIndexPos aa $audioIndexPos",
                  Toast.LENGTH_SHORT
              ).show()*/

            if (queueAudioList.isNotEmpty()) {
                if (isShuffled) {
                    binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle_on_24)
                } else {
                    binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle)
                }

                Log.d("prevPlayingAudioIndex", "onReceive:  $audioIndexPos")
                // AllSongFragment.musicService?.pausedByManually = false

                //this condition is used to prevent redundant update of views , this piece of code will only run when Play Pause Btn clicked
                var updateHeartImage = true
                if (isPlayPauseClicked) {
                    updateHeartImage =
                        false // if play pause btn is clicked then restrict to show animation in heart image
                    isPlayPauseClicked = false
                    if (AllSongFragment.musicService?.mediaPlayer != null) {
                        if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                            binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_pause_audio)
                        } else {
                            binding.bottomSheet.playPauseMiniPlayer.setImageResource(R.drawable.ic_play_audio)
                        }

                        binding.bottomSheet.playPauseExpandedPlayer.isSelected =
                            AllSongFragment.musicService?.mediaPlayer?.isPlaying!!
                    }
                } else {
                    binding.bottomSheet.audioProgress.max =
                        queueAudioList[audioIndexPos].duration
                    binding.bottomSheet.audioProgress.progress = storage.getAudioResumePos()

                    handleAudioProgressBar()

                    //update ui
                    if (!isDestroyedActivity) {
                        updateMiniPlayer(audioIndexPos)
                        updateExpandedPlayerViews(audioIndexPos)
                        updateAudioController()
                    }

                    val currentTime = System.currentTimeMillis()
                    updateCurrentPlayedTime(queueAudioList, currentTime)
                    //incrementMostPlayedCount(audioList,audioIndexPos)
                    val mostPlayedCount = queueAudioList[audioIndexPos].mostPlayedCount + 1
                    mViewModelClass.updateMostPlayedAudioCount(
                        queueAudioList[audioIndexPos].songId,
                        mostPlayedCount,
                        lifecycleScope
                    )

                    // update audio highlight while skipping prev and next audio
                    var hasSetFavFromNoti = false
                    if (bundle != null) {
                        hasSetFavFromNoti = bundle.getBoolean("hasSetFavFromNoti", false)
                        if (hasSetFavFromNoti) {
                            val isFav = bundle.getBoolean("isFav", false)
                            if (isFav) {
                                binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_fill)
                                showLikedAudioAnim()
                            } else {
                                binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_break)
                                showLikedAudioAnim()
                            }
                            addAudioToFavourites(isFav)
                        } else {
                            var previousRunningAudioIndex = bundle.getInt("index")
                            if (previousRunningAudioIndex == -1) {
                                previousRunningAudioIndex = 0
                                storage.storeAudioIndex(previousRunningAudioIndex)
                            }
                            Log.d(
                                "previousRunningAudioIndex",
                                "onReceive:$previousRunningAudioIndex "
                            )
                            // remove previous audio highlight
                            mViewModelClass.updateSong(
                                queueAudioList[previousRunningAudioIndex].songId,
                                queueAudioList[previousRunningAudioIndex].songName,
                                -1,//default
                                lifecycleScope
                            )
                            mViewModelClass.updateQueueAudio(
                                queueAudioList[previousRunningAudioIndex].songId,
                                queueAudioList[previousRunningAudioIndex].songName,
                                -1,//default
                                lifecycleScope
                            )
                        }
                    }

                    //saving list with current playing audio only if hasSetFavFromNoti is FALSE
                    if (!hasSetFavFromNoti) {
                        Log.d("ActivityminiPlayer", "onReceive: $queueAudioList ")
                        val list = CopyOnWriteArrayList<AllSongsModel>()
                        for ((index, audio) in queueAudioList.withIndex()) {
                            val allSongsModel = AllSongsModel(
                                audio.songId,
                                audio.albumId,
                                audio.songName,
                                audio.artistsName,
                                audio.albumName,
                                audio.size,
                                audio.duration,
                                audio.data,
                                audio.contentUri,
                                audio.artUri,
                                audio.dateAdded,
                                audio.isFavourite,
                                audio.favAudioAddedTime,
                                audio.artistId,
                                audio.displayName,
                                audio.contentType,
                                audio.year,
                                audio.folderId,
                                audio.folderName,
                                audio.noOfSongs
                            )

                            if (index == audioIndexPos) {
                                allSongsModel.mostPlayedCount = mostPlayedCount
                                if (AllSongFragment.musicService?.mediaPlayer != null) {
                                    if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                                        allSongsModel.playingOrPause = 1 /*playing*/
                                    } else {
                                        allSongsModel.playingOrPause = 0 /*pause*/
                                    }
                                }

                                // if play pause btn is not clicked then only update this and animation will be shown
                                if (updateHeartImage) {
                                    updateHeartImage = false
                                    if (audio.isFavourite) {
                                        binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.ic_filled_red_heart)
                                    } else {
                                        binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.vd_trimclip_heart_empty)
                                    }
                                }

                                allSongsModel.currentPlayedAudioTime = currentTime
                                list.add(allSongsModel)
                            } else {
                                allSongsModel.mostPlayedCount = audio.mostPlayedCount
                                allSongsModel.currentPlayedAudioTime =
                                    audio.currentPlayedAudioTime
                                list.add(allSongsModel)
                            }

                            Log.d(
                                "MostPlayedAudio1111",
                                "onReceive:  ${allSongsModel.songName} , ${allSongsModel.mostPlayedCount} "
                            )

                        }
                        storage.storeQueueAudio(list)
                    }

                }

            } else {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.GONE
                    nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    if (queueListBottomSheet != null) {
                        queueListBottomSheet!!.dismiss()
                    }
                }

                animateMiniPlayerToGone()
                /* nowPlayingBottomSheetBehavior.peekHeight =
                     ConvertDpToPx.dpToPx(
                         65,
                         this@ActivityBottomBarFragmentContainer
                     )*/

                nowPlayingBottomSheetBehavior.isDraggable = false
            }
            //}
        }
    }

    private fun updateCurrentPlayedTime(
        audioList: CopyOnWriteArrayList<AllSongsModel>,
        currentTime: Long
    ) {
        mViewModelClass.updateCurrentPlayedTime(
            audioList[audioIndexPos].songId,
            currentTime,
            lifecycleScope
        )
    }

    private fun registerUpdatePlayerUI() {
        //Register playNewMedia receiver
        val filter = IntentFilter(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        registerReceiver(updatePlayerUI, filter)
    }

    override fun onResume() {
        super.onResume()
        //Toast.makeText(this, "host activity", Toast.LENGTH_SHORT).show()
        SavedAppTheme(
            this,
            null,
            null,
            binding.hostActivityBG,
            false,
            isHostActivity = true,
            tagEditorsBG = null,
            isTagEditor = false,
            bottomBar = binding.bottomBar,
            rlMiniPlayerBottomSheet = binding.bottomSheet.rlMiniPlayerBottomsheet,
            bottomShadowIVAlbumFrag = null,
            isAlbumFrag = false,
            topViewIV = null,
            bottomShadowIVArtistFrag = null,
            isArtistFrag = false,
            topViewIVArtistFrag = null,
            parentViewArtistAndAlbumFrag = null,
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            playlistBG = null,
            isPlaylistFrag = false,
            searchFragBg = null,
            isSearchFrag = false,
            settingFragBg = null,
            isSettingFrag = false
        ).settingSavedBackgroundTheme()
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                startActivity(Intent(this, SplashScreenActivity::class.java))
                finishAffinity()
            }
        } else {*/
        /* mPermRequest!!.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)*/
        val checkSelfPermission: Int = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, SplashScreenActivity::class.java))
            finishAffinity()
        }

        // }
        //mCastContext!!.addCastStateListener { state ->
        //    if (state == CastState.NO_DEVICES_AVAILABLE) {
        //        binding.bottomSheet.castBtn.visibility = View.GONE
        //        binding.bottomSheet.skipNextAudioMP.visibility = View.VISIBLE
        //    } else {
        //        binding.bottomSheet.castBtn.visibility = View.VISIBLE
        //        binding.bottomSheet.skipNextAudioMP.visibility = View.GONE
        //   }
        // }

        //if (mCastContext!!.castState == CastState.NO_DEVICES_AVAILABLE) {
        //    binding.bottomSheet.castBtn.visibility = View.GONE
        //    binding.bottomSheet.skipNextAudioMP.visibility = View.VISIBLE
        //} else {
        //    binding.bottomSheet.castBtn.visibility = View.VISIBLE
        //    binding.bottomSheet.skipNextAudioMP.visibility = View.GONE
        // }

        // CastButtonFactory.setUpMediaRouteButton(this, binding.bottomSheet.castBtn)

        //mCastContext!!.sessionManager.addSessionManagerListener(
        //    mSessionManagerListener!!, CastSession::class.java
        // )

        //loadEqualizerSettings()
    }

    /* private fun setupCastListener() {
         mSessionManagerListener = object : SessionManagerListener<CastSession> {
             override fun onSessionEnded(session: CastSession, error: Int) {
                 onApplicationDisconnected()
             }

             override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                 onApplicationConnected(session)
             }

             override fun onSessionResumeFailed(session: CastSession, error: Int) {
                 onApplicationDisconnected()
             }

             override fun onSessionStarted(session: CastSession, sessionId: String) {
                 onApplicationConnected(session)
             }

             override fun onSessionStartFailed(session: CastSession, error: Int) {
                 onApplicationDisconnected()
             }

             override fun onSessionStarting(session: CastSession) {}
             override fun onSessionEnding(session: CastSession) {}
             override fun onSessionResuming(session: CastSession, sessionId: String) {}
             override fun onSessionSuspended(session: CastSession, reason: Int) {}

             private fun onApplicationConnected(castSession: CastSession) {
                 mCastSession = castSession
                 Toast.makeText(
                     this@ActivityBottomBarFragmentContainer,
                     "RemoteServer Connected",
                     Toast.LENGTH_SHORT
                 ).show()
                 *//* if (null != mSelectedMedia) {
                     if (mPlaybackState === PlaybackState.PLAYING) {
                         mVideoView.pause()
                         loadRemoteMedia(mSeekbar.getProgress(), true)
                         return
                     } else {
                         mPlaybackState = PlaybackState.IDLE
                         updatePlaybackLocation(PlaybackLocation.REMOTE)
                     }
                 }
                 updatePlayButton(mPlaybackState)
                 supportInvalidateOptionsMenu()*//*
                // loadRemoteMedia(binding.bottomSheet.seekBar.progress, true)
            }

            private fun onApplicationDisconnected() {
                *//* updatePlaybackLocation(PlaybackLocation.LOCAL)
                 mPlaybackState = PlaybackState.IDLE
                 mLocation = PlaybackLocation.LOCAL
                 updatePlayButton(mPlaybackState)
                 supportInvalidateOptionsMenu()*//*
                Toast.makeText(
                    this@ActivityBottomBarFragmentContainer,
                    "RemoteServer disconnected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }*/

    /* private fun loadRemoteMedia(position: Int, autoPlay: Boolean) {
         if (mCastSession == null) {
             return
         }
         val remoteMediaClient = mCastSession!!.remoteMediaClient ?: return
         remoteMediaClient.load(
             MediaLoadRequestData.Builder()
                 .setMediaInfo(buildMediaInfo())
                 .setAutoplay(autoPlay)
                 .setCurrentTime(position.toLong()).build()
         )
     }

     private fun buildMediaInfo(): MediaInfo {
         val mediaTrack = MediaTrack.Builder(audioList[audioIndexPos].albumId, MediaTrack.TYPE_AUDIO)
             .setName(audioList[audioIndexPos].songName)
             .setContentId(audioList[audioIndexPos].audioUri)
             .setLanguage("en-US")
             .build()
         val mediaTrackList = ArrayList<MediaTrack>()
         mediaTrackList.add(mediaTrack)

         val audioMetaData = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK)
         *//*audioMetaData.putString(MediaMetadata.KEY_SUBTITLE, audioList[audioIndexPos].artistsName)
        audioMetaData.putString(MediaMetadata.KEY_TITLE,audioList[audioIndexPos].songName)
        audioMetaData.addImage(WebImage(Uri.parse(audioList[audioIndexPos].artUri)))
        audioMetaData.addImage(WebImage(Uri.parse(audioList[audioIndexPos].artUri)))*//*
        audioMetaData.putString(MediaMetadata.KEY_TITLE, audioList[audioIndexPos].songName)
        audioMetaData.putString(MediaMetadata.KEY_ALBUM_TITLE, audioList[audioIndexPos].albumName)
        audioMetaData.putString(MediaMetadata.KEY_ARTIST, audioList[audioIndexPos].artistsName)
        audioMetaData.putString(
            MediaMetadata.KEY_ALBUM_ARTIST,
            audioList[audioIndexPos].artistsName
        )
        audioMetaData.addImage(WebImage(Uri.parse(albumArt)))

        return MediaInfo.Builder("https://${audioList[audioIndexPos].artUri}")
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mp3")
            .setMetadata(audioMetaData)
            .setMediaTracks(mediaTrackList)
            .setStreamDuration(audioList[audioIndexPos].duration.toLong() * 1000)
            .build()
    }*/
    private fun showLikedAudioAnim() {
        val animatedVectorDrawable =
            binding.bottomSheet.likedAudioIV.drawable as AnimatedVectorDrawable
        animatedVectorDrawable.start()
    }

    override fun onStop() {
        super.onStop()
        if (appUpdateManager != null) {
            appUpdateManager!!.unregisterListener(installStateUpdatedListener!!)
        }
    }


}
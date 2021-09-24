package com.knesarcreation.playbeat.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
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
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.ActivityBottomBarFragmentBinding
import com.knesarcreation.playbeat.fragment.*
import com.knesarcreation.playbeat.utils.*
import me.ibrahimsn.lib.OnItemSelectedListener
import java.util.concurrent.CopyOnWriteArrayList


class ActivityBottomBarFragmentContainer : AppCompatActivity()/*, ServiceConnection*/,
    AllAlbumsFragment.OnAlbumItemClicked, AllArtistsFragment.OpenArtisFragment,
    ArtistsTracksAndAlbumFragment.OnArtistAlbumItemClicked {
    private lateinit var binding: ActivityBottomBarFragmentBinding
    private val homeFragment = HomeFragment()
    private val playlistsFragment = PlaylistsFragment()
    private val settingFragment = SettingFragment()
    private val albumSongFragment = AlbumFragment()
    private val artistsTracksAndAlbumFragment = ArtistsTracksAndAlbumFragment()
    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var viewModel: DataObservableClass
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var isAlbumFragOpened = false
    private var isArtistsFragOpened = false
    private var isAlbumOpenedFromArtisFrag = false
    private var isNotiBuild = false
    private var runnableAudioProgress: Runnable? = null
    private var newState: Int = 0
    private lateinit var nowPlayingBottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private var audioRunnable: Runnable? = null
    private var shuffledList = CopyOnWriteArrayList<AllSongsModel>()
    private var isShuffled = false
    private var isPlayPauseClicked = false
    private var mCastSession: CastSession? = null
    private var mSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var mCastContext: CastContext? = null
    private lateinit var storage: StorageUtil
    private lateinit var mViewModelClass: ViewModelClass

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomBarFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //navigationController = findNavController(R.id.fragmentContainer)
        //setUpSmoothBottomMenu()

        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        MakeStatusBarTransparent().transparent(this)
        storage = StorageUtil(this)
        //setupCastListener()
        // setting cast context
        mCastContext = CastContext.getSharedInstance(this)
        mCastSession = mCastContext!!.sessionManager.currentCastSession

        binding.bottomSheet.songNameTV.isSelected = true
        binding.bottomSheet.songTextTV.isSelected = true
        binding.bottomSheet.bottomBar.onItemSelectedListener = onItemSelectedListener

        viewModel = this.run {
            ViewModelProvider(this)[DataObservableClass::class.java]
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, homeFragment)
            .add(R.id.fragmentContainer, playlistsFragment)
            .add(R.id.fragmentContainer, settingFragment)
            .add(R.id.fragmentContainer, albumSongFragment)
            .add(R.id.fragmentContainer, artistsTracksAndAlbumFragment)
            .commit()
        setBottomBarFragmentState(FragmentState.HOME).commit()

        registerUpdatePlayerUI()
        controlAudio()
        handleSeekBarChangeListener()
        handlePlayPauseNextPrev()
        setupNowPlayingBottomSheet()
        shuffledAudioList()
        openAudioQueueList()
        manageRepeatAudioList()
        openSleepTimerSheet()

        /*if (AllSongFragment.musicService != null) {
            // if service is bounded
            if (AllSongFragment.musicService?.mediaPlayer != null) {
                audioList = StorageUtil(this).loadAudio()
                audioIndexPos = StorageUtil(this).loadAudioIndex()
                updatePlayingMusic(audioIndexPos)
                updateViews(audioIndexPos)
            }
        }*/


        binding.bottomSheet.likedAudioIV.setOnClickListener {
            Toast.makeText(this, "Coming soon...", Toast.LENGTH_SHORT).show()
        }

        binding.bottomSheet.seekBar.setOnClickListener { }

        //set resume progress in audio progress bar and seek bar
        // val storageUtil = StorageUtil(this)
        binding.bottomSheet.seekBar.max =
            (storage.getLastAudioMaxSeekProg().toDouble() / 1000).toInt()
        binding.bottomSheet.seekBar.progress =
            (storage.getAudioResumePos().toDouble() / 1000).toInt()
        /* (storage.getAudioResumePos().toDouble() / 1000).toInt()*/
        Toast.makeText(
            this,
            "Start: ${(storage.getAudioResumePos().toDouble() / 1000).toInt()}",
            Toast.LENGTH_SHORT
        ).show()

        binding.bottomSheet.startTimeTV.text =
            millisToMinutesAndSeconds(storage.getAudioResumePos())

        showSleepTimerIfEnable()
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
                Toast.makeText(this, "Repeat current audio", Toast.LENGTH_SHORT).show()
                binding.bottomSheet.loopOneSong.setImageResource(R.drawable.repeat_one_on_24)
                storage.saveIsRepeatAudio(true)
            } else {
                //repeat audio list
                Toast.makeText(this, "Repeat audio list", Toast.LENGTH_SHORT).show()
                binding.bottomSheet.loopOneSong.setImageResource(R.drawable.ic_repeat_24)
                storage.saveIsRepeatAudio(false)
            }
        }
    }

    private fun openAudioQueueList() {
        binding.bottomSheet.queueAudioIV.setOnClickListener {
            val queueListBottomSheet = BottomSheetAudioQueueList(this)
            queueListBottomSheet.show(supportFragmentManager, "queueListBottomSheet")

            queueListBottomSheet.listener =
                object : BottomSheetAudioQueueList.OnRepeatAudioListener {
                    override fun onRepeatIconClicked() {
                        //Toast.makeText(this@ActivityBottomBarFragmentContainer, "clicked", Toast.LENGTH_SHORT).show()
                        manageRepeatAudioList()
                    }
                }
        }
    }

    private fun setupNowPlayingBottomSheet() {
        nowPlayingBottomSheetBehavior =
            BottomSheetBehavior.from(binding.bottomSheet.nowPlayingBottomSheetContainer)
        newState = BottomSheetBehavior.STATE_COLLAPSED

        nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_DRAGGING
        nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        if (!storage.getIsAudioPlayedFirstTime()) {
            nowPlayingBottomSheetBehavior.peekHeight = 330
            binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE
            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin = 330
            binding.fragmentContainer.layoutParams = layoutParams
            nowPlayingBottomSheetBehavior.isDraggable = true
        } else {
            // app open first time
            nowPlayingBottomSheetBehavior.isDraggable = false
        }

        binding.bottomSheet.closeSheetIV.setOnClickListener {
            nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            //binding.bottomSheet.bottomBar.visibility = View.VISIBLE
            //binding.bottomSheet.rlSmallbottomBarPlayingSong.visibility = View.VISIBLE
        }
        binding.bottomSheet.rlMiniPlayerBottomsheet.setOnClickListener {
            nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            //binding.bottomSheet.bottomBar.visibility = View.GONE
            //binding.bottomSheet.rlSmallbottomBarPlayingSong.visibility = View.GONE
        }

        nowPlayingBottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                this@ActivityBottomBarFragmentContainer.newState = newState
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.bottomSheet.bottomBar.visibility = View.GONE
                    binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.GONE
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    binding.bottomSheet.bottomBar.visibility = View.VISIBLE
                    binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE
                    binding.bottomSheet.bottomBar.alpha = 1F
                    binding.bottomSheet.rlMiniPlayerBottomsheet.alpha = 1F
                    binding.bottomSheet.rlNowPlayingBottomSheet.alpha = 0F
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.d("NowPlayingSheetSlideOffset", "onSlide: $slideOffset ")
                binding.bottomSheet.bottomBar.alpha = (0.4 - slideOffset).toFloat()
                binding.bottomSheet.rlMiniPlayerBottomsheet.alpha =
                    (0.4 - slideOffset).toFloat()
                binding.bottomSheet.rlNowPlayingBottomSheet.alpha = slideOffset
            }
        })

    }

    private fun updatePlayingMusic(audioIndex: Int) {
        binding.bottomSheet.songNameTV.text = audioList[audioIndex].songName
        binding.bottomSheet.artistOrAlbumNameTV.text = audioList[audioIndex].artistsName

        Glide.with(this).load(audioList[audioIndexPos].artUri)
            .apply(
                RequestOptions.placeholderOf(R.drawable.audio_icon_placeholder).centerCrop()
            )
            .into(binding.bottomSheet.albumArtIV)

        if (AllSongFragment.musicService?.mediaPlayer != null) {
            if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                // highlight the pause audio and  pause anim
                mViewModelClass.updateSong(
                    audioList[audioIndex].songId,
                    audioList[audioIndex].songName,
                    1,
                    lifecycleScope
                )
                mViewModelClass.updateQueueAudio(
                    audioList[audioIndex].songId,
                    audioList[audioIndex].songName,
                    1,
                    lifecycleScope
                )
                binding.bottomSheet.playPauseIV1.setImageResource(R.drawable.ic_pause_audio)
            } else {
                // highlight the playing audio
                mViewModelClass.updateSong(
                    audioList[audioIndex].songId,
                    audioList[audioIndex].songName,
                    0,
                    lifecycleScope
                )
                mViewModelClass.updateQueueAudio(
                    audioList[audioIndex].songId,
                    audioList[audioIndex].songName,
                    0,
                    lifecycleScope
                )
                binding.bottomSheet.playPauseIV1.setImageResource(R.drawable.ic_play_audio)
            }
        }
    }

    private fun controlAudio() {
        binding.bottomSheet.skipNextAudio1.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            audioList = storage.loadAudio()
            // remove previous audio highlight
            mViewModelClass.updateSong(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                -1, // default
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                -1, // default
                lifecycleScope
            )
            incrementPosByOne()
            storage.storeAudioIndex(audioIndexPos)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            updatePlayingMusic(audioIndexPos)
        }

        binding.bottomSheet.playPauseIV1.setOnClickListener {
            if (AllSongFragment.musicService != null) {
                isPlayPauseClicked = true
                updateBottomPlayPauseIV()
            }
        }

        //expandedPlayingDialog controllers
        binding.bottomSheet.playPauseIV.setOnClickListener {
            //handleStartTimeAndSeekBar()
            isPlayPauseClicked = true
            updateBottomPlayPauseIV()
        }

        binding.bottomSheet.skipNextAudio.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            audioList = storage.loadAudio()
            // remove previous audio highlight
            mViewModelClass.updateSong(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                -1, // default
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                -1, // default
                lifecycleScope
            )

            incrementPosByOne()
            storage.storeAudioIndex(audioIndexPos)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            //updateViews(audioIndexPos)
            //updateAudioController() // seek bar, start time, end time, play pause btn
            binding.bottomSheet.skipNextAudio.setImageResource(R.drawable.avd_music_next)
            val animatedVectorDrawable =
                binding.bottomSheet.skipNextAudio.drawable as AnimatedVectorDrawable
            animatedVectorDrawable.start()
            binding.bottomSheet.playPauseIV.isSelected = true
        }

        binding.bottomSheet.skipPrevAudio.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            audioList = storage.loadAudio()
            // remove previous audio highlight
            mViewModelClass.updateSong(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                -1, // default
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                -1, // default
                lifecycleScope
            )

            decrementPosByOne()
            storage.storeAudioIndex(audioIndexPos)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            //updateViews(audioIndexPos)
            //updateAudioController() // seek bar, start time, end time, play pause btn
            binding.bottomSheet.skipPrevAudio.setImageResource(R.drawable.avd_music_previous)
            val animatedVectorDrawable =
                binding.bottomSheet.skipPrevAudio.drawable as AnimatedVectorDrawable
            animatedVectorDrawable.start()
            binding.bottomSheet.playPauseIV.isSelected = true
        }
    }

    private fun updateBottomPlayPauseIV() {
        if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
            // pause through button
            AllSongFragment.musicService?.pauseMedia()
            AllSongFragment.musicService?.pausedByManually = true
            AllSongFragment.musicService?.buildNotification(
                PlaybackStatus.PAUSED,
                PlaybackStatus.UN_FAVOURITE,
                0f
            )
            binding.bottomSheet.playPauseIV1.setImageResource(R.drawable.ic_play_audio)
            // highlight pause audio with pause anim
            mViewModelClass.updateSong(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                0,
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                0,
                lifecycleScope
            )
        } else if (!AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
            // resume through button
            AllSongFragment.musicService?.resumeMedia()
            AllSongFragment.musicService?.pausedByManually = false
            binding.bottomSheet.playPauseIV1.setImageResource(R.drawable.ic_pause_audio)
            AllSongFragment.musicService?.updateMetaData()
            AllSongFragment.musicService?.buildNotification(
                PlaybackStatus.PLAYING,
                PlaybackStatus.UN_FAVOURITE,
                1f
            )
            // calling two times for the first time
            if (!isNotiBuild) {
                isNotiBuild = true
                AllSongFragment.musicService?.buildNotification(
                    PlaybackStatus.PLAYING,
                    PlaybackStatus.UN_FAVOURITE,
                    1f
                )

            }
            // highlight the playing audio
            mViewModelClass.updateSong(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                1,
                lifecycleScope
            )
            mViewModelClass.updateQueueAudio(
                audioList[audioIndexPos].songId,
                audioList[audioIndexPos].songName,
                1,
                lifecycleScope
            )
        }

        binding.bottomSheet.playPauseIV.isSelected =
            AllSongFragment.musicService?.mediaPlayer?.isPlaying!!

        // update progress bar
        //binding.bottomSheet.audioProgress.max = audioList[audioIndexPos].duration
        // AllSongFragment.musicService?.mediaPlayer?.duration!!
        handleAudioProgressBar()
        handleStartTimeAndSeekBar()


    }

    /*override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean("ServiceState", AllSongFragment.serviceBound);
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        AllSongFragment.serviceBound = savedInstanceState!!.getBoolean("ServiceState")
    }*/

    private fun handleAudioProgressBar() {
        if (runnableAudioProgress != null) {
            handler.removeCallbacks(runnableAudioProgress!!)
        }
        runnableAudioProgress = object : Runnable {
            override fun run() {
                if (AllSongFragment.musicService != null) {
                    if (AllSongFragment.musicService?.mediaPlayer != null) {
                        binding.bottomSheet.audioProgress.progress =
                            AllSongFragment.musicService?.mediaPlayer?.currentPosition!!
                        handler.postDelayed(this, 1000)
                    }
                }
            }

        }
        runOnUiThread(runnableAudioProgress)
    }


    private fun setBottomBarFragmentState(state: FragmentState): FragmentTransaction {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
        when (state) {
            FragmentState.HOME -> {
                when {
                    isAlbumFragOpened -> {
                        transaction.hide(homeFragment)
                        transaction.hide(playlistsFragment)
                        transaction.hide(settingFragment)
                        transaction.hide(artistsTracksAndAlbumFragment)
                        transaction.show(albumSongFragment)
                    }
                    isArtistsFragOpened || isAlbumOpenedFromArtisFrag -> {
                        transaction.hide(homeFragment)
                        transaction.hide(playlistsFragment)
                        transaction.hide(settingFragment)
                        transaction.show(artistsTracksAndAlbumFragment)
                        transaction.hide(albumSongFragment)
                    }
                    else -> {
                        transaction.show(homeFragment)
                        transaction.hide(playlistsFragment)
                        transaction.hide(settingFragment)
                        transaction.hide(albumSongFragment)
                        transaction.hide(artistsTracksAndAlbumFragment)
                    }
                }
                binding.bottomSheet.bottomBar.itemActiveIndex = 0
            }
            FragmentState.FAVOURITE -> {
                transaction.hide(homeFragment)
                transaction.show(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                binding.bottomSheet.bottomBar.itemActiveIndex = 1
            }
            FragmentState.SETTING -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.show(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                binding.bottomSheet.bottomBar.itemActiveIndex = 2
            }
            FragmentState.ALBUM_FRAGMENT -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.show(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)

                isAlbumFragOpened = !isAlbumOpenedFromArtisFrag
                isArtistsFragOpened = false
                /*  Toast.makeText(
                      this,
                      "$isAlbumOpenedFromArtisFrag , $isAlbumFragOpened",
                      Toast.LENGTH_SHORT
                  ).show()*/
            }
            FragmentState.ARTIST_TRACK_ALBUM_FRAGMENT -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.show(artistsTracksAndAlbumFragment)
                isArtistsFragOpened = true
                isAlbumFragOpened = false
            }
        }
        return transaction
    }

    private val onItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelect(pos: Int): Boolean {
            when (pos) {
                0 -> {
                    setBottomBarFragmentState(FragmentState.HOME).commit()
                    return true
                }
                1 -> {
                    setBottomBarFragmentState(FragmentState.FAVOURITE).commit()
                    return true
                }
                2 -> {
                    setBottomBarFragmentState(FragmentState.SETTING).commit()
                    return true
                }
            }
            return false
        }
    }

    internal enum class FragmentState {
        HOME,
        FAVOURITE,
        SETTING,
        ALBUM_FRAGMENT,
        ARTIST_TRACK_ALBUM_FRAGMENT,
    }

    override fun onBackPressed() {
        if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
            //Toast.makeText(this, "Collapsed State", Toast.LENGTH_SHORT).show()
            if (supportFragmentManager.backStackEntryCount > 0 || !homeFragment.isHidden) {
                super.onBackPressed()
            } else {
                if (isAlbumFragOpened) {
                    if (!playlistsFragment.isHidden || !settingFragment.isHidden) {
                        setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
                    } else {
                        isAlbumFragOpened = false
                        setBottomBarFragmentState(FragmentState.HOME).commit()
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
                    }
                } else if (isArtistsFragOpened || isAlbumOpenedFromArtisFrag) {
                    if (!playlistsFragment.isHidden /* if open*/ || !settingFragment.isHidden /*if open*/) {
                        setBottomBarFragmentState(FragmentState.ARTIST_TRACK_ALBUM_FRAGMENT).commit()
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
                    } else {
                        isArtistsFragOpened = false
                        setBottomBarFragmentState(FragmentState.HOME).commit()
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
                        isAlbumOpenedFromArtisFrag = false
                    }
                } else {
                    setBottomBarFragmentState(FragmentState.HOME).commit()
                    binding.bottomSheet.bottomBar.itemActiveIndex = 0
                }

            }
        } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
            //Toast.makeText(this, "State Expanded", Toast.LENGTH_SHORT).show()
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
        setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
    }

    override fun onOpenArtistFragment(artistsData: String) {
        viewModel.artistsData.value = artistsData
        setBottomBarFragmentState(FragmentState.ARTIST_TRACK_ALBUM_FRAGMENT).commit()
    }

    override fun openArtistAlbum(album: String) {
        viewModel.albumData.value = album

        isAlbumOpenedFromArtisFrag = true
        setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
    }

    /** Managing Now playing bottom sheet........................................................*/

    private fun updateExpandedPlayerViews(audioIndexPos: Int) {
        if (audioIndexPos != -1) {
            val allSongsModel = audioList[audioIndexPos]
            binding.bottomSheet.songTextTV.text = allSongsModel.songName
            binding.bottomSheet.artistsNameTV.text = allSongsModel.artistsName
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

            val albumArt = UriToBitmapConverter.getBitmap(
                contentResolver!!,
                allSongsModel.artUri.toUri()
            )

            //setting player background
            if (albumArt != null) {
                Glide.with(this).load(allSongsModel.artUri).transition(
                    withCrossFade(
                        factory
                    )
                )
                    /* .apply(RequestOptions.placeholderOf(R.drawable.ic_audio_file_placeholder_svg))*/
                    .into(binding.bottomSheet.nowPlayingAlbumArtIV)
                Palette.from(albumArt).generate {
                    val swatch = it?.darkVibrantSwatch
                    if (swatch != null) {
                        binding.bottomSheet.albumArtBottomGradient.setBackgroundResource(R.drawable.gradient_background_bottom_shadow)
                        binding.bottomSheet.bottomBackground.setBackgroundResource(R.drawable.app_theme_background_drawable)
                        // binding.bottomSheet.albumArtTopGradient.setBackgroundResource(R.drawable.gradient_background_top_shadow)

                        val gradientDrawableBottomTop = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(swatch.rgb, 0x00000000)
                        )

                        // Log.d("Swatch11", "updateViews: ${swatch.rgb}")
                        //Toast.makeText(this, "${swatch.rgb}", Toast.LENGTH_SHORT).show()
                        //val gradientDrawableTopBottom = GradientDrawable(
                        //    GradientDrawable.Orientation.TOP_BOTTOM,
                        //    intArrayOf(swatch.rgb, swatch.population)
                        // )
                        //  binding.bottomSheet.albumArtBottomGradient.background =
                        //     gradientDrawableBottomTop
                        //binding.bottomSheet.albumArtTopGradient.background =
                        //   gradientDrawableTopBottom
                        Glide.with(this).load(gradientDrawableBottomTop)
                            .transition(withCrossFade(factory))
                            .into(binding.bottomSheet.albumArtBottomGradient)

                        //Glide.with(this).load(gradientDrawableTopBottom)
                        //   .transition(withCrossFade(factory))
                        //  .into(binding.bottomSheet.albumArtTopGradient)

                        // parent background
                        val gradientDrawableParent = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(swatch.rgb, swatch.rgb)
                        )

                        Glide.with(this).load(gradientDrawableParent)
                            .transition(withCrossFade(factory))
                            .into(binding.bottomSheet.bottomBackground)
//                        binding.bottomSheet.bottomBackground.background = gradientDrawableParent

                    }
                }

            } else {
                val originalBitmap = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.audio_icon_placeholder
                )
                Glide.with(this).load(originalBitmap)
                    .transition(withCrossFade(factory))
                    .into(binding.bottomSheet.nowPlayingAlbumArtIV)

                Glide.with(this).load(R.drawable.purple_background_gradient)
                    .transition(withCrossFade(factory))
                    .into(binding.bottomSheet.albumArtBottomGradient)

                Glide.with(this).load(R.drawable.purple_solid_background)
                    .transition(withCrossFade(factory))
                    .into(binding.bottomSheet.bottomBackground)

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
//                        // parent background
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

    private fun handlePlayPauseNextPrev() {

    }

    private fun updateAudioController() {
        binding.bottomSheet.seekBar.max =
            (audioList[audioIndexPos].duration.toDouble() / 1000).toInt()
        val audioDuration =
            millisToMinutesAndSeconds(audioList[audioIndexPos].duration)
        binding.bottomSheet.endTimeTV.text = audioDuration

        handleStartTimeAndSeekBar()

        // change state of play pause
        if (AllSongFragment.musicService != null) {
            if (AllSongFragment.musicService?.mediaPlayer != null) {
                binding.bottomSheet.playPauseIV.isSelected =
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
                        binding.bottomSheet.startTimeTV.text =
                            millisToMinutesAndSeconds(AllSongFragment.musicService?.mediaPlayer?.currentPosition!!)
                        binding.bottomSheet.seekBar.progress =
                            (AllSongFragment.musicService?.mediaPlayer?.currentPosition!!.toDouble() / 1000).toInt()
                        handler.postDelayed(this, 1000)
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

    private fun incrementPosByOne() {
        audioIndexPos = storage.loadAudioIndex()
        if (audioIndexPos != audioList.size - 1) {
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

    private fun decrementPosByOne() {
        audioIndexPos = storage.loadAudioIndex()
        if (audioIndexPos != 0) {
            --audioIndexPos
        } else {
            audioIndexPos = audioList.size
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
                    audioList[audioIndexPos].songId,
                    audioList[audioIndexPos].albumId,
                    audioList[audioIndexPos].songName,
                    audioList[audioIndexPos].artistsName,
                    audioList[audioIndexPos].albumName,
                    audioList[audioIndexPos].size,
                    audioList[audioIndexPos].duration,
                    audioList[audioIndexPos].data,
                    audioList[audioIndexPos].audioUri,
                    audioList[audioIndexPos].artUri
                )
                allSongsModel.playingOrPause = audioList[audioIndexPos].playingOrPause
                shuffledList.add(
                    0, allSongsModel
                )
                //remove current playing audio list after adding
                audioList.removeAt(audioIndexPos)
                //shuffle the list and add all data to Shuffled list
                shuffledList.addAll(audioList.shuffled())
                audioList.clear()
                audioList.addAll(shuffledList)
                binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle_on_24)
                Log.d("ShuffledAudioList", "onCreateView: $shuffledList")
                storage.storeAudio(audioList)
                audioIndexPos = 0
                //If user shuffled audio, assigning audio index to zero since zero index audio is already playing : when user will click next button it will play index 1 audio.
                storage.storeAudioIndex(audioIndexPos)

            } else {
                storage.saveIsShuffled(false)
                //val index = if (audioIndexPos == -1) 0 else audioIndexPos
                val currentPlayingAudio = audioList[audioIndexPos]
                Log.d("currentPlayingAudio", "shuffledAudioList: $currentPlayingAudio")
                val sortedList: List<AllSongsModel> =
                    audioList.sortedBy { allSongsModel -> allSongsModel.songName }
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
                audioList.clear()
                audioList.addAll(sortedList)
                storage.storeAudio(audioList)
                storage.storeAudioIndex(audioIndexPos)
                binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle)
                //Toast.makeText(this, "$audioIndexPos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val updatePlayerUI: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isShuffled = storage.getIsShuffled()
            audioList = storage.loadAudio()
            val bundle = intent.extras

            Log.d("AudioListFragmentContainerActivity", "onReceive:  $audioList ")

            //show the mini player
            if (storage.getIsAudioPlayedFirstTime()) {
                storage.saveIsAudioPlayedFirstTime(false)
                nowPlayingBottomSheetBehavior.peekHeight = 350
                binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE
                val layoutParams =
                    binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
                layoutParams.bottomMargin = 350
                binding.fragmentContainer.layoutParams = layoutParams
                nowPlayingBottomSheetBehavior.isDraggable = true
            }

            if (isShuffled) {
                binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle_on_24)
            } else {
                binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle)
            }

            var prevPlayingAudioIndex = 0
            if (audioIndexPos != -1) {
                prevPlayingAudioIndex = audioIndexPos
            }

            /*  if(AllSongFragment.musicService?.mediaPlayer !=null){
                  if(AllSongFragment.musicService?.mediaPlayer?.isPlaying!!){
                       prevPlayingAudioIndex =
                  }
              }*/

            //Get the current playing media index form SharedPreferences
            audioIndexPos = storage.loadAudioIndex()

            Log.d("prevPlayingAudioIndex", "onReceive: $prevPlayingAudioIndex , $audioIndexPos")
            // AllSongFragment.musicService?.pausedByManually = false

            //this condition is used to prevent redundant update of views when pause or play the audio
            if (isPlayPauseClicked) {
                isPlayPauseClicked = false
                if (AllSongFragment.musicService?.mediaPlayer != null) {
                    if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                        binding.bottomSheet.playPauseIV1.setImageResource(R.drawable.ic_pause_audio)
                    } else {
                        binding.bottomSheet.playPauseIV1.setImageResource(R.drawable.ic_play_audio)
                    }

                    binding.bottomSheet.playPauseIV.isSelected =
                        AllSongFragment.musicService?.mediaPlayer?.isPlaying!!
                }
            } else {
                binding.bottomSheet.audioProgress.max = audioList[audioIndexPos].duration
                binding.bottomSheet.audioProgress.progress = storage.getAudioResumePos()

                handleAudioProgressBar()

                //update ui
                if (!isDestroyedActivity) {
                    /*if (audioList[prevPlayingAudioIndex].songId != audioList[audioIndexPos].songId) {
                    }
                    else {
                        // if playPause btn pressed from notification
                        if (bundle != null) {
                            val pausedFromNoti = bundle.getBoolean("pausedFromNoti", false)
                            if (pausedFromNoti) {
                                updatePlayingMusic(audioIndexPos)
                            }
                        }
                    }*/
                    updatePlayingMusic(audioIndexPos)
                    updateExpandedPlayerViews(audioIndexPos)
                    updateAudioController()
                }
            }

            /*if (audioList[prevPlayingAudioIndex].songId != audioList[audioIndexPos].songId) {*/
                if (bundle != null) {
                    val previousRunningAudioIndex = bundle.getInt("index")
                    // remove previous audio highlight
                    mViewModelClass.updateSong(
                        audioList[previousRunningAudioIndex].songId,
                        audioList[previousRunningAudioIndex].songName,
                        -1,//default
                        lifecycleScope
                    )
                    mViewModelClass.updateQueueAudio(
                        audioList[previousRunningAudioIndex].songId,
                        audioList[previousRunningAudioIndex].songName,
                        -1,//default
                        lifecycleScope
                    )
                }

                //saving list with current playing audio
                val list = CopyOnWriteArrayList<AllSongsModel>()
                for ((index, audio) in audioList.withIndex()) {
                    val allSongsModel = AllSongsModel(
                        audio.songId,
                        audio.albumId,
                        audio.songName,
                        audio.artistsName,
                        audio.albumName,
                        audio.size,
                        audio.duration,
                        audio.data,
                        audio.audioUri,
                        audio.artUri
                    )
                    if (index == audioIndexPos) {
                        if (AllSongFragment.musicService?.mediaPlayer != null) {
                            if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                                allSongsModel.playingOrPause = 1 /*playing*/
                            } else {
                                allSongsModel.playingOrPause = 0 /*pause*/
                            }
                        }

                        list.add(allSongsModel)
                    } else {
                        list.add(allSongsModel)
                    }
                }

                storage.storeAudio(list)
           /* }*/
        }
    }

    private fun registerUpdatePlayerUI() {
        //Register playNewMedia receiver
        val filter = IntentFilter(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        registerReceiver(updatePlayerUI, filter)
    }


    override fun onPause() {
        super.onPause()
        //mCastContext!!.sessionManager.removeSessionManagerListener(
        //   mSessionManagerListener!!, CastSession::class.java
        // )
    }

    override fun onResume() {
        super.onResume()
        mCastContext!!.addCastStateListener { state ->
            if (state == CastState.NO_DEVICES_AVAILABLE) {
                binding.bottomSheet.castBtn.visibility = View.GONE
                binding.bottomSheet.skipNextAudio1.visibility = View.VISIBLE
            } else {
                binding.bottomSheet.castBtn.visibility = View.VISIBLE
                binding.bottomSheet.skipNextAudio1.visibility = View.GONE
            }
        }

        if (mCastContext!!.castState == CastState.NO_DEVICES_AVAILABLE) {
            binding.bottomSheet.castBtn.visibility = View.GONE
            binding.bottomSheet.skipNextAudio1.visibility = View.VISIBLE
        } else {
            binding.bottomSheet.castBtn.visibility = View.VISIBLE
            binding.bottomSheet.skipNextAudio1.visibility = View.GONE
        }

        CastButtonFactory.setUpMediaRouteButton(this, binding.bottomSheet.castBtn)

        //mCastContext!!.sessionManager.addSessionManagerListener(
        //    mSessionManagerListener!!, CastSession::class.java
        // )
    }


    private fun setupCastListener() {
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
                /* if (null != mSelectedMedia) {
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
                 supportInvalidateOptionsMenu()*/
                // loadRemoteMedia(binding.bottomSheet.seekBar.progress, true)
            }

            private fun onApplicationDisconnected() {
                /* updatePlaybackLocation(PlaybackLocation.LOCAL)
                 mPlaybackState = PlaybackState.IDLE
                 mLocation = PlaybackLocation.LOCAL
                 updatePlayButton(mPlaybackState)
                 supportInvalidateOptionsMenu()*/
                Toast.makeText(
                    this@ActivityBottomBarFragmentContainer,
                    "RemoteServer disconnected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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

}
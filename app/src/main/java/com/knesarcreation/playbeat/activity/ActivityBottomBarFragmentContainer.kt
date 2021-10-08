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
    ArtistsTracksAndAlbumFragment.OnArtistAlbumItemClicked,
    PlaylistsFragment.OnPlayListCategoryClicked {
    private lateinit var binding: ActivityBottomBarFragmentBinding
    private val homeFragment = HomeFragment()
    private val playlistsFragment = PlaylistsFragment()
    private val settingFragment = SettingFragment()
    private val albumSongFragment = AlbumFragment()
    private val searchFragment = SearchFragment()
    private val favouriteAudiosFragment = FavouriteAudiosFragment()
    private val lastAddedAudioFragment = LastAddedAudioFragment()
    private val historyAudiosFragment = HistoryAudiosFragment()
    private val mostPlayedFragment = MostPlayedFragment()
    private val artistsTracksAndAlbumFragment = ArtistsTracksAndAlbumFragment()
    private val customPlaylist = CustomPlaylist()
    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var viewModel: DataObservableClass
    private var queueAudioList = CopyOnWriteArrayList<AllSongsModel>()
    private var isAlbumFragOpened = false
    private var isArtistsFragOpened = false
    private var isPlaylstCategoryOpened = false
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
    private var isContextMenuEnabled = false

    //private var isFavAudioObserved = false
    //private var launchFavAudioJob: Job? = null
    private var _playlistCategory = ""

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

        viewModel.isContextMenuEnabled.observe(this, {
            if (it != null) {
                isContextMenuEnabled = it
                nowPlayingBottomSheetBehavior.isDraggable = !it
            }
        })

        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, homeFragment)
            .add(R.id.fragmentContainer, playlistsFragment)
            .add(R.id.fragmentContainer, settingFragment)
            .add(R.id.fragmentContainer, albumSongFragment)
            .add(R.id.fragmentContainer, artistsTracksAndAlbumFragment)
            .add(R.id.fragmentContainer, searchFragment)
            .add(R.id.fragmentContainer, favouriteAudiosFragment)
            .add(R.id.fragmentContainer, lastAddedAudioFragment)
            .add(R.id.fragmentContainer, historyAudiosFragment)
            .add(R.id.fragmentContainer, mostPlayedFragment)
            .add(R.id.fragmentContainer, customPlaylist)
            .commit()
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

        /*if (AllSongFragment.musicService != null) {
            // if service is bounded
            if (AllSongFragment.musicService?.mediaPlayer != null) {
                audioList = StorageUtil(this).loadAudio()
                audioIndexPos = StorageUtil(this).loadAudioIndex()
                updatePlayingMusic(audioIndexPos)
                updateViews(audioIndexPos)
            }
        }*/

        binding.bottomSheet.seekBar.setOnClickListener { }

        //set resume progress in audio progress bar and seek bar
        binding.bottomSheet.seekBar.max =
            (storage.getLastAudioMaxSeekProg().toDouble() / 1000).toInt()
        binding.bottomSheet.seekBar.progress =
            (storage.getAudioResumePos().toDouble() / 1000).toInt()
        Toast.makeText(
            this,
            "Start: ${(storage.getAudioResumePos().toDouble() / 1000).toInt()}",
            Toast.LENGTH_SHORT
        ).show()

        binding.bottomSheet.startTimeTV.text =
            millisToMinutesAndSeconds(storage.getAudioResumePos())

        showSleepTimerIfEnable()

        binding.bottomSheet.likedAudioIV.setOnClickListener {
            val isFav = !storage.loadQueueAudio()[audioIndexPos].isFavourite
            if (isFav) {
                binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_fill)
                showLikedAudioAnim()
            } else {
                binding.bottomSheet.likedAudioIV.setImageResource(R.drawable.avd_trimclip_heart_break)
                showLikedAudioAnim()
            }
            //binding.bottomSheet.likedAudioIV.isSelected = isFav

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
                    audio.audioUri,
                    audio.artUri,
                    audio.dateAdded,
                    isFav,
                    favAudioAddedTime
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
                    audio.audioUri,
                    audio.artUri,
                    audio.dateAdded,
                    audio.isFavourite,
                    audio.favAudioAddedTime,
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
            nowPlayingBottomSheetBehavior.peekHeight = 350
            binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE
            val layoutParams =
                binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin = 350
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
            if (!isContextMenuEnabled) {
                nowPlayingBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
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
        binding.bottomSheet.skipNextAudio1.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            queueAudioList = storage.loadQueueAudio()
            // remove previous audio highlight
            mViewModelClass.updateSong(
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
            incrementPosByOne()
            storage.storeAudioIndex(audioIndexPos)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            updatePlayingMusic(audioIndexPos)
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

        binding.bottomSheet.skipNextAudio.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            queueAudioList = storage.loadQueueAudio()
            // remove previous audio highlight
            mViewModelClass.updateSong(
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
            binding.bottomSheet.playPauseExpandedPlayer.isSelected = true
        }

        binding.bottomSheet.skipPrevAudio.setOnClickListener {
            audioIndexPos = storage.loadAudioIndex()
            queueAudioList = storage.loadQueueAudio()
            // remove previous audio highlight
            mViewModelClass.updateSong(
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
            binding.bottomSheet.playPauseExpandedPlayer.isSelected = true
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

        binding.bottomSheet.playPauseExpandedPlayer.isSelected =
            AllSongFragment.musicService?.mediaPlayer?.isPlaying!!

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
                        transaction.hide(searchFragment)
                        transaction.hide(favouriteAudiosFragment)
                        transaction.hide(lastAddedAudioFragment)
                        transaction.hide(historyAudiosFragment)
                        transaction.hide(mostPlayedFragment)
                        transaction.hide(customPlaylist)
                        transaction.show(albumSongFragment)
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
                    }
                }
                binding.bottomSheet.bottomBar.itemActiveIndex = 0
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
                binding.bottomSheet.bottomBar.itemActiveIndex = 1
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
                binding.bottomSheet.bottomBar.itemActiveIndex = 2
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
                binding.bottomSheet.bottomBar.itemActiveIndex = 3
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
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
                    }
                }
                1 -> {
                    if (!isContextMenuEnabled) {
                        setBottomBarFragmentState(FragmentState.PLAYLIST).commit()
                        return true
                    } else {
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
                    }
                }
                2 -> {
                    if (!isContextMenuEnabled) {
                        setBottomBarFragmentState(FragmentState.SEARCH).commit()
                        return true
                    } else {
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
                    }
                }
                3 -> {
                    if (!isContextMenuEnabled) {

                        setBottomBarFragmentState(FragmentState.SETTING).commit()
                        return true
                    } else {
                        binding.bottomSheet.bottomBar.itemActiveIndex = 0
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
        PLAYLIST_CATEGORY
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
                } else if (isPlaylstCategoryOpened) {
                    if (!playlistsFragment.isHidden /* if open*/ || !settingFragment.isHidden /*if open*/) {
                        setBottomBarFragmentState(FragmentState.PLAYLIST_CATEGORY).commit()
                        binding.bottomSheet.bottomBar.itemActiveIndex = 1
                    } else {
                        isPlaylstCategoryOpened = false
                        setBottomBarFragmentState(FragmentState.PLAYLIST).commit()
                        binding.bottomSheet.bottomBar.itemActiveIndex = 1
                        //isAlbumOpenedFromArtisFrag = false
                    }
                } else {
                    setBottomBarFragmentState(FragmentState.HOME).commit()
                    binding.bottomSheet.bottomBar.itemActiveIndex = 0
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

    override fun playlistCategory(category: String, mode: Int) {
        _playlistCategory = if (mode == 1) {
            viewModel.customPlaylistData.value = category
            "customPlaylist"
        } else {
            viewModel.playlistCategory.value = category
            category
        }
        setBottomBarFragmentState(FragmentState.PLAYLIST_CATEGORY).commit()
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
                Glide.with(this).load(allSongsModel.artUri).transition(
                    withCrossFade(
                        factory
                    )
                )
                    /* .apply(RequestOptions.placeholderOf(R.drawable.ic_audio_file_placeholder_svg))*/
                    .into(binding.bottomSheet.nowPlayingAlbumArtIV)
                Palette.from(albumArt).generate {
                    val swatch = it?.darkMutedSwatch

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

    private fun decrementPosByOne() {
        audioIndexPos = storage.loadAudioIndex()
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
                    queueAudioList[audioIndexPos].audioUri,
                    queueAudioList[audioIndexPos].artUri,
                    queueAudioList[audioIndexPos].dateAdded,
                    queueAudioList[audioIndexPos].isFavourite,
                    queueAudioList[audioIndexPos].favAudioAddedTime,
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

            if (intent.action == AllSongFragment.Broadcast_UPDATE_MINI_PLAYER) {
                isShuffled = storage.getIsShuffled()
                queueAudioList = storage.loadQueueAudio()
                val bundle = intent.extras

                //show the mini player
                if (storage.getIsAudioPlayedFirstTime()) {
                    storage.saveIsAudioPlayedFirstTime(false)
                    nowPlayingBottomSheetBehavior.peekHeight = 350
                    binding.bottomSheet.rlMiniPlayerBottomsheet.visibility = View.VISIBLE
                    val layoutParams =
                        binding.fragmentContainer.layoutParams as CoordinatorLayout.LayoutParams
                    layoutParams.bottomMargin = 350
                    binding.fragmentContainer.layoutParams = layoutParams

                    nowPlayingBottomSheetBehavior.isDraggable = !isContextMenuEnabled
                }

                if (isShuffled) {
                    binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle_on_24)
                } else {
                    binding.bottomSheet.shuffleSongIV.setImageResource(R.drawable.ic_shuffle)
                }

                /* var prevPlayingAudioIndex = 0
                 if (audioIndexPos != -1) {
                     prevPlayingAudioIndex = audioIndexPos
                 }*/


                //Get the current playing media index form SharedPreferences
                audioIndexPos = storage.loadAudioIndex()

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
                    binding.bottomSheet.audioProgress.max = queueAudioList[audioIndexPos].duration
                    binding.bottomSheet.audioProgress.progress = storage.getAudioResumePos()

                    handleAudioProgressBar()

                    //update ui
                    if (!isDestroyedActivity) {
                        updatePlayingMusic(audioIndexPos)
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
                            val previousRunningAudioIndex = bundle.getInt("index")
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
                                audio.audioUri,
                                audio.artUri,
                                audio.dateAdded,
                                audio.isFavourite,
                                audio.favAudioAddedTime
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
                                allSongsModel.currentPlayedAudioTime = audio.currentPlayedAudioTime
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

            }
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

    private fun showLikedAudioAnim() {
        val animatedVectorDrawable =
            binding.bottomSheet.likedAudioIV.drawable as AnimatedVectorDrawable
        animatedVectorDrawable.start()
    }
}
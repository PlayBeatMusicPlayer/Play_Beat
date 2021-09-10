package com.knesarcreation.playbeat.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.ActivityBottomBarFragmentBinding
import com.knesarcreation.playbeat.fragment.*
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.utils.DataObservableClass
import com.knesarcreation.playbeat.utils.PlaybackStatus
import com.knesarcreation.playbeat.utils.StorageUtil
import me.ibrahimsn.lib.OnItemSelectedListener

class ActivityBottomBarFragmentContainer : AppCompatActivity()/*, ServiceConnection*/,
    AllAlbumsFragment.OnAlbumItemClicked, AllArtistsFragment.OpenArtisFragment {
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
    private var audioList = ArrayList<AllSongsModel>()
    private var isAlbumFragOpened = false
    private var isArtistsFragOpened = false
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityBottomBarFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //navigationController = findNavController(R.id.fragmentContainer)
        //setUpSmoothBottomMenu()

        binding.songNameTV.isSelected = true
        binding.bottomBar.onItemSelectedListener = onItemSelectedListener

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

        if (AllSongFragment.musicService?.mediaPlayer != null) {
            audioList = StorageUtil(this).loadAudio()
            audioIndexPos = StorageUtil(this).loadAudioIndex()
            updatePlayingMusic(audioIndexPos)
        }
    }

    private fun updatePlayingMusic(audioIndex: Int) {
        binding.songNameTV.text = audioList[audioIndex].songName
        binding.artistOrAlbumNameTV.text = audioList[audioIndex].artistsName

        Glide.with(this).load(audioList[audioIndexPos].artUri)
            .apply(
                RequestOptions.placeholderOf(R.drawable.ic_audio_file_placeholder_svg).centerCrop()
            )
            .into(binding.albumArtIV)

        if (AllSongFragment.musicService?.mediaPlayer != null) {
            if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                binding.playPauseIV.setImageResource(R.drawable.ic_pause_audio)
                binding.audioProgress.max = AllSongFragment.musicService?.mediaPlayer?.duration!!
                handleAudioProgress()
            } else {
                binding.playPauseIV.setImageResource(R.drawable.ic_play_audio)
            }
        }
    }

    private fun controlAudio() {
        binding.skipNextAudio.setOnClickListener {
            val audioIndexPos = getSongFromPos()
            val storage = StorageUtil(this)
            storage.storeAudioIndex(audioIndexPos)
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            sendBroadcast(broadcastIntent)
            updatePlayingMusic(audioIndexPos)
//            Toast.makeText(this, "Skipped Next", Toast.LENGTH_SHORT).show()
        }

        binding.playPauseIV.setOnClickListener {
            if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                AllSongFragment.musicService?.pauseMedia()
                AllSongFragment.musicService?.buildNotification(
                    PlaybackStatus.PAUSED,
                    PlaybackStatus.UN_FAVOURITE,
                    0f
                )
                binding.playPauseIV.setImageResource(R.drawable.ic_play_audio)
            } else if (!AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                AllSongFragment.musicService?.resumeMedia()
                binding.playPauseIV.setImageResource(R.drawable.ic_pause_audio)
                AllSongFragment.musicService?.updateMetaData()
                AllSongFragment.musicService?.buildNotification(
                    PlaybackStatus.PLAYING,
                    PlaybackStatus.UN_FAVOURITE,
                    1f
                )
            }
        }
    }

    private fun handleAudioProgress() {
        this.runOnUiThread(object : Runnable {
            override fun run() {
                if (AllSongFragment.musicService?.mediaPlayer != null) {
                    binding.audioProgress.progress =
                        AllSongFragment.musicService?.mediaPlayer?.currentPosition!!
                    handler.postDelayed(this, 1000)
                }
            }

        })
    }

    private fun getSongFromPos(): Int {
        return if (audioIndexPos == audioList.size - 1) {
            //if last in playlist
            //audioIndexPos = 0
            //audioList[audioIndexPos]
            0
        } else {
            //get next in playlist
            /*audioList[++audioIndexPos]*/
            ++audioIndexPos
        }
    }

    private val updatePlayerUI: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val storageUtil = StorageUtil(context)
            audioList = storageUtil.loadAudio()
            Log.d("AudioListFragmentContainerActivity", "onReceive:  $audioList ")

            //Get the new media index form SharedPreferences
            audioIndexPos = storageUtil.loadAudioIndex()
            //UpdatePlayer UI
            if (!isDestroyedActivity)
                updatePlayingMusic(audioIndexPos)
        }
    }

    private fun registerUpdatePlayerUI() {
        //Register playNewMedia receiver
        val filter = IntentFilter(AllSongFragment.Broadcast_BOTTOM_UPDATE_PLAYER_UI)
        registerReceiver(updatePlayerUI, filter)
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
                    isArtistsFragOpened -> {
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
                binding.bottomBar.itemActiveIndex = 0
            }
            FragmentState.FAVOURITE -> {
                transaction.hide(homeFragment)
                transaction.show(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                binding.bottomBar.itemActiveIndex = 1
            }
            FragmentState.SETTING -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.show(settingFragment)
                transaction.hide(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                binding.bottomBar.itemActiveIndex = 2
            }
            FragmentState.ALBUM_FRAGMENT -> {
                transaction.hide(homeFragment)
                transaction.hide(playlistsFragment)
                transaction.hide(settingFragment)
                transaction.show(albumSongFragment)
                transaction.hide(artistsTracksAndAlbumFragment)
                isAlbumFragOpened = true
                isArtistsFragOpened = false
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
        if (supportFragmentManager.backStackEntryCount > 0 || !homeFragment.isHidden) {
            super.onBackPressed()
        } else {
            if (isAlbumFragOpened) {
                if (!playlistsFragment.isHidden || !settingFragment.isHidden) {
                    setBottomBarFragmentState(FragmentState.ALBUM_FRAGMENT).commit()
                    binding.bottomBar.itemActiveIndex = 0
                } else {
                    isAlbumFragOpened = false
                    setBottomBarFragmentState(FragmentState.HOME).commit()
                    binding.bottomBar.itemActiveIndex = 0
                }
            } else if (isArtistsFragOpened) {
                if (!playlistsFragment.isHidden || !settingFragment.isHidden) {
                    setBottomBarFragmentState(FragmentState.ARTIST_TRACK_ALBUM_FRAGMENT).commit()
                    binding.bottomBar.itemActiveIndex = 0
                } else {
                    isArtistsFragOpened = false
                    setBottomBarFragmentState(FragmentState.HOME).commit()
                    binding.bottomBar.itemActiveIndex = 0
                }
            } else {
                setBottomBarFragmentState(FragmentState.HOME).commit()
                binding.bottomBar.itemActiveIndex = 0
            }

        }

    }


    override fun onDestroy() {
        super.onDestroy()
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


    /* private fun setUpSmoothBottomMenu() {
         val popupMenu = PopupMenu(this, null)
         popupMenu.inflate(R.menu.bottom_bar_menu)
         val menu = popupMenu.menu
         binding.bottomBar.setupWithNavController(menu, navigationController)
     }

     override fun onSupportNavigateUp(): Boolean {
         return navigationController.navigateUp() || super.onSupportNavigateUp()
     }*/

}
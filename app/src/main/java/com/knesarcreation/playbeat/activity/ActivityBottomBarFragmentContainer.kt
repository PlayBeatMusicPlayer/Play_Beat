package com.knesarcreation.playbeat.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.ActivityBottomBarFragmentBinding
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.fragment.HomeFragment
import com.knesarcreation.playbeat.fragment.PlaylistsFragment
import com.knesarcreation.playbeat.fragment.SettingFragment
import com.knesarcreation.playbeat.utils.PlaybackStatus
import com.knesarcreation.playbeat.utils.StorageUtil
import me.ibrahimsn.lib.OnItemSelectedListener

class ActivityBottomBarFragmentContainer : AppCompatActivity()/*, ServiceConnection*/ {
    //private lateinit var navigationController: NavController
    private lateinit var binding: ActivityBottomBarFragmentBinding
    private val homeFragment = HomeFragment()
    private val favFragment = PlaylistsFragment()
    private val settingFragment = SettingFragment()
    private var audioIndexPos = -1
    private var isDestroyedActivity = false
    private var handler = Handler(Looper.getMainLooper())
//    private var musicService: PlayBeatMusicService? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityBottomBarFragmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //navigationController = findNavController(R.id.fragmentContainer)
        //setUpSmoothBottomMenu()

        binding.songNameTV.isSelected = true
        binding.bottomBar.onItemSelectedListener = onItemSelectedListener
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, homeFragment)
            .add(R.id.fragmentContainer, favFragment)
            .add(R.id.fragmentContainer, settingFragment)
            .commit()
        setTabStateFragment(TabState.HOME).commit()

        registerUpdatePlayerUI()
        controlAudio()
        audioIndexPos = StorageUtil(this).loadAudioIndex()
        if(AllSongFragment.musicService?.mediaPlayer!=null) {
            updatePlayingMusic(audioIndexPos)
        }
    }

    private fun updatePlayingMusic(audioIndex: Int) {
        binding.songNameTV.text = PlayerActivity.audioList[audioIndex].songName
        binding.artistOrAlbumNameTV.text = PlayerActivity.audioList[audioIndex].artistsName

        Glide.with(this).load(PlayerActivity.audioList[audioIndexPos].artUri)
            .apply(RequestOptions.placeholderOf(R.drawable.music_note_1).centerCrop())
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
                AllSongFragment.musicService?.buildNotification(PlaybackStatus.PAUSED, PlaybackStatus.UN_FAVOURITE, 0f)
                binding.playPauseIV.setImageResource(R.drawable.ic_play_audio)
            } else if (!AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                AllSongFragment.musicService?.playMedia()
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
        return if (audioIndexPos == PlayerActivity.audioList.size - 1) {
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

            //Get the new media index form SharedPreferences
            audioIndexPos = StorageUtil(context).loadAudioIndex()
            //Toast.makeText(this@PlayerActivity, "$audioIndexPos", Toast.LENGTH_SHORT).show()
            //UpdatePlayer UI
            if (!isDestroyedActivity)
                updatePlayingMusic(audioIndexPos)
        }
    }

    private fun registerUpdatePlayerUI() {
        //Register playNewMedia receiver
        val filter = IntentFilter(AllSongFragment.Broadcast_UPDATE_PLAYER_UI)
        registerReceiver(updatePlayerUI, filter)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0 || !homeFragment.isHidden) {
            super.onBackPressed()
        } else {
            setTabStateFragment(TabState.HOME).commit()
            binding.bottomBar.itemActiveIndex = 0
        }
    }

    private fun setTabStateFragment(state: TabState): FragmentTransaction {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
        when (state) {
            TabState.HOME -> {
                transaction.show(homeFragment)
                transaction.hide(favFragment)
                transaction.hide(settingFragment)
                binding.bottomBar.itemActiveIndex = 0
            }
            TabState.FAVOURITE -> {
                transaction.hide(homeFragment)
                transaction.show(favFragment)
                transaction.hide(settingFragment)
                binding.bottomBar.itemActiveIndex = 1
            }
            TabState.SETTING -> {
                transaction.hide(homeFragment)
                transaction.hide(favFragment)
                transaction.show(settingFragment)
                binding.bottomBar.itemActiveIndex = 2
            }
        }
        return transaction
    }

    private val onItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelect(pos: Int): Boolean {
            when (pos) {
                0 -> {
                    setTabStateFragment(TabState.HOME).commit()
                    return true
                }
                1 -> {
                    setTabStateFragment(TabState.FAVOURITE).commit()
                    return true
                }
                2 -> {
                    setTabStateFragment(TabState.SETTING).commit()
                    return true
                }
            }
            return false
        }
    }

    internal enum class TabState {
        HOME,
        FAVOURITE,
        SETTING,
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyedActivity = true
//        AllSongFragment.musicService?.stopSelf()
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
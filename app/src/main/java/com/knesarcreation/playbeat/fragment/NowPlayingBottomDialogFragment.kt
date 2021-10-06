package com.knesarcreation.playbeat.fragment

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.BottomSheetNowPlayingBinding
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.utils.PlaybackStatus
import com.knesarcreation.playbeat.utils.StorageUtil
import com.knesarcreation.playbeat.utils.UriToBitmapConverter
import java.util.concurrent.CopyOnWriteArrayList


class NowPlayingBottomDialogFragment(var mContext: Context) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNowPlayingBinding? = null
    private val binding get() = _binding
    private var audioList = CopyOnWriteArrayList<AllSongsModel>()
    private var audioIndex = -1
    private var handler = Handler(Looper.getMainLooper())
    private var audioRunnable: Runnable? = null
    private var isNotiBuild = false
    private var shuffledList = CopyOnWriteArrayList<AllSongsModel>()
    private var isShuffled = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener { dialog ->
            val dialogc = dialog as BottomSheetDialog
            val bottomSheet: FrameLayout? =
                dialogc.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val bottomSheetBehaviour = BottomSheetBehavior.from(bottomSheet!!)
            bottomSheetBehaviour.peekHeight = Resources.getSystem().displayMetrics.heightPixels
            bottomSheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheet.fitsSystemWindows = true

            val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset < 1) {
                        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_COLLAPSED
                        bottomSheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            }
            bottomSheetBehaviour.addBottomSheetCallback(bottomSheetCallback)
        }
        return bottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = BottomSheetNowPlayingBinding.inflate(inflater, container, false)
        val view = binding?.root

        binding?.songTextTV!!.isSelected = true
        binding?.closeSheetIV!!.setOnClickListener { dismiss() }

        //MakeStatusBarTransparent().transparent(mContext as AppCompatActivity)

        registerUpdatePlayerUI()

        loadAudio()

        updateViews()

        updateAudioController()

        handleSeekBarChangeListener()

        handlePlayPauseNextPrev()

        val storageUtil = StorageUtil(mContext)
        binding?.shuffleSongIV?.setOnClickListener {
            isShuffled = storageUtil.getIsShuffled()
            if (!isShuffled) {
                storageUtil.saveIsShuffled(true)
                shuffledList.addAll(audioList.shuffled())
                audioList.clear()
                audioList.addAll(shuffledList)
                binding?.shuffleSongIV?.setImageResource(R.drawable.ic_shuffle_on_24)
                Log.d("ShuffledaudioList", "onCreateView: ${shuffledList}")
                storageUtil.storeQueueAudio(shuffledList)
                audioIndex = -1

            } else {
                storageUtil.saveIsShuffled(false)
                val index = if (audioIndex == -1) 0 else audioIndex
                val currentPlayingAudio = shuffledList[index]
                val sortedList: List<AllSongsModel> =
                    shuffledList.sortedBy { allSongsModel -> allSongsModel.songName }
                Log.d(
                    "ShuffledaudioList",
                    "onCreateView: $sortedList"
                )
                val currentAudioIndexInSortedList = sortedList.indexOf(currentPlayingAudio)
                audioIndex = currentAudioIndexInSortedList
                shuffledList.clear()
                audioList.clear()
                audioList.addAll(sortedList)
                storageUtil.storeQueueAudio(audioList)
                binding?.shuffleSongIV?.setImageResource(R.drawable.ic_shuffle)
            }
        }

        return view

    }

    private fun handlePlayPauseNextPrev() {
        binding?.playPauseExpandedPlayer!!.setOnClickListener {
            //handleStartTimeAndSeekBar()
            if (AllSongFragment.musicService != null) {
                if (AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                    AllSongFragment.musicService?.pauseMedia()
                    AllSongFragment.musicService?.pausedByManually = true
                    AllSongFragment.musicService?.buildNotification(
                        PlaybackStatus.PAUSED,
                        PlaybackStatus.UN_FAVOURITE,
                        0f
                    )
                    binding!!.playPauseExpandedPlayer.isSelected = false
                } else if (!AllSongFragment.musicService?.mediaPlayer!!.isPlaying) {
                    // resume through button
                    AllSongFragment.musicService?.resumeMedia()
                    AllSongFragment.musicService?.pausedByManually = false
                    binding!!.playPauseExpandedPlayer.isSelected = true
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
                    updateAudioController()
                }
            }
        }

        binding?.skipNextAudio!!.setOnClickListener {
            incrementPosByOne()
            val storage = StorageUtil(mContext)
            storage.storeAudioIndex(audioIndex)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            (mContext as AppCompatActivity).sendBroadcast(broadcastIntent)
            updateViews()
            updateAudioController() // seek bar, start time, end time, play pause btn
            binding?.skipNextAudio?.setImageResource(R.drawable.avd_music_next)
            val animatedVectorDrawable = binding?.skipNextAudio?.drawable as AnimatedVectorDrawable
            animatedVectorDrawable.start()
            binding?.playPauseExpandedPlayer?.isSelected = true
        }

        binding?.skipPrevAudio!!.setOnClickListener {
            decrementPosByOne()
            val storage = StorageUtil(mContext)
            storage.storeAudioIndex(audioIndex)
            AllSongFragment.musicService?.pausedByManually = false
            val broadcastIntent = Intent(AllSongFragment.Broadcast_PLAY_NEW_AUDIO)
            (mContext as AppCompatActivity).sendBroadcast(broadcastIntent)
            updateViews()
            updateAudioController() // seek bar, start time, end time, play pause btn
            binding?.skipPrevAudio?.setImageResource(R.drawable.avd_music_previous)
            val animatedVectorDrawable = binding?.skipPrevAudio?.drawable as AnimatedVectorDrawable
            animatedVectorDrawable.start()
            binding?.playPauseExpandedPlayer?.isSelected = true
        }

    }

    private fun handleSeekBarChangeListener() {
        binding?.seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding?.seekBar?.progress = progress
                    binding?.startTimeTV?.text = millisToMinutesAndSeconds(progress * 1000)
                    if (audioRunnable != null)
                        handler.removeCallbacks(audioRunnable!!)
                }
            }

            override fun onStartTrackingTouch(seekbar: SeekBar?) {}

            override fun onStopTrackingTouch(seekbar: SeekBar?) {
                if (audioRunnable != null) {
                    handler.postDelayed(audioRunnable!!, 1000)
                    AllSongFragment.musicService?.onSeekTo((seekbar?.progress!! * 1000).toLong())
                }
            }
        })
    }

    private fun updateAudioController() {
        if (AllSongFragment.musicService != null) {
            if (AllSongFragment.musicService?.mediaPlayer != null) {
                binding?.seekBar?.max =
                    (audioList[audioIndex].duration.toDouble() / 1000).toInt()
                val audioDuration =
                    millisToMinutesAndSeconds(audioList[audioIndex].duration)
                binding?.endTimeTV?.text = audioDuration

                handleStartTimeAndSeekBar()

                binding!!.playPauseExpandedPlayer.isSelected =
                    AllSongFragment.musicService?.mediaPlayer?.isPlaying!!
            }
        }
    }

    private fun handleStartTimeAndSeekBar() {
        audioRunnable = object : Runnable {
            override fun run() {
                binding?.startTimeTV!!.text =
                    millisToMinutesAndSeconds(AllSongFragment.musicService?.mediaPlayer?.currentPosition!!)
                binding?.seekBar?.progress =
                    (AllSongFragment.musicService?.mediaPlayer?.currentPosition!!.toDouble() / 1000).toInt()
                handler.postDelayed(this, 1000)
            }
        }
        (mContext as AppCompatActivity).runOnUiThread(audioRunnable)
    }

    private fun millisToMinutesAndSeconds(millis: Int): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds "
    }

    private fun updateViews() {
        if (audioIndex != -1) {
            val allSongsModel = audioList[audioIndex]
            binding?.songTextTV!!.text = allSongsModel.songName
            binding?.artistsNameTV!!.text = allSongsModel.artistsName
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            Glide.with(mContext).load(allSongsModel.artUri).transition(withCrossFade(factory))
                /* .apply(RequestOptions.placeholderOf(R.drawable.ic_audio_file_placeholder_svg))*/
                .into(binding!!.nowPlayingAlbumArtIV)

            val albumArt = UriToBitmapConverter.getBitmap(
                (mContext).contentResolver!!,
                allSongsModel.artUri.toUri()
            )

            //setting player background
            if (albumArt != null) {
                Palette.from(albumArt).generate {
                    val swatch = it?.darkVibrantSwatch
                    if (swatch != null) {
                        binding?.albumArtBottomGradient?.setBackgroundResource(R.drawable.gradient_background_bottom_shadow)
                        binding?.bottomBackground!!.setBackgroundResource(R.drawable.app_theme_background_drawable)
                        binding?.albumArtTopGradient!!.setBackgroundResource(R.drawable.gradient_background_top_shadow)

                        val gradientDrawableBottomTop = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(swatch.rgb, 0x00000000)
                        )
                        val gradientDrawableTopBottom = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(swatch.rgb, swatch.population)
                        )
                        binding?.albumArtBottomGradient!!.background = gradientDrawableBottomTop
                        binding?.albumArtTopGradient!!.background = gradientDrawableTopBottom
//                        Glide.with(mContext).load(gradientDrawableBottomTop)
//                            .transition(withCrossFade(factory))
//                            .into(binding!!.albumArtBottomGradient)
//
//                        Glide.with(mContext).load(gradientDrawableTopBottom)
//                            .transition(withCrossFade(factory))
//                            .into(binding!!.albumArtTopGradient)

                        // parent background
                        val gradientDrawableParent = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(swatch.rgb, swatch.rgb)
                        )

//                        Glide.with(mContext).load(gradientDrawableParent)
//                            .transition(withCrossFade(factory))
//                            .into(binding!!.bottomBackground)
                        binding?.bottomBackground!!.background = gradientDrawableParent

                    }
                }

            } else {
                val originalBitmap = BitmapFactory.decodeResource(
                    mContext.resources,
                    R.drawable.audio_icon_placeholder
                )
                Glide.with(mContext).load(originalBitmap)
                    .transition(withCrossFade(factory))
                    .into(binding!!.nowPlayingAlbumArtIV)

                Palette.from(originalBitmap).generate {
                    val swatch = it?.dominantSwatch
                    if (swatch != null) {
                        binding?.albumArtBottomGradient?.setBackgroundResource(R.drawable.gradient_background_bottom_shadow)
                        binding?.bottomBackground!!.setBackgroundResource(R.drawable.app_theme_background_drawable)
                        binding?.albumArtTopGradient!!.setBackgroundResource(R.drawable.gradient_background_top_shadow)

                        val gradientDrawableBottomTop = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(swatch.rgb, 0x00000000)
                        )
                        val gradientDrawableTopBottom = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(swatch.rgb, swatch.population)
                        )
                        binding?.albumArtBottomGradient!!.background = gradientDrawableBottomTop
                        binding?.albumArtTopGradient!!.background = gradientDrawableTopBottom


                        // parent background
                        val gradientDrawableParent = GradientDrawable(
                            GradientDrawable.Orientation.BOTTOM_TOP,
                            intArrayOf(swatch.rgb, swatch.rgb)
                        )
                        binding?.bottomBackground!!.background = gradientDrawableParent

                    }
                }

            }
        }
    }

    /*private fun incrementPosByOne(): Int {
        return if (audioIndex == audioList.size - 1) {
            //if last in playlist
            0
        } else {
            //get next in playlist
            ++audioIndex
        }
    }*/

    private fun incrementPosByOne() {
        if (audioIndex != audioList.size - 1) {
            ++audioIndex
        } else {
            audioIndex = -1
            ++audioIndex
        }
        Toast.makeText(mContext, "$audioIndex  size: ${audioList.size - 1}", Toast.LENGTH_SHORT)
            .show()
    }

    private fun decrementPosByOne() {
        if (audioIndex != 0) {
            --audioIndex
        } else {
            audioIndex = audioList.size
            --audioIndex
        }
        Toast.makeText(mContext, "$audioIndex  size: ${audioList.size - 1}", Toast.LENGTH_SHORT)
            .show()
    }

    private fun loadAudio() {
        val storageUtil = StorageUtil(mContext)
        isShuffled = storageUtil.getIsShuffled()
        if (isShuffled) {
            binding?.shuffleSongIV?.setImageResource(R.drawable.ic_shuffle_on_24)
        } else {
            binding?.shuffleSongIV?.setImageResource(R.drawable.ic_shuffle)
        }
        audioList.clear()
        audioList = storageUtil.loadQueueAudio()
        audioIndex = storageUtil.loadAudioIndex()
    }

    private val updatePlayerUI: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val storageUtil = StorageUtil(context)
            //audioList = storageUtil.loadAudio()
            Log.d("AudioListFragmentContainerActivity", "onReceive:  $audioList ")

            // AllSongFragment.musicService?.pausedByManually = false
            //Get the new media index form SharedPreferences
            audioIndex = storageUtil.loadAudioIndex()
            //UpdatePlayer UI

            updateViews()
            updateAudioController()

        }
    }

    private fun registerUpdatePlayerUI() {
        //Register playNewMedia receiver
        val filter = IntentFilter(AllSongFragment.Broadcast_UPDATE_MINI_PLAYER)
        (mContext as AppCompatActivity).registerReceiver(updatePlayerUI, filter)
    }
}
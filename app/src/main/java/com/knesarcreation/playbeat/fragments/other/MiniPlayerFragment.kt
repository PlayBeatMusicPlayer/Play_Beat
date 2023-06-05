package com.knesarcreation.playbeat.fragments.other

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.google.android.material.textview.MaterialTextView
import com.knesarcreation.appthemehelper.ThemeStore
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.FragmentMiniPlayerBinding
import com.knesarcreation.playbeat.dialogs.SleepTimerDialog
import com.knesarcreation.playbeat.extensions.*
import com.knesarcreation.playbeat.fragments.base.AbsMusicServiceFragment
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatColoredTarget
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.MusicProgressViewUpdateHelper
import com.knesarcreation.playbeat.interfaces.IPlaybackStateChanged
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor
import com.knesarcreation.playbeat.util.theme.ThemeMode
import java.lang.StringBuilder
import kotlin.math.abs

open class MiniPlayerFragment : AbsMusicServiceFragment(R.layout.fragment_mini_player),
    MusicProgressViewUpdateHelper.Callback, View.OnClickListener,
    SleepTimerDialog.ISleepTimerCallback {

    private var sleepCountDownTimer: CountDownTimer? = null
    private var colorFinal = 0

    override fun onSleepTimerStart() {
        binding.shadowIV.visibility = View.VISIBLE
        binding.sleepTimeTV.visibility = View.VISIBLE
        binding.sleepTimeTextTV.visibility = View.VISIBLE

        PreferenceUtil.isSleepTimeEnable = true
        timerUpdater(
            binding.shadowIV,
            binding.sleepTimeTV,
            binding.sleepTimeTextTV,
            PreferenceUtil.nextSleepTimerElapsedRealTime.toLong(),
            "oldSleepTime"
        )

    }

    override fun onSleepTimerCancel() {
        if (sleepCountDownTimer != null) {
            binding.shadowIV.visibility = View.GONE
            binding.sleepTimeTV.visibility = View.GONE
            binding.sleepTimeTextTV.visibility = View.GONE

            PreferenceUtil.isSleepTimeEnable = false
            PreferenceUtil.sleepTime = 0

            sleepCountDownTimer?.cancel()
        }
    }

    companion object {
        var iPlaybackStateChanged: IPlaybackStateChanged? = null

        @JvmStatic
        fun newInstance(): MiniPlayerFragment {
            return MiniPlayerFragment()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            iPlaybackStateChanged = context as IPlaybackStateChanged
        } catch (e: ClassCastException) {
            //Log.d("OnStateChanged", "onStateChanged: null ")
            e.printStackTrace()
        }
    }

    private var _binding: FragmentMiniPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.actionNext -> MusicPlayerRemote.playNextSong()
            R.id.actionPrevious -> MusicPlayerRemote.back()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMiniPlayerBinding.inflate(inflater, container, false)
        val view = binding.root
        view.setOnTouchListener(FlingPlayBackController(requireContext()))
        setUpMiniPlayer()

        if (PlayBeatUtil.isTablet()) {
            binding.actionNext.show()
            binding.actionPrevious.show()
        } else {
            binding.actionNext.isVisible = PreferenceUtil.isExtraControls
            binding.actionPrevious.isVisible = PreferenceUtil.isExtraControls
        }
        binding.actionNext.setOnClickListener(this)
        binding.actionPrevious.setOnClickListener(this)

        SleepTimerDialog.sleepListener = this

        Log.d(
            "newSleepTime",
            "onCreateView:${PreferenceUtil.sleepTime} , ${PreferenceUtil.isSleepTimeEnable} "
        )
        if (PreferenceUtil.isSleepTimeEnable) {
            binding.shadowIV.visibility = View.VISIBLE
            binding.sleepTimeTV.visibility = View.VISIBLE
            binding.sleepTimeTextTV.visibility = View.VISIBLE
            timerUpdater(
                binding.shadowIV,
                binding.sleepTimeTV,
                binding.sleepTimeTextTV,
                PreferenceUtil.sleepTime,
                "newSleepTime"
            )
        }
        binding.shadowIV.setOnClickListener {
            SleepTimerDialog().show(
                parentFragmentManager,
                "SLEEP_TIMER"
            )
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    private fun setUpMiniPlayer() {
        setUpPlayPauseButton()
        binding.progressBar.accentColor()
    }

    private fun setUpPlayPauseButton() {
        binding.miniPlayerPlayPauseButton.setOnClickListener {
            //listener?.onStateChanged()
            if (MusicPlayerRemote.isPlaying) {
                MusicPlayerRemote.pauseSong()
            } else {
                MusicPlayerRemote.resumePlaying()
            }
        }
    }

    private fun updateSongTitle() {

        val song = MusicPlayerRemote.currentSong

        val builder = StringBuilder()

        /*val title = song.title.toSpannable()
        title.setSpan(ForegroundColorSpan(colorFinal), 0, title.length, 0)

        val text = song.artistName.toSpannable()
        text.setSpan(ForegroundColorSpan(colorFinal), 0, text.length, 0)*/

        builder.append(song.title).append(" • ").append(song.artistName)

        binding.miniPlayerTitle.isSelected = true
        binding.miniPlayerTitle.text = builder

//        binding.title.isSelected = true
//        binding.title.text = song.title
//        binding.text.isSelected = true
//        binding.text.text = song.artistName
    }

    private fun updateSongCover() {
        val song = MusicPlayerRemote.currentSong
        /*GlideApp.with(requireContext())
            .load(PlayBeatGlideExtension.getSongModel(song))
            .transition(PlayBeatGlideExtension.getDefaultTransition())
            .songCoverOptions(song)
            .into(binding.image)*/
        GlideApp.with(this)
            .asBitmapPalette()
            .songCoverOptions(song)
            //.checkIgnoreMediaStore()
            .load(PlayBeatGlideExtension.getSongModel(song))
            .dontAnimate()
            .into(object : PlayBeatColoredTarget(binding.image) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    /*setColor(colors)*/
                    val drawable = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(
                            colors.backgroundColor,
                            colors.backgroundColor
                        )
                    )
                    drawable.cornerRadius = 20f

                    binding.miniPlayerBg.background = drawable

                    colorFinal = if (PreferenceUtil.isAdaptiveColor) {
                        colors.primaryTextColor
                    } else {
                        ThemeStore.accentColor(requireContext())
                    }.ripAlpha()
                    binding.progressBar.applyColor(colorFinal)
                    binding.miniPlayerTitle.setTextColor(colorFinal)
                    binding.miniPlayerPlayPauseButton.setColorFilter(colorFinal)
                }
            })
    }

    override fun onServiceConnected() {
        updateSongTitle()
        updateSongCover()
        updatePlayPauseDrawableState()
    }

    override fun onPlayingMetaChanged() {
        updateSongTitle()
        updateSongCover()
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        binding.progressBar.max = total
        val animator = ObjectAnimator.ofInt(binding.progressBar, "progress", progress)
        animator.duration = 1000
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
        when (App.getContext().generalThemeValue) {
            ThemeMode.LIGHT -> binding.shadowIV.setImageResource(R.drawable.shadow_full_light)

            ThemeMode.DARK -> binding.shadowIV.setImageResource(R.drawable.shadow_full_dark)

            ThemeMode.BLACK -> binding.shadowIV.setImageResource(R.drawable.shadow_full_dark)

            ThemeMode.AUTO -> binding.shadowIV.setImageResource(R.drawable.shadow_up_full_follow_system)
        }

    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    protected fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.miniPlayerPlayPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    class FlingPlayBackController(context: Context) : View.OnTouchListener {

        private var flingPlayBackController = GestureDetector(context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (abs(velocityX) > abs(velocityY)) {
                        if (velocityX < 0) {
                            MusicPlayerRemote.playNextSong()
                            return true
                        } else if (velocityX > 0) {
                            MusicPlayerRemote.playPreviousSong()
                            return true
                        }
                    }
                    return false
                }
            })

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return flingPlayBackController.onTouchEvent(event)
        }
    }

    private fun timerUpdater(
        shadowIV: AppCompatImageView,
        sleepTimeTV: MaterialTextView,
        sleepTimeTextTV: ImageView,
        sleepTime: Long,
        log: String

    ) {
        if (sleepCountDownTimer != null) {
            sleepCountDownTimer?.cancel()
            Log.d("SleepTime..", "onViewCreated: canceled")
        }

        sleepCountDownTimer = object : CountDownTimer(
            sleepTime,
            1000
        ) {

            override fun onTick(millisUntilFinished: Long) {
                Log.d(log, "onTick: ${millisToMinutesAndSeconds(millisUntilFinished)} ")
                sleepTimeTV.text = millisToMinutesAndSeconds(millisUntilFinished)
                PreferenceUtil.sleepTime = millisUntilFinished
            }

            override fun onFinish() {
                shadowIV.visibility = View.GONE
                sleepTimeTV.visibility = View.GONE
                sleepTimeTextTV.visibility = View.GONE
                PreferenceUtil.isSleepTimeEnable = false
                //MusicPlayerRemote.pauseSong()
                Log.d("SleepTimerUpdateFinished", "onTick: Finished ")
            }

        }.start()
    }

    private fun millisToMinutesAndSeconds(millis: Long): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds.toInt() == 60) "${(minutes.toInt() + 1)} : 00" else "${minutes.toInt()} : ${if (seconds < 10) "0" else ""}$seconds "
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //_binding = null
        // sleepCountDownTimer?.cancel()
        // Log.d("SleepTimeOnDestroyMiniPlayer", "onDestroyView: sleep Timer Canceled")
    }

}

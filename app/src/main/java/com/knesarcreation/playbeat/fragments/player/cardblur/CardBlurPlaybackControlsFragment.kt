package com.knesarcreation.playbeat.fragments.player.cardblur

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.knesarcreation.appthemehelper.util.ColorUtil
import com.knesarcreation.appthemehelper.util.TintHelper
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.FragmentCardBlurPlayerPlaybackControlsBinding
import com.knesarcreation.playbeat.extensions.applyColor
import com.knesarcreation.playbeat.extensions.getSongInfo
import com.knesarcreation.playbeat.extensions.hide
import com.knesarcreation.playbeat.extensions.show
import com.knesarcreation.playbeat.fragments.base.AbsPlayerControlsFragment
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.PlayPauseButtonOnClickHandler
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor

class CardBlurPlaybackControlsFragment :
    AbsPlayerControlsFragment(R.layout.fragment_card_blur_player_playback_controls) {

    private var _binding: FragmentCardBlurPlayerPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val progressSlider: SeekBar
        get() = binding.progressSlider

    override val shuffleButton: ImageButton
        get() = binding.mediaButton.shuffleButton

    override val repeatButton: ImageButton
        get() = binding.mediaButton.repeatButton

    override val nextButton: ImageButton
        get() = binding.mediaButton.nextButton

    override val previousButton: ImageButton
        get() = binding.mediaButton.previousButton

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCardBlurPlayerPlaybackControlsBinding.bind(view)
        setUpPlayPauseFab()
        binding.progressSlider.applyColor(Color.WHITE)
    }

    override fun setColor(color: MediaNotificationProcessor) {
        lastPlaybackControlsColor = Color.WHITE
        lastDisabledPlaybackControlsColor = ColorUtil.withAlpha(Color.WHITE, 0.3f)

        updateRepeatState()
        updateShuffleState()
        updatePrevNextColor()
        updateProgressTextColor()

        volumeFragment?.tintWhiteColor()
    }

    private fun setUpPlayPauseFab() {
        binding.mediaButton.playPauseButton.apply {
            TintHelper.setTintAuto(this, Color.WHITE, true)
            TintHelper.setTintAuto(this, Color.BLACK, false)
            setOnClickListener(PlayPauseButtonOnClickHandler())
        }
    }

    private fun updatePlayPauseDrawableState() {
        when {
            MusicPlayerRemote.isPlaying -> binding.mediaButton.playPauseButton.setImageResource(R.drawable.ic_pause)
            else -> binding.mediaButton.playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_32dp)
        }
    }

    private fun updateProgressTextColor() {
        val color = Color.WHITE
        binding.songTotalTime.setTextColor(color)
        binding.songCurrentProgress.setTextColor(color)
        binding.songInfo.setTextColor(color)
    }

    override fun onServiceConnected() {
        updatePlayPauseDrawableState()
        updateRepeatState()
        updateShuffleState()
        updateSong()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
    }

    private fun updateSong() {
        if (PreferenceUtil.isSongInfo) {
            binding.songInfo.text = getSongInfo(MusicPlayerRemote.currentSong)
            binding.songInfo.show()
        } else {
            binding.songInfo.hide()
        }
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onRepeatModeChanged() {
        updateRepeatState()
    }

    override fun onShuffleModeChanged() {
        updateShuffleState()
    }

    public override fun show() {
        binding.mediaButton.playPauseButton.animate()
            .scaleX(1f)
            .scaleY(1f)
            .rotation(360f)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    public override fun hide() {
        binding.mediaButton.playPauseButton.apply {
            scaleX = 0f
            scaleY = 0f
            rotation = 0f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

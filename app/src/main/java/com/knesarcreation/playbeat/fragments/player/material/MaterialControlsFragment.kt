package com.knesarcreation.playbeat.fragments.player.material

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.knesarcreation.appthemehelper.util.ATHUtil
import com.knesarcreation.appthemehelper.util.MaterialValueHelper
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.FragmentMaterialPlaybackControlsBinding
import com.knesarcreation.playbeat.extensions.*
import com.knesarcreation.playbeat.fragments.base.AbsPlayerControlsFragment
import com.knesarcreation.playbeat.fragments.base.goToAlbum
import com.knesarcreation.playbeat.fragments.base.goToArtist
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.PlayPauseButtonOnClickHandler
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor
import com.knesarcreation.playbeat.extensions.ripAlpha
import com.knesarcreation.playbeat.extensions.textColorSecondary

/**
 * @author Hemanth S (h4h13).
 */
class MaterialControlsFragment :
    AbsPlayerControlsFragment(R.layout.fragment_material_playback_controls) {


    private var _binding: FragmentMaterialPlaybackControlsBinding? = null
    private val binding get() = _binding!!

    override val progressSlider: SeekBar
        get() = binding.progressSlider

    override val shuffleButton: ImageButton
        get() = binding.shuffleButton

    override val repeatButton: ImageButton
        get() = binding.repeatButton

    override val nextButton: ImageButton
        get() = binding.nextButton

    override val previousButton: ImageButton
        get() = binding.previousButton

    override val songTotalTime: TextView
        get() = binding.songTotalTime

    override val songCurrentProgress: TextView
        get() = binding.songCurrentProgress

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMaterialPlaybackControlsBinding.bind(view)
        setUpPlayPauseFab()
        binding.title.isSelected = true
        binding.text.isSelected = true
        binding.title.setOnClickListener {
            goToAlbum(requireActivity())
        }
        binding.text.setOnClickListener {
            goToArtist(requireActivity())
        }
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.title.text = song.title
        binding.text.text = song.artistName

        if (PreferenceUtil.isSongInfo) {
            binding.songInfo.text = getSongInfo(song)
            binding.songInfo.show()
        } else {
            binding.songInfo.hide()
        }
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

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onRepeatModeChanged() {
        updateRepeatState()
    }

    override fun onShuffleModeChanged() {
        updateShuffleState()
    }

    override fun setColor(color: MediaNotificationProcessor) {
        if (ATHUtil.isWindowBackgroundDark(requireContext())) {
            lastPlaybackControlsColor =
                MaterialValueHelper.getPrimaryTextColor(requireContext(), false)
            lastDisabledPlaybackControlsColor =
                MaterialValueHelper.getPrimaryDisabledTextColor(requireContext(), false)
        } else {
            lastPlaybackControlsColor =
                MaterialValueHelper.getSecondaryTextColor(requireContext(), true)
            lastDisabledPlaybackControlsColor =
                MaterialValueHelper.getSecondaryDisabledTextColor(requireContext(), true)
        }
        updateRepeatState()
        updateShuffleState()

        val colorFinal = if (PreferenceUtil.isAdaptiveColor) {
            lastPlaybackControlsColor
        } else {
            textColorSecondary()
        }.ripAlpha()

        binding.text.setTextColor(colorFinal)
        binding.progressSlider.applyColor(colorFinal)

        volumeFragment?.setTintable(colorFinal)

        updateRepeatState()
        updateShuffleState()
        updatePlayPauseColor()
        updatePrevNextColor()
    }

    private fun updatePlayPauseColor() {
        binding.playPauseButton.setColorFilter(lastPlaybackControlsColor, PorterDuff.Mode.SRC_IN)
    }

    private fun setUpPlayPauseFab() {
        binding.playPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    private fun updatePlayPauseDrawableState() {
        if (MusicPlayerRemote.isPlaying) {
            binding.playPauseButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_pause_outline
                )
            )
        } else if (!MusicPlayerRemote.isPlaying) {
            binding.playPauseButton.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_play_arrow_outline
                )
            )
        }
    }

    public override fun show() {}

    public override fun hide() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

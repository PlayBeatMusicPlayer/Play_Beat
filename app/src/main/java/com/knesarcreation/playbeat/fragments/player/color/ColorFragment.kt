package com.knesarcreation.playbeat.fragments.player.color

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.animation.doOnEnd
import com.knesarcreation.appthemehelper.util.ToolbarContentTintHelper
import com.knesarcreation.playbeat.NATIVE_AD_NOW_PLAYING
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.ads.NativeAdHelper
import com.knesarcreation.playbeat.databinding.FragmentColorPlayerBinding
import com.knesarcreation.playbeat.extensions.colorControlNormal
import com.knesarcreation.playbeat.extensions.drawAboveSystemBars
import com.knesarcreation.playbeat.fragments.base.AbsPlayerFragment
import com.knesarcreation.playbeat.fragments.player.PlayerAlbumCoverFragment
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor
import java.lang.Exception

class ColorFragment : AbsPlayerFragment(R.layout.fragment_color_player) {

    private var lastColor: Int = 0
    private var navigationColor: Int = 0
    private lateinit var playbackControlsFragment: ColorPlaybackControlsFragment
    private var valueAnimator: ValueAnimator? = null
    private var _binding: FragmentColorPlayerBinding? = null
    private val binding get() = _binding

    override fun playerToolbar(): Toolbar? {
        return binding?.playerToolbar
    }

    override val paletteColor: Int
        get() = navigationColor

    override fun onColorChanged(color: MediaNotificationProcessor) {
        libraryViewModel.updateColor(color.backgroundColor)
        lastColor = color.secondaryTextColor
        playbackControlsFragment.setColor(color)
        navigationColor = color.backgroundColor

        try {
            binding?.colorGradientBackground?.setBackgroundColor(color.backgroundColor)
            val animator =
                binding?.let { playbackControlsFragment.createRevealAnimator(it.colorGradientBackground) }
            animator?.doOnEnd {
                _binding?.root?.setBackgroundColor(color.backgroundColor)
            }
            animator?.start()
            binding?.playerToolbar?.post {
                ToolbarContentTintHelper.colorizeToolbar(
                    binding?.playerToolbar,
                    color.secondaryTextColor,
                    requireActivity()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun onShow() {
        playbackControlsFragment.show()
    }

    override fun onHide() {
        binding?.adFrame.let {
            it?.let { it1 ->
                NativeAdHelper(requireContext()).refreshAd(
                    it1, NATIVE_AD_NOW_PLAYING
                )
            }
        }
        playbackControlsFragment.hide()
        onBackPressed()
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun toolbarIconColor(): Int {
        return lastColor
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (valueAnimator != null) {
            valueAnimator!!.cancel()
            valueAnimator = null
        }
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentColorPlayerBinding.bind(view)
        setUpSubFragments()
        setUpPlayerToolbar()
        val playerAlbumCoverFragment =
            childFragmentManager.findFragmentById(R.id.playerAlbumCoverFragment) as PlayerAlbumCoverFragment
        playerAlbumCoverFragment.setCallbacks(this)
        playerToolbar()?.drawAboveSystemBars()
        binding?.adFrame.let {
            if (it != null) {
                NativeAdHelper(requireContext()).refreshAd(it, NATIVE_AD_NOW_PLAYING)
            }
        }

    }

    private fun setUpSubFragments() {
        playbackControlsFragment =
            childFragmentManager.findFragmentById(R.id.playbackControlsFragment) as ColorPlaybackControlsFragment
    }

    private fun setUpPlayerToolbar() {
        binding?.playerToolbar?.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressed() }
            setOnMenuItemClickListener(this@ColorFragment)
            ToolbarContentTintHelper.colorizeToolbar(
                this,
                colorControlNormal(),
                requireActivity()
            )
        }
    }

    companion object {
        fun newInstance(): ColorFragment {
            return ColorFragment()
        }
    }
}

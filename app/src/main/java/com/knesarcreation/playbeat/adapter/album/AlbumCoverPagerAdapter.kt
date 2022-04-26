package com.knesarcreation.playbeat.adapter.album

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.MainActivity
import com.knesarcreation.playbeat.fragments.AlbumCoverStyle
import com.knesarcreation.playbeat.fragments.NowPlayingScreen.*
import com.knesarcreation.playbeat.fragments.base.goToLyrics
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatColoredTarget
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.misc.CustomFragmentStatePagerAdapter
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.MusicUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumCoverPagerAdapter(
    fragmentManager: FragmentManager,
    private val dataSet: List<Song>
) : CustomFragmentStatePagerAdapter(fragmentManager) {

    private var currentColorReceiver: AlbumCoverFragment.ColorReceiver? = null
    private var currentColorReceiverPosition = -1

    override fun getItem(position: Int): Fragment {
        return AlbumCoverFragment.newInstance(dataSet[position])
    }

    override fun getCount(): Int {
        return dataSet.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val o = super.instantiateItem(container, position)
        if (currentColorReceiver != null && currentColorReceiverPosition == position) {
            receiveColor(currentColorReceiver!!, currentColorReceiverPosition)
        }
        return o
    }

    /**
     * Only the latest passed [AlbumCoverFragment.ColorReceiver] is guaranteed to receive a
     * response
     */
    fun receiveColor(colorReceiver: AlbumCoverFragment.ColorReceiver, position: Int) {

        if (getFragment(position) is AlbumCoverFragment) {
            val fragment = getFragment(position) as AlbumCoverFragment
            currentColorReceiver = null
            currentColorReceiverPosition = -1
            fragment.receiveColor(colorReceiver, position)
        } else {
            currentColorReceiver = colorReceiver
            currentColorReceiverPosition = position
        }
    }

    class AlbumCoverFragment : Fragment() {

        private lateinit var albumCover: ImageView
        private var isColorReady: Boolean = false
        private lateinit var color: MediaNotificationProcessor
        private lateinit var song: Song
        private var colorReceiver: ColorReceiver? = null
        private var request: Int = 0
        private val mainActivity get() = activity as MainActivity

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if (arguments != null) {
                song = requireArguments().getParcelable(SONG_ARG)!!
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(getLayoutWithPlayerTheme(), container, false)
            view.setTransitionName("lyrics")
            albumCover = view.findViewById(R.id.player_image)
            view.setOnClickListener {
                if (mainActivity.getBottomSheetBehavior().state == STATE_EXPANDED) {
                    showLyricsDialog()
                }
            }
            return view
        }

        private fun showLyricsDialog() {
            lifecycleScope.launch(Dispatchers.IO) {
                val data: String? = MusicUtil.getLyrics(song)
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(
                        requireContext(),
                        R.style.ThemeOverlay_MaterialComponents_Dialog_Alert
                    ).apply {
                        setTitle(song.title)
                        setMessage(if (data.isNullOrEmpty()) "No lyrics found" else data)
                        setNegativeButton(R.string.synced_lyrics) { _, _ ->
                            goToLyrics(requireActivity())
                        }
                        show()
                    }
                }
            }
        }

        private fun getLayoutWithPlayerTheme(): Int {
            return when (PreferenceUtil.nowPlayingScreen) {
                Fit, Gradient, Full -> R.layout.fragment_album_full_cover

                else -> {
                    if (PreferenceUtil.isCarouselEffect) {
                        R.layout.fragment_album_carousel_cover
                    } else {
                        when (PreferenceUtil.albumCoverStyle) {
                            AlbumCoverStyle.Normal -> R.layout.fragment_album_cover
                            AlbumCoverStyle.Flat -> R.layout.fragment_album_flat_cover
                            AlbumCoverStyle.Circle -> R.layout.fragment_album_circle_cover
                            AlbumCoverStyle.Card -> R.layout.fragment_album_card_cover
                            AlbumCoverStyle.Full -> R.layout.fragment_album_full_cover
                            AlbumCoverStyle.FullCard -> R.layout.fragment_album_full_card_cover
                        }
                    }
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            loadAlbumCover()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            colorReceiver = null
        }

        private fun loadAlbumCover() {
            GlideApp.with(this).asBitmapPalette().songCoverOptions(song)
                //.checkIgnoreMediaStore()
                .load(PlayBeatGlideExtension.getSongModel(song))
                .dontAnimate()
                .into(object : PlayBeatColoredTarget(albumCover) {
                    override fun onColorReady(colors: MediaNotificationProcessor) {
                        setColor(colors)
                    }
                })
        }

        private fun setColor(color: MediaNotificationProcessor) {
            this.color = color
            isColorReady = true
            if (colorReceiver != null) {
                colorReceiver!!.onColorReady(color, request)
                colorReceiver = null
            }
        }

        internal fun receiveColor(colorReceiver: ColorReceiver, request: Int) {
            if (isColorReady) {
                colorReceiver.onColorReady(color, request)
            } else {
                this.colorReceiver = colorReceiver
                this.request = request
            }
        }

        interface ColorReceiver {
            fun onColorReady(color: MediaNotificationProcessor, request: Int)
        }

        companion object {

            private const val SONG_ARG = "song"

            fun newInstance(song: Song): AlbumCoverFragment {
                val frag = AlbumCoverFragment()
                frag.arguments = bundleOf(SONG_ARG to song)
                return frag
            }
        }
    }

    companion object {
        val TAG: String = AlbumCoverPagerAdapter::class.java.simpleName
    }
}

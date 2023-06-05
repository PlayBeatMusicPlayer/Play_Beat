package com.knesarcreation.playbeat.fragments.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.EXTRA_ALBUM_ID
import com.knesarcreation.playbeat.EXTRA_ARTIST_ID
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.MainActivity
import com.knesarcreation.playbeat.activities.tageditor.AbsTagEditorActivity
import com.knesarcreation.playbeat.activities.tageditor.SongTagEditorActivity
import com.knesarcreation.playbeat.db.PlaylistEntity
import com.knesarcreation.playbeat.db.toSongEntity
import com.knesarcreation.playbeat.dialogs.*
import com.knesarcreation.playbeat.extensions.currentFragment
import com.knesarcreation.playbeat.extensions.hide
import com.knesarcreation.playbeat.extensions.keepScreenOn
import com.knesarcreation.playbeat.extensions.whichFragment
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.fragments.player.PlayerAlbumCoverFragment
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.interfaces.IPaletteColorHolder
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.repository.RealRepository
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.util.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import kotlin.math.abs

abstract class AbsPlayerFragment(@LayoutRes layout: Int) : AbsMainActivityFragment(layout),
    Toolbar.OnMenuItemClickListener, IPaletteColorHolder, PlayerAlbumCoverFragment.Callbacks {

    private var playerAlbumCoverFragment: PlayerAlbumCoverFragment? = null

    override fun onMenuItemClick(
        item: MenuItem
    ): Boolean {
        val song = MusicPlayerRemote.currentSong
        when (item.itemId) {
            /* R.id.action_playback_speed -> {
                 PlaybackSpeedDialog.newInstance().show(childFragmentManager, "PLAYBACK_SETTINGS")
                 return true
             }*/
            R.id.action_toggle_lyrics -> {
                PreferenceUtil.showLyrics = !PreferenceUtil.showLyrics
                showLyricsIcon(item)
                if (PreferenceUtil.lyricsScreenOn && PreferenceUtil.showLyrics) {
                    mainActivity.keepScreenOn(true)
                } else if (!PreferenceUtil.isScreenOnEnabled && !PreferenceUtil.showLyrics) {
                    mainActivity.keepScreenOn(false)
                }
                return true
            }
            R.id.action_go_to_lyrics -> {
                goToLyrics(requireActivity())
                return true
            }
            R.id.action_toggle_favorite -> {
                toggleFavorite(song)
                return true
            }
            R.id.action_share -> {
                startActivity(Intent.createChooser(song.let {
                    MusicUtil.createShareSongFileIntent(
                        it,
                        requireContext()
                    )
                }, null))
                return true
            }

            R.id.action_delete_from_device -> {
                DeleteSongsDialog.create(song).show(childFragmentManager, "DELETE_SONGS")
                return true
            }
            R.id.action_add_to_playlist -> {
                lifecycleScope.launch(IO) {
                    val playlists = get<RealRepository>().fetchPlaylists()
                    withContext(Main) {
                        AddToPlaylistDialog.create(playlists, song)
                            .show(childFragmentManager, "ADD_PLAYLIST")
                    }
                }
                return true
            }
            R.id.action_clear_playing_queue -> {
                MusicPlayerRemote.clearQueue()
                return true
            }
            R.id.action_save_playing_queue -> {
                CreatePlaylistDialog.create(ArrayList(MusicPlayerRemote.playingQueue))
                    .show(childFragmentManager, "ADD_TO_PLAYLIST")
                return true
            }
            R.id.action_tag_editor -> {
                val intent = Intent(activity, SongTagEditorActivity::class.java)
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id)
                startActivity(intent)
                return true
            }
            R.id.action_details -> {
                SongDetailDialog.create(song).show(childFragmentManager, "SONG_DETAIL")
                return true
            }
            /* R.id.action_go_to_album -> {
                 //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
                 mainActivity.setBottomNavVisibility(false)
                 mainActivity.collapsePanel()
                 requireActivity().findNavController(R.id.fragment_container).navigate(
                     R.id.albumDetailsFragment,
                     bundleOf(EXTRA_ALBUM_ID to song.albumId)
                 )
                 return true
             }*/
            /*R.id.action_go_to_artist -> {
                goToArtist(requireActivity())
                return true
            }*/
            R.id.now_playing -> {
                requireActivity().findNavController(R.id.fragment_container).navigate(
                    R.id.playing_queue_fragment,
                    null
                )
                return true
            }
            R.id.action_show_lyrics -> {
                goToLyrics(requireActivity())
                return true
            }
            R.id.action_equalizer -> {
                NavigationUtil.openEqualizer(requireActivity())
                return true
            }
            R.id.action_sleep_timer -> {
                SleepTimerDialog().show(parentFragmentManager, "SLEEP_TIMER")
                return true
            }
            R.id.action_set_as_ringtone -> {
                if (RingtoneManager.requiresDialog(requireActivity())) {
                    RingtoneManager.getDialog(requireActivity())
                }
                val ringtoneManager = RingtoneManager(requireActivity())
                ringtoneManager.setRingtone(song)
                return true
            }
            R.id.action_go_to_genre -> {
                val retriever = MediaMetadataRetriever()
                val trackUri =
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.id
                    )
                retriever.setDataSource(activity, trackUri)
                var genre: String? =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                if (genre == null) {
                    genre = "Not Specified"
                }
                Toast.makeText(context, genre, Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return false
    }

    private fun showLyricsIcon(item: MenuItem) {
        val icon =
            if (PreferenceUtil.showLyrics) R.drawable.ic_lyrics else R.drawable.ic_lyrics_outline
        val drawable: Drawable = PlayBeatUtil.getTintedVectorDrawable(
            requireContext(),
            icon,
            toolbarIconColor()
        )
        item.isChecked = PreferenceUtil.showLyrics
        item.icon = drawable
    }

    abstract fun playerToolbar(): Toolbar?

    abstract fun onShow()

    abstract fun onHide()

    abstract fun onBackPressed(): Boolean

    abstract fun toolbarIconColor(): Int

    override fun onServiceConnected() {
        updateIsFavorite()
    }

    override fun onPlayingMetaChanged() {
        updateIsFavorite()
    }

    override fun onFavoriteStateChanged() {
        updateIsFavorite(animate = true)
    }

    protected open fun toggleFavorite(song: Song) {
        lifecycleScope.launch(IO) {
            val playlist: PlaylistEntity = libraryViewModel.favoritePlaylist()
            if (playlist != null) {
                val songEntity = song.toSongEntity(playlist.playListId)
                val isFavorite = libraryViewModel.isSongFavorite(song.id)
                if (isFavorite) {
                    libraryViewModel.removeSongFromPlaylist(songEntity)
                } else {
                    libraryViewModel.insertSongs(listOf(song.toSongEntity(playlist.playListId)))
                }
            }
            libraryViewModel.forceReload(ReloadType.Playlists)
            requireContext().sendBroadcast(Intent(MusicService.FAVORITE_STATE_CHANGED))
        }
    }

    fun updateIsFavorite(animate: Boolean = false) {
        lifecycleScope.launch(IO) {
            val isFavorite: Boolean =
                libraryViewModel.isSongFavorite(MusicPlayerRemote.currentSong.id)
            withContext(Main) {
                val icon = if (animate && VersionUtils.hasMarshmallow()) {
                    if (isFavorite) R.drawable.avd_favorite else R.drawable.avd_unfavorite
                } else {
                    if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                }
                val drawable: Drawable = PlayBeatUtil.getTintedVectorDrawable(
                    requireContext(),
                    icon,
                    toolbarIconColor()
                )
                if (playerToolbar() != null) {
                    playerToolbar()?.menu?.findItem(R.id.action_toggle_favorite)?.apply {
                        setIcon(drawable)
                        title =
                            if (isFavorite) getString(R.string.action_remove_from_favorites)
                            else getString(R.string.action_add_to_favorites)
                        getIcon().also {
                            if (it is AnimatedVectorDrawable) {
                                it.start()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (PreferenceUtil.isFullScreenMode &&
            view.findViewById<View>(R.id.status_bar) != null
        ) {
            view.findViewById<View>(R.id.status_bar).isVisible = false
        }
        playerAlbumCoverFragment = whichFragment(R.id.playerAlbumCoverFragment)
        playerAlbumCoverFragment?.setCallbacks(this)

        if (VersionUtils.hasMarshmallow())
            view.findViewById<RelativeLayout>(R.id.statusBarShadow)?.hide()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()
        val nps = PreferenceUtil.nowPlayingScreen

        playerToolbar()?.menu?.findItem(R.id.action_toggle_lyrics)?.apply {
            isChecked = PreferenceUtil.showLyrics
            showLyricsIcon(this)
        }

        requireView().setOnTouchListener(
            if (PreferenceUtil.swipeAnywhereToChangeSong) {
                SwipeDetector(
                    requireContext(),
                    playerAlbumCoverFragment?.viewPager,
                    requireView()
                )
            } else null
        )
    }

    class SwipeDetector(val context: Context, val viewPager: ViewPager?, val view: View) :
        View.OnTouchListener {
        private var flingPlayBackController: GestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    return when {
                        abs(distanceX) > abs(distanceY) -> {
                            // Disallow Intercept Touch Event so that parent(BottomSheet) doesn't consume the events
                            view.parent.requestDisallowInterceptTouchEvent(true)
                            true
                        }
                        else -> {
                            false
                        }
                    }
                }
            })

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            viewPager?.dispatchTouchEvent(event)
            return flingPlayBackController.onTouchEvent(event)
        }
    }

    companion object {
        val TAG: String = AbsPlayerFragment::class.java.simpleName
        const val VISIBILITY_ANIM_DURATION: Long = 300
    }

    protected fun getUpNextAndQueueTime(): String {
        val duration = MusicPlayerRemote.getQueueDurationMillis(MusicPlayerRemote.position)

        return MusicUtil.buildInfoString(
            resources.getString(R.string.up_next),
            MusicUtil.getReadableDurationString(duration)
        )
    }
}

fun goToArtist(activity: Activity) {
    if (activity !is MainActivity) return
    val song = MusicPlayerRemote.currentSong
    activity.apply {

        // Remove exit transition of current fragment so
        // it doesn't exit with a weird transition
        currentFragment(R.id.fragment_container)?.exitTransition = null

        //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        findNavController(R.id.fragment_container).navigate(
            R.id.artistDetailsFragment,
            bundleOf(EXTRA_ARTIST_ID to song.artistId),
            navOptions {
                launchSingleTop = true
            },
            null
        )
    }
}

fun goToAlbum(activity: Activity) {
    if (activity !is MainActivity) return
    val song = MusicPlayerRemote.currentSong
    activity.apply {
        currentFragment(R.id.fragment_container)?.exitTransition = null

        //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        findNavController(R.id.fragment_container).navigate(
            R.id.albumDetailsFragment,
            bundleOf(EXTRA_ALBUM_ID to song.albumId),
            navOptions {
                launchSingleTop = true
            },
            null
        )
    }
}

fun goToLyrics(activity: Activity) {
    if (activity !is MainActivity) return
    activity.apply {
        //Hide Bottom Bar First, else Bottom Sheet doesn't collapse fully
        setBottomNavVisibility(false)
        if (getBottomSheetBehavior().state == BottomSheetBehavior.STATE_EXPANDED) {
            collapsePanel()
        }

        findNavController(R.id.fragment_container).navigate(
            R.id.lyrics_fragment,
            null,
            navOptions { launchSingleTop = true },
            null
        )
    }
}
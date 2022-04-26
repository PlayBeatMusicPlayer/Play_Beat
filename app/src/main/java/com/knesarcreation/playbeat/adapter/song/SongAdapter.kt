package com.knesarcreation.playbeat.adapter.song

import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import com.knesarcreation.appthemehelper.ThemeStore
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.EXTRA_ALBUM_ID
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.base.AbsMultiSelectAdapter
import com.knesarcreation.playbeat.adapter.base.MediaEntryViewHolder
import com.knesarcreation.playbeat.extensions.generalThemeValue
import com.knesarcreation.playbeat.fragments.songs.SongsFragment
import com.knesarcreation.playbeat.glide.GlideApp
import com.knesarcreation.playbeat.glide.PlayBeatColoredTarget
import com.knesarcreation.playbeat.glide.PlayBeatGlideExtension
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.SortOrder
import com.knesarcreation.playbeat.helper.menu.BottomSheetSongMenuHelper
import com.knesarcreation.playbeat.helper.menu.SongsMenuHelper
import com.knesarcreation.playbeat.interfaces.ICabCallback
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.interfaces.IPlaybackStateChanged
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.MusicUtil
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.color.MediaNotificationProcessor
import com.knesarcreation.playbeat.util.theme.ThemeMode
import me.zhanghai.android.fastscroll.PopupTextProvider

open class SongAdapter(
    override val activity: FragmentActivity,
    var dataSet: MutableList<Song>,
    protected var itemLayoutRes: Int,
    ICabHolder: ICabHolder?,
    showSectionName: Boolean = true
) : AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song>(
    activity,
    ICabHolder,
    R.menu.menu_media_selection
), ICabCallback, PopupTextProvider, IPlaybackStateChanged.TempInter {

    private var showSectionName = true

    init {
        this.showSectionName = showSectionName
        this.setHasStableIds(true)
        SongsFragment.listener = this
    }

    open fun swapDataSet(dataSet: List<Song>) {
        this.dataSet = ArrayList(dataSet)
        notifyDataSetChanged()
    }

    override fun getItemId(position: Int): Long {
        return dataSet[position].id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            try {
                LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false)
            } catch (e: Resources.NotFoundException) {
                LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false)
            }
        return createViewHolder(view)
    }

    protected open fun createViewHolder(view: View): ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = dataSet[position]

        showEqualizer(holder, song)

        val isChecked = isChecked(song)
        holder.itemView.isActivated = isChecked
        holder.menu?.isGone = isChecked
        holder.title?.text = getSongTitle(song)
        holder.text?.text = getSongText(song)
        holder.text2?.text = getSongText(song)
        loadAlbumCover(song, holder)
        val landscape = PlayBeatUtil.isLandscape()
        if ((PreferenceUtil.songGridSize > 2 && !landscape) || (PreferenceUtil.songGridSizeLand > 5 && landscape)) {
            holder.menu?.isVisible = false
        }
    }

    private fun setColors(color: MediaNotificationProcessor, holder: ViewHolder) {
        if (holder.paletteColorContainer != null) {
            holder.title?.setTextColor(color.primaryTextColor)
            holder.text?.setTextColor(color.secondaryTextColor)
            holder.paletteColorContainer?.setBackgroundColor(color.backgroundColor)
            holder.menu?.imageTintList = ColorStateList.valueOf(color.primaryTextColor)
        }
        holder.mask?.backgroundTintList = ColorStateList.valueOf(color.primaryTextColor)
    }

    protected open fun loadAlbumCover(song: Song, holder: ViewHolder) {
        if (holder.image == null) {
            return
        }
        GlideApp.with(activity).asBitmapPalette().songCoverOptions(song)
            .load(PlayBeatGlideExtension.getSongModel(song))
            .into(object : PlayBeatColoredTarget(holder.image!!) {
                override fun onColorReady(colors: MediaNotificationProcessor) {
                    setColors(colors, holder)
                }
            })
    }

    private fun getSongTitle(song: Song): String {
        return song.title
    }

    private fun getSongText(song: Song): String {
        return song.artistName
    }

    private fun getSongText2(song: Song): String {
        return song.albumName
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun getIdentifier(position: Int): Song? {
        return dataSet[position]
    }

    override fun getName(song: Song): String {
        return song.title
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        SongsMenuHelper.handleMenuClick(activity, selection, menuItem.itemId)
    }

    override fun getPopupText(position: Int): String {
        val sectionName: String? = when (PreferenceUtil.songSortOrder) {
            SortOrder.SongSortOrder.SONG_A_Z, SortOrder.SongSortOrder.SONG_Z_A -> dataSet[position].title
            SortOrder.SongSortOrder.SONG_ALBUM_ASC -> dataSet[position].albumName
            SortOrder.SongSortOrder.SONG_ARTIST_ASC -> dataSet[position].artistName
            SortOrder.SongSortOrder.SONG_YEAR_DESC -> return MusicUtil.getYearString(dataSet[position].year)
            SortOrder.SongSortOrder.COMPOSER_ACS -> dataSet[position].composer
            SortOrder.SongSortOrder.SONG_ALBUM_ARTIST_ASC -> dataSet[position].albumArtist
            else -> {
                return ""
            }
        }
        return MusicUtil.getSectionName(sectionName)
    }

    open inner class ViewHolder(itemView: View) : MediaEntryViewHolder(itemView) {
        //protected open var songMenuRes = SongMenuHelper.MENU_RES
        protected open val song: Song
            get() = dataSet[layoutPosition]

        init {
            menu?.setOnClickListener {
                BottomSheetSongMenuHelper(activity, 0/*default*/).handleMenuClick(
                    song,
                    layoutPosition
                )
            }
        }

        protected open fun onSongMenuItemClick(item: MenuItem): Boolean {
            if (image != null && image!!.isVisible) {
                when (item.itemId) {
                    R.id.action_go_to_album -> {
                        activity.findNavController(R.id.fragment_container)
                            .navigate(
                                R.id.albumDetailsFragment,
                                bundleOf(EXTRA_ALBUM_ID to song.albumId)
                            )
                        return true
                    }
                }
            }
            return false
        }

        override fun onClick(v: View?) {
            if (isInQuickSelectMode) {
                toggleChecked(layoutPosition)
            } else {
                MusicPlayerRemote.openQueue(dataSet, layoutPosition, true)
                Handler(Looper.myLooper()!!).postDelayed({
                    notifyDataSetChanged()
                }, 500)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            return toggleChecked(layoutPosition)
        }
    }

    private fun showEqualizer(holder: ViewHolder, song: Song) {
        var playingSongId = -1L
        if (MusicPlayerRemote.playingQueue.isNotEmpty()) {
            playingSongId = MusicPlayerRemote.playingQueue[MusicPlayerRemote.position].id
        }

        holder.equalizerView?.setBarColor(ThemeStore.accentColor(activity))

        if (song.id == playingSongId) {
            Handler(Looper.myLooper()!!).postDelayed({
                holder.rlCurrentPlayingLottie?.visibility = View.VISIBLE
                if (MusicPlayerRemote.musicService!!.playback!!.isPlaying) {
                    //Log.d("SongsDetails", "onBindViewHolder:${song.title} ")
                    holder.equalizerView?.animateBars()
                } else
                    holder.equalizerView?.stopBars()

                val accentColor = ThemeStore.accentColor(activity)
                holder.title?.setTextColor(accentColor)
                holder.text?.setTextColor(accentColor)
            }, 500)


        } else {
            holder.equalizerView?.stopBars()
            holder.rlCurrentPlayingLottie?.visibility = View.GONE
            val blackColor = ContextCompat.getColor(activity, R.color.black_color)
            val whiteColor = ContextCompat.getColor(activity, R.color.md_white_1000)
            when (App.getContext().generalThemeValue) {
                ThemeMode.LIGHT -> {
                    holder.title?.setTextColor(blackColor)
                    holder.text?.setTextColor(blackColor)
                }

                ThemeMode.DARK -> {
                    holder.title?.setTextColor(whiteColor)
                    holder.text?.setTextColor(whiteColor)
                }

                ThemeMode.BLACK -> {
                    holder.title?.setTextColor(whiteColor)
                    holder.text?.setTextColor(whiteColor)
                }

                ThemeMode.AUTO -> {
                    holder.title?.setTextColor(
                        ContextCompat.getColor(
                            activity,
                            R.color.textColor
                        )
                    )
                    holder.text?.setTextColor(
                        ContextCompat.getColor(
                            activity,
                            R.color.textColor
                        )
                    )
                }
            }

        }
    }

    companion object {
        val TAG: String = SongAdapter::class.java.simpleName
    }

    override fun mNotify() {
        notifyDataSetChanged()
    }

}

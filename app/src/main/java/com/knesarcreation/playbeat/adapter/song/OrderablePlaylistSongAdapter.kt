package com.knesarcreation.playbeat.adapter.song

import android.content.Intent
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.button.MaterialButton
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.knesarcreation.playbeat.*
import com.knesarcreation.playbeat.activities.tageditor.AbsTagEditorActivity
import com.knesarcreation.playbeat.activities.tageditor.SongTagEditorActivity
import com.knesarcreation.playbeat.db.PlaylistEntity
import com.knesarcreation.playbeat.db.toSongEntity
import com.knesarcreation.playbeat.db.toSongsEntity
import com.knesarcreation.playbeat.dialogs.AddToPlaylistDialog
import com.knesarcreation.playbeat.dialogs.DeleteSongsDialog
import com.knesarcreation.playbeat.dialogs.RemoveSongFromPlaylistDialog
import com.knesarcreation.playbeat.dialogs.SongDetailDialog
import com.knesarcreation.playbeat.extensions.accentColor
import com.knesarcreation.playbeat.extensions.accentOutlineColor
import com.knesarcreation.playbeat.fragments.LibraryViewModel
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.fragments.bottomSheets.BottomSheetAudioMoreOptions
import com.knesarcreation.playbeat.fragments.playlists.mInterstitialAdHelper
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.interfaces.ICabHolder
import com.knesarcreation.playbeat.interfaces.IPaletteColorHolder
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.providers.BlacklistStore
import com.knesarcreation.playbeat.repository.RealRepository
import com.knesarcreation.playbeat.util.MusicUtil
import com.knesarcreation.playbeat.util.RingtoneManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

class OrderablePlaylistSongAdapter(
    private val playlist: PlaylistEntity,
    activity: FragmentActivity,
    dataSet: MutableList<Song>,
    itemLayoutRes: Int,
    ICabHolder: ICabHolder?,
) : AbsOffsetSongAdapter(activity, dataSet, itemLayoutRes, ICabHolder),
    DraggableItemAdapter<OrderablePlaylistSongAdapter.ViewHolder> {

    val libraryViewModel: LibraryViewModel by activity.viewModel()

    init {
        this.setHasStableIds(true)
        this.setMultiSelectMenuRes(R.menu.menu_playlists_songs_selection)
    }

    override fun getItemId(position: Int): Long {
        // requires static value, it means need to keep the same value
        // even if the item position has been changed.
        return if (position != 0) {
            dataSet[position - 1].id
        } else {
            -1
        }
    }

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) OFFSET_ITEM else SONG
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        if (holder.itemViewType == OFFSET_ITEM) {
            val viewHolder = holder as ViewHolder
            viewHolder.playAction?.let {
                it.setOnClickListener {
                    mInterstitialAdHelper?.showInterstitial(
                        INTERSTITIAL_PLAYLIST_DETAILS_PLAY_SHUFFLE,
                        PLAY_BUTTON,
                        dataSet,
                        0
                    )
                    //MusicPlayerRemote.openQueue(dataSet, 0, true)
                }
                it.accentOutlineColor()
            }
            viewHolder.shuffleAction?.let {
                it.setOnClickListener {
                    mInterstitialAdHelper?.showInterstitial(
                        INTERSTITIAL_PLAYLIST_DETAILS_PLAY_SHUFFLE,
                        SHUFFLE_BUTTON,
                        dataSet,
                        0
                    )
                    // MusicPlayerRemote.openAndShuffleQueue(dataSet, true)
                }
                it.accentColor()
            }
        } else {
            /* var playingSongId = -1L
             if (MusicPlayerRemote.playingQueue.isNotEmpty()) {
                 playingSongId = MusicPlayerRemote.playingQueue[MusicPlayerRemote.position].id
             }

             val song = dataSet.toSongsEntity(playlist)
             if (song[position - 1].id == playingSongId) {
                 Handler(Looper.myLooper()!!).postDelayed({
                     if (MusicPlayerRemote.musicService!!.playback!!.isPlaying) {
                         holder.equalizerView?.animateBars()
                     } else
                         holder.equalizerView?.stopBars()

                     holder.rlCurrentPlayingLottie?.visibility = View.VISIBLE

                 }, 500)


             } else {
                 holder.equalizerView?.stopBars()
                 holder.rlCurrentPlayingLottie?.visibility = View.GONE
             }*/
            super.onBindViewHolder(holder, position - 1)
        }
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>) {
        when (menuItem.itemId) {
            R.id.action_remove_from_playlist -> RemoveSongFromPlaylistDialog.create(
                selection.toSongsEntity(
                    playlist
                )
            )
                .show(activity.supportFragmentManager, "REMOVE_FROM_PLAYLIST")
            else -> super.onMultipleItemAction(menuItem, selection)
        }
    }

    inner class ViewHolder(itemView: View) : AbsOffsetSongAdapter.ViewHolder(itemView),
        KoinComponent {
        val playAction: MaterialButton? = itemView.findViewById(R.id.playAction)
        val shuffleAction: MaterialButton? = itemView.findViewById(R.id.shuffleAction)

        /*  override var songMenuRes: Int
              get() = R.menu.menu_item_playlist_song
              set(value) {
                  super.songMenuRes = value
              }

          override fun onSongMenuItemClick(item: MenuItem): Boolean {
              when (item.itemId) {
                  R.id.action_remove_from_playlist -> {
                      RemoveSongFromPlaylistDialog.create(song.toSongEntity(playlist.playListId))
                          .show(activity.supportFragmentManager, "REMOVE_FROM_PLAYLIST")
                      return true
                  }
              }
              return super.onSongMenuItemClick(item)
          }*/

        init {
            dragView?.isVisible = true
            menu?.setOnClickListener {
                val libraryViewModel = activity.getViewModel() as LibraryViewModel
                val bottomSheetAudioMoreOption = BottomSheetAudioMoreOptions(
                    2,
                    song,
                    activity /*playlist song*/
                )
                bottomSheetAudioMoreOption.show(
                    activity.supportFragmentManager,
                    "bottomSheetAudioMoreOption"
                )
                bottomSheetAudioMoreOption.listener =
                    object : BottomSheetAudioMoreOptions.SingleSelectionMenuOption {
                        override fun playNext() {
                            MusicPlayerRemote.playNext(song)
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun addToPlayingQueue() {
                            MusicPlayerRemote.enqueue(song)
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun addToPlaylist() {
                            CoroutineScope(Dispatchers.IO).launch {
                                val playlists = get<RealRepository>().fetchPlaylists()
                                withContext(Dispatchers.Main) {
                                    AddToPlaylistDialog.create(playlists, song)
                                        .show(activity.supportFragmentManager, "ADD_PLAYLIST")
                                }
                                bottomSheetAudioMoreOption.dismiss()
                            }
                        }

                        override fun goToAlbum() {
                            activity.findNavController(R.id.fragment_container).navigate(
                                R.id.albumDetailsFragment,
                                bundleOf(EXTRA_ALBUM_ID to song.albumId)
                            )
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun goToArtist() {
                            activity.findNavController(R.id.fragment_container).navigate(
                                R.id.artistDetailsFragment,
                                bundleOf(EXTRA_ARTIST_ID to song.artistId)
                            )
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun share() {
                            activity.startActivity(
                                Intent.createChooser(
                                    MusicUtil.createShareSongFileIntent(song, activity),
                                    null
                                )
                            )
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun tagEditor() {
                            val tagEditorIntent =
                                Intent(activity, SongTagEditorActivity::class.java)
                            tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id)
                            if (activity is IPaletteColorHolder)
                                tagEditorIntent.putExtra(
                                    AbsTagEditorActivity.EXTRA_PALETTE,
                                    (activity as IPaletteColorHolder).paletteColor
                                )
                            activity.startActivity(tagEditorIntent)
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun details() {
                            SongDetailDialog.create(song)
                                .show(activity.supportFragmentManager, "SONG_DETAILS")
                            bottomSheetAudioMoreOption.dismiss()

                        }

                        override fun setAsRingtone() {
                            if (RingtoneManager.requiresDialog(activity)) {
                                RingtoneManager.getDialog(activity)
                            } else {
                                val ringtoneManager = RingtoneManager(activity)
                                ringtoneManager.setRingtone(song)
                            }
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun addToBlackList() {
                            BlacklistStore.getInstance(App.getContext()).addPath(File(song.data))
                            libraryViewModel.forceReload(ReloadType.Songs)
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun deleteFromDevice() {
                            DeleteSongsDialog.create(song)
                                .show(activity.supportFragmentManager, "DELETE_SONGS")
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun removeFromPlayingQueue() {
                            MusicPlayerRemote.removeFromQueue(layoutPosition)
                            bottomSheetAudioMoreOption.dismiss()
                        }

                        override fun removeFromPlaylist() {
                            RemoveSongFromPlaylistDialog.create(song.toSongEntity(playlist.playListId))
                                .show(activity.supportFragmentManager, "REMOVE_FROM_PLAYLIST")
                            bottomSheetAudioMoreOption.dismiss()
                        }
                    }
            }
        }
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean {
        if (dataSet.size == 0 or 1 || isInQuickSelectMode) {
            return false
        }
        val dragHandle = holder.dragView ?: return false

        val handleWidth = dragHandle.width
        val handleHeight = dragHandle.height
        val handleLeft = dragHandle.left
        val handleTop = dragHandle.top

        return (x >= handleLeft && x < handleLeft + handleWidth &&
                y >= handleTop && y < handleTop + handleHeight) && position != 0
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        dataSet.add(toPosition - 1, dataSet.removeAt(fromPosition - 1))
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange {
        return ItemDraggableRange(1, itemCount - 1)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        return true
    }

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    fun saveSongs(playlistEntity: PlaylistEntity) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            libraryViewModel.insertSongs(dataSet.toSongsEntity(playlistEntity))
        }
    }
}

package com.knesarcreation.playbeat.helper.menu

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.EXTRA_ALBUM_ID
import com.knesarcreation.playbeat.EXTRA_ARTIST_ID
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.tageditor.AbsTagEditorActivity
import com.knesarcreation.playbeat.activities.tageditor.SongTagEditorActivity
import com.knesarcreation.playbeat.dialogs.AddToPlaylistDialog
import com.knesarcreation.playbeat.dialogs.DeleteSongsDialog
import com.knesarcreation.playbeat.dialogs.SongDetailDialog
import com.knesarcreation.playbeat.fragments.LibraryViewModel
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.fragments.bottomSheets.BottomSheetAudioMoreOptions
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.helper.MusicPlayerRemote.removeFromQueue
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.File

class BottomSheetSongMenuHelper(private var activity: FragmentActivity, private var mode: Int) :
    KoinComponent {

    fun handleMenuClick(song: Song, layoutPosition: Int) {
        val libraryViewModel = activity.getViewModel() as LibraryViewModel
        val bottomSheetAudioMoreOption = BottomSheetAudioMoreOptions(mode, song, activity)
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
                    val tagEditorIntent = Intent(activity, SongTagEditorActivity::class.java)
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
                    removeFromQueue(layoutPosition)
                    bottomSheetAudioMoreOption.dismiss()
                }

                override fun removeFromPlaylist() {
                    /*RemoveSongFromPlaylistDialog.create(song.toSongEntity(playlist.playListId))
                        .show(activity.supportFragmentManager, "REMOVE_FROM_PLAYLIST")*/
                }
            }
    }
}
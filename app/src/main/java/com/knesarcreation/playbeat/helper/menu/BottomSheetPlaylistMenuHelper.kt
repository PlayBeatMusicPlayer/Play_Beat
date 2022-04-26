package com.knesarcreation.playbeat.helper.menu

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.knesarcreation.playbeat.db.PlaylistWithSongs
import com.knesarcreation.playbeat.db.toSongs
import com.knesarcreation.playbeat.dialogs.AddToPlaylistDialog
import com.knesarcreation.playbeat.dialogs.DeletePlaylistDialog
import com.knesarcreation.playbeat.dialogs.RenamePlaylistDialog
import com.knesarcreation.playbeat.dialogs.SavePlaylistDialog
import com.knesarcreation.playbeat.fragments.bottomSheets.BottomSheetPlaylistMoreOptions
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.repository.RealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class BottomSheetPlaylistMenuHelper(private var context: Context) : KoinComponent {

    fun handleMenuClick(playlistWithSongs: PlaylistWithSongs) {
        val bottomSheetPlaylistMoreOption = BottomSheetPlaylistMoreOptions(playlistWithSongs)
        bottomSheetPlaylistMoreOption.show(
            (context as AppCompatActivity).supportFragmentManager,
            "bottomSheetPlaylistMoreOption"
        )
        bottomSheetPlaylistMoreOption.listener =
            object : BottomSheetPlaylistMoreOptions.OnPlaylistMoreOptionsClicked {
                override fun play() {
                    MusicPlayerRemote.openQueue(playlistWithSongs.songs.toSongs(), 0, true)
                    bottomSheetPlaylistMoreOption.dismiss()
                }

                override fun playNext() {
                    MusicPlayerRemote.playNext(playlistWithSongs.songs.toSongs())
                    bottomSheetPlaylistMoreOption.dismiss()
                }

                override fun addToPlayingQueue() {
                    MusicPlayerRemote.enqueue(playlistWithSongs.songs.toSongs())
                    bottomSheetPlaylistMoreOption.dismiss()
                }

                override fun addToPlaylist() {
                    CoroutineScope(Dispatchers.IO).launch {
                        val playlists = get<RealRepository>().fetchPlaylists()
                        withContext(Dispatchers.Main) {
                            AddToPlaylistDialog.create(playlists, playlistWithSongs.songs.toSongs())
                                .show((context as AppCompatActivity).supportFragmentManager, "ADD_PLAYLIST")
                        }
                    }
                    bottomSheetPlaylistMoreOption.dismiss()
                }

                override fun rename() {
                    RenamePlaylistDialog.create(playlistWithSongs.playlistEntity)
                        .show((context as AppCompatActivity).supportFragmentManager, "RENAME_PLAYLIST")
                    bottomSheetPlaylistMoreOption.dismiss()
                }

                override fun deletePlaylist() {
                    DeletePlaylistDialog.create(playlistWithSongs.playlistEntity)
                        .show((context as AppCompatActivity).supportFragmentManager, "DELETE_PLAYLIST")
                    bottomSheetPlaylistMoreOption.dismiss()
                }

                override fun saveAsFile() {
                    SavePlaylistDialog.create(playlistWithSongs)
                        .show((context as AppCompatActivity).supportFragmentManager, "SavePlaylist")
                    bottomSheetPlaylistMoreOption.dismiss()
                }
            }
    }
}
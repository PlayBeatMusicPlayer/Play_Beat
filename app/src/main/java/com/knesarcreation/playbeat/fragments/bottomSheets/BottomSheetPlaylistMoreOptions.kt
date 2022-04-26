package com.knesarcreation.playbeat.fragments.bottomSheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.databinding.BottomSheetPlaylistOptionsMenuBinding
import com.knesarcreation.playbeat.db.PlaylistWithSongs

class BottomSheetPlaylistMoreOptions(
    private var playlistWithSongs: PlaylistWithSongs
) :
    BottomSheetDialogFragment() {

    private var _binding: BottomSheetPlaylistOptionsMenuBinding? = null
    private val binding get() = _binding

    var listener: OnPlaylistMoreOptionsClicked? = null

    interface OnPlaylistMoreOptionsClicked {
        fun play()
        fun playNext()
        fun addToPlayingQueue()
        fun addToPlaylist()
        fun rename()
        fun deletePlaylist()
        fun saveAsFile()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnPlaylistMoreOptionsClicked
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetPlaylistOptionsMenuBinding.inflate(inflater, container, false)
        val view = binding?.root

        binding?.playlistNameTV?.text = playlistWithSongs.playlistEntity.playlistName
        binding?.totalAudioTV?.text = "${playlistWithSongs.songs.size} Songs"

        handleClickListener()
        return view
    }

    private fun handleClickListener() {

        binding?.llPlayPlaylist?.setOnClickListener {
            listener?.play()

        }

        binding?.llPlayNext?.setOnClickListener {
            listener?.playNext()

        }

        binding?.llAddToQueue?.setOnClickListener {
            listener?.addToPlayingQueue()

        }

        binding?.llAddToPlaylist?.setOnClickListener {
            listener?.addToPlaylist()
        }

        binding?.llRename?.setOnClickListener {
            listener?.rename()

        }
        binding?.llDelete?.setOnClickListener {
            listener?.deletePlaylist()

        }
        binding?.llSaveAsFile?.setOnClickListener {
            listener?.saveAsFile()

        }
    }

}
package com.knesarcreation.playbeat.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.knesarcreation.playbeat.activities.ShareAsStory
import com.knesarcreation.playbeat.EXTRA_SONG
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.extensions.colorButtons
import com.knesarcreation.playbeat.extensions.materialDialog
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.MusicUtil

class SongShareDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song: Song? = requireArguments().getParcelable(EXTRA_SONG)
        val listening: String =
            String.format(
                getString(R.string.currently_listening_to_x_by_x),
                song?.title,
                song?.artistName
            )
        return materialDialog(R.string.what_do_you_want_to_share)
            .setItems(
                arrayOf(
                    getString(R.string.the_audio_file),
                    "\u201C" + listening + "\u201D",
                    getString(R.string.social_stories)
                )
            ) { _, which ->
                withAction(which, song, listening)
            }
            .create()
            .colorButtons()
    }

    private fun withAction(
        which: Int,
        song: Song?,
        currentlyListening: String
    ) {
        when (which) {
            0 -> {
                startActivity(Intent.createChooser(song?.let {
                    MusicUtil.createShareSongFileIntent(
                        it,
                        requireContext()
                    )
                }, null))
            }
            1 -> {
                startActivity(
                    Intent.createChooser(
                        Intent()
                            .setAction(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_TEXT, currentlyListening)
                            .setType("text/plain"),
                        null
                    )
                )
            }
            2 -> {
                if (song != null) {
                    startActivity(
                        Intent(
                            requireContext(),
                            ShareAsStory::class.java
                        ).putExtra(
                            ShareAsStory.EXTRA_SONG,
                            song
                        )
                    )
                }
            }
        }
    }

    companion object {

        fun create(song: Song): SongShareDialog {
            return SongShareDialog().apply {
                arguments = bundleOf(
                    EXTRA_SONG to song
                )
            }
        }
    }
}
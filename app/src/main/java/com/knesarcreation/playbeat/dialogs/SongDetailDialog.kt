package com.knesarcreation.playbeat.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.os.bundleOf
import androidx.core.text.parseAsHtml
import androidx.fragment.app.DialogFragment
import com.knesarcreation.playbeat.EXTRA_SONG
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.extensions.colorButtons
import com.knesarcreation.playbeat.extensions.materialDialog
import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.MusicUtil
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import java.io.File
import java.io.IOException

class SongDetailDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context: Context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_file_details, null)

        val song = requireArguments().getParcelable<Song>(EXTRA_SONG)
        val fileName: TextView = dialogView.findViewById(R.id.fileName)
        val filePath: TextView = dialogView.findViewById(R.id.filePath)
        val fileSize: TextView = dialogView.findViewById(R.id.fileSize)
        val dateModified: TextView = dialogView.findViewById(R.id.dateModified)
        val fileFormat: TextView = dialogView.findViewById(R.id.fileFormat)
        val trackLength: TextView = dialogView.findViewById(R.id.trackLength)
        val bitRate: TextView = dialogView.findViewById(R.id.bitrate)
        val samplingRate: TextView = dialogView.findViewById(R.id.samplingRate)

        fileName.text = makeTextWithTitle(context, R.string.label_file_name, "-")
        filePath.text = makeTextWithTitle(context, R.string.label_file_path, "-")
        fileSize.text = makeTextWithTitle(context, R.string.label_file_size, "-")
        fileFormat.text = makeTextWithTitle(context, R.string.label_file_format, "-")
        trackLength.text = makeTextWithTitle(context, R.string.label_track_length, "-")
        bitRate.text = makeTextWithTitle(context, R.string.label_bit_rate, "-")
        samplingRate.text = makeTextWithTitle(context, R.string.label_sampling_rate, "-")
        if (song != null) {
            val songFile = File(song.data)
            if (songFile.exists()) {
                fileName.text = makeTextWithTitle(context, R.string.label_file_name, songFile.name)
                filePath.text =
                    makeTextWithTitle(context, R.string.label_file_path, songFile.absolutePath)

                dateModified.text = makeTextWithTitle(
                    context, R.string.label_last_modified,
                    MusicUtil.getDateModifiedString(songFile.lastModified())
                )

                fileSize.text =
                    makeTextWithTitle(
                        context,
                        R.string.label_file_size,
                        getFileSizeString(songFile.length())
                    )
                try {
                    val audioFile = AudioFileIO.read(songFile)
                    val audioHeader = audioFile.audioHeader

                    fileFormat.text =
                        makeTextWithTitle(context, R.string.label_file_format, audioHeader.format)
                    trackLength.text = makeTextWithTitle(
                        context,
                        R.string.label_track_length,
                        MusicUtil.getReadableDurationString((audioHeader.trackLength * 1000).toLong())
                    )
                    bitRate.text = makeTextWithTitle(
                        context,
                        R.string.label_bit_rate,
                        audioHeader.bitRate + " kb/s"
                    )
                    samplingRate.text =
                        makeTextWithTitle(
                            context,
                            R.string.label_sampling_rate,
                            audioHeader.sampleRate + " Hz"
                        )
                } catch (@NonNull e: CannotReadException) {
                    Log.e(TAG, "error while reading the song file", e)
                    // fallback
                    trackLength.text = makeTextWithTitle(
                        context,
                        R.string.label_track_length,
                        MusicUtil.getReadableDurationString(song.duration)
                    )
                } catch (@NonNull e: IOException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(
                        context,
                        R.string.label_track_length,
                        MusicUtil.getReadableDurationString(song.duration)
                    )
                } catch (@NonNull e: TagException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(
                        context,
                        R.string.label_track_length,
                        MusicUtil.getReadableDurationString(song.duration)
                    )
                } catch (@NonNull e: ReadOnlyFileException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(
                        context,
                        R.string.label_track_length,
                        MusicUtil.getReadableDurationString(song.duration)
                    )
                } catch (@NonNull e: InvalidAudioFrameException) {
                    Log.e(TAG, "error while reading the song file", e)
                    trackLength.text = makeTextWithTitle(
                        context,
                        R.string.label_track_length,
                        MusicUtil.getReadableDurationString(song.duration)
                    )
                }
            } else {
                // fallback
                fileName.text = makeTextWithTitle(context, R.string.label_file_name, song.title)
                trackLength.text = makeTextWithTitle(
                    context,
                    R.string.label_track_length,
                    MusicUtil.getReadableDurationString(song.duration)
                )
            }
        }
        return materialDialog(R.string.action_details)
            .setPositiveButton(android.R.string.ok, null)
            .setView(dialogView)
            .create()
            .colorButtons()
    }

    companion object {

        val TAG: String = SongDetailDialog::class.java.simpleName

        fun create(song: Song): SongDetailDialog {
            return SongDetailDialog().apply {
                arguments = bundleOf(
                    EXTRA_SONG to song
                )
            }
        }

        private fun makeTextWithTitle(context: Context, titleResId: Int, text: String?): Spanned {
            return ("<b>" + context.resources.getString(titleResId) + ": " + "</b>" + text)
                .parseAsHtml()
        }

        private fun getFileSizeString(sizeInBytes: Long): String {
            val fileSizeInKB = sizeInBytes / 1024
            val fileSizeInMB = fileSizeInKB / 1024
            return "$fileSizeInMB MB"
        }
    }
}

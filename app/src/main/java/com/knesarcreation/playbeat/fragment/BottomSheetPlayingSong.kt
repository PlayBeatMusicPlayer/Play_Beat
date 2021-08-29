package com.knesarcreation.playbeat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.model.AllSongsModel
import kotlin.math.floor

class BottomSheetPlayingSong(
    private var allSongList: ArrayList<AllSongsModel>,
    private var clickedSongPos: Int,
    private var listener: OnControlSongFromBottomSheet
) :
    BottomSheetDialogFragment() {

    private lateinit var songTextTV: TextView
    private lateinit var artistNameTV: TextView
    private lateinit var backwardIv: ImageView
    private lateinit var playPauseIV: ImageView
    private lateinit var forwardIV: ImageView
    private lateinit var fullScreenAlbumArt: ImageView
    private lateinit var mSeekBar: SeekBar

    interface OnControlSongFromBottomSheet {
        fun onSeekChangeListener(seekBar: SeekBar)
        fun onForwardClick()
        fun onBackWardClick()
        fun onPauseOrPlayClick()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_song_tray, container, false)
        backwardIv = view.findViewById(R.id.backwardIv)
        playPauseIV = view.findViewById(R.id.playPauseIV)
        forwardIV = view.findViewById(R.id.forwardIV)
        songTextTV = view.findViewById(R.id.songTextTV)
        artistNameTV = view.findViewById(R.id.artistNameTV)
        fullScreenAlbumArt = view.findViewById(R.id.fullScreenAlbumArt)
        mSeekBar = view.findViewById(R.id.seekBar)

        playPauseIV.setOnClickListener {
            listener.onPauseOrPlayClick()
        }

        forwardIV.setOnClickListener {
            listener.onForwardClick()
        }

        backwardIv.setOnClickListener {
            listener.onBackWardClick()
        }

        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                listener.onSeekChangeListener(seekBar!!)
            }
        })

        setUpViews()

        return view
    }

    private fun setUpViews() {
        val allSongsModel = allSongList[clickedSongPos]
        songTextTV.text = allSongsModel.songName
        artistNameTV.text = allSongsModel.artistsName
        Glide.with(this).asBitmap().load(allSongsModel.bitmap).into(fullScreenAlbumArt)
        mSeekBar.max = ((allSongsModel.duration.toDouble() / 1000).toInt())
    }

    private fun millisToMinutesAndSeconds(millis: Int): String {
        val minutes = floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds "
    }
}
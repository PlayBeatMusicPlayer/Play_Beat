package com.knesarcreation.playbeat.fragment

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.net.toUri
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activity.AllSongsActivity.Companion.backStackedSongs
import com.knesarcreation.playbeat.activity.AllSongsActivity.Companion.isLoop
import com.knesarcreation.playbeat.activity.AllSongsActivity.Companion.isShuffled
import com.knesarcreation.playbeat.activity.AllSongsActivity.Companion.musicPlayerService
import com.knesarcreation.playbeat.activity.AllSongsActivity.Companion.random
import com.knesarcreation.playbeat.model.AllSongsModel
import com.knesarcreation.playbeat.utils.UriToBitmapConverter
import java.util.*
import kotlin.math.floor


class BottomSheetPlayingSong(
    private var mContext: Context,
    private var allSongList: ArrayList<AllSongsModel>,
    private var clickedSongPos: Int,
    /* private var millisLeft: Long,*/
    private var listener: OnControlSongFromBottomSheet
) :
    BottomSheetDialogFragment() {

    private lateinit var songTextTV: TextView
    private lateinit var artistNameTV: TextView
    private lateinit var endTimeTV: TextView
    private lateinit var startDurationTV: TextView
    private lateinit var backwardIv: ImageView
    private lateinit var playPauseIV: ImageView
    private lateinit var forwardIV: ImageView
    private lateinit var closeSheetIV: ImageView
    private lateinit var fullScreenBlurredAlbumArt: ImageView
    private lateinit var nowPlayingAlbumArtIV: ImageView
    private lateinit var mSeekBar: SeekBar
    private var elapsedRunningSong: CountDownTimer? = null
    private var totalDurationInMillis = 0L
    private lateinit var rlToolbar: RelativeLayout
    private lateinit var shuffleSongIV: ImageView
    private lateinit var loopSongIV: ImageView
    private lateinit var albumArtBottomGradient: ImageView
    private lateinit var albumArtTopGradient: ImageView
    private lateinit var fullScreenBG: ImageView
    private lateinit var rlParentBG: RelativeLayout
    private lateinit var controllingSongIVBG: ImageView
    private var randomPosition = 0

    interface OnControlSongFromBottomSheet {
        fun onSeekChangeListener(seekBar: SeekBar)
        fun onNextBtnClicked(clickedSongPos: Int)
        fun onPrevBtnClicked(clickedSongPos: Int)
        fun onPauseOrPlayClick()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_song_tray, container, false)
        backwardIv = view.findViewById(R.id.skipPrevAudio)
        playPauseIV = view.findViewById(R.id.playPauseIV)
        forwardIV = view.findViewById(R.id.skipNextAudio)
        songTextTV = view.findViewById(R.id.songTextTV)
        artistNameTV = view.findViewById(R.id.artistsNameTV)
        nowPlayingAlbumArtIV = view.findViewById(R.id.nowPlayingAlbumArtIV)
        mSeekBar = view.findViewById(R.id.seekBar)
        endTimeTV = view.findViewById(R.id.endTimeTV)
        startDurationTV = view.findViewById(R.id.startTimeTV)
        rlToolbar = view.findViewById(R.id.rlToolbar)
        /*blurImgToolbar = view.findViewById(R.id.blurImgToolbar)*/
        loopSongIV = view.findViewById(R.id.loopOneSong)
        closeSheetIV = view.findViewById(R.id.closeSheetIV)
        fullScreenBlurredAlbumArt = view.findViewById(R.id.fullScreenBlurredAlbumArt)
        shuffleSongIV = view.findViewById(R.id.shuffleSongIV)
        albumArtBottomGradient = view.findViewById(R.id.albumArtBottomGradient)
        albumArtTopGradient = view.findViewById(R.id.albumArtTopGradient)
        rlParentBG = view.findViewById(R.id.rlParentBG)
        fullScreenBG = view.findViewById(R.id.fullScreenBG)
        controllingSongIVBG = view.findViewById(R.id.controllingSongIVBG)

        songTextTV.isSelected = true


        playPauseIV.setOnClickListener {
            if (musicPlayerService?.isPlaying() == true) {
                playPauseIV.setImageResource(R.drawable.ic_play_audio)
                elapsedRunningSong?.cancel()
            } else {
                playPauseIV.setImageResource(R.drawable.ic_pause)
                runningSongCountDownTime(totalDurationInMillis - (musicPlayerService?.currentPosition())!!.toLong())
            }
            listener.onPauseOrPlayClick()
        }

        forwardIV.setOnClickListener {
            if (isShuffled) {
                randomPosition = Random().nextInt(allSongList.size)
                clickedSongPos = randomPosition
                // adding back stack songs to go play previous song
                backStackedSongs.add(clickedSongPos)
            } else {
                incrementSongByOne()
            }
            playSong()
            runningSongCountDownTime(allSongList[clickedSongPos].duration.toLong())
            listener.onNextBtnClicked(clickedSongPos)
            Log.d("SheetRandomF", "onFinish:$clickedSongPos ")
            playPauseIV.setImageResource(R.drawable.ic_pause)
        }

        backwardIv.setOnClickListener {
            Log.d("backStackSongs", "onCreateView:$backStackedSongs ")
            if (isShuffled) {
                //getting index of last played song
                if (backStackedSongs.isNotEmpty() && backStackedSongs.size >= 2) {
                    val lastIndex =
                        backStackedSongs[backStackedSongs.size - 2]
                    clickedSongPos = lastIndex
                    backStackedSongs.removeAt(backStackedSongs.size - 1)
                } else {
                    backStackedSongs.clear()
                    decrementSongPosByOne()
                }
            } else {
                decrementSongPosByOne()
            }
            playSong()
            runningSongCountDownTime(allSongList[clickedSongPos].duration.toLong())
            listener.onPrevBtnClicked(clickedSongPos)
            Log.d("SheetRandomB", "onFinish:$clickedSongPos ")
            playPauseIV.setImageResource(R.drawable.ic_pause)
        }

        manageLoopSong()
        manageShuffledSing()

        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {}

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                runningSongCountDownTime((totalDurationInMillis - (seekBar?.progress!! * 1000)))
                listener.onSeekChangeListener(seekBar)
            }
        })

        closeSheetIV.setOnClickListener {
            dismiss()
        }

        if (clickedSongPos != -1) {
            playSong()
        }
        runningSongCountDownTime(totalDurationInMillis - (musicPlayerService?.currentPosition())!!.toLong())

        return view
    }

    private fun manageShuffledSing() {
        if (!isShuffled) {
            shuffleSongIV.setImageResource(R.drawable.ic_shuffle_disable)
        } else {
            shuffleSongIV.setImageResource(R.drawable.ic_shuffle_enable)
        }

        shuffleSongIV.setOnClickListener {
            if (!isShuffled) {
                shuffleSongIV.setImageResource(R.drawable.ic_shuffle_enable)
                //disable shuffle if loop enabled
                loopSongIV.setImageResource(R.drawable.loop_songs)
                isLoop = false

                Toast.makeText(mContext, "Shuffled enable", Toast.LENGTH_SHORT).show()
                isShuffled = true
            } else {
                shuffleSongIV.setImageResource(R.drawable.ic_shuffle_disable)
                Toast.makeText(mContext, "Shuffled disabled", Toast.LENGTH_SHORT).show()
                isShuffled = false
            }
        }
    }

    private fun manageLoopSong() {
        if (!isLoop) {
            loopSongIV.setImageResource(R.drawable.loop_songs)
        } else {
            loopSongIV.setImageResource(R.drawable.loop_one_song)
        }

        loopSongIV.setOnClickListener {
            if (!isLoop) {
                loopSongIV.setImageResource(R.drawable.loop_one_song)
                //disable shuffle if loop enabled
                shuffleSongIV.setImageResource(R.drawable.ic_shuffle_disable)
                isShuffled = false

                Toast.makeText(mContext, "Loop song", Toast.LENGTH_SHORT).show()
                isLoop = true
            } else {
                loopSongIV.setImageResource(R.drawable.loop_songs)
                Toast.makeText(mContext, "Loop list", Toast.LENGTH_SHORT).show()
                isLoop = false
            }
        }
    }

    private fun playSong() {
        val allSongsModel = allSongList[clickedSongPos]
        totalDurationInMillis = allSongsModel.duration.toLong()
        songTextTV.text = allSongsModel.songName
        artistNameTV.text = allSongsModel.artistsName


        if (musicPlayerService != null) {
            if (musicPlayerService?.isPlaying() == true) {
                playPauseIV.setImageResource(R.drawable.ic_pause)
            } else {
                playPauseIV.setImageResource(R.drawable.ic_play_audio)
            }
        } else {
            // rare case
            playPauseIV.setImageResource(R.drawable.ic_play_audio)
        }

//        val albumArt = SongAlbumArt.get((allSongsModel.path))
        val albumArtUri = allSongsModel.artUri
        val albumArt = UriToBitmapConverter.getBitmap(
            (mContext).contentResolver!!,
            albumArtUri.toUri()
        )
        val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        if (albumArt != null) {
//            val bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
            Glide.with(mContext).asBitmap().load(albumArt).transition(withCrossFade(factory))
                .into(nowPlayingAlbumArtIV)

            //Blurred Full screen image
            /* controllingSongIVBG.setImageResource(R.drawable.spydi)

             val bgCurveBitmap = ((controllingSongIVBG.drawable)as BitmapDrawable).toBitmap()

             val blurBitmap = BlurBuilder().blur(mContext, bgCurveBitmap, 23f)
             Glide.with(mContext).asBitmap().load(blurBitmap).centerCrop()
                 .transition(withCrossFade(factory))
                 .into(controllingSongIVBG)*/

            Palette.from(albumArt).generate {
                val swatch = it?.dominantSwatch
                if (swatch != null) {
                    albumArtBottomGradient.setBackgroundResource(R.drawable.gradient_background_bottom_shadow)
                    rlParentBG.setBackgroundResource(R.drawable.app_theme_background_drawable)
                    albumArtTopGradient.setBackgroundResource(R.drawable.gradient_background_top_shadow)

                    val gradientDrawableBottomTop = GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(swatch.rgb, 0x00000000)
                    )
                    val gradientDrawableTopBottom = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(swatch.rgb, swatch.population)
                    )
                    albumArtBottomGradient.background = gradientDrawableBottomTop
                    albumArtTopGradient.background = gradientDrawableTopBottom


                    // parent background
                    val gradientDrawableParent = GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(swatch.rgb, swatch.rgb)
                    )
                    rlParentBG.background = gradientDrawableParent

                }
            }

        } else {
            val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.music_note_1)
            Glide.with(mContext).asBitmap().load(originalBitmap).transition(withCrossFade(factory))
                .into(nowPlayingAlbumArtIV)

            Palette.from(originalBitmap).generate {
                val swatch = it?.dominantSwatch
                if (swatch != null) {
                    albumArtBottomGradient.setBackgroundResource(R.drawable.gradient_background_bottom_shadow)
                    rlParentBG.setBackgroundResource(R.drawable.app_theme_background_drawable)
                    albumArtTopGradient.setBackgroundResource(R.drawable.gradient_background_top_shadow)

                    val gradientDrawableBottomTop = GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(swatch.rgb, 0x00000000)
                    )
                    val gradientDrawableTopBottom = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(swatch.rgb, swatch.population)
                    )
                    albumArtBottomGradient.background = gradientDrawableBottomTop
                    albumArtTopGradient.background = gradientDrawableTopBottom


                    // parent background
                    val gradientDrawableParent = GradientDrawable(
                        GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(swatch.rgb, swatch.rgb)
                    )
                    rlParentBG.background = gradientDrawableParent

                }
            }
            //Blurred Full screen image
            /* val blurredBitMap = BlurBuilder().blur(mContext, originalBitmap, 25f)
             Glide.with(mContext).asBitmap().load(blurredBitMap).centerCrop()
                 .transition(withCrossFade(factory))
                 .into(fullScreenBlurredAlbumArt)*/
        }
        mSeekBar.max = ((allSongsModel.duration.toDouble() / 1000).toInt())
        val songDuration = millisToMinutesAndSeconds(totalDurationInMillis)
        endTimeTV.text = songDuration
    }

    private fun millisToMinutesAndSeconds(millis: Long): String {
        val minutes = floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds.toInt() == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds "
    }

    private fun runningSongCountDownTime(durationInMillis: Long) {
        if (elapsedRunningSong != null) {
            elapsedRunningSong?.cancel()
        }
        elapsedRunningSong = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                /* millisLeft = millisUntilFinished*/
                setUpSeekBar(millisUntilFinished)
            }

            override fun onFinish() {
                /*if (clickedSongPos == (allSongList.size - 1)) {
                    clickedSongPos = 0
                } else {

                }*/
                if (!isLoop && !isShuffled) {
                    // normal running player
                    incrementSongByOne()
                    playSong()
                    runningSongCountDownTime(allSongList[clickedSongPos].duration.toLong())
                } else {
                    if (isShuffled) {
                        // shuffled
                        randomPosition = random
                        if (musicPlayerService != null) {
                            clickedSongPos = randomPosition
                            //getting random songs and playing it
                            playSong()
                            runningSongCountDownTime(allSongList[randomPosition].duration.toLong())
                        }
                    } else if (isLoop) {
                        // looped one song
                        if (musicPlayerService != null) {
                            playSong()
                            runningSongCountDownTime(allSongList[clickedSongPos].duration.toLong())
                        }
                    }
                }

            }
        }
        elapsedRunningSong?.start()
    }

    private fun decrementSongPosByOne() {
        if (clickedSongPos != 0) {
            clickedSongPos--
        } else {
            clickedSongPos = allSongList.size
            clickedSongPos--
        }
    }

    private fun incrementSongByOne() {
        if (clickedSongPos != allSongList.size - 1) {
            clickedSongPos++
        } else {
            clickedSongPos = -1
            clickedSongPos++
        }
    }

    private fun setUpSeekBar(millisUntilFinished: Long) {
        val elapsedTime = (totalDurationInMillis - millisUntilFinished)
        val millisToMinutesAndSeconds = millisToMinutesAndSeconds(elapsedTime)
        startDurationTV.text = millisToMinutesAndSeconds

        val maxSeekProgress = totalDurationInMillis / 1000
        val seekProgress =
            ((elapsedTime.toDouble() / totalDurationInMillis.toDouble()) * maxSeekProgress)

        mSeekBar.progress = seekProgress.toInt()

        if (musicPlayerService != null) {
            if (musicPlayerService?.isPlaying() == false) {
                elapsedRunningSong?.cancel()
            }
        }
    }

}
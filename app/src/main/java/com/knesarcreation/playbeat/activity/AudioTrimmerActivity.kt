package com.knesarcreation.playbeat.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.customAudioViews.MarkerView
import com.knesarcreation.playbeat.customAudioViews.SamplePlayer
import com.knesarcreation.playbeat.customAudioViews.SoundFile
import com.knesarcreation.playbeat.customAudioViews.WaveformView
import com.knesarcreation.playbeat.database.AllSongsModel
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.databinding.ActivityTrimBinding
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.utils.InterstitialAdHelper
import com.knesarcreation.playbeat.utils.MakeStatusBarTransparent
import com.knesarcreation.playbeat.utils.StorageUtil
import com.knesarcreation.playbeat.utils.Utility
import java.io.File
import java.io.RandomAccessFile
import java.util.*
import kotlin.math.roundToInt


class AudioTrimmerActivity : AppCompatActivity(), View.OnClickListener, MarkerView.MarkerListener,
    WaveformView.WaveformListener {

    /* Audio trimmer*/

    private lateinit var binding: ActivityTrimBinding
    private var mLoadedSoundFile: SoundFile? = null
    private var mRecordedSoundFile: SoundFile? = null
    private var mPlayer: SamplePlayer? = null

    private var mHandler: Handler? = null

    private var mTouchDragging = false
    private var mTouchStart = 0f
    private var mTouchInitialOffset = 0
    private var mTouchInitialStartPos = 0
    private var mTouchInitialEndPos = 0
    private var mDensity = 0f
    private var mMarkerLeftInset = 0
    private var mMarkerRightInset = 0
    private var mMarkerTopOffset = 0
    private var mMarkerBottomOffset = 0

    private var mTextLeftInset = 0
    private var mTextRightInset = 0
    private var mTextTopOffset = 0
    private var mTextBottomOffset = 0

    private var mOffset = 0
    private var mOffsetGoal = 0
    private var mFlingVelocity = 0
    private var mPlayEndMillSec = 0
    private var mWidth = 0
    private var mMaxPos = 0
    private var mStartPos = 0
    private var mEndPos = 0

    private var mStartVisible = false
    private var mEndVisible = false
    private var mLastDisplayedStartPos = 0
    private var mLastDisplayedEndPos = 0
    private var mIsPlaying = false
    private var mKeyDown = false

    private var mProgressDialog: ProgressDialog? = null

    // private var savingProgressBar: ProgressBar? = null
    private var dialogTitle: TextView? = null
    private var mLoadingLastUpdateTime: Long = 0
    private var mLoadingKeepGoing = false
    private var mFile: File? = null
    private var reqWriteToSystemSetting: ActivityResultLauncher<Intent>? = null
    private lateinit var allSongModle: AllSongsModel
    private var radioBtn = ""
    private lateinit var loadingDialog: androidx.appcompat.app.AlertDialog
    private var storage = StorageUtil(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTrimBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MakeStatusBarTransparent().transparent(this)

        binding.arrowBackIV.setOnClickListener {
            onBackPressed()
        }

        reqWriteToSystemSetting =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // writeAudioToSystemSetting()
            }

        val progressDialogAlert = MaterialAlertDialogBuilder(this)
        val customProgressView = layoutInflater.inflate(R.layout.custom_progress_bar, null)
        progressDialogAlert.setView(customProgressView)
        loadingDialog = progressDialogAlert.create()
        //mProgressDialog = customProgressView.findViewById(R.id.progressView)
        //savingProgressBar = customProgressView.findViewById(R.id.savingProgressBar)
        // dialogTitle = customProgressView.findViewById(R.id.title)
        loadingDialog.setCanceledOnTouchOutside(false)
        loadingDialog.setCancelable(false)
        binding.txtAudioPlay.isEnabled = false
        // loadingDialog.show()
        binding.trimAudio.visibility = View.GONE


        if (intent != null) {
            val audioData = intent.getStringExtra("AudioData")
            val gson = Gson()
            val type = object : TypeToken<AllSongsModel>() {}.type
            allSongModle = gson.fromJson(audioData, type)
            binding.audioName.text = allSongModle.songName
        }

        mHandler = Handler(Looper.getMainLooper())

        mRecordedSoundFile = null
        mKeyDown = false
        binding.audioWaveform.setListener(this)

        binding.markerStart.setListener(this)
        binding.markerStart.alpha = 1f
        binding.markerStart.isFocusable = true
        binding.markerStart.isFocusableInTouchMode = true
        mStartVisible = true

        binding.markerEnd.setListener(this)
        binding.markerEnd.alpha = 1f
        binding.markerEnd.isFocusable = true
        binding.markerEnd.isFocusableInTouchMode = true
        mEndVisible = true

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mDensity = metrics.density

        /**
         * Change this for marker handle as per your view
         */
        mMarkerLeftInset = (-8 * mDensity).toInt()
        mMarkerRightInset = (30 * mDensity).toInt()
        mMarkerTopOffset = (6 * mDensity).toInt()
        mMarkerBottomOffset = (6 * mDensity).toInt()

        /**
         * Change this for duration text as per your view
         */
        mTextLeftInset = (2 * mDensity).toInt()
        mTextTopOffset = (-1 * mDensity).toInt()
        mTextRightInset = (35 * mDensity).toInt()
        mTextBottomOffset = (-40 * mDensity).toInt()

        binding.trimAudio.setOnClickListener(this)
        binding.txtAudioPlay.setOnClickListener(this)
        binding.leftMarkerSkipPrev.setOnClickListener(this)
        binding.leftMarkerSkipNext.setOnClickListener(this)
        binding.rightMarkerSkipNextIV.setOnClickListener(this)
        binding.rightMarkerSkipPrevIV.setOnClickListener(this)

        binding.resetAudio.setOnClickListener(this)

        mHandler!!.postDelayed(mTimerRunnable, 100)

        loadFromFile(allSongModle.data)


        if (storage.getShouldWeShowInterstitialAdOnTrimActivity()) {
            InterstitialAdHelper(this, this, 1).loadAd()
            //storage.showInterstitialAddForTrimActivity(false)
        }
    }


    private val mTimerRunnable: Runnable = object : Runnable {
        override fun run() {
            // Updating Text is slow on Android.  Make sure
            // we only do the update if the text has actually changed.
            if (mStartPos != mLastDisplayedStartPos) {
                binding.txtStartPosition.text = formatTime(mStartPos)
                mLastDisplayedStartPos = mStartPos
                binding.leftMarkerTime.text = formatTime(mStartPos)
                /*String.format(Locale.US, "%02d:%05.2f", min, sec)*/
            }
            if (mEndPos != mLastDisplayedEndPos) {
                binding.txtEndPosition.text = formatTime(mEndPos)
                mLastDisplayedEndPos = mEndPos
                binding.rightMarkerTime.text = formatTime(mEndPos)
            }
            mHandler!!.postDelayed(this, 100)
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onClick(view: View) {
        if (view === binding.txtAudioPlay) {
            if (!mIsPlaying) {
                binding.txtAudioPlay.setImageResource(R.drawable.ic_pause)
            } else {
                binding.txtAudioPlay.setImageResource(R.drawable.ic_play)
            }
            onPlay(mStartPos)
        } else if (view === binding.trimAudio) {
            val alertSaveTrimmedAudio = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
            val customDialog = layoutInflater.inflate(R.layout.dialog_save_trim_audio, null)
            alertSaveTrimmedAudio.setView(customDialog)

            val dialog = alertSaveTrimmedAudio.create()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.show()
            dialog.setCancelable(false)

            val etAudioName = customDialog.findViewById<TextInputEditText>(R.id.etAudioName)
            etAudioName.setText("PlayBeat_${allSongModle.songName}")
            val cancelButton = customDialog.findViewById<MaterialButton>(R.id.cancelDialog)
            val saveAudio = customDialog.findViewById<MaterialButton>(R.id.saveAudio)
            val setAsRG = customDialog.findViewById<RadioGroup>(R.id.setAsRG)
            setAsRG.setOnCheckedChangeListener { group, checkedId ->
                val rb = group.findViewById<View>(checkedId) as RadioButton
                if (checkedId > -1) {
                    radioBtn = rb.text.toString()
                }
            }

            saveAudio.setOnClickListener {
                checkSystemWritePermission(etAudioName, dialog)
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }


        } else if (view === binding.resetAudio) {
            try {
                binding.audioWaveform.setIsDrawBorder(true)
                mMaxPos = binding.audioWaveform.maxPos()
                mLastDisplayedStartPos = -1
                mLastDisplayedEndPos = -1
                mTouchDragging = false
                mOffset = 0
                mOffsetGoal = 0
                mFlingVelocity = 0
                resetPositions()
                if (mEndPos > mMaxPos) mEndPos = mMaxPos
                handlePause()
                updateDisplay()
            } catch (e: java.lang.Exception) {
            }

        } else if (view === binding.leftMarkerSkipPrev) {
            if (mStartPos != 0) {
                mStartPos -= 10
                updateDisplay()
            }
        } else if (view === binding.leftMarkerSkipNext) {
            if (mStartPos < mEndPos - 10) {
                mStartPos += 10
                updateDisplay()
            }
        } else if (view === binding.rightMarkerSkipNextIV) {
            if (mEndPos < mMaxPos) {
                mEndPos += 10
                updateDisplay()
            }
        } else if (view === binding.rightMarkerSkipPrevIV) {
            if (mEndPos > mStartPos + 10) {
                mEndPos -= 10
                updateDisplay()
            }
        }
    }


    /**
     * After loading audio do necessary steps
     *
     * @param mSoundFile sound file
     * @param isReset    isReset
     */
    @SuppressLint("SetTextI18n")
    private fun finishOpeningSoundFile(mSoundFile: SoundFile) {
        binding.audioWaveform.visibility = View.VISIBLE
        binding.audioWaveform.setSoundFile(mSoundFile)
        binding.audioWaveform.recomputeHeights(mDensity)
        mMaxPos = binding.audioWaveform.maxPos()
        mLastDisplayedStartPos = -1
        mLastDisplayedEndPos = -1
        mTouchDragging = false
        mOffset = 0
        mOffsetGoal = 0
        mFlingVelocity = 0
        resetPositions()
        if (mEndPos > mMaxPos) mEndPos = mMaxPos

        /* if (isReset == 1) {
             handlePause()
         }*/
        //mIsPlaying = false
        /*if (isReset == 1) {
            mStartPos = binding.audioWaveform.secondsToPixels(0.0)
            mEndPos =
                binding.audioWaveform.secondsToPixels(binding.audioWaveform.pixelsToSeconds(mMaxPos))
        }*/

        if (binding.audioWaveform.isInitialized) {
            val seconds = binding.audioWaveform.pixelsToSeconds(mMaxPos)
            val min = (seconds / 60).toInt()
            val sec = (seconds - 60 * min).toFloat()
            binding.txtAudioRecordTimeUpdate.text = "Total: ${
                String.format(
                    Locale.US,
                    "%02d:%05.2f",
                    min,
                    sec
                )
            }"
        }

        updateDisplay()
    }

    /**
     * Update views
     */
    @Synchronized
    private fun updateDisplay() {
        if (mIsPlaying) {
            val now = mPlayer!!.currentPosition
            val frames: Int = binding.audioWaveform.millisecsToPixels(now)
            binding.audioWaveform.setPlayback(frames)
            Log.e("mWidth >> ", "" + mWidth)
            setOffsetGoalNoUpdate(frames - mWidth / 2)
            if (now >= mPlayEndMillSec) {
                handlePause()
            }
        }
        if (!mTouchDragging) {
            var offsetDelta: Int
            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80
                } else {
                    mFlingVelocity = 0
                }
                mOffset += offsetDelta
                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2
                    mFlingVelocity = 0
                }
                if (mOffset < 0) {
                    mOffset = 0
                    mFlingVelocity = 0
                }
                mOffsetGoal = mOffset
            } else {
                offsetDelta = mOffsetGoal - mOffset
                offsetDelta =
                    if (offsetDelta > 10) offsetDelta / 10 else if (offsetDelta > 0) 1 else if (offsetDelta < -10) offsetDelta / 10 else if (offsetDelta < 0) -1 else 0
                mOffset += offsetDelta
            }
        }
        binding.audioWaveform.setParameters(mStartPos, mEndPos, mOffset)
        binding.audioWaveform.invalidate()
        binding.markerStart.contentDescription = " Start Marker" +
                formatTime(mStartPos)
        binding.markerEnd.contentDescription = " End Marker" +
                formatTime(mEndPos)
        var startX = mStartPos - mOffset - mMarkerLeftInset
        if (startX + binding.markerStart.width >= 0) {
            if (!mStartVisible) {
                // Delay this to avoid flicker
                mHandler!!.postDelayed({
                    mStartVisible = true
                    binding.markerStart.alpha = 1f
                    binding.txtStartPosition.alpha = 1f
                }, 0)
            }
        } else {
            if (mStartVisible) {
                binding.markerStart.alpha = 0f
                binding.txtStartPosition.alpha = 0f
                mStartVisible = false
            }
            startX = 0
        }
        var startTextX = mStartPos - mOffset - mTextLeftInset
        if (startTextX + binding.markerStart.width < 0) {
            startTextX = 0
        }
        var endX: Int = mEndPos - mOffset - binding.markerEnd.width + mMarkerRightInset
        if (endX + binding.markerEnd.width >= 0) {
            if (!mEndVisible) {
                // Delay this to avoid flicker
                mHandler!!.postDelayed({
                    mEndVisible = true
                    binding.markerEnd.alpha = 1f
                }, 0)
            }
        } else {
            if (mEndVisible) {
                binding.markerEnd.alpha = 0f
                mEndVisible = false
            }
            endX = 0
        }
        var endTextX: Int = mEndPos - mOffset - binding.txtEndPosition.width + mTextRightInset
        if (endTextX + binding.markerEnd.width < 0) {
            endTextX = 0
        }
        var params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        //        params.setMargins(
//                startX,
//                mMarkerTopOffset,
//                -markerStart.getWidth(),
//                -markerStart.getHeight());
        params.setMargins(
            startX,
            binding.audioWaveform.measuredHeight / 2 + mMarkerTopOffset,
            -binding.markerStart.width,
            -binding.markerStart.height
        )
        binding.markerStart.layoutParams = params

        params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            startTextX,
            mTextTopOffset,
            -binding.txtStartPosition.width,
            -binding.txtStartPosition.height
        )
        binding.txtStartPosition.layoutParams = params
        params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            endX,
            binding.audioWaveform.measuredHeight / 2 + mMarkerBottomOffset,
            -binding.markerEnd.width,
            -binding.markerEnd.height
        )
        //        params.setMargins(
//                endX,
//                audioWaveform.getMeasuredHeight() - markerEnd.getHeight() - mMarkerBottomOffset,
//                -markerEnd.getWidth(),
//                -markerEnd.getHeight());
        binding.markerEnd.layoutParams = params
        params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            endTextX,
            binding.audioWaveform.measuredHeight - binding.txtEndPosition.height - mTextBottomOffset,
            -binding.txtEndPosition.width,
            -binding.txtEndPosition.height
        )
        binding.txtEndPosition.layoutParams = params
    }

    /**
     * Reset all positions
     */
    private fun resetPositions() {
        mStartPos = binding.audioWaveform.secondsToPixels(0.0)
        mEndPos = binding.audioWaveform.secondsToPixels(15.0)
    }

    private fun setOffsetGoalNoUpdate(offset: Int) {
        if (mTouchDragging) {
            return
        }
        mOffsetGoal = offset
        if (mOffsetGoal + mWidth / 2 > mMaxPos) mOffsetGoal = mMaxPos - mWidth / 2
        if (mOffsetGoal < 0) mOffsetGoal = 0
    }

    private fun formatTime(pixels: Int): String {
        return if (binding.audioWaveform.isInitialized) {
            formatDecimal(binding.audioWaveform.pixelsToSeconds(pixels))
        } else {
            ""
        }
    }

    private fun formatDecimal(x: Double): String {
        var xWhole = x.toInt()
        var xFrac = (10 * (x - xWhole) + 0.5).toInt()
        if (xFrac >= 10) {
            xWhole++ //Round up
            xFrac -= 10 //Now we need the remainder after the round up
        }
        val s: String = millisToMinutesAndSeconds(xWhole * 1000)
        return "$s.$xFrac"
    }

    private fun millisToMinutesAndSeconds(millis: Int): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds == 60) "${(minutes.toInt() + 1)}:00" else "${minutes.toInt()}:${if (seconds < 10) "0" else ""}$seconds"
    }

    private fun trap(pos: Int): Int {
        if (pos < 0) return 0
        return if (pos > mMaxPos) mMaxPos else pos
    }

    private fun setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2)
    }

    private fun setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2)
    }

    private fun setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2)
    }

    private fun setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2)
    }

    private fun setOffsetGoal(offset: Int) {
        setOffsetGoalNoUpdate(offset)
        updateDisplay()
    }

    override fun markerDraw() {}

    override fun markerTouchStart(marker: MarkerView?, x: Float) {
        mTouchDragging = true
        mTouchStart = x
        mTouchInitialStartPos = mStartPos
        mTouchInitialEndPos = mEndPos
        handlePause()
    }

    override fun markerTouchMove(marker: MarkerView, x: Float) {
        val delta = x - mTouchStart
        if (marker === binding.markerStart) {
            mStartPos = trap((mTouchInitialStartPos + delta).toInt())
            mEndPos = trap((mTouchInitialEndPos + delta).toInt())
        } else {
            mEndPos = trap((mTouchInitialEndPos + delta).toInt())
            if (mEndPos < mStartPos) mEndPos = mStartPos
        }
        updateDisplay()
    }

    override fun markerTouchEnd(marker: MarkerView) {
        mTouchDragging = false
        if (marker === binding.markerStart) {
            setOffsetGoalStart()
        } else {
            setOffsetGoalEnd()
        }
    }

    override fun markerLeft(marker: MarkerView, velocity: Int) {
        mKeyDown = true
        if (marker === binding.markerStart) {
            val saveStart = mStartPos
            mStartPos = trap(mStartPos - velocity)
            mEndPos = trap(mEndPos - (saveStart - mStartPos))
            setOffsetGoalStart()
        }
        if (marker === binding.markerEnd) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity)
                mEndPos = mStartPos
            } else {
                mEndPos = trap(mEndPos - velocity)
            }
            setOffsetGoalEnd()
        }
        updateDisplay()
    }

    override fun markerRight(marker: MarkerView, velocity: Int) {
        mKeyDown = true
        if (marker === binding.markerStart) {
            val saveStart = mStartPos
            mStartPos += velocity
            if (mStartPos > mMaxPos) mStartPos = mMaxPos
            mEndPos += mStartPos - saveStart
            if (mEndPos > mMaxPos) mEndPos = mMaxPos
            setOffsetGoalStart()
        }
        if (marker === binding.markerEnd) {
            mEndPos += velocity
            if (mEndPos > mMaxPos) mEndPos = mMaxPos
            setOffsetGoalEnd()
        }
        updateDisplay()
    }

    override fun markerEnter(marker: MarkerView?) {}

    override fun markerKeyUp() {
        mKeyDown = false
        updateDisplay()
    }

    override fun markerFocus(marker: MarkerView) {
        mKeyDown = false
        if (marker === binding.markerStart) {
            setOffsetGoalStartNoUpdate()
        } else {
            setOffsetGoalEndNoUpdate()
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler!!.postDelayed({ updateDisplay() }, 100)
    }

    //
    // WaveformListener
    //

    /**
     * Every time we get a message that our waveform drew, see if we need to
     * animate and trigger another redraw.
     */
    override fun waveformDraw() {
        mWidth = binding.audioWaveform.measuredWidth
        if (mOffsetGoal != mOffset && !mKeyDown) updateDisplay() else if (mIsPlaying) {
            updateDisplay()
        } else if (mFlingVelocity != 0) {
            updateDisplay()
        }
    }

    override fun waveformTouchStart(x: Float) {
        mTouchDragging = true
        mTouchStart = x
        mTouchInitialOffset = mOffset
        mFlingVelocity = 0
//        long mWaveformTouchStartMsec = Utility.getCurrentTime();
    }

    override fun waveformTouchMove(x: Float) {
        mOffset = trap((mTouchInitialOffset + (mTouchStart - x)).toInt())
        updateDisplay()
    }

    override fun waveformTouchEnd() {
        /*mTouchDragging = false;
        mOffsetGoal = mOffset;

        long elapsedMsec = Utility.Utility.getCurrentTime() - mWaveformTouchStartMsec;
        if (elapsedMsec < 300) {
            if (mIsPlaying) {
                int seekMsec = audioWaveform.pixelsToMillisecs(
                        (int) (mTouchStart + mOffset));
                if (seekMsec >= mPlayStartMsec &&
                        seekMsec < mPlayEndMillSec) {
                    mPlayer.seekTo(seekMsec);
                } else {
//                    handlePause();
                }
            } else {
                onPlay((int) (mTouchStart + mOffset));
            }
        }*/
    }

    @Synchronized
    private fun handlePause() {
        binding.txtAudioPlay.setImageResource(R.drawable.ic_play)
        if (mPlayer != null && mPlayer!!.isPlaying) {
            mPlayer!!.pause()
        }
        binding.audioWaveform.setPlayback(-1)
        mIsPlaying = false
    }

    @Synchronized
    private fun onPlay(startPosition: Int) {
        if (mIsPlaying) {
            handlePause()
            return
        }
        if (mPlayer == null) {
            // Not initialized yet
            return
        }
        try {
            val mPlayStartMsec: Int = binding.audioWaveform.pixelsToMillisecs(startPosition)
            mPlayEndMillSec = when {
                startPosition < mStartPos -> {
                    binding.audioWaveform.pixelsToMillisecs(mStartPos)
                }
                startPosition > mEndPos -> {
                    binding.audioWaveform.pixelsToMillisecs(mMaxPos)
                }
                else -> {
                    binding.audioWaveform.pixelsToMillisecs(mEndPos)
                }
            }

            if (AllSongFragment.musicService != null) {
                val mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]
                val queueAudioList = StorageUtil(this).loadQueueAudio()
                val playingAudioIndex = StorageUtil(this).loadAudioIndex()
                if (AllSongFragment.musicService?.mediaPlayer?.isPlaying!!) {
                    AllSongFragment.musicService?.pauseMedia()
                    AllSongFragment.musicService?.pausedByManually = true
                    AllSongFragment.musicService?.updateNotification(isAudioPlaying = false)
                    // highlight pause audio with pause anim
                    mViewModelClass.updateSong(
                        queueAudioList[playingAudioIndex].songId,
                        queueAudioList[playingAudioIndex].songName,
                        0,
                        lifecycleScope
                    )
                    mViewModelClass.updateQueueAudio(
                        queueAudioList[playingAudioIndex].songId,
                        queueAudioList[playingAudioIndex].songName,
                        0,
                        lifecycleScope
                    )
                }
            }
            mPlayer!!.setOnCompletionListener { handlePause() }
            mPlayer!!.seekTo(mPlayStartMsec)
            mPlayer!!.start()
            mIsPlaying = true
            updateDisplay()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun waveformFling(vx: Float) {
        mTouchDragging = false
        mOffsetGoal = mOffset
        mFlingVelocity = (-vx).toInt()
        updateDisplay()
    }

    override fun waveformZoomIn() {
        binding.audioWaveform.zoomIn()
        mStartPos = binding.audioWaveform.start
        mEndPos = binding.audioWaveform.end
        binding.audioWaveform.maxPos().also { mMaxPos = it }
        mOffset = binding.audioWaveform.offset
        mOffsetGoal = mOffset
        updateDisplay()
    }

    override fun waveformZoomOut() {
        binding.audioWaveform.zoomOut()
        mStartPos = binding.audioWaveform.start
        mEndPos = binding.audioWaveform.end
        mMaxPos = binding.audioWaveform.maxPos()
        mOffset = binding.audioWaveform.offset
        mOffsetGoal = mOffset
        updateDisplay()
    }

    /**
     * Save sound file as ringtone
     *
     * @param finish flag for finish
     */
    private fun saveRingtone(newAudioName: String) {
        val startTime: Double = binding.audioWaveform.pixelsToSeconds(mStartPos)
        val endTime: Double = binding.audioWaveform.pixelsToSeconds(mEndPos)
        val startFrame: Int = binding.audioWaveform.secondsToFrames(startTime)
        val endFrame: Int = binding.audioWaveform.secondsToFrames(endTime - 0.04)
        val duration = (endTime - startTime + 0.5).toInt()

        // mProgressDialog!!.autoAnimate
        loadingDialog.show()

        // Save the sound file in a background thread
        val mSaveSoundFileThread: Thread = object : Thread() {
            override fun run() {
                // Try AAC first.
                val outPath = makeRingtoneFilename(newAudioName, Utility.AUDIO_FORMAT)
                if (outPath == null) {
                    Log.e(" >> ", "Unable to find unique filename")
                    return
                }
                val outFile = File(outPath)

                try {
                    // Write the new file
                    mLoadedSoundFile!!.WriteFile(outFile, startFrame, endFrame - startFrame)
                } catch (e: Exception) {
                    // log the error and try to create a .wav file instead
                    if (outFile.exists()) {
                        outFile.delete()
                    }
                    e.printStackTrace()
                }
                loadingDialog.dismiss()
                val finalOutPath: String = outPath
                val runnable = Runnable {
                    afterSavingAudioFile(
                        newAudioName,
                        finalOutPath,
                        duration
                    )
                }
                mHandler!!.post(runnable)
            }
        }
        mSaveSoundFileThread.start()
    }


    /**
     * After saving as ringtone set its content values
     *
     * @param title    title
     * @param outPath  output path
     * @param duration duration of file
     * @param finish   flag for finish
     */
    private fun afterSavingAudioFile(
        title: CharSequence,
        outPath: String,
        duration: Int
    ) {

        val currentTime = Calendar.getInstance()
        val outFile = File(outPath)
        val fileSize = outFile.length()
        val values = ContentValues()
        // val newAudioId = System.currentTimeMillis()
        // values.put(MediaStore.MediaColumns._ID, newAudioId)
        values.put(MediaStore.Audio.Media.DATA, outPath)
        values.put(MediaStore.Audio.Media.TITLE, title.toString())
        values.put(MediaStore.Audio.Media.SIZE, fileSize)
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.mp3")
        values.put(MediaStore.Audio.Media.ARTIST, allSongModle.artistsName)
        values.put(MediaStore.Audio.Media.DURATION, duration)
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
        values.put(MediaStore.Audio.Media.DATE_ADDED, currentTime.time.toString())
        values.put(MediaStore.Audio.Media.DATE_MODIFIED, currentTime.time.toString())
        /* values.put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
         values.put(MediaStore.Audio.Media.IS_ALARM, true)
         values.put(MediaStore.Audio.Media.IS_RINGTONE, true)
         values.put(MediaStore.Audio.Media.IS_MUSIC, false)*/

        val uri = MediaStore.Audio.Media.getContentUriForPath(outPath)


        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            val newUri: Uri? =
                contentResolver.insert(uri!!, values)
            val outputStream = contentResolver.openOutputStream(newUri!!)

            outputStream!!.write(outFile.readBytes())
            outputStream.close()

            if (radioBtn == "Ringtone") {
                RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_RINGTONE,
                    newUri
                )
                Toast.makeText(this, "Call ringtone set", Toast.LENGTH_LONG).show()
            } else if (radioBtn == "Alarm") {
                RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_ALARM,
                    newUri
                )
                Toast.makeText(this, "Alarm ringtone set", Toast.LENGTH_LONG).show()
            }

            try {
                if (outFile.exists()) {
                    // deleting redundant file after saving audio to mediaStore
                    outFile.delete()
                }
            } catch (e: Exception) {
                // Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }


        } else {
            val filePathToDelete =
                MediaStore.MediaColumns.DATA + "=\"" + outPath + "\""
            (this as AppCompatActivity).contentResolver.delete(
                uri!!,
                filePathToDelete,
                null
            )

            val newUri1: Uri? =
                contentResolver.insert(uri, values)
            if (radioBtn == "Ringtone") {
                RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_RINGTONE,
                    newUri1
                )
                Toast.makeText(this, "Call ringtone set", Toast.LENGTH_LONG).show()
            } else if (radioBtn == "Alarm") {
                RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_ALARM,
                    newUri1
                )
                Toast.makeText(this, "Alarm ringtone set", Toast.LENGTH_LONG).show()
            }
        }

        /*  dialogTitle!!.text = getString(R.string.loading)
          savingProgressBar!!.visibility = View.GONE
          mProgressDialog!!.visibility = View.VISIBLE
          loadingDialog.show()
          mProgressDialog!!.progress = 0f
          loadFromFile(outPath)*/
        finish()
        /*else if (finish == 1) {
            val conData = Bundle()
            conData.putString("INTENT_AUDIO_FILE", outPath)
            val intent = intent
            intent.putExtras(conData)
            setResult(RESULT_OK, intent)
            finish()
        }*/
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkSystemWritePermission(
        etAudioName: TextInputEditText,
        dialog: androidx.appcompat.app.AlertDialog
    ): Boolean {
        var permAllowed = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            permAllowed = Settings.System.canWrite(this)
            Log.d("WriteSettingPerm", "Can Write Settings: $permAllowed")
            if (permAllowed) {
                //Toast.makeText(mContext, "Write allowed :-)", Toast.LENGTH_LONG).show()
                writeAudioToSystemSetting(etAudioName, dialog)
            } else {
                //Toast.makeText(mContext, "Write not allowed :-(", Toast.LENGTH_LONG).show()
                val alertDialog =
                    MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
                alertDialog.setMessage("Play Beat requires permission of WRITE SYSTEM SETTING in order to set your selected audio as default ringtone.\n\nAfter granting permission, try setting your ringtone again.")
                alertDialog.setPositiveButton("Allow") { dialog, _ ->
                    permAllowed = true
                    openAndroidPermissionsMenu()
                    dialog.dismiss()
                }
                alertDialog.setNegativeButton("Cancel") { dialog, _ ->
                    permAllowed = true
                    dialog.dismiss()
                    //dismiss()
                }
                alertDialog.show()

            }
        }
        return permAllowed
    }

    private fun writeAudioToSystemSetting(
        etAudioName: TextInputEditText,
        dialog: androidx.appcompat.app.AlertDialog
    ) {
        if (etAudioName.text!!.isEmpty()) {
            etAudioName.error = "Please enter new name"
        } else {
            etAudioName.error = null
            val startTime: Double = binding.audioWaveform.pixelsToSeconds(mStartPos)
            val endTime: Double = binding.audioWaveform.pixelsToSeconds(mEndPos)
            val difference = endTime - startTime
            when {
                difference <= 0 -> {
                    Toast.makeText(
                        this@AudioTrimmerActivity,
                        "Trim seconds should be greater than 0 seconds",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                /*difference > 60 -> {
                    Toast.makeText(
                        this@AudioTrimmerActivity,
                        "Trim seconds should be less than 1 minute",
                        Toast.LENGTH_SHORT
                    ).show()
                }*/
                else -> {
                    if (mIsPlaying) {
                        handlePause()
                    }
                    saveRingtone(etAudioName.text.toString())
                    binding.trimAudio.visibility = View.GONE
                    binding.resetAudio.visibility = View.VISIBLE
                    binding.markerStart.visibility = View.INVISIBLE
                    binding.markerEnd.visibility = View.INVISIBLE
                    binding.txtStartPosition.visibility = View.INVISIBLE
                    binding.txtEndPosition.visibility = View.INVISIBLE
                }
            }
            dialog.dismiss()
        }
    }

    private fun openAndroidPermissionsMenu() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:" + applicationContext.packageName)
        reqWriteToSystemSetting!!.launch(intent)
    }

    /**
     * Generating name for ringtone
     *
     * @param title     title of file
     * @param extension extension for file
     * @return filename
     */
    private fun makeRingtoneFilename(title: CharSequence, extension: String): String? {

        /* val file =
             Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)*/


        val folder = File(
            getExternalFilesDir(Environment.getExternalStorageState()),
            "PlayBeat/Ringtone/"
        )
        // when folder is not exist
        folder.mkdirs()

        val filePath = getExternalFilesDir(Environment.getExternalStorageState())
        val file = File(filePath!!.absolutePath + "/PlayBeat/Ringtone")
        val parentDir = File(file, title.toString().trim())

        // Turn the title into a filename
        var filename = ""
        for (i in title.indices) {
            if (Character.isLetterOrDigit(title[i])) {
                filename += title[i]
            }
        }

        // Try to make the filename unique
        var path: String? = null
        for (i in 0..99) {
            val testPath: String =
                if (i > 0) parentDir.toString().trim() + i + extension else parentDir.toString()
                    .trim() + extension
            try {
                val f = RandomAccessFile(File(testPath), "r")
                f.close()
            } catch (e: Exception) {
                // Good, the file didn't exist
                path = testPath
                break
            }
        }
        return path
    }

    /**
     * Load file from path
     *
     * @param mFilename file name
     */
    @SuppressLint("SetTextI18n")
    private fun loadFromFile(mFilename: String) {
        mFile = File(mFilename)
        mLoadingLastUpdateTime = Utility.getCurrentTime()
        mLoadingKeepGoing = true

        val listener: SoundFile.ProgressListener =
            SoundFile.ProgressListener { fractionComplete ->
                val now: Long = Utility.getCurrentTime()
                if (now - mLoadingLastUpdateTime > 100) {
                    // mProgressDialog!!.progress =
                    //    (mProgressDialog!!.max * fractionComplete).toFloat()
                    runOnUiThread {
                        binding.progressTV.text =
                            "${(100 * fractionComplete).toFloat().roundToInt()} %"
                    }

                    mLoadingLastUpdateTime = now
                }
                mLoadingKeepGoing
            }

        // Load the sound file in a background thread
        val mLoadSoundFileThread: Thread = object : Thread() {
            override fun run() {
                try {
                    mLoadedSoundFile = SoundFile.create(mFile!!.absolutePath, listener)
                    if (mLoadedSoundFile == null) {
                        //loadingDialog.dismiss()
                        val name = mFile!!.name.lowercase(Locale.getDefault())
                        val components = name.split("\\.").toTypedArray()
                        val err: String = if (components.size < 2) {
                            "No Extension"
                        } else {
                            "Bad Extension"
                        }
                        Log.e(" >> ", "" + err)
                        return
                    }
                    mPlayer = SamplePlayer(mLoadedSoundFile)
                    val runnable = Runnable { //mPlayer = new SamplePlayer(mLoadedSoundFile);
                        binding.audioWaveform.setIsDrawBorder(true)
                        finishOpeningSoundFile(mLoadedSoundFile!!)
                        binding.audioWaveform.setBackgroundColor(resources.getColor(R.color.black))
                        binding.txtStartPosition.visibility = View.VISIBLE
                        binding.txtEndPosition.visibility = View.VISIBLE
                        binding.markerEnd.visibility = View.VISIBLE
                        binding.markerStart.visibility = View.VISIBLE
                        binding.llProgress.visibility = View.GONE
                        binding.rlAudioEdit.visibility = View.VISIBLE
                        binding.resetAudio.visibility = View.VISIBLE
                        binding.trimAudio.visibility = View.VISIBLE
                        binding.txtAudioPlay.isEnabled = true
                    }
                    mHandler!!.post(runnable)
                } catch (e: Exception) {
                    /*runOnUiThread {
                        Toast.makeText(
                            this@AudioTrimmerActivity,
                            "${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }*/
                    e.printStackTrace()
                    return
                }
                /* if (mLoadingKeepGoing) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(AudioTrimmerActivity.this, ""+mLoadedSoundFile, Toast.LENGTH_SHORT).show();
                                }
                            });
                           */
                /* audioWaveform.setVisibility(View.INVISIBLE);
                            audioWaveform.setBackgroundColor(getResources().getColor(R.color.waveformUnselectedBackground));
                            audioWaveform.setIsDrawBorder(false);*/
                /*
                            //finishOpeningSoundFile(mLoadedSoundFile, 0);
                        }
                    };
                    mHandler.post(runnable);
                }*/
            }
        }
        mLoadSoundFileThread.start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        handlePause()
        if (mPlayer != null && mPlayer!!.isPlaying) {
            mPlayer!!.release()
        }
    }

}
package com.knesarcreation.playbeat.dialogs

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.DialogFragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.DialogSleepTimerBinding
import com.knesarcreation.playbeat.extensions.addAccentColor
import com.knesarcreation.playbeat.extensions.colorButtons
import com.knesarcreation.playbeat.extensions.materialDialog
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.service.MusicService
import com.knesarcreation.playbeat.service.MusicService.Companion.ACTION_QUIT
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.TimerUpdater

class SleepTimerDialog : DialogFragment() {

    companion object {
        var sleepListener: ISleepTimerCallback? = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            sleepListener = context as ISleepTimerCallback
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    interface ISleepTimerCallback {
        fun onSleepTimerStart()
        fun onSleepTimerCancel()
    }

    private var seekArcProgress: Int = 0

    //private lateinit var timerUpdater: TimerUpdater
    private lateinit var dialog: MaterialDialog
    private lateinit var shouldFinishLastSong: CheckBox
    private lateinit var timerDisplay: TextView
    private var cancelBtnName = ""
    private var timerUpdater: TimerUpdater? = null
    @SuppressLint("ScheduleExactAlarm")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogSleepTimerBinding.inflate(layoutInflater)
        shouldFinishLastSong = binding.shouldFinishLastSong
        timerDisplay = binding.timerDisplay

        cancelBtnName = if (PreferenceUtil.isSleepTimeEnable) {
            "Stop"
        } else {
            "Cancel"
        }

        if (PreferenceUtil.isSleepTimeEnable) {
            timerUpdater = TimerUpdater(binding.countingTime, PreferenceUtil.sleepTime)
            timerUpdater?.start()
        }

        val finishMusic = PreferenceUtil.isSleepTimerFinishMusic
        shouldFinishLastSong.apply {
            addAccentColor()
            isChecked = finishMusic
        }

        binding.seekBar.apply {
            addAccentColor()
            seekArcProgress = PreferenceUtil.lastSleepTimerValue
            updateTimeDisplayTime()
            progress = seekArcProgress
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (i < 1) {
                    seekBar.progress = 1
                    return
                }
                seekArcProgress = i
                updateTimeDisplayTime()

                shouldFinishLastSong.isChecked = false
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                PreferenceUtil.lastSleepTimerValue = seekArcProgress
            }
        })
        return materialDialog(R.string.action_sleep_timer)
            .setView(binding.root)
            .setPositiveButton(R.string.action_set) { _, _ ->
                PreferenceUtil.isSleepTimerFinishMusic = shouldFinishLastSong.isChecked

                val duration: Long = MusicPlayerRemote.currentSong.duration
                val minutes: Long = if (shouldFinishLastSong.isChecked) {
                    duration
                } else {
                    seekArcProgress.toLong() * 60 * 1000
                }
                //val minutes = seekArcProgress
                val pendingIntent = makeTimerPendingIntent(PendingIntent.FLAG_CANCEL_CURRENT)
                PreferenceUtil.nextSleepTimerElapsedRealTime = minutes.toInt()
                val alarmManager = requireContext().getSystemService<AlarmManager>()
                if (pendingIntent != null) {
                    alarmManager?.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + minutes,
                        pendingIntent
                    )
                }

                Toast.makeText(
                    requireContext(),
                    "Sleet Timer is enabled",
                    Toast.LENGTH_SHORT
                ).show()

                sleepListener?.onSleepTimerStart()
                //timerUpdater = TimerUpdater()
                //timerUpdater.start()
            }
            .setNegativeButton(cancelBtnName) { _, _ ->
                val previous = makeTimerPendingIntent(PendingIntent.FLAG_NO_CREATE)
                if (previous != null) {
                    val am = requireContext().getSystemService<AlarmManager>()
                    am?.cancel(previous)
                    previous.cancel()
                    Toast.makeText(
                        requireContext(),
                        requireContext().resources.getString(R.string.sleep_timer_canceled),
                        Toast.LENGTH_SHORT
                    ).show()
                    val musicService = MusicPlayerRemote.musicService
                    if (musicService != null && musicService.pendingQuit) {
                        musicService.pendingQuit = false
                        Toast.makeText(
                            requireContext(),
                            requireContext().resources.getString(R.string.sleep_timer_canceled),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    sleepListener?.onSleepTimerCancel()
                }
            }
            .create()
            .colorButtons()
    }

    private fun updateTimeDisplayTime() {
        timerDisplay.text = "$seekArcProgress min"
    }

    private fun makeTimerPendingIntent(flag: Int): PendingIntent? {
        return PendingIntent.getService(
            requireActivity(), 0, makeTimerIntent(), flag or if (VersionUtils.hasMarshmallow())
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )
    }

    private fun makeTimerIntent(): Intent {
        val intent = Intent(requireActivity(), MusicService::class.java)
        return intent.setAction(ACTION_QUIT)
    }

    private fun updateCancelButton() {
        val musicService = MusicPlayerRemote.musicService
        if (musicService != null && musicService.pendingQuit) {
            dialog.getActionButton(WhichButton.NEUTRAL).text =
                dialog.context.getString(R.string.cancel_current_timer)
        } else {
            dialog.getActionButton(WhichButton.NEUTRAL).text = null
        }

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (timerUpdater != null) {
            timerUpdater?.cancel()
        }
    }

}

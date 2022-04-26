package com.knesarcreation.playbeat.util

import android.os.CountDownTimer
import android.view.View
import com.google.android.material.textview.MaterialTextView

class TimerUpdater(
    private var sleepTimeTV: MaterialTextView,
    sleepTime: Long,
) : CountDownTimer(
    sleepTime,
    1000
) {

    override fun onTick(millisUntilFinished: Long) {
        sleepTimeTV.visibility = View.VISIBLE
        sleepTimeTV.text = millisToMinutesAndSeconds(millisUntilFinished)
        PreferenceUtil.sleepTime = millisUntilFinished
    }

    override fun onFinish() {
    }

    private fun millisToMinutesAndSeconds(millis: Long): String {
        val minutes = kotlin.math.floor((millis / 60000).toDouble())
        val seconds = ((millis % 60000) / 1000)
        return if (seconds.toInt() == 60) "${(minutes.toInt() + 1)} : 00" else "${minutes.toInt()} : ${if (seconds < 10) "0" else ""}$seconds "
    }
}


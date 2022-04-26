package com.knesarcreation.playbeat.util


import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.*
import com.knesarcreation.playbeat.activities.bugreport.BugReportActivity
import com.knesarcreation.playbeat.equailizer.EqualizerControlActivity
import com.knesarcreation.playbeat.helper.MusicPlayerRemote.audioSessionId

object NavigationUtil {
    fun bugReport(activity: Activity) {
        activity.startActivity(
            Intent(activity, BugReportActivity::class.java), null
        )
    }

    fun goToOpenSource(activity: Activity) {
        activity.startActivity(
            Intent(activity, LicenseActivity::class.java), null
        )
    }

    /* fun goToSupportDevelopment(activity: Activity) {
         activity.startActivity(
             Intent(activity, SupportDevelopmentActivity::class.java), null
         )
     }*/


    fun gotoWhatNews(activity: Activity) {
        activity.startActivity(
            Intent(activity, WhatsNewActivity::class.java), null
        )
    }

    fun openEqualizer(activity: Activity) {
        //stockEqualizer(activity)
        activity.startActivity(
            Intent(activity, EqualizerControlActivity::class.java), null
        )
    }

    private fun stockEqualizer(activity: Activity) {
        val sessionId = audioSessionId
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(
                activity, activity.resources.getString(R.string.no_audio_ID), Toast.LENGTH_LONG
            )
                .show()
        } else {
            try {
                val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                effects.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                activity.startActivityForResult(effects, 0)
            } catch (notFound: ActivityNotFoundException) {
                Toast.makeText(
                    activity,
                    activity.resources.getString(R.string.no_equalizer),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }
}
package com.knesarcreation.playbeat.utils

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.jaredrummler.cyanea.Cyanea
import com.jaredrummler.cyanea.inflator.CyaneaViewProcessor
import com.jaredrummler.cyanea.inflator.decor.CyaneaDecorator
import com.jaredrummler.cyanea.inflator.decor.FontDecorator

class ApplicationChannel : Application(), CyaneaDecorator.Provider, CyaneaViewProcessor.Provider {

    companion object {
        const val CHANNEL_NAME = "PlayBeat"
        const val CHANNEL_ID = "PlayBeat_ID"
        const val ACTION_PREV = "action_prev"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PLAY_PAUSE = "action_play"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels()
        }
        Cyanea.init(this, resources)
        Cyanea.loggingEnabled = true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannels() {
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
//        channel.enableLights(true)
        channel.enableVibration(false)
//        channel.lightColor = R.color.green
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

    }

    override fun getViewProcessors(): Array<CyaneaViewProcessor<out View>> = arrayOf(
        // Add a view processor to manipulate a view when inflated.
    )

    override fun getDecorators(): Array<CyaneaDecorator> = arrayOf(
        // Add a decorator to apply custom attributes to any view
        FontDecorator()
    )
}

package com.knesarcreation.playbeat.utils

import android.content.Context
import com.google.android.gms.cast.CredentialsData
import com.google.android.gms.cast.LaunchOptions
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.knesarcreation.playbeat.R


class CastOptionProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        /*val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()*/
        /*val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()*/
        val credentialsData = CredentialsData.Builder()
            .setCredentials("{\"userId\": \"abc\"}")
            .build()
        val launchOptions = LaunchOptions.Builder()
            .setAndroidReceiverCompatible(false)
            .setCredentialsData(credentialsData)
            .build()
        return CastOptions.Builder()
            .setLaunchOptions(launchOptions)
            .setReceiverApplicationId(context.getString(R.string.app_id))
            //.setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
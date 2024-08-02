package com.knesarcreation.playbeat.util

import android.app.Activity
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.knesarcreation.playbeat.R

abstract class FirebaseRemote(private val activity: Activity) {
    private val mFirebaseRemoteConfig: FirebaseRemoteConfig
    private val editor: SharedPreferences.Editor
    abstract fun onFetchComplete(updated: Boolean)

    init {
        //Log.e("TAG", "FirebaseRemote constructor ");
        FirebaseApp.initializeApp(activity)
        val msharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        editor = msharedPreferences.edit()
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    }

    fun fetchRemoteConfig() {
        // Log.e("TAG", "fetchRemoteConfig ");

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }

        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(
            activity
        ) { task ->
            if (task.isSuccessful) {
                val updated = task.result
                if (updated) {
//                    val INTERSTITIAL_DOWNLOAD: String =
//                        mFirebaseRemoteConfig.getString("INTERSTITIAL_DOWNLOAD")
//                    val INTERSTITIAL_HISTORY: String =
//                        mFirebaseRemoteConfig.getString("INTERSTITIAL_HISTORY")
//                    val OPEN_AD: String = mFirebaseRemoteConfig.getString("OPEN_AD")
//                    val INTERSTITIAL_WHATSAPP: String =
//                        mFirebaseRemoteConfig.getString("INTERSTITIAL_WHATSAPP")
//                    val NATIVE_EXIT_AD: String =
//                        mFirebaseRemoteConfig.getString("NATIVE_EXIT_AD")
//                    val NATIVE_HISTORY_AD: String =
//                        mFirebaseRemoteConfig.getString("NATIVE_HISTORY_AD")
//                    val NATIVE_HOME_AD: String =
//                        mFirebaseRemoteConfig.getString("NATIVE_HOME_AD")
//                    val NATIVE_LOADING_DIALOG: String =
//                        mFirebaseRemoteConfig.getString("NATIVE_LOADING_DIALOG")
//                    val REWARD_DOWNLOAD_HD_VIDEO: String =
//                        mFirebaseRemoteConfig.getString("REWARD_DOWNLOAD_HD_VIDEO")
//                    val policyViolateText: String = mFirebaseRemoteConfig.getString("policy_violate_text")
                    val atMeGamesBanner: String = mFirebaseRemoteConfig.getString("atMeGamesBanner")
                    val atMeGamesQuizIcon: String =
                        mFirebaseRemoteConfig.getString("atMeGamesQuizIcon")
                    val atMeNativeAtMeGameAd: String =
                        mFirebaseRemoteConfig.getString("atMeNativeAtMeGameAd")

//                    editor.putString("INTERSTITIAL_DOWNLOAD", INTERSTITIAL_DOWNLOAD)
//                    editor.putString("INTERSTITIAL_HISTORY", INTERSTITIAL_HISTORY)
//                    editor.putString("OPEN_AD", OPEN_AD)
//                    editor.putString("INTERSTITIAL_WHATSAPP", INTERSTITIAL_WHATSAPP)
//                    editor.putString("NATIVE_EXIT_AD", NATIVE_EXIT_AD)
//                    editor.putString("NATIVE_HISTORY_AD", NATIVE_HISTORY_AD)
//                    editor.putString("NATIVE_HOME_AD", NATIVE_HOME_AD)
//                    editor.putString("NATIVE_LOADING_DIALOG", NATIVE_LOADING_DIALOG)
//                    editor.putString("REWARD_DOWNLOAD_HD_VIDEO", REWARD_DOWNLOAD_HD_VIDEO)
//                    editor.putString("policyViolateText", policyViolateText)
                    editor.putString("atMeGamesBanner", atMeGamesBanner)
                    editor.putString("atMeGamesQuizIcon", atMeGamesQuizIcon)
                    editor.putString("atMeNativeAtMeGameAd", atMeNativeAtMeGameAd)
                    editor.apply()
                    onFetchComplete(updated)
                } else {
                    onFetchComplete(updated)
                }
            } else {
            }
        }
    }
}
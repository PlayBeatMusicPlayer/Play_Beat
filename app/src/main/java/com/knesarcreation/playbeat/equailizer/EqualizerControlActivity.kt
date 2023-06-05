package com.knesarcreation.playbeat.equailizer

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.knesarcreation.playbeat.INTERSTITIAL_EQUALIZER
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.base.AbsThemeActivity
import com.knesarcreation.playbeat.ads.InterstitialAdHelperClass
import com.knesarcreation.playbeat.extensions.setStatusBarColorAuto
import com.knesarcreation.playbeat.extensions.setTaskDescriptionColorAuto
import com.knesarcreation.playbeat.helper.MusicPlayerRemote.musicService
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.theme.ThemeManager


class EqualizerControlActivity : AbsThemeActivity() {
    private var sessionId: Int = 0
    private val equalizerSetting = Settings()
    private var mInterstitialAdHelperClass: InterstitialAdHelperClass? = null

    override fun onDestroy() {
        if (mInterstitialAdHelperClass != null)
            mInterstitialAdHelperClass = null
        super.onDestroy()
    }

    private fun updateTheme() {
        AppCompatDelegate.setDefaultNightMode(ThemeManager.getNightMode())

        // Apply dynamic colors to activity if enabled
        if (PreferenceUtil.materialYou) {
            DynamicColors.applyIfAvailable(
                this,
                com.google.android.material.R.style.ThemeOverlay_Material3_DynamicColors_DayNight
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        updateTheme()
        super.onCreate(savedInstanceState)
        setStatusBarColorAuto()
        setTaskDescriptionColorAuto()
        setContentView(R.layout.equalizer_control_activity)
        // Initialize the Mobile Ads SDK.
        // MobileAds.initialize(this) {}

        equalizerSetting.loadEqualizerSettings(this)
        if (musicService != null) {
            if (musicService!!.playback != null) {
                sessionId = musicService!!.audioSessionId
            }
        }

        mInterstitialAdHelperClass = InterstitialAdHelperClass(this)
        mInterstitialAdHelperClass?.loadInterstitialAd(INTERSTITIAL_EQUALIZER)

        // mediaPlayer!!.isLooping = true
        val equalizerFragment = EqualizerFragment.newBuilder()
            .setAccentColor(Color.parseColor("#FF03DAC5"))
            .setAudioSessionId(sessionId)
            .build()
        supportFragmentManager.beginTransaction()
            .replace(R.id.eqFrame, equalizerFragment)
            .commit()


        // if (storage.getShouldWeShowInterstitialAdOnEQActivity()) {
        //     InterstitialAdHelper(this, this, 0).loadAd()
        //storage.shouldInterstitialAddForEQActivity(false)
        // }


    }

    override fun onStop() {
        super.onStop()
        equalizerSetting.saveEqualizerSettings(this)
    }


    companion object {
        const val PREF_KEY = "equalizer"
        const val AD_UNIT_ID = "ca-app-pub-3285111861884715/7699743038"
        const val AD_UNIT_ID_TEST = "ca-app-pub-3940256099942544/1033173712"
    }

    override fun onBackPressed() {
        if (!EqualizerFragment.userHasRewardGotOnce) {
            mInterstitialAdHelperClass?.showInterstitial(INTERSTITIAL_EQUALIZER)
        } else {
            EqualizerFragment.userHasRewardGotOnce = false
        }
        super.onBackPressed()
    }
}

package com.knesarcreation.playbeat.activity.equailizer

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.utils.InterstitialAdHelper
import com.knesarcreation.playbeat.utils.Settings
import com.knesarcreation.playbeat.utils.StorageUtil


class EqualizerControlActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var sessionId: Int = 0
    private val equalizerSetting = Settings()
    private var storage = StorageUtil(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.equalizer_control_activity)

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this) {}

        equalizerSetting.loadEqualizerSettings(this)
        if (AllSongFragment.musicService!!.mediaPlayer != null) {
            mediaPlayer = AllSongFragment.musicService!!.mediaPlayer
            sessionId = mediaPlayer!!.audioSessionId
        }

        // mediaPlayer!!.isLooping = true
        val equalizerFragment = EqualizerFragment.newBuilder()
            .setAccentColor(Color.parseColor("#FF03DAC5"))
            .setAudioSessionId(sessionId)
            .build()
        supportFragmentManager.beginTransaction()
            .replace(R.id.eqFrame, equalizerFragment)
            .commit()


        if (storage.getShouldWeShowInterstitialAdOnEQActivity()) {
            InterstitialAdHelper(this, this, 0).loadAd()
            //storage.shouldInterstitialAddForEQActivity(false)
        }


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
}

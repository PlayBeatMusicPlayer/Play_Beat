package com.knesarcreation.playbeat.activity.equailizer

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.fragment.AllSongFragment
import com.knesarcreation.playbeat.utils.Settings


class EqualizerControlActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var sessionId: Int = 0
    private val equalizerSetting = Settings()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.equalizer_control_activity)
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
    }

    /* private fun showInDialog() {
         val sessionId = mediaPlayer!!.audioSessionId
         if (sessionId > 0) {
             val fragment: DialogEqualizerFragment = DialogEqualizerFragment.newBuilder()
                 .setAudioSessionId(sessionId)
                 .title("Equalizer")
                 .themeColor(ContextCompat.getColor(this, R.color.teal_200))
                 .textColor(ContextCompat.getColor(this, R.color.white))
                 .accentAlpha(ContextCompat.getColor(this, R.color.teal_200))
                 .darkColor(ContextCompat.getColor(this, R.color.colorPrimary))
                 .setAccentColor(ContextCompat.getColor(this, R.color.colorAccent))
                 .build()
             fragment.show(supportFragmentManager, "eq")
         }
     }*/

    override fun onStop() {
        super.onStop()
        equalizerSetting.saveEqualizerSettings(this)
    }

    /*override fun onPause() {
        try {
            mediaPlayer!!.pause()
        } catch (ex: Exception) {
            //ignore
        }
        super.onPause()
    }*/

    /*override fun onResume() {
        super.onResume()
        try {
            Handler().postDelayed({ mediaPlayer!!.start() }, 2000)
        } catch (ex: Exception) {
            //ignore
        }
    }*/

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }*/

    /*override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.itemEqDialog) {
            showInDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }*/


    companion object {
        const val PREF_KEY = "equalizer"
    }
}

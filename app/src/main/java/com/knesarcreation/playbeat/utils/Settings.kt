package com.knesarcreation.playbeat.utils

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.knesarcreation.playbeat.activity.equailizer.EqualizerControlActivity
import com.knesarcreation.playbeat.fragment.AllSongFragment

class Settings {
    companion object {
        var isEqualizerEnabled = false
        var isEqualizerReloaded = false
        var seekbarpos = IntArray(5)
        var presetPos = 0
        var reverbPreset: Short = -1
        var bassStrength: Short = -1
        var equalizerModel: EqualizerModel? = null
        var ratio = 1.0
        var isEditing = false
    }

    fun applyEqualizer(context: Context) {

        if (AllSongFragment.musicService != null) {
            val preferences: SharedPreferences =
                context.getSharedPreferences(
                    EqualizerControlActivity.PREF_KEY,
                    AppCompatActivity.MODE_PRIVATE
                )
            val gson = Gson()
            val settings = gson.fromJson(
                preferences.getString(EqualizerControlActivity.PREF_KEY, "{}"),
                EqualizerSettings::class.java
            )

            if (AllSongFragment.musicService!!.mEqualizer != null)
                AllSongFragment.musicService!!.mEqualizer!!.release()
            if (AllSongFragment.musicService!!.bassBoost != null)
                AllSongFragment.musicService!!.bassBoost!!.release()
            if (AllSongFragment.musicService!!.presetReverb != null)
                AllSongFragment.musicService!!.presetReverb!!.release()

            AllSongFragment.musicService!!.mEqualizer =
                Equalizer(1000, AllSongFragment.musicService!!.mediaPlayer!!.audioSessionId)
            AllSongFragment.musicService!!.bassBoost =
                BassBoost(1000, AllSongFragment.musicService!!.mediaPlayer!!.audioSessionId)
            AllSongFragment.musicService!!.presetReverb =
                PresetReverb(1000, AllSongFragment.musicService!!.mediaPlayer!!.audioSessionId)

            AllSongFragment.musicService!!.bassBoost!!.enabled = settings.isEqualizerEnabled
            AllSongFragment.musicService!!.bassBoost!!.setStrength(settings.bassStrength)

            AllSongFragment.musicService!!.presetReverb!!.preset = settings.reverbPreset
            AllSongFragment.musicService!!.presetReverb!!.enabled = settings.isEqualizerEnabled

            AllSongFragment.musicService!!.mEqualizer!!.enabled =
                settings.isEqualizerEnabled

            if (settings.presetPos == 0) {
                //if preset is Custom
                for (bandIdx in 0 until AllSongFragment.musicService!!.mEqualizer!!.numberOfBands) {
                    AllSongFragment.musicService!!.mEqualizer!!.setBandLevel(
                        bandIdx.toShort(),
                        settings.seekbarpos[bandIdx].toShort()
                    )

                    /** writing this code again in for loop is **Just to avoid one bug**
                     * BUG:  when skipping audio from notification, AUDIO EQUALIZER is not getting enabled.
                     * Only for this condition I am writing this code in for loop.
                     * This is not a solution but a simple hack to avoid this weird bug */

                    AllSongFragment.musicService!!.mEqualizer!!.enabled =
                        settings.isEqualizerEnabled
                    AllSongFragment.musicService!!.bassBoost!!.enabled = settings.isEqualizerEnabled
                    AllSongFragment.musicService!!.presetReverb!!.enabled =
                        settings.isEqualizerEnabled

                }
            } else {
                AllSongFragment.musicService!!.mEqualizer!!.usePreset((settings.presetPos - 1).toShort())

                /** writing this code again in HANDLER is **Just to avoid one bug**
                 * BUG:  when skipping audio from notification, AUDIO EQUALIZER is not getting enabled.
                 * Only for this condition I am writing this code in HANDLER.
                 * This is not a solution but a simple hack to avoid this weird bug */
                android.os.Handler(Looper.myLooper()!!).postDelayed({
                    AllSongFragment.musicService!!.mEqualizer!!.enabled =
                        settings.isEqualizerEnabled
                    AllSongFragment.musicService!!.bassBoost!!.enabled = settings.isEqualizerEnabled
                    AllSongFragment.musicService!!.presetReverb!!.enabled =
                        settings.isEqualizerEnabled
                }, 1000)
            }
        }
    }

    fun saveEqualizerSettings(context: Context) {
        if (equalizerModel != null) {
            val settings = EqualizerSettings()
            settings.bassStrength = equalizerModel!!.bassStrength
            settings.presetPos = equalizerModel!!.presetPos
            settings.reverbPreset = equalizerModel!!.reverbPreset
            settings.seekbarpos = equalizerModel!!.seekbarpos
            settings.isEqualizerEnabled = isEqualizerEnabled
            settings.isEqualizerReloaded = isEqualizerReloaded
            val preferences: SharedPreferences = context.getSharedPreferences(
                EqualizerControlActivity.PREF_KEY,
                AppCompatActivity.MODE_PRIVATE
            )
            val gson = Gson()
            preferences.edit()
                .putString(EqualizerControlActivity.PREF_KEY, gson.toJson(settings))
                .apply()
        }
    }

    fun loadEqualizerSettings(context: Context) {
        val preferences = context.getSharedPreferences(
            EqualizerControlActivity.PREF_KEY,
            AppCompatActivity.MODE_PRIVATE
        )
        val gson = Gson()
        val settings = gson.fromJson(
            preferences.getString(EqualizerControlActivity.PREF_KEY, "{}"),
            EqualizerSettings::class.java
        )
        val model = EqualizerModel()
        model.bassStrength = settings.bassStrength
        model.presetPos = settings.presetPos
        model.reverbPreset = settings.reverbPreset
        model.seekbarpos = settings.seekbarpos
        model.isEqualizerReloaded = settings.isEqualizerReloaded
        model.isEqualizerEnabled = settings.isEqualizerEnabled
        isEqualizerEnabled = settings.isEqualizerEnabled
        isEqualizerReloaded = settings.isEqualizerReloaded
        bassStrength = settings.bassStrength
        presetPos = settings.presetPos
        reverbPreset = settings.reverbPreset
        seekbarpos = settings.seekbarpos

        equalizerModel = model
    }
}
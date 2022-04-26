package com.knesarcreation.playbeat.equailizer

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.knesarcreation.playbeat.helper.MusicPlayerRemote.musicService

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

        if (musicService != null) {
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

            if (musicService!!.mEqualizer != null)
                musicService!!.mEqualizer!!.release()
            if (musicService!!.bassBoost != null)
                musicService!!.bassBoost!!.release()
            if (musicService!!.presetReverb != null)
                musicService!!.presetReverb!!.release()

            musicService!!.mEqualizer =
                Equalizer(1000, musicService!!.audioSessionId)
            musicService!!.bassBoost =
                BassBoost(1000, musicService!!.audioSessionId)
            musicService!!.presetReverb =
                PresetReverb(1000, musicService!!.audioSessionId)

            musicService!!.bassBoost!!.enabled = settings.isEqualizerEnabled
            musicService!!.bassBoost!!.setStrength(settings.bassStrength)

            musicService!!.presetReverb!!.preset = settings.reverbPreset
            musicService!!.presetReverb!!.enabled = settings.isEqualizerEnabled

            musicService!!.mEqualizer!!.enabled =
                settings.isEqualizerEnabled

            if (settings.presetPos == 0) {
                //if preset is Custom
                for (bandIdx in 0 until musicService!!.mEqualizer!!.numberOfBands) {
                    musicService!!.mEqualizer!!.setBandLevel(
                        bandIdx.toShort(),
                        settings.seekbarpos[bandIdx].toShort()
                    )

                    /** writing this code again in for loop is **Just to avoid one bug**
                     * BUG:  when skipping audio from notification, AUDIO EQUALIZER is not getting enabled.
                     * Only for this condition I am writing this code in for loop.
                     * This is not a solution but a simple hack to avoid this weird bug */

                    musicService!!.mEqualizer!!.enabled =
                        settings.isEqualizerEnabled
                    musicService!!.bassBoost!!.enabled = settings.isEqualizerEnabled
                    musicService!!.presetReverb!!.enabled =
                        settings.isEqualizerEnabled

                }
            } else {
                musicService!!.mEqualizer!!.usePreset((settings.presetPos - 1).toShort())

                /** writing this code again in HANDLER is **Just to avoid one bug**
                 * BUG:  when skipping audio from notification, AUDIO EQUALIZER is not getting enabled.
                 * Only for this condition I am writing this code in HANDLER.
                 * This is not a solution but a simple hack to avoid this weird bug */
                android.os.Handler(Looper.myLooper()!!).postDelayed({
                    musicService!!.mEqualizer!!.enabled =
                        settings.isEqualizerEnabled
                    musicService!!.bassBoost!!.enabled = settings.isEqualizerEnabled
                    musicService!!.presetReverb!!.enabled =
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
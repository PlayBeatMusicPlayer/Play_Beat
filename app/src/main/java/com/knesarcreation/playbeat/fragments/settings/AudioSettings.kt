package com.knesarcreation.playbeat.fragments.settings

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.preference.Preference
import com.knesarcreation.playbeat.EQUALIZER
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.util.NavigationUtil

/**
 * @author Hemanth S (h4h13).
 */

class AudioSettings : AbsSettingsFragment() {
    override fun invalidateSettings() {
        val findPreference: Preference? = findPreference(EQUALIZER)
        if (!hasEqualizer()) {
            findPreference?.isEnabled = false
            findPreference?.summary = resources.getString(R.string.no_equalizer)
        } else {
            findPreference?.isEnabled = true
        }
        findPreference?.setOnPreferenceClickListener {
            NavigationUtil.openEqualizer(requireActivity())
            true
        }
    }

    private fun hasEqualizer(): Boolean {
        val effects = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)

        val pm = requireActivity().packageManager
        val ri = pm.resolveActivity(effects, 0)
        return ri != null
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_audio)
    }
}

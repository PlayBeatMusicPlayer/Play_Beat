package com.knesarcreation.playbeat.fragments.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.afollestad.materialdialogs.color.colorChooser
import com.google.android.material.color.DynamicColors
import com.knesarcreation.appthemehelper.ACCENT_COLORS
import com.knesarcreation.appthemehelper.ACCENT_COLORS_SUB
import com.knesarcreation.appthemehelper.ThemeStore
import com.knesarcreation.appthemehelper.common.prefs.supportv7.ATEColorPreference
import com.knesarcreation.appthemehelper.common.prefs.supportv7.ATEListPreference
import com.knesarcreation.appthemehelper.common.prefs.supportv7.ATESwitchPreference
import com.knesarcreation.appthemehelper.util.ColorUtil
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.*
import com.knesarcreation.playbeat.appshortcuts.DynamicShortcutManager
import com.knesarcreation.playbeat.extensions.materialDialog
import com.knesarcreation.playbeat.fragments.LibraryViewModel
import com.knesarcreation.playbeat.fragments.ReloadType
import com.knesarcreation.playbeat.util.NavigationUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class PlayBeatSettingPref : AbsSettingsFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val libraryViewModel by sharedViewModel<LibraryViewModel>()

    private var mContext: Context? = null

    @SuppressLint("CheckResult")
    override fun invalidateSettings() {
        val generalTheme: Preference? = findPreference(GENERAL_THEME)
        generalTheme?.let {
            setSummary(it)
            it.setOnPreferenceChangeListener { _, newValue ->
                val theme = newValue as String
                setSummary(it, newValue)
                ThemeStore.markChanged(requireContext())

                if (VersionUtils.hasNougatMR()) {
                    requireActivity().setTheme(PreferenceUtil.themeResFromPrefValue(theme))
                    DynamicShortcutManager(requireContext()).updateDynamicShortcuts()
                }
                restartActivity()
                true
            }
        }

        val accentColorPref: ATEColorPreference? = findPreference(ACCENT_COLOR)
        if (mContext != null) {
            val accentColor = ThemeStore.accentColor(mContext!!)
            accentColorPref?.setColor(accentColor, ColorUtil.darkenColor(accentColor))
            accentColorPref?.setOnPreferenceClickListener {
                materialDialog().show {
                    colorChooser(
                        initialSelection = accentColor,
                        showAlphaSelector = false,
                        colors = ACCENT_COLORS,
                        subColors = ACCENT_COLORS_SUB, allowCustomArgb = true
                    ) { _, color ->
                        ThemeStore.editTheme(requireContext()).accentColor(color).commit()
                        if (VersionUtils.hasNougatMR())
                            DynamicShortcutManager(requireContext()).updateDynamicShortcuts()
                        restartActivity()
                    }
                }
                return@setOnPreferenceClickListener true
            }
        }

        val blackTheme: ATESwitchPreference? = findPreference(BLACK_THEME)
        blackTheme?.setOnPreferenceChangeListener { _, _ ->
            ThemeStore.markChanged(requireContext())
            if (VersionUtils.hasNougatMR()) {
                requireActivity().setTheme(PreferenceUtil.themeResFromPrefValue("black"))
                DynamicShortcutManager(requireContext()).updateDynamicShortcuts()
            }
            restartActivity()
            true
        }

        val desaturatedColor: ATESwitchPreference? = findPreference(DESATURATED_COLOR)
        desaturatedColor?.setOnPreferenceChangeListener { _, value ->
            val desaturated = value as Boolean
            ThemeStore.prefs(requireContext()).edit {
                putBoolean("desaturated_color", desaturated)
            }
            PreferenceUtil.isDesaturatedColor = desaturated
            restartActivity()
            true
        }

        val colorAppShortcuts: TwoStatePreference? = findPreference(SHOULD_COLOR_APP_SHORTCUTS)
        if (!VersionUtils.hasNougatMR()) {
            colorAppShortcuts?.isVisible = false
        } else {
            colorAppShortcuts?.isChecked = PreferenceUtil.isColoredAppShortcuts
            colorAppShortcuts?.setOnPreferenceChangeListener { _, newValue ->
                PreferenceUtil.isColoredAppShortcuts = newValue as Boolean
                DynamicShortcutManager(requireContext()).updateDynamicShortcuts()
                true
            }
        }

        val materialYou: ATESwitchPreference? = findPreference(MATERIAL_YOU)
        materialYou?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                DynamicColors.applyToActivitiesIfAvailable(App.getContext())
            }
            restartActivity()
            true
        }
        val wallpaperAccent: ATESwitchPreference? = findPreference(WALLPAPER_ACCENT)
        wallpaperAccent?.setOnPreferenceChangeListener { _, _ ->
            restartActivity()
            true
        }
        val customFont: ATESwitchPreference? = findPreference(CUSTOM_FONT)
        customFont?.setOnPreferenceChangeListener { _, _ ->
            restartActivity()
            true
        }

        val toggleFullScreen: TwoStatePreference? = findPreference(TOGGLE_FULL_SCREEN)
        toggleFullScreen?.setOnPreferenceChangeListener { _, _ ->
            restartActivity()
            true
        }

        updateNowPlayingScreenSummary()
        updateAlbumCoverStyleSummary()

        val carouselEffect: TwoStatePreference? = findPreference(CAROUSEL_EFFECT)
        carouselEffect?.setOnPreferenceChangeListener { _, newValue ->
            return@setOnPreferenceChangeListener true
        }

        val autoDownloadImagesPolicy: Preference = findPreference(AUTO_DOWNLOAD_IMAGES_POLICY)!!
        setSummary(autoDownloadImagesPolicy)
        autoDownloadImagesPolicy.setOnPreferenceChangeListener { _, o ->
            setSummary(autoDownloadImagesPolicy, o)
            true
        }

        val findPreference: Preference? = findPreference(EQUALIZER)
        /*if (!hasEqualizer()) {
            findPreference?.isEnabled = false
            findPreference?.summary = resources.getString(R.string.no_equalizer)
        } else {
            findPreference?.isEnabled = true
        }*/
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabTextMode: ATEListPreference? = findPreference(TAB_TEXT_MODE)
        tabTextMode?.setOnPreferenceChangeListener { prefs, newValue ->
            setSummary(prefs, newValue)
            true
        }
        val preference: Preference? = findPreference(LAST_ADDED_CUTOFF)
        preference?.setOnPreferenceChangeListener { lastAdded, newValue ->
            setSummary(lastAdded, newValue)
            libraryViewModel.forceReload(ReloadType.HomeSections)
            true
        }

        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
        val preferenceAlbum: Preference? = findPreference(ALBUM_COVER_TRANSFORM)
        preferenceAlbum?.setOnPreferenceChangeListener { albumPrefs, newValue ->
            setSummary(albumPrefs, newValue)
            true
        }

        val preferenceImageSetting: Preference? = findPreference(AUTO_DOWNLOAD_IMAGES_POLICY)
        preferenceImageSetting?.let { setSummary(it) }
    }

    private fun updateAlbumCoverStyleSummary() {
        val preference: Preference? = findPreference(ALBUM_COVER_STYLE)
        preference?.setSummary(PreferenceUtil.albumCoverStyle.titleRes)
    }

    private fun updateNowPlayingScreenSummary() {
        val preference: Preference? = findPreference(NOW_PLAYING_SCREEN_ID)
        preference?.setSummary(PreferenceUtil.nowPlayingScreen.titleRes)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.setting_preference_ui)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            NOW_PLAYING_SCREEN_ID -> updateNowPlayingScreenSummary()
            ALBUM_COVER_STYLE -> updateAlbumCoverStyleSummary()
            CIRCULAR_ALBUM_ART, CAROUSEL_EFFECT -> invalidateSettings()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
}
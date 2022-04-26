package com.knesarcreation.playbeat.util.theme

import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import com.knesarcreation.playbeat.App
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.extensions.generalThemeValue
import com.knesarcreation.playbeat.util.PreferenceUtil
import com.knesarcreation.playbeat.util.theme.ThemeMode.*

object ThemeManager {

    @StyleRes
    fun getThemeResValue(): Int =
        if (PreferenceUtil.materialYou) {
            R.style.Theme_PlayBeat_MD3
        } else {
            when (App.getContext().generalThemeValue) {
                LIGHT -> R.style.Theme_PlayBeat_Light
                DARK -> R.style.Theme_PlayBeat_Base
                BLACK -> R.style.Theme_PlayBeat_Black
                AUTO -> R.style.Theme_PlayBeat_FollowSystem
            }
        }

    fun getNightMode(): Int = when (App.getContext().generalThemeValue) {
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
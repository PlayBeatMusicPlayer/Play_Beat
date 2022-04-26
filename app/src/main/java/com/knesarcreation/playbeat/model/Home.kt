package com.knesarcreation.playbeat.model


import androidx.annotation.StringRes
import com.knesarcreation.playbeat.HomeSection

data class Home(
    val arrayList: List<Any>,
    @HomeSection
    val homeSection: Int,
    @StringRes
    val titleRes: Int
)
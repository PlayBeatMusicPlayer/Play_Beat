package com.knesarcreation.playbeat.fragments

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.knesarcreation.playbeat.R

enum class NowPlayingScreen constructor(
    @param:StringRes @field:StringRes
    val titleRes: Int,
    @param:DrawableRes @field:DrawableRes val drawableResId: Int,
    val id: Int,
    val defaultCoverTheme: AlbumCoverStyle?
) {
    // Some Now playing themes look better with particular Album cover theme
    Normal(R.string.normal, R.drawable.np_normal, 0, AlbumCoverStyle.Normal),
    Blur(R.string.blur, R.drawable.np_blur, 4, AlbumCoverStyle.Normal),
    BlurCard(R.string.blur_card, R.drawable.np_blur_card, 9, AlbumCoverStyle.Card),
    Color(R.string.color, R.drawable.np_color, 5, AlbumCoverStyle.Normal),
    Fit(R.string.fit, R.drawable.np_fit, 12, AlbumCoverStyle.Full),
    Full(R.string.full, R.drawable.np_full, 2, AlbumCoverStyle.Full),
    Gradient(R.string.gradient, R.drawable.np_gradient, 17, AlbumCoverStyle.Full),
    Material(R.string.material, R.drawable.np_material, 11, AlbumCoverStyle.Normal),

    // Tiny(R.string.tiny, R.drawable.np_tiny, 7, null),
}

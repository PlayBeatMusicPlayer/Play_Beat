package com.knesarcreation.playbeat.views


import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.knesarcreation.appthemehelper.ThemeStore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.knesarcreation.appthemehelper.util.ATHUtil
import com.knesarcreation.appthemehelper.util.ColorUtil
import com.knesarcreation.appthemehelper.util.NavigationViewUtil
import com.knesarcreation.playbeat.util.PreferenceUtil
import dev.chrisbanes.insetter.applyInsetter

class BottomNavigationBarTinted @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    init {
        // If we are in Immersive mode we have to just set empty OnApplyWindowInsetsListener as
        // bottom, start, and end padding is always applied (with the help of OnApplyWindowInsetsListener) to
        // BottomNavigationView to dodge the system navigation bar (so we basically clear that listenerAudio).
        if (PreferenceUtil.isFullScreenMode) {
            setOnApplyWindowInsetsListener { _, insets ->
                insets
            }
        } else {
            applyInsetter {
                type(navigationBars = true) {
                    padding(vertical = true)
                    margin(horizontal = true)
                }
            }
        }

        labelVisibilityMode = PreferenceUtil.tabTitleMode

        if (!PreferenceUtil.materialYou) {
            val iconColor = ATHUtil.resolveColor(context, android.R.attr.colorControlNormal)
            val accentColor = ThemeStore.accentColor(context)
            NavigationViewUtil.setItemIconColors(
                this,
                ColorUtil.withAlpha(iconColor, 0.5f),
                accentColor
            )
            NavigationViewUtil.setItemTextColors(
                this,
                ColorUtil.withAlpha(iconColor, 0.5f),
                accentColor
            )
            itemRippleColor = ColorStateList.valueOf(accentColor.addAlpha(0.08F))
            itemActiveIndicatorColor = ColorStateList.valueOf(accentColor.addAlpha(0.12F))
        }
    }
}

fun Int.addAlpha(alpha: Float): Int {
    return ColorUtil.withAlpha(this, alpha)
}

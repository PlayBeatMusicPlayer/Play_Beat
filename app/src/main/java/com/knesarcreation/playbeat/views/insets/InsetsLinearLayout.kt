package com.knesarcreation.playbeat.views.insets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.knesarcreation.playbeat.extensions.drawAboveSystemBarsWithPadding
import com.knesarcreation.playbeat.util.PlayBeatUtil

class InsetsLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    init {
        if (!PlayBeatUtil.isLandscape())
            drawAboveSystemBarsWithPadding()
    }
}
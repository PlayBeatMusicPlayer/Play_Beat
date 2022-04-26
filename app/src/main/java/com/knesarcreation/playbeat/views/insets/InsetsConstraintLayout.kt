package com.knesarcreation.playbeat.views.insets

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.knesarcreation.playbeat.extensions.drawAboveSystemBarsWithPadding
import com.knesarcreation.playbeat.util.PlayBeatUtil

class InsetsConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        if (!PlayBeatUtil.isLandscape())
            drawAboveSystemBarsWithPadding()
    }
}
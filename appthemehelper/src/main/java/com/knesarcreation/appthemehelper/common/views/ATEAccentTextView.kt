
package com.knesarcreation.appthemehelper.common.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.knesarcreation.appthemehelper.ThemeStore

class ATEAccentTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        setTextColor(
            // Set MD3 accent if MD3 is enabled or in-app accent otherwise
            ThemeStore.accentColor(context)
        )
    }
}

package com.knesarcreation.appthemehelper.common

import androidx.appcompat.widget.Toolbar

import com.knesarcreation.appthemehelper.util.ToolbarContentTintHelper

class ATHActionBarActivity : ATHToolbarActivity() {

    override fun getATHToolbar(): Toolbar? {
        return ToolbarContentTintHelper.getSupportActionBarView(supportActionBar)
    }
}

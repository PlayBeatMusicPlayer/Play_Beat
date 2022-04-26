/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.knesarcreation.playbeat.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.ListItemViewNoCardBinding
import com.knesarcreation.playbeat.extensions.hide
import com.knesarcreation.playbeat.extensions.show

/**
 * Created by hemanths on 2019-10-02.
 */
class ListItemView : FrameLayout {

    private lateinit var binding: ListItemViewNoCardBinding

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        binding = ListItemViewNoCardBinding.inflate(LayoutInflater.from(context), this, true)

        context.withStyledAttributes(attrs, R.styleable.ListItemView) {
            if (hasValue(R.styleable.ListItemView_listItemIcon)) {
                binding.icon.setImageDrawable(getDrawable(R.styleable.ListItemView_listItemIcon))
            } else {
                binding.icon.hide()
            }

            binding.title.text = getText(R.styleable.ListItemView_listItemTitle)
            if (hasValue(R.styleable.ListItemView_listItemSummary)) {
                binding.summary.text = getText(R.styleable.ListItemView_listItemSummary)
            } else {
                binding.summary.hide()
            }
        }
    }

    fun setSummary(appVersion: String) {
        binding.summary.show()
        binding.summary.text = appVersion
    }
}
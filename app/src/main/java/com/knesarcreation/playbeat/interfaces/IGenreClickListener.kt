package com.knesarcreation.playbeat.interfaces

import android.view.View
import com.knesarcreation.playbeat.model.Genre

interface IGenreClickListener {
    fun onClickGenre(genre: Genre, view: View)
}
package com.knesarcreation.playbeat.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class DataObservableClass : ViewModel() {
    val albumData = MutableLiveData<String>()
    val artistsData = MutableLiveData<String>()
    val playlistCategory = MutableLiveData<String>()
    val customPlaylistData = MutableLiveData<String>()

    fun data(item: String) {
        albumData.value = item
    }
}
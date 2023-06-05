package com.knesarcreation.playbeat.interfaces

interface IPlaybackStateChanged {
    fun onStateChanged()

    interface TempInter {
        fun mNotify()
    }
}
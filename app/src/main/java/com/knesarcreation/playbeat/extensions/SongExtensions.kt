package com.knesarcreation.playbeat.extensions

import com.knesarcreation.playbeat.model.Song
import com.knesarcreation.playbeat.util.MusicUtil

val Song.uri get() = MusicUtil.getSongFileUri(songId = id)
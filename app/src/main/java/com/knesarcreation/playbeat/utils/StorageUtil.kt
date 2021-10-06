package com.knesarcreation.playbeat.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.database.AllSongsModel
import java.lang.reflect.Type
import java.util.concurrent.CopyOnWriteArrayList


class StorageUtil(context: Context) {
    companion object {
        const val STORAGE = "com.knesarcreation.playbeat.utils.STORAGE"
        const val AUDIO_KEY = "com.knesarcreation.playbeat.utils_AUDIO_KEY"
        const val ALBUM_AUDIO_KEY = "com.knesarcreation.playbeat.utils_ALBUM_AUDIO_KEY"
        const val FAV_AUDIO_KEY = "com.knesarcreation.playbeat.utils_FAV_AUDIO_KEY"
        const val PLAYLIST_KEY = "com.knesarcreation.playbeat.utils_PLAYLIST_KEY"
        const val PLAYLIST_AUDIO_KEY = "com.knesarcreation.playbeat.utils_PLAYLIST_AUDIO_KEY"
    }

    private var preferences: SharedPreferences? = null
    private val mContext: Context = context

    fun storeAudio(arrayList: CopyOnWriteArrayList<AllSongsModel>?) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        val gson = Gson()
        val json: String = gson.toJson(arrayList)
        editor.putString("audios", json)
        editor.apply()
    }

    fun loadAudio(): CopyOnWriteArrayList<AllSongsModel> {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val gson = Gson()
        val json = preferences!!.getString("audios", null)
        val type: Type = object : TypeToken<CopyOnWriteArrayList<AllSongsModel?>>() {}.type
        return gson.fromJson(json, type)
    }

    fun storeQueueAudio(arrayList: CopyOnWriteArrayList<AllSongsModel>?) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        val gson = Gson()
        val json: String = gson.toJson(arrayList)
        editor.putString("audioArrayList", json)
        editor.apply()
    }

    fun loadQueueAudio(): CopyOnWriteArrayList<AllSongsModel> {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val gson = Gson()
        val json = preferences!!.getString("audioArrayList", null)
        val type: Type = object : TypeToken<CopyOnWriteArrayList<AllSongsModel?>>() {}.type
        return gson.fromJson(json, type)
    }

    fun storeAudioIndex(index: Int) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putInt("audioIndex", index)
        editor.apply()
    }

    fun loadAudioIndex(): Int {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getInt("audioIndex", -1) //return -1 if no data found
    }

    fun storeAudioResumePos(position: Int) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putInt("resumePos", position)
        editor.apply()
    }

    fun getAudioResumePos(): Int {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getInt("resumePos", 0)
    }

    fun storeLastAudioMaxSeekProg(position: Int) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putInt("maxSeekProg", position)
        editor.apply()
    }

    fun getLastAudioMaxSeekProg(): Int {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getInt("maxSeekProg", 100)
    }

    fun saveIsShuffled(isShuffled: Boolean) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putBoolean("isShuffled", isShuffled)
        editor.apply()
    }

    fun getIsShuffled(): Boolean {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getBoolean("isShuffled", false)
    }

    fun saveIsAudioPlayedFirstTime(value: Boolean) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putBoolean("firstTime", value)
        editor.apply()
    }

    fun getIsAudioPlayedFirstTime(): Boolean {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getBoolean("firstTime", true)
    }

    fun saveIsRepeatAudio(isRepeat: Boolean) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putBoolean("isRepeat", isRepeat)
        editor.apply()
    }

    fun getIsRepeatAudio(): Boolean {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getBoolean("isRepeat", false)
    }

    fun saveSleepTime(timeInMin: Long) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putLong("sleepTime", timeInMin)
        editor.apply()
    }

    fun getSleepTime(): Long {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getLong("sleepTime", 0L)
    }

    /*fun saveSystemTime(systemTime: Long) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putLong("systemTime", systemTime)
        editor.apply()
    }

    fun getSystemTime(): Long {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getLong("systemTime", 0)
    }*/

    fun saveAudioSortingMethod(key: String, value: String) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getAudioSortedValue(key: String): String? {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getString(key, "normal")
    }

    /*fun saveFavAudioSortingMethod(value: String) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putString("favSortedAudio", value)
        editor.apply()
    }

    fun getFavAudioSortedValue(): String? {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getString("favSortedAudio", "normal")
    }

    fun saveAlbumSortingMethod(value: String) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putString("sortedAlbum", value)
        editor.apply()
    }

    fun getAlbumSortedValue(): String? {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getString("sortedAlbum", "normal")
    }

    fun savePlaylistSortingMethod(value: String) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putString("playlistSort", value)
        editor.apply()
    }

    fun getPlayListSortedValue(): String? {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getString("playlistSort", "normal")
    }*/


    fun saveAudioCount(audioCount: Int) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putInt("audioCount", audioCount)
        editor.apply()
    }

    fun getAudioCount(): Int {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getInt("audioCount", 0)
    }

    fun clearCachedAudioPlaylist() {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.clear()
        editor.apply()
    }
}
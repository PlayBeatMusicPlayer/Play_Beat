package com.knesarcreation.playbeat.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.model.AllSongsModel
import java.lang.reflect.Type
import java.util.concurrent.CopyOnWriteArrayList


class StorageUtil(context: Context) {
    companion object {
        const val STORAGE = "com.knesarcreation.playbeat.utils.STORAGE"
    }

    private var preferences: SharedPreferences? = null
    private val mContext: Context = context

    fun storeAudio(arrayList: CopyOnWriteArrayList<AllSongsModel>?) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        val gson = Gson()
        val json: String = gson.toJson(arrayList)
        editor.putString("audioArrayList", json)
        editor.apply()
    }

    fun loadAudio(): CopyOnWriteArrayList<AllSongsModel> {
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

    fun clearCachedAudioPlaylist() {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.clear()
        editor.apply()
    }

    fun saveIsShuffled(isShuffled:Boolean) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        editor.putBoolean("isShuffled", isShuffled)
        editor.apply()
    }

    fun getIsShuffled(): Boolean {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        return preferences!!.getBoolean("isShuffled", false)
    }

}
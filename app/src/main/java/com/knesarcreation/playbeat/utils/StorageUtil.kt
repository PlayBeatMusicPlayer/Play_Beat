package com.knesarcreation.playbeat.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.knesarcreation.playbeat.model.AllSongsModel
import java.lang.reflect.Type


class StorageUtil(context: Context) {
    companion object {
        const val STORAGE = "com.knesarcreation.playbeat.utils.STORAGE"
    }

    private var preferences: SharedPreferences? = null
    private val mContext: Context = context

    fun storeAudio(arrayList: ArrayList<AllSongsModel>?) {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val editor = preferences!!.edit()
        val gson = Gson()
        val json: String = gson.toJson(arrayList)
        editor.putString("audioArrayList", json)
        editor.apply()

        val gsona = Gson()
        val jsona = preferences!!.getString("audioArrayList", null)
        val type: Type = object : TypeToken<ArrayList<AllSongsModel?>>() {}.type
        val fromJson = gsona.fromJson<ArrayList<AllSongsModel>>(jsona, type)
        Log.d("fromJsonDAta", "storeAudio: $fromJson")
    }

    fun loadAudio(): ArrayList<AllSongsModel> {
        preferences = mContext.getSharedPreferences(STORAGE, AppCompatActivity.MODE_PRIVATE)
        val gson = Gson()
        val json = preferences!!.getString("audioArrayList", null)
        val type: Type = object : TypeToken<ArrayList<AllSongsModel?>>() {}.type
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

}
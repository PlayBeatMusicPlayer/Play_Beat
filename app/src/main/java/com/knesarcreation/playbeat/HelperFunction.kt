package com.knesarcreation.playbeat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.ImageView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import com.knesarcreation.playbeat.adapter.SliderAdapter

fun bannerSliderItems(firebasePrefs: SharedPreferences): MutableList<SliderAdapter.Page> {
    return when (firebasePrefs.getString("atMeGamesBanner", "0")) {
        "1" -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_01, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_01, PLAY_FREE_GAMES),
            )
        }
        "2" -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_02, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_02, PLAY_FREE_GAMES),
            )
        }
        "3" -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_03, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_03, PLAY_FREE_GAMES),
            )
        }
        "4" -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_04, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_04, PLAY_FREE_GAMES),
            )
        }
        "5" -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_05, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_05, PLAY_FREE_GAMES),
            )
        }
        "6" -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_02, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_06, PLAY_FREE_GAMES),
            )
        }
        "7" -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_03, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_07, PLAY_FREE_GAMES),
            )
        }
        else -> {
            mutableListOf(
                SliderAdapter.Page(R.drawable.banner_play_quiz_05, PLAY_QUIZ),
                SliderAdapter.Page(R.drawable.banner_free_game_03, PLAY_FREE_GAMES),
            )
        }
    }
}

fun setIconForQuiz(iv: ImageView, firebasePrefs: SharedPreferences) {
    when (firebasePrefs.getString("atMeGamesQuizIcon", "0")) {
        "1" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_01)
        }
        "2" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_02)
        }
        "3" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_03)
        }
        "4" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_04)
        }
        "5" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_05)
        }
        "6" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_06)
        }
        "7" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_07)
        }
        "8" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_08)
        }
        "9" -> {
            iv.setImageResource(R.drawable.icon_play_quiz_09)
        }
        else -> {
            iv.setImageResource(R.drawable.icon_play_quiz_07)
        }
    }
}

fun Fragment.openCustomTab(customTabsIntent: CustomTabsIntent, uri: Uri?) {
    // package name is the default package
    // for our custom chrome tab
    val packageName = "com.android.chrome"
    // we are checking if the package name is not null
    // if package name is not null then we are calling
    // that custom chrome tab with intent by passing its
    // package name.
    try {
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.launchUrl(requireContext(), uri!!)
    } catch (e: java.lang.Exception) {
        // in that custom tab intent we are passing
        // our url which we have to browse.
        this.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

fun Activity.openCustomTab(customTabsIntent: CustomTabsIntent, uri: Uri?) {
    // package name is the default package
    // for our custom chrome tab
    val packageName = "com.android.chrome"
    // we are checking if the package name is not null
    // if package name is not null then we are calling
    // that custom chrome tab with intent by passing its
    // package name.
    try {
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.launchUrl(this, uri!!)
    } catch (e: java.lang.Exception) {
        // in that custom tab intent we are passing
        // our url which we have to browse.
        this.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

fun Fragment.isConnectedToInternet(): Boolean {
    var isConnected = false
    val cm = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val hasTransport = cm.getNetworkCapabilities(cm.activeNetwork)
    if (hasTransport != null) {
        // connected to the internet
        if (hasTransport.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // connected to mobile data
            isConnected = true
        } else if (hasTransport.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            // connected to wifi
            isConnected = true
        }
    } else {
        // not connected to the internet
        isConnected = false
    }
    return isConnected
}
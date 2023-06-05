package com.knesarcreation.playbeat.fragments

import android.annotation.SuppressLint
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.knesarcreation.playbeat.BACK_SONG
import com.knesarcreation.playbeat.INTERSTITIAL_NEXT_BACK_AND_SWIPE_ANYWHERE_SONG
import com.knesarcreation.playbeat.NEXT_SONG
import com.knesarcreation.playbeat.ads.InterstitialAdHelperClass
import com.knesarcreation.playbeat.ads.NEXT_ADS_SHOW_TIME
import com.knesarcreation.playbeat.fragments.player.gradient.mInterstitialAdHelperClass
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import kotlinx.coroutines.*

/**
 * @param activity, Activity
 * @param next, if the button is next, if false then it's considered previous
 */
class MusicSeekSkipTouchListener(val activity: FragmentActivity, val next: Boolean) :
    View.OnTouchListener {

    var job: Job? = null
    var counter = 0
    var wasSeeking = false

    private val gestureDetector = GestureDetector(activity, object :
        GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            job = activity.lifecycleScope.launch(Dispatchers.Default) {
                counter = 0
                while (isActive) {
                    delay(500)
                    wasSeeking = true
                    var seekingDuration = MusicPlayerRemote.songProgressMillis
                    if (next) {
                        seekingDuration += 5000 * (counter.floorDiv(2) + 1)
                    } else {
                        seekingDuration -= 5000 * (counter.floorDiv(2) + 1)
                    }
                    MusicPlayerRemote.seekTo(seekingDuration)
                    counter += 1
                }
            }
            return super.onDown(e)
        }
    })

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            job?.cancel()
            if (!wasSeeking) {
                Log.d(
                    "Differencetime",
                    "onTouch: ${(System.currentTimeMillis() - InterstitialAdHelperClass.prevSeenAdsTime) / 1000}"
                )
                if (next) {
//                    if ((System.currentTimeMillis() - InterstitialAdHelperClass.prevSeenAdsTime) / 1000 >= NEXT_ADS_SHOW_TIME) {
//                        /* mInterstitialAdHelperClass?.showInterstitial(
//                             INTERSTITIAL_NEXT_BACK_AND_SWIPE_ANYWHERE_SONG,
//                             NEXT_SONG
//                         )*/
//                        if (
//                            com.knesarcreation.playbeat.fragments.base.mInterstitialAdHelperClass /*initialized in AbsPlayerFragment*/ != null) {
//                            /**
//                             * here mInterstitialAdHelperClass is initialized in all [AbsPlayerControlsFragment] which extends in all player fragment except the [GradientPlayerFragment]
//                             *  so it will be null when [GradientPlayerFragment] fragment is added on view.*/
//                            com.knesarcreation.playbeat.fragments.base.mInterstitialAdHelperClass?.showInterstitial(
//                                INTERSTITIAL_NEXT_BACK_AND_SWIPE_ANYWHERE_SONG,
//                                NEXT_SONG
//                            )
//                        } else {
//                            // for gradientFragmentPlayer : if mInterstitialAd is null then this code will run
//                            /** here mInterstitialAdHelperClass is initialized in [GradientPlayerFragment] so it will not be null,
//                             *  when [GradientPlayerFragment] fragment is added on view.*/
//                            mInterstitialAdHelperClass?.showInterstitial(
//                                INTERSTITIAL_NEXT_BACK_AND_SWIPE_ANYWHERE_SONG,
//                                NEXT_SONG
//                            )
//                        }
//                    } else {
                    MusicPlayerRemote.playNextSong()
                    // }

                } else {
//                    if ((System.currentTimeMillis() - InterstitialAdHelperClass.prevSeenAdsTime) / 1000 >= NEXT_ADS_SHOW_TIME) {
//                        /* mInterstitialAdHelperClass?.showInterstitial(
//                             INTERSTITIAL_NEXT_BACK_AND_SWIPE_ANYWHERE_SONG,
//                             BACK_SONG
//                         )*/
//                        if (
//                            com.knesarcreation.playbeat.fragments.base.mInterstitialAdHelperClass /*initialized in AbsPlayerFragment*/ != null) {
//                            /**
//                             * here mInterstitialAdHelperClass is initialized in all [AbsPlayerControlsFragment] which extends in all player fragment except the [GradientPlayerFragment]
//                             *  so it will be null when [GradientPlayerFragment] fragment is added on view.*/
//                            com.knesarcreation.playbeat.fragments.base.mInterstitialAdHelperClass?.showInterstitial(
//                                INTERSTITIAL_NEXT_BACK_AND_SWIPE_ANYWHERE_SONG,
//                                BACK_SONG
//                            )
//                        } else {
//                            // for gradientFragmentPlayer : if mInterstitialAd is null then this code will run
//                            /** here mInterstitialAdHelperClass is initialized in [GradientPlayerFragment] so it will not be null,
//                             *  when [GradientPlayerFragment] fragment is added on view.*/
//                            mInterstitialAdHelperClass?.showInterstitial(
//                                INTERSTITIAL_NEXT_BACK_AND_SWIPE_ANYWHERE_SONG,
//                                BACK_SONG
//                            )
//                        }
//                    } else {
                    MusicPlayerRemote.back()
//                    }
                }
            }
            wasSeeking = false
        }
        return event.let { gestureDetector.onTouchEvent(it) }
    }
}
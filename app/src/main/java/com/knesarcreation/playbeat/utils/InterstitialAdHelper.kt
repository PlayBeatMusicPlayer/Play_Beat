package com.knesarcreation.playbeat.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.knesarcreation.playbeat.activity.equailizer.EqualizerControlActivity

class InterstitialAdHelper(
    var context: Context,
    var activity: Activity,
    val mode: Int
) {
    private var storage = StorageUtil(context)
    private var mInterstitialAd: InterstitialAd? = null
    private var TAG = "EqualizerControlActivity"

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context, EqualizerControlActivity.AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("InterstialAddError", adError.message)
                    mInterstitialAd = null
                    //mAdIsLoading = false
                    val error = "domain: ${adError.domain}, code: ${adError.code}, " +
                            "message: ${adError.message}"
                    Log.d(TAG, "onAdFailedToLoad: $error")
                    //Toast.makeText(
                    //    context,
                    //    "onAdFailedToLoad() with error $error",
                    //    Toast.LENGTH_SHORT
                    //).show()

                    when (mode) {
                        0 -> {// equalizer
                            storage.shouldInterstitialAddForEQActivity(true)
                        }
                        1 -> {//trim}
                            storage.showInterstitialAddForTrimActivity(true)
                        }

                        2 -> {//tag}
                            storage.showInterstitialAddForTagActivity(true)
                        }
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("InterstialAddError", "Ad was loaded.")
                    //Toast.makeText(context, "onAdLoaded()", Toast.LENGTH_SHORT).show()
                    mInterstitialAd = interstitialAd
                    showInterstitial()
                    when (mode) {
                        0 -> {// equalizer
                            storage.shouldInterstitialAddForEQActivity(false)
                        }
                        1 -> {//trim}
                            storage.showInterstitialAddForTrimActivity(false)
                        }
                        2 -> {//tag}
                            storage.showInterstitialAddForTagActivity(false)
                        }
                    }
                }
            }
        )
    }

    // Show the ad if it's ready. Otherwise toast and restart the game.
    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mInterstitialAd = null
                        //loadAd()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(TAG, "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mInterstitialAd = null
                        when (mode) {
                            0 -> {// equalizer
                                storage.shouldInterstitialAddForEQActivity(true)
                            }
                            1 -> {//trim}
                                storage.showInterstitialAddForTrimActivity(true)
                            }
                            2 -> {//tag}
                                storage.showInterstitialAddForTagActivity(true)
                            }

                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad showed fullscreen content.")
                        when (mode) {
                            0 -> {// equalizer
                                storage.shouldInterstitialAddForEQActivity(false)
                            }
                            1 -> {//trim}
                                storage.showInterstitialAddForTrimActivity(false)
                            }

                            2 -> {//tag}
                                storage.showInterstitialAddForTagActivity(false)
                            }

                        }

                        // Called when ad is dismissed.
                    }
                }
            mInterstitialAd?.show(activity)
        } else {
            // Toast.makeText(this, "Ad wasn't loaded.", Toast.LENGTH_SHORT).show()
            //startGame()
        }
    }
}
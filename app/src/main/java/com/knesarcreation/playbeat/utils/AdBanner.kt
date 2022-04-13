package com.knesarcreation.playbeat.utils

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.*

class AdBanner(var context: Context, var adViewContainer: FrameLayout) {
    private val adSize: AdSize
        get() {
            val display = (context as AppCompatActivity).windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = adViewContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                context,
                adWidth
            )
        }

    fun initializeAddMob() {
        // initialize adMob inside the expandable Player

        val adView = AdView(context)
        adViewContainer.addView(adView)
        adView.adSize = adSize
        adView.adUnitId = "ca-app-pub-3285111861884715/7459974421"
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Log.d("onAdLoaded..", "onAdLoaded: ad Loaded ")
                adViewContainer.visibility = View.VISIBLE
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Code to be executed when an ad request fails.
                Log.d("onAdFailedToLoad..", "onAdLoaded: ad Failed $adError ")
                //Toast.makeText(context, "${adError.responseInfo}", Toast.LENGTH_SHORT).show()
                adViewContainer.visibility = View.GONE

            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Log.d("onAdOpened", "onAdLoaded: ad opened ")
                adViewContainer.visibility = View.VISIBLE

            }

            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
                Log.d("onAdClicked", "onAdLoaded: ad clicked ")
                adViewContainer.visibility = View.VISIBLE

            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
                Log.d("onAdClosed", "onAdLoaded: ad closed ")
                adViewContainer.visibility = View.VISIBLE
            }
        }

    }

}
package com.knesarcreation.playbeat.ads

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.button.MaterialButton
import com.google.android.gms.ads.*
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.showAtMeNativeBannerWithoutMedia
import com.knesarcreation.playbeat.util.isConnectedToInternet

class NewNativeAdHelper(var context: Context) {

    var currentNativeAd: NativeAd? = null

    //var prefs: Prefs? = null
    var isAdLoaded = false
    var nativeShimmer: ShimmerFrameLayout? = null

    init {
        //  prefs = Prefs(context)
    }

    // MobileAds.initialize(this) {}

    /**
     * Populates a [UnifiedNativeAdView] object with data from a given
     * [UnifiedNativeAd].
     *
     * @param nativeAd the object containing the ad's assets
     * @param adView the view to be populated
     */

    private fun populateUnifiedNativeAdView(
        nativeAd: NativeAd,
        adView: NativeAdView,
        showMedia: Boolean
    ) {
        // Set the media view.
        if (showMedia)
            adView.mediaView = adView.findViewById(R.id.ad_media)

        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        //adView.priceView = adView.findViewById(R.id.ad_price)
        //  adView.starRatingView = adView.findViewById(R.id.ad_stars)
        //  adView.storeView = adView.findViewById(R.id.ad_store)
        //  adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        // adView.advertiserView?.animate()
        // The headline and media content are guaranteed to be in every UnifiedNativeAd.
        // val llNative = adView.findViewById<LinearLayout>(R.id.llNative)
        //if (showMedia) llNative.setBackgroundResource(R.drawable.curved_all_egdes)

        if (showMedia)
            adView.mediaView?.isVisible = true
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let { adView.mediaView?.setMediaContent(it) }

        //val nativeAdViewL = adView.findViewById<NativeAdView>(R.id.nativeAd)
        //nativeAdViewL.animate().setDuration(1000).alpha(1f)

        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as MaterialButton).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // if (nativeAd.price == null) {
        //     adView.priceView?.visibility = View.INVISIBLE
        // } else {
        //     adView.priceView?.visibility = View.VISIBLE
        //    (adView.priceView as TextView).text = nativeAd.price
        // }

        /*if (nativeAd.store == null) {
            adView.storeView?.visibility = View.INVISIBLE
        } else {
            adView.storeView?.visibility = View.VISIBLE
            (adView.storeView as TextView).text = nativeAd.store
        }*/

        /*if (nativeAd.starRating == null) {
            adView.starRatingView?.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar).rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView?.visibility = View.VISIBLE
        }*/

        /*if (nativeAd.advertiser == null) {
              adView.advertiserView?.visibility = View.INVISIBLE
          } else {
              (adView.advertiserView as TextView).text = nativeAd.advertiser
              adView.advertiserView?.visibility = View.VISIBLE
          }*/

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)

        // Get the video controller for the ad. One will always be provided, even if the ad doesn't
        // have a video asset.
        val vc = nativeAd.mediaContent?.videoController

        // Updates the UI to say whether or not this ad has a video asset.
        if (vc?.hasVideoContent()!!) {
            // adView.findViewById(R.id.videostatus_text).text = String.format(
            //     Locale.getDefault(),
            //     "Video status: Ad contains a %.2f:1 video asset.",
            //     vc.aspectRatio
            // )

            // Create a new VideoLifecycleCallbacks object and pass it to the VideoController. The
            // VideoController will call methods on this object when events occur in the video
            // lifecycle.
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                override fun onVideoEnd() {
                    // Publishers should allow native ads to complete video playback before
                    // refreshing or replacing them with another ad in the same UI location.
                    // refresh_button.isEnabled = true
                    // videostatus_text.text = "Video status: Video playback has ended."
                    super.onVideoEnd()
                }
            }
        } else {
            Log.d(
                "No_Native_video",
                "populateUnifiedNativeAdView: Video status: Ad does not contain a video asset. "
            )
            // videostatus_text.text = "Video status: Ad does not contain a video asset."
            //  refresh_button.isEnabled = true
        }
    }

    /**
     * Creates a request for a new native ad based on the boolean parameters and calls the
     * corresponding "populate" method when one is successfully returned.
     *
     */
    @SuppressLint("ObsoleteSdkInt")
    fun refreshAd(
        adId: String,
        nativeLayout: FrameLayout,
        nativeShimmer: ShimmerFrameLayout,
        //nativeShimmer: ShimmerFrameLayout
    ) {
        //refresh_button.isEnabled = false
        val builder = AdLoader.Builder(context, adId)

        //this.nativeShimmer = nativeShimmer

        if (!context.isConnectedToInternet()) {
            //    nativeShimmer.isVisible = false
            //   nativeShimmer.stopShimmer()
        } else {
            //    nativeShimmer.isVisible = true
            //    nativeShimmer.startShimmer()

            builder.forNativeAd { nativeAd ->
                // OnUnifiedNativeAdLoadedListener implementation.
                // If this callback occurs after the activity is destroyed, you must call
                // destroy and return or you may get a memory leak.
                //this@NativeAdHelper.nativeAd = nativeAd
                isAdLoaded = true
                var activityDestroyed = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    activityDestroyed = (context as AppCompatActivity).isDestroyed
                }
                if (activityDestroyed /*|| isFinishing || isChangingConfigurations*/) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                // You must call destroy on old ads when you are done with them,
                // otherwise you will have a memory leak.
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd

                showNativeAd(
                    false, R.layout.native_layout_small, nativeLayout, nativeShimmer
                )
            }

            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()

            val adOptions = NativeAdOptions.Builder().setVideoOptions(videoOptions).build()

            builder.withNativeAdOptions(adOptions)

            val adLoader = builder.withAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    // nativeShimmer?.isVisible = false
                    // nativeShimmer?.stopShimmer()

                    super.onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val error = """
                   domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
                  """"

                    val firebasePrefs = PreferenceManager.getDefaultSharedPreferences(context)
                    nativeShimmer.stopShimmer()
                    nativeShimmer.isVisible = false
                    nativeLayout.removeAllViews()
                    context.showAtMeNativeBannerWithoutMedia(firebasePrefs, nativeLayout)
                    
                    Log.d(
                        "NativeAdFailed",
                        "onAdFailedToLoad: Small Failed to load native ad with error $error "
                    )
                }
            }).build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
        //videostatus_text.text = ""
    }

    private fun showNativeAd(
        showMedia: Boolean,
        layoutId: Int,
        frameLayout: FrameLayout,
        shimmer: ShimmerFrameLayout
    ) {
        val firebasePrefs = PreferenceManager.getDefaultSharedPreferences(context)

        if (isAdLoaded) {
            shimmer.stopShimmer()
            shimmer.isVisible = false
            val view = (context as AppCompatActivity).layoutInflater.inflate(
                layoutId, null
            )
            val adView = view.findViewById<NativeAdView>(R.id.nativeAd_small)
            currentNativeAd?.let { populateUnifiedNativeAdView(it, adView, showMedia) }
            frameLayout.removeAllViews()
            frameLayout.addView(view)
        } else {
            shimmer.stopShimmer()
            shimmer.isVisible = false
            frameLayout.removeAllViews()
            context.showAtMeNativeBannerWithoutMedia(firebasePrefs, frameLayout)
        }
    }
}
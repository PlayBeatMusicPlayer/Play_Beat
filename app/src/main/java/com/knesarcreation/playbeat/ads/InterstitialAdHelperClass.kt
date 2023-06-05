package com.knesarcreation.playbeat.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.knesarcreation.playbeat.*
import com.knesarcreation.playbeat.helper.MusicPlayerRemote
import com.knesarcreation.playbeat.model.Song

private var mInterstitialAd: InterstitialAd? = null

private const val TAG = "Interstitial_AD"
const val NEXT_ADS_SHOW_TIME = 60L //20sec
const val AD_LOADING_DIALOG_TIME = 1000

class InterstitialAdHelperClass(var context: Context) {

    private var adLoadingDialog: MaterialAlertDialogBuilder? = null
    private var createdAdLoadAlertDialog: AlertDialog? = null

    companion object {
        var prevSeenAdsTime = 0L
    }

    fun loadInterstitialAd(interstitial_ad: String) {
        val adRequest = AdRequest.Builder().build()

        //Interstitial_VIDEO : ca-app-pub-3940256099942544/8691691433
        ///Interstitial : ca-app-pub-3940256099942544/1033173712

        InterstitialAd.load(context,
            interstitial_ad,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Failed: $adError")
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                }
            })
    }

    fun showInterstitial(
        interstitial_ad: String, playORShuffle: String?, dataSet: List<Song>, layoutPosition: Int
    ) {
        if (mInterstitialAd != null) {
            if (createdAdLoadAlertDialog?.isShowing != true) showAdLoadingDialog()
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    mInterstitialAd = null
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }

                    loadInterstitialAd(interstitial_ad)
                    playSong(playORShuffle, dataSet, layoutPosition)
                    prevSeenAdsTime = System.currentTimeMillis()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    mInterstitialAd = null
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }
                    loadInterstitialAd(interstitial_ad)
                    playSong(playORShuffle, dataSet, layoutPosition)
                    //prevSeenAdsTime = System.currentTimeMillis()
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        createdAdLoadAlertDialog = null
                        adLoadingDialog = null
                    }

                    Log.d(TAG, "Ad showed fullscreen content.")
                    MusicPlayerRemote.pauseSong()
                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                (context as Activity).runOnUiThread {
                    mInterstitialAd?.show(context as Activity)
                }
            }, AD_LOADING_DIALOG_TIME.toLong())

        } else {
            playSong(playORShuffle, dataSet, layoutPosition)
        }
    }

    private fun playSong(playORShuffle: String?, dataSet: List<Song>, layoutPosition: Int) {
        when (playORShuffle) {
            PLAY_BUTTON -> MusicPlayerRemote.openQueue(dataSet, 0, true)

            SHUFFLE_BUTTON -> MusicPlayerRemote.openAndShuffleQueue(dataSet, true)

            SONG_CLICK -> MusicPlayerRemote.openQueue(dataSet, layoutPosition, true)
        }
    }

    fun showInterstitial(
        interstitial_ad: String, nextOrBack: String
    ) {
        if (mInterstitialAd != null) {
            if (createdAdLoadAlertDialog?.isShowing != true) showAdLoadingDialog()
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    mInterstitialAd = null
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }
                    loadInterstitialAd(interstitial_ad)
                    playNextOrBackSong(nextOrBack)
                    //MainActivity.mCountDownTimer?.start()
                    prevSeenAdsTime = System.currentTimeMillis()

                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    mInterstitialAd = null
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }
                    loadInterstitialAd(interstitial_ad)
                    playNextOrBackSong(nextOrBack)
                    // MainActivity.isCountDownFinished = true
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        createdAdLoadAlertDialog = null
                        adLoadingDialog = null
                    }

                    Log.d(TAG, "Ad showed fullscreen content.")
                    MusicPlayerRemote.pauseSong()

                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                (context as Activity).runOnUiThread {
                    mInterstitialAd?.show(context as Activity)
                }
            }, AD_LOADING_DIALOG_TIME.toLong())
        } else {
            playNextOrBackSong(nextOrBack)
        }
    }

    private fun playNextOrBackSong(nextOrBack: String) {
        when (nextOrBack) {
            NEXT_SONG -> MusicPlayerRemote.playNextSong()
            BACK_SONG -> MusicPlayerRemote.back()
        }
    }

    fun showInterstitial(
        interstitial_ad: String, position: Int
    ) {
        if (mInterstitialAd != null) {
            if (createdAdLoadAlertDialog?.isShowing != true) showAdLoadingDialog()
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    mInterstitialAd = null
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }
                    loadInterstitialAd(interstitial_ad)
                    MusicPlayerRemote.playSongAt(position)
                    //MainActivity.mCountDownTimer?.start()
                    prevSeenAdsTime = System.currentTimeMillis()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    mInterstitialAd = null
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }
                    loadInterstitialAd(interstitial_ad)
                    MusicPlayerRemote.playSongAt(position)
                    // MainActivity.isCountDownFinished = true
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        createdAdLoadAlertDialog = null
                        adLoadingDialog = null
                    }

                    Log.d(TAG, "Ad showed fullscreen content.")
                    MusicPlayerRemote.pauseSong()

                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                (context as Activity).runOnUiThread {
                    mInterstitialAd?.show(context as Activity)
                }
            }, AD_LOADING_DIALOG_TIME.toLong())
        } else {
            MusicPlayerRemote.playSongAt(position)
        }
    }

    fun showInterstitial(
        interstitial_ad: String,
    ) {
        if (mInterstitialAd != null) {
            if (createdAdLoadAlertDialog?.isShowing != true) showAdLoadingDialog()
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    mInterstitialAd = null
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }
                    loadInterstitialAd(interstitial_ad)
                    prevSeenAdsTime = System.currentTimeMillis()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        adLoadingDialog = null
                        createdAdLoadAlertDialog = null
                    }
                    mInterstitialAd = null
                    loadInterstitialAd(interstitial_ad)
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    if (createdAdLoadAlertDialog != null) {
                        createdAdLoadAlertDialog?.dismiss()
                        createdAdLoadAlertDialog = null
                        adLoadingDialog = null
                    }
                    Log.d(TAG, "Ad showed fullscreen content.")
                    MusicPlayerRemote.pauseSong()

                }
            }
            Handler(Looper.getMainLooper()).postDelayed({
                (context as Activity).runOnUiThread {
                    mInterstitialAd?.show(context as Activity)
                }
            }, AD_LOADING_DIALOG_TIME.toLong())
        }
    }

    private fun showAdLoadingDialog() {
        (context as Activity).runOnUiThread {
            adLoadingDialog = MaterialAlertDialogBuilder(context)
            val view = (context as AppCompatActivity).layoutInflater.inflate(
                R.layout.ad_loading_dialog, null
            )
            val okButton = view.findViewById<MaterialButton>(R.id.okButton)
            adLoadingDialog?.setView(view)
            /* adLoadingDialog?.setNegativeButton("Ok"){dialog,_->
                 dialog.dismiss()
             }*/
            createdAdLoadAlertDialog = adLoadingDialog?.create()
            okButton.setOnClickListener {
                createdAdLoadAlertDialog?.dismiss()
            }
            createdAdLoadAlertDialog?.show()
        }
    }
}
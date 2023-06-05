package com.knesarcreation.playbeat.fragments.settings

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.knesarcreation.appthemehelper.common.prefs.supportv7.ATEPreferenceFragmentCompat
import com.knesarcreation.playbeat.activities.OnThemeChangedListener
import com.knesarcreation.playbeat.fragments.NowPlayingScreen
import com.knesarcreation.playbeat.interfaces.IOnWatchRewardAdClick
import com.knesarcreation.playbeat.network.ConnectionManagerHelper
import com.knesarcreation.playbeat.preferences.*
import com.knesarcreation.playbeat.util.PreferenceUtil
import dev.chrisbanes.insetter.applyInsetter

/**
 * @author Hemanth S (h4h13).
 */

abstract class AbsSettingsFragment : ATEPreferenceFragmentCompat() {


    private var connectionManagerHelper: ConnectionManagerHelper? = null
    private lateinit var nowPlayingScreenPreferenceDialog: NowPlayingScreenPreferenceDialog
    private var mView: View? = null
    internal fun setSummary(preference: Preference, value: Any?) {
        val stringValue = value.toString()
        if (preference is ListPreference) {
            val index = preference.findIndexOfValue(stringValue)
            preference.setSummary(if (index >= 0) preference.entries[index] else null)
        } else {
            preference.summary = stringValue
        }
    }

    abstract fun invalidateSettings()

    protected fun setSummary(preference: Preference?) {
        preference?.let {
            setSummary(
                it, PreferenceManager
                    .getDefaultSharedPreferences(it.context)
                    .getString(it.key, "")
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.mView = view
        setDivider(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            listView.overScrollMode = View.OVER_SCROLL_NEVER
        }

        listView.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }
        invalidateSettings()
    }


    override fun onDisplayPreferenceDialog(preference: Preference) {
        when (preference) {
            is LibraryPreference -> {
                val fragment = LibraryPreferenceDialog.newInstance()
                fragment.show(childFragmentManager, preference.key)
            }
            is NowPlayingScreenPreference -> {
                nowPlayingScreenPreferenceDialog =
                    NowPlayingScreenPreferenceDialog.newInstance(object : IOnWatchRewardAdClick {
                        override fun onClickWatch(viewPagerPosition: Int) {
                            val selectedPlayingScreen = NowPlayingScreen.values()[viewPagerPosition]
                            val currentNowPlaying = PreferenceUtil.nowPlayingScreen

                            if (selectedPlayingScreen != currentNowPlaying) {
                                val showRewardAd =
                                    MaterialAlertDialogBuilder(requireContext())
                                showRewardAd.setTitle("Watch an Ad.")
                                showRewardAd.setMessage("Watch an ad to set the player theme.")
                                showRewardAd.setPositiveButton("Watch") { dialog, _ ->
                                    connectionManagerHelper =
                                        ConnectionManagerHelper(requireContext())
                                    val isConnected = connectionManagerHelper?.checkConnection()
                                    if (isConnected!!) {
                                        //load reward ad...
                                        showRewardedVideo(viewPagerPosition)
                                    } else {
                                        connectionManagerHelper?.showSnackBar(isConnected)
                                    }

                                    dialog.dismiss()
                                }
                                showRewardAd.setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                showRewardAd.show()
                            } else {
                                // do nothing
                            }

                        }
                    })
                nowPlayingScreenPreferenceDialog.show(childFragmentManager, preference.key)
            }
            is AlbumCoverStylePreference -> {
                val fragment = AlbumCoverStylePreferenceDialog.newInstance()
                fragment.show(childFragmentManager, preference.key)
            }
            is BlacklistPreference -> {
                val fragment = BlacklistPreferenceDialog.newInstance()
                fragment.show(childFragmentManager, preference.key)
            }
            is DurationPreference -> {
                val fragment = DurationPreferenceDialog.newInstance()
                fragment.show(childFragmentManager, preference.key)
            }
            else -> super.onDisplayPreferenceDialog(preference)
        }
    }

    fun restartActivity() {
        if (activity is OnThemeChangedListener) {
            (activity as OnThemeChangedListener).onThemeValuesChanged()
        } else {
            activity?.recreate()
        }
    }

    private fun showRewardedVideo(viewPagerPosition: Int) {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("onAdDismissedFullScreenContent", "Ad was dismissed.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null

                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.d("onAdFailedToShowFullScreenContent", "Ad failed to show.")
                        // Don't forget to set the ad reference to null so you
                        // don't show the ad a second time.
                        mRewardedAd = null
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("onAdShowedFullScreenContent", "Ad showed fullscreen content.")
                        // Called when ad is dismissed.
                    }
                }

            mRewardedAd?.show(
                requireActivity()
            ) {
                //addCoins(rewardAmount)
                applyTheme(viewPagerPosition)
            }
        } else {
            if (mView != null) {
                Snackbar.make(
                    mView!!, "No ad loaded this time, try again.",
                    Snackbar.LENGTH_SHORT
                ).show()
                applyTheme(viewPagerPosition)
            }
            // nowPlayingScreenPreferenceDialog.loadRewardedAd()
        }
    }

    private fun applyTheme(viewPagerPosition: Int) {
        val nowPlayingScreen = NowPlayingScreen.values()[viewPagerPosition]
        PreferenceUtil.nowPlayingScreen = nowPlayingScreen

        Toast.makeText(requireContext(), "Player theme applied.", Toast.LENGTH_SHORT).show()
        Log.d("UserEarnedreward", "User earned the reward.")
    }
}

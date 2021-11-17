package com.knesarcreation.playbeat.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.BuildConfig
import com.knesarcreation.playbeat.activity.AppThemesActivity
import com.knesarcreation.playbeat.databinding.FragmentSettingBinding
import com.knesarcreation.playbeat.utils.SavedAppTheme


class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
            duration = 200L
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
            duration = 200L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        val view = binding!!.root


        openLinkedIn()

        openGitHub()

        openMail()

        shareApp()

        rateOnPlayStore()

        openThemActivity()

        return view


    }

    private fun openThemActivity() {
        binding?.themeTV!!.setOnClickListener {
            startActivity(Intent(activity as Context, AppThemesActivity::class.java))
        }
    }

    private fun shareApp() {
        binding?.shareApp!!.setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Play Beat - Music Player\n\n")
                val shareMessage =
                    "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                Log.d("ExceptionShareApp", "shareApp: ${e.message}")
            }
        }
    }

    private fun rateOnPlayStore() {
        binding?.rateApp?.setOnClickListener {
            var intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.knesarcreation.playbeat")
            )
            val packageManager = (activity as AppCompatActivity).packageManager
            val list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (list.isEmpty()) {
                intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.knesarcreation.playbeat")
                    )
            }
            startActivity(intent)
        }
    }

    private fun openMail() {
        binding?.openMailBtn?.setOnClickListener {
            try {
                val email = Intent(Intent.ACTION_SEND)
                email.putExtra(Intent.EXTRA_EMAIL, arrayOf("kaunainnesar26@gmail.com"))
                // email.addCategory(Intent.CATEGORY_APP_EMAIL)
                email.putExtra(Intent.EXTRA_SUBJECT, "")
                email.putExtra(Intent.EXTRA_TEXT, "")

                //need this to prompts email client only
                email.type = "message/rfc822"
                startActivity(Intent.createChooser(email, "Send mail..."))
            } catch (e: ActivityNotFoundException) {
                Snackbar.make(
                    (activity as AppCompatActivity).window.decorView,
                    "There is no email client installed.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openGitHub() {
        binding?.openGitHubBtn?.setOnClickListener {
            var intent = Intent(Intent.ACTION_VIEW, Uri.parse("github://Kaunain26"))
            val packageManager = (activity as AppCompatActivity).packageManager
            val list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (list.isEmpty()) {
                intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kaunain26"))
            }
            startActivity(intent)
        }
    }

    private fun openLinkedIn() {
        binding?.openLinkedInBtn?.setOnClickListener {
            var intent = Intent(Intent.ACTION_VIEW, Uri.parse("linkedin://kaunainesar"))
            val packageManager = (activity as AppCompatActivity).packageManager
            val list =
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (list.isEmpty()) {
                intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.linkedin.com/in/kaunainesar")
                    )
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        SavedAppTheme(
            activity as Context,
            null,
            null,
            null,
            isHomeFrag = false,
            isHostActivity = false,
            tagEditorsBG = null,
            isTagEditor = false,
            bottomBar = null,
            rlMiniPlayerBottomSheet = null,
            bottomShadowIVAlbumFrag = null,
            isAlbumFrag = false,
            topViewIV = null,
            bottomShadowIVArtistFrag = null,
            isArtistFrag = false,
            topViewIVArtistFrag = null,
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            null,
            false,
            null,
            false,
            binding!!.settingFragBg,
            true
        ).settingSavedBackgroundTheme()
    }

}
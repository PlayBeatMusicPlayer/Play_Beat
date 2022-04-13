package com.knesarcreation.playbeat.fragment

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.BuildConfig
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.databinding.FragmentSettingBinding
import com.knesarcreation.playbeat.utils.LoadAllAudios
import com.knesarcreation.playbeat.utils.SavedAppTheme
import com.knesarcreation.playbeat.utils.StorageUtil
import kotlin.math.roundToInt


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
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        val view = binding!!.root


        binding?.versionNameTV!!.text = "Version: ${BuildConfig.VERSION_NAME}"

        openLinkedIn()

        openGitHub()

        openMail()

        shareApp()

        rateOnPlayStore()

        openThemActivity()

        openPlayBeatInstaPage()

        filterAudio()

        openChangeLog()

        return view


    }

    private fun openChangeLog() {
        binding?.appChangeLog?.setOnClickListener {
            val bottomSheetWhatsNew = BottomSheetWhatsNew()
            bottomSheetWhatsNew.show(
                (activity as AppCompatActivity).supportFragmentManager,
                "bottomSheetWhatsNew"
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun filterAudio() {
        binding?.filterSongs?.setOnClickListener {
            val showFilterDialog =
                MaterialAlertDialogBuilder(activity as Context, R.style.CustomAlertDialog)
            val customView = layoutInflater.inflate(R.layout.dialog_filter_songs, null)
            val sliderFilterAudio = customView.findViewById<Slider>(R.id.sliderfilterAudio)
            val cancelButton = customView.findViewById<MaterialButton>(R.id.cancelButton)
            val positiveBtn = customView.findViewById<MaterialButton>(R.id.positiveBtn)
            val secTV = customView.findViewById<TextView>(R.id.secTV)
            showFilterDialog.setView(customView)
            val dialog = showFilterDialog.create()
            dialog.show()

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }


            val storageUtil = StorageUtil(activity as Context)
            var duration = storageUtil.getFilterAudioDuration()

            if (storageUtil.getIsAudioPlayedFirstTime()) {
                duration = 20L
            }

            //Toast.makeText(activity as Context, "${duration.toFloat()}", Toast.LENGTH_SHORT).show()
            sliderFilterAudio.valueFrom = 0f
            sliderFilterAudio.valueTo = 120f

            sliderFilterAudio.value = duration.toFloat()
            secTV.text = "${duration.toFloat().roundToInt()} s"


            var selectedDuration = duration.toFloat().roundToInt()
            sliderFilterAudio.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}
                override fun onStopTrackingTouch(slider: Slider) {}
            })

            sliderFilterAudio.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    secTV.text = "${value.roundToInt()} s"
                    selectedDuration = value.roundToInt()
                }
            })

            positiveBtn.setOnClickListener {
                storageUtil.saveFilterAudioDuration(selectedDuration.toLong())
                val mAudioThread: Thread = object : Thread() {
                    override fun run() {
                        super.run()
                        LoadAllAudios(activity as Context, false).loadAudio(true)
                    }
                }
                mAudioThread.start()
                dialog.dismiss()

                val progressDialogAlert = MaterialAlertDialogBuilder(
                    activity as Context,
                    R.style.CustomAlertDialog
                )
                val customProgressView = layoutInflater.inflate(R.layout.custom_progress_bar, null)
                progressDialogAlert.setView(customProgressView)
                val loadingDialog = progressDialogAlert.create()
                //mProgressDialog = customProgressView.findViewById(R.id.progressView)
                //savingProgressBar = customProgressView.findViewById(R.id.savingProgressBar)
                val dialogTitle: TextView = customProgressView.findViewById(R.id.title)
                dialogTitle.text = "Scanning audio..."
                loadingDialog.setCanceledOnTouchOutside(false)
                loadingDialog.setCancelable(false)
                loadingDialog.show()

                Handler(Looper.getMainLooper()).postDelayed({
                    loadingDialog.dismiss()
                }, 3000)
            }
        }
    }

    private fun openThemActivity() {
        binding?.themeTV!!.setOnClickListener {
            //startActivity(Intent(activity as Context, AppThemesActivity::class.java))
            Toast.makeText(
                activity as Context,
                "For now App theme support disabled due to some incompatibility.",
                Toast.LENGTH_SHORT
            ).show()
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
            var openLinkedIn: Intent
            try {
                openLinkedIn =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/kaunainesar"))
                openLinkedIn.setPackage("com.linkedin.android")
                startActivity(openLinkedIn)
            } catch (e: Exception) {
                openLinkedIn =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/kaunainesar"))
                startActivity(openLinkedIn)
            }

            /* val packageManager = (activity as AppCompatActivity).packageManager
             val list =
                 packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
             if (list.isEmpty()) {
                 intent =
                     Intent(
                         Intent.ACTION_VIEW,
                         Uri.parse("https://www.linkedin.com/in/kaunainesar")
                     )
             }*/
        }
    }

    private fun openPlayBeatInstaPage() {
        binding!!.openPlayBeatInstaPage.setOnClickListener {
            var openPlayBeatInsta: Intent
            try {
                openPlayBeatInsta =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/play_beat01/"))
                openPlayBeatInsta.setPackage("com.instagram.android")
                startActivity(openPlayBeatInsta)
            } catch (e: Exception) {
                openPlayBeatInsta =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/play_beat01/"))
                startActivity(openPlayBeatInsta)
            }
            /* val packageManager = (activity as AppCompatActivity).packageManager
             val list = packageManager.queryIntentActivities(
                 openPlayBeatInsta,
                 PackageManager.MATCH_DEFAULT_ONLY
             )
             if (list.isEmpty()) {
                 openPlayBeatInsta =
                     Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/play_beat01/"))
             }*/
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
            parentViewArtistAndAlbumFrag = null,
            bottomShadowIVPlaylist = null,
            isPlaylistFragCategory = false,
            topViewIVPlaylist = null,
            playlistBG = null,
            isPlaylistFrag = false,
            searchFragBg = null,
            isSearchFrag = false,
            settingFragBg = binding!!.settingFragBg,
            isSettingFrag = true
        ).settingSavedBackgroundTheme()
    }

}
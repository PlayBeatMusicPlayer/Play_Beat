package com.knesarcreation.playbeat.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.knesarcreation.playbeat.databinding.FragmentSettingBinding


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
        return view


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

}
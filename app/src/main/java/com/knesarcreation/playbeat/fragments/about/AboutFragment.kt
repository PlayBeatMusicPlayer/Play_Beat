package com.knesarcreation.playbeat.fragments.about

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.knesarcreation.playbeat.Constants
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.adapter.ContributorAdapter
import com.knesarcreation.playbeat.databinding.FragmentAboutBinding
import com.knesarcreation.playbeat.extensions.applyToolbar
import com.knesarcreation.playbeat.fragments.LibraryViewModel
import com.knesarcreation.playbeat.util.NavigationUtil
import com.knesarcreation.playbeat.util.PlayBeatUtil
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class AboutFragment : Fragment(R.layout.fragment_about), View.OnClickListener {
    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private val libraryViewModel by sharedViewModel<LibraryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAboutBinding.bind(view)
        enterTransition = MaterialFadeThrough().addTarget(binding.container)
        reenterTransition = MaterialFadeThrough().addTarget(binding.container)
        binding.aboutContent.cardOther.version.setSummary(getAppVersion())
        binding.aboutContent.cardApp?.versionNameTV?.text = getAppVersion()
        setUpView()
        setupToolbar()
        loadContributors()

        openGitHub()
        openLinkedIn()
        openMail()
        openPlayBeatInstaPage()

        if (!PlayBeatUtil.isLandscape()) {
            binding.aboutContent.root.updatePadding(bottom = PlayBeatUtil.getNavigationBarHeight())
        }
    }

    private fun setupToolbar() {
        applyToolbar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun openUrl(url: String) {
        val i = Intent(Intent.ACTION_VIEW)
        i.data = url.toUri()
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(i)
    }

    private fun setUpView() {
        binding.aboutContent.cardPlayBeatInfo.appGithub.setOnClickListener(this)
        binding.aboutContent.cardPlayBeatInfo.appRate.setOnClickListener(this)
        binding.aboutContent.cardPlayBeatInfo.appShare.setOnClickListener(this)
        binding.aboutContent.cardOther.changelog.setOnClickListener(this)
        binding.aboutContent.cardOther.openSource.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.appGithub -> openUrl(Constants.GITHUB_PROJECT)
            R.id.appRate -> openUrl(Constants.RATE_ON_GOOGLE_PLAY)
            R.id.appShare -> shareApp()
            R.id.changelog -> NavigationUtil.gotoWhatNews(childFragmentManager)
            R.id.openSource -> NavigationUtil.goToOpenSource(requireActivity())
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo =
                requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "0.0.0"
        }
    }

    private fun shareApp() {
        ShareCompat.IntentBuilder(requireActivity()).setType("text/plain")
            .setChooserTitle(R.string.share_app)
            .setText(String.format(getString(R.string.app_share), requireActivity().packageName))
            .startChooser()
    }

    private fun loadContributors() {
        val contributorAdapter = ContributorAdapter(emptyList())
        binding.aboutContent.cardCredit.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = DefaultItemAnimator()
            adapter = contributorAdapter
        }
        libraryViewModel.fetchContributors().observe(viewLifecycleOwner) { contributors ->
            contributorAdapter.swapData(contributors)
        }
    }

    private fun openPlayBeatInstaPage() {
        binding.aboutContent.cardApp?.openPlayBeatInstaPage?.setOnClickListener {
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
        }
    }

    private fun openMail() {
        binding.aboutContent.cardCredit.openMailBtn.setOnClickListener {
            try {
                val email = Intent(Intent.ACTION_SEND)
                email.putExtra(Intent.EXTRA_EMAIL, arrayOf("nesar.kaunain@gmail.com"))
                email.putExtra(Intent.EXTRA_SUBJECT, "")
                email.putExtra(Intent.EXTRA_TEXT, "")
                email.setPackage("com.google.android.gm")

                //need this to prompts email client only
                email.type = "message/rfc822"
                startActivity(Intent.createChooser(email, "Send mail..."))
            } catch (e: java.lang.Exception) {
                val email = Intent(Intent.ACTION_SEND)
                email.putExtra(Intent.EXTRA_EMAIL, arrayOf("nesar.kaunain@gmail.com"))
                email.putExtra(Intent.EXTRA_SUBJECT, "")
                email.putExtra(Intent.EXTRA_TEXT, "")

                //need this to prompts email client only
                email.type = "message/rfc822"
                startActivity(Intent.createChooser(email, "Send mail..."))
            }
        }
    }

    private fun openGitHub() {
        binding.aboutContent.cardCredit.openGitHubBtn.setOnClickListener {
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
        binding.aboutContent.cardCredit.openLinkedInBtn.setOnClickListener {
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

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

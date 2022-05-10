package com.knesarcreation.playbeat.fragments.bottomSheets

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.knesarcreation.appthemehelper.ThemeStore.Companion.accentColor
import com.knesarcreation.appthemehelper.util.ATHUtil
import com.knesarcreation.appthemehelper.util.ColorUtil
import com.knesarcreation.appthemehelper.util.MaterialValueHelper
import com.knesarcreation.playbeat.databinding.BottomSheetWhatsNewBinding
import com.knesarcreation.playbeat.extensions.drawAboveSystemBars
import com.knesarcreation.playbeat.extensions.surfaceColor
import com.knesarcreation.playbeat.util.PreferenceUtil.lastVersion
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

class BottomSheetWhatsNew : BottomSheetDialogFragment() {
    private var _binding: BottomSheetWhatsNewBinding? = null
    private val binding get() = _binding!!
    private var mCtx: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mCtx = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetWhatsNewBinding.inflate(inflater, container, false)

        try {
            val buf = StringBuilder()
            val json = mCtx?.assets?.open("play_beat-changelog.html")
            BufferedReader(InputStreamReader(json, StandardCharsets.UTF_8)).use { br ->
                var str: String?
                while (br.readLine().also { str = it } != null) {
                    buf.append(str)
                }
            }

            // Inject color values for WebView body background and links
            val isDark = ATHUtil.isWindowBackgroundDark(mCtx!!)
            val accentColor = accentColor(mCtx!!)
            val backgroundColor = colorToCSS(
                surfaceColor(Color.parseColor(if (isDark) "#424242" else "#ffffff"))
            )
            val contentColor =
                colorToCSS(Color.parseColor(if (isDark) "#ffffff" else "#000000"))
            val textColor =
                colorToCSS(Color.parseColor(if (isDark) "#60FFFFFF" else "#80000000"))
            val accentColorString = colorToCSS(accentColor(mCtx!!))
            val cardBackgroundColor =
                colorToCSS(Color.parseColor(if (isDark) "#353535" else "#ffffff"))
            val accentTextColor = colorToCSS(
                MaterialValueHelper.getPrimaryTextColor(
                    mCtx!!, ColorUtil.isColorLight(accentColor)
                )
            )
            val changeLog = buf.toString()
                .replace(
                    "{style-placeholder}",
                    "body { background-color: $backgroundColor; color: $contentColor; } li {color: $textColor;} h3 {color: $accentColorString;} .tag {background-color: $accentColorString; color: $accentTextColor; } div{background-color: $cardBackgroundColor;}"
                )
                .replace("{link-color}", colorToCSS(accentColor(mCtx!!)))
                .replace(
                    "{link-color-active}",
                    colorToCSS(
                        ColorUtil.lightenColor(accentColor(mCtx!!))
                    )
                )
            binding.webView.loadData(changeLog, "text/html", "UTF-8")
        } catch (e: Throwable) {
            binding.webView.loadData(
                "<h1>Unable to load</h1><p>" + e.localizedMessage + "</p>", "text/html", "UTF-8"
            )
        }

        setChangelogRead(mCtx!!)

        binding.webView.drawAboveSystemBars()

        return binding.root
    }

    companion object {
        private fun colorToCSS(color: Int): String {
            return String.format(
                Locale.getDefault(),
                "rgba(%d, %d, %d, %d)",
                Color.red(color),
                Color.green(color),
                Color.blue(color),
                Color.alpha(color)
            ) // on API 29, WebView doesn't load with hex colors
        }

        private fun setChangelogRead(context: Context) {
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = pInfo.versionCode
                lastVersion = currentVersion
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
    }

}
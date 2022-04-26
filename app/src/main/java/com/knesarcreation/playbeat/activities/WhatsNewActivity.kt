package com.knesarcreation.playbeat.activities

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import com.knesarcreation.appthemehelper.ThemeStore.Companion.accentColor
import com.knesarcreation.appthemehelper.util.ATHUtil.isWindowBackgroundDark
import com.knesarcreation.appthemehelper.util.ColorUtil.isColorLight
import com.knesarcreation.appthemehelper.util.ColorUtil.lightenColor
import com.knesarcreation.appthemehelper.util.MaterialValueHelper.getPrimaryTextColor
import com.knesarcreation.appthemehelper.util.ToolbarContentTintHelper
import com.knesarcreation.playbeat.Constants
import com.knesarcreation.playbeat.activities.base.AbsThemeActivity
import com.knesarcreation.playbeat.databinding.ActivityWhatsNewBinding
import com.knesarcreation.playbeat.extensions.accentColor
import com.knesarcreation.playbeat.extensions.drawAboveSystemBars
import com.knesarcreation.playbeat.extensions.setTaskDescriptionColorAuto
import com.knesarcreation.playbeat.extensions.surfaceColor
import com.knesarcreation.playbeat.util.PlayBeatUtil
import com.knesarcreation.playbeat.util.PreferenceUtil.lastVersion
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

class WhatsNewActivity : AbsThemeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityWhatsNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTaskDescriptionColorAuto()
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        ToolbarContentTintHelper.colorBackButton(binding.toolbar)
        try {
            val buf = StringBuilder()
            val json = assets.open("play_beat-changelog.html")
            BufferedReader(InputStreamReader(json, StandardCharsets.UTF_8)).use { br ->
                var str: String?
                while (br.readLine().also { str = it } != null) {
                    buf.append(str)
                }
            }

            // Inject color values for WebView body background and links
            val isDark = isWindowBackgroundDark(this)
            val accentColor = accentColor(this)
            val backgroundColor = colorToCSS(
                surfaceColor(Color.parseColor(if (isDark) "#424242" else "#ffffff"))
            )
            val contentColor = colorToCSS(Color.parseColor(if (isDark) "#ffffff" else "#000000"))
            val textColor = colorToCSS(Color.parseColor(if (isDark) "#60FFFFFF" else "#80000000"))
            val accentColorString = colorToCSS(accentColor(this))
            val cardBackgroundColor =
                colorToCSS(Color.parseColor(if (isDark) "#353535" else "#ffffff"))
            val accentTextColor = colorToCSS(
                getPrimaryTextColor(
                    this, isColorLight(accentColor)
                )
            )
            val changeLog = buf.toString()
                .replace(
                    "{style-placeholder}",
                    "body { background-color: $backgroundColor; color: $contentColor; } li {color: $textColor;} h3 {color: $accentColorString;} .tag {background-color: $accentColorString; color: $accentTextColor; } div{background-color: $cardBackgroundColor;}"
                )
                .replace("{link-color}", colorToCSS(accentColor(this)))
                .replace(
                    "{link-color-active}",
                    colorToCSS(
                        lightenColor(accentColor(this))
                    )
                )
            binding.webView.loadData(changeLog, "text/html", "UTF-8")
        } catch (e: Throwable) {
            binding.webView.loadData(
                "<h1>Unable to load</h1><p>" + e.localizedMessage + "</p>", "text/html", "UTF-8"
            )
        }
        setChangelogRead(this)
        /*binding.tgFab.setOnClickListener {
            PlayBeatUtil.openUrl(
                this,
                Constants.TELEGRAM_CHANGE_LOG
            )
        }*/
       // binding.tgFab.accentColor()
       // binding.tgFab.shrink()
        /*binding.container.setOnScrollChangeListener { _: NestedScrollView?, _: Int, scrollY: Int, _: Int, oldScrollY: Int ->
            val dy = scrollY - oldScrollY
            if (dy > 0) {
                binding.tgFab.shrink()
            } else if (dy < 0) {
                binding.tgFab.extend()
            }
        }*/
        binding.webView.drawAboveSystemBars()
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
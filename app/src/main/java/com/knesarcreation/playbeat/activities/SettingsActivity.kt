package com.knesarcreation.playbeat.activities

import android.content.Intent
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.color.ColorCallback
import com.knesarcreation.appthemehelper.ThemeStore
import com.knesarcreation.appthemehelper.util.VersionUtils
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.activities.base.AbsThemeActivity
import com.knesarcreation.playbeat.appshortcuts.DynamicShortcutManager
import com.knesarcreation.playbeat.databinding.ActivitySettingsBinding
import com.knesarcreation.playbeat.extensions.*

class SettingsActivity : AbsThemeActivity(), ColorCallback, OnThemeChangedListener {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        val mSavedInstanceState = extra<Bundle>(TAG).value ?: savedInstanceState
        super.onCreate(mSavedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.contentFrame).navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onResume() {
        super.onResume()
        setNavigationBarColorPreOreo(surfaceColor())
    }


    private fun setupToolbar() {
        applyToolbar(binding.toolbar)
        binding.collapsingToolbarLayout.title = getString(R.string.setting)
          binding.toolbar.setNavigationOnClickListener {
              finish()
          }
    }

    override fun invoke(dialog: MaterialDialog, color: Int) {
        ThemeStore.editTheme(this).accentColor(color).commit()
        if (VersionUtils.hasNougatMR())
            DynamicShortcutManager(this).updateDynamicShortcuts()
        restart()
    }

    private fun restart() {
        val savedInstanceState = Bundle().apply {
            onSaveInstanceState(this)
        }
        finish()
        val intent =
            Intent(this, this::class.java).putExtra(TAG, savedInstanceState)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onThemeValuesChanged() {
        restart()
    }

    companion object {
        val TAG: String = SettingsActivity::class.java.simpleName
    }

    /*  override fun onSupportNavigateUp(): Boolean {
          return findNavController(R.id.contentFrame).navigateUp() || super.onSupportNavigateUp()
      }*/
}


interface OnThemeChangedListener {
    fun onThemeValuesChanged()
}

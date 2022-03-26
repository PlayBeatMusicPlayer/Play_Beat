package com.knesarcreation.playbeat.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat.animate
import androidx.lifecycle.ViewModelProvider
import com.knesarcreation.playbeat.BuildConfig
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.database.ViewModelClass
import com.knesarcreation.playbeat.utils.LoadAllAudios
import com.knesarcreation.playbeat.utils.MakeStatusBarTransparent
import com.knesarcreation.playbeat.utils.StorageUtil
import java.util.*


@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private var mPermRequest: ActivityResultLauncher<String>? = null
    private var openSettingReq: ActivityResultLauncher<Intent>? = null
    private var mReqPermForManageAllFiles: ActivityResultLauncher<Intent>? = null
    private lateinit var mViewModelClass: ViewModelClass
    private lateinit var storage: StorageUtil

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (!hasFocus /*|| animationStarted*/) {
            return
        }
       // animate()
        super.onWindowFocusChanged(hasFocus)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen()
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash_screen)

        storage = StorageUtil(this)
        mViewModelClass = ViewModelProvider(this)[ViewModelClass::class.java]

        MakeStatusBarTransparent().transparent(this)
        requestStoragePermission()
        reqOpenSetting()

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadAudio()
            } else {
                showPermissionAlert()
            }
        } else {*/
        mPermRequest!!.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //}

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mPermRequest!!.launch(android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            mPermRequest!!.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }*/

    }

    private fun reqOpenSetting() {
        openSettingReq =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                mPermRequest!!.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

    }

    private fun requestStoragePermission() {
        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             mReqPermForManageAllFiles = registerForActivityResult(
                 ActivityResultContracts.StartActivityForResult()
             ) {
                 // for android 11 and above
                 if (Environment.isExternalStorageManager()) {
                     // Permission granted. Now resume workflow.
                     loadAudio()
                 } else {
                     showPermissionAlert()
                 }
             }
         } else {*/
        // for android 10 and below
        mPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    // do stuff if permission granted
                    LoadAllAudios(this, true).loadAudio(false)
                    //loadAudio()

                } else {
                    val openSetting = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.knesarcreation.playbeat"))
                    val permAlert = AlertDialog.Builder(this)
                    permAlert.setMessage("Storage permission is required to read Media Files. Please grant permission to proceed further.")
                    permAlert.setPositiveButton("Settings") { dialog, _ ->
                        //mPermRequest!!.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        openSettingReq!!.launch(openSetting)
                        dialog.dismiss()
                    }
                    permAlert.setNegativeButton("Dismiss") { dialog, _ ->
                        Toast.makeText(
                            this,
                            "Permission is required to access media files",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        finish()
                        dialog.dismiss()
                    }
                    permAlert.setCancelable(false)
                    permAlert.show()

                }
            }
        //}
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionAlert() {
        val permAlert = AlertDialog.Builder(this)
        permAlert.setMessage("Play beat required external storage permission to manage audio files. Please grant permission to proceed further.")
        permAlert.setPositiveButton("Grant") { dialog, _ ->
            val intent = Intent(
                ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + BuildConfig.APPLICATION_ID)
            )

            mReqPermForManageAllFiles?.launch(intent)
            dialog.dismiss()
        }
        permAlert.setNegativeButton("Dismiss") { dialog, _ ->
            Toast.makeText(
                this,
                "Permission is required to access media files",
                Toast.LENGTH_SHORT
            )
                .show()
            finish()
            dialog.dismiss()
        }
        permAlert.setCancelable(false)
        permAlert.show()
    }

}
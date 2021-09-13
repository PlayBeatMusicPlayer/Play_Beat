package com.knesarcreation.playbeat.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.knesarcreation.playbeat.R

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private var mPermRequest: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
//        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen)

        requestStoragePermission()

        mPermRequest!!.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    }

    private fun requestStoragePermission() {
        mPermRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    // do stuff if permission granted
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this, ActivityBottomBarFragmentContainer::class.java))
                        finish()
                    }, 2000)

                } else {
                    val permAlert = AlertDialog.Builder(this)
                    permAlert.setMessage("Storage permission is required to access Media Files")
                    permAlert.setPositiveButton("Allow") { dialog, _ ->
                        mPermRequest!!.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
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
    }
}
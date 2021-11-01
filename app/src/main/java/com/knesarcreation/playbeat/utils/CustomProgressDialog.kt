package com.knesarcreation.playbeat.utils

import android.app.ActionBar
import android.app.Dialog
import android.content.Context
import com.knesarcreation.playbeat.R

class CustomProgressDialog(var context: Context) {
    private val dialog: Dialog = Dialog(context)

    fun show() {
        dialog.setContentView(R.layout.dialog_progress)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        val window = dialog.window
        // val progressLottie = dialog.findViewById<LottieAnimationView>(R.id.progressLottie)
        //  val progressStatusTV = dialog.findViewById<TextView>(R.id.progressStatusTV)

        //progressLottie.playAnimation()
        // progressStatusTV.text = "message"
        dialog.setCancelable(false)
        window?.setLayout(
            ActionBar.LayoutParams.WRAP_CONTENT,
            ActionBar.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }

    fun setIsCancelable(value: Boolean) {
        dialog.setCancelable(value)
    }

    fun setCanceledOnOutsideTouch(value: Boolean) {
        dialog.setCanceledOnTouchOutside(value)
    }

    fun isShowing(): Boolean {
        return dialog.isShowing
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
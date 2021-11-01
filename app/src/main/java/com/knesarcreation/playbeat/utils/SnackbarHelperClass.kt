package com.knesarcreation.playbeat.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.knesarcreation.playbeat.R

class SnackbarHelperClass(var context: Context) {

    /*fun showActionableSnackbar(
        view: View,
        *//*duration: Int*//*
    ): Snackbar { // Create the Snackbar
        val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)
        // 15 is margin from all the sides for snackbar
        val height = 160

        //inflate view
        val snackView: View = (context as AppCompatActivity).layoutInflater.inflate(
            R.layout.snackbar_actionable_layout,
            null
        )

        // Transparent background
        snackbar.view.setBackgroundColor(Color.TRANSPARENT)

        val snackBarView = snackbar.view as Snackbar.SnackbarLayout
        val parentParams = snackBarView.layoutParams as FrameLayout.LayoutParams
        parentParams.setMargins(15, 0, 15, 15)
        parentParams.height = height
        parentParams.width = FrameLayout.LayoutParams.MATCH_PARENT
        snackBarView.layoutParams = parentParams
        snackBarView.addView(snackView, 0)
        return snackbar
    }*/

    fun showNormalSnackBar(view: View, message: String)/*: Snackbar*/ {
        val snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)

        val height = 200

        //inflate view
        val snackView: View = (context as AppCompatActivity).layoutInflater.inflate(
            R.layout.snackbar_normal_layout,
            null
        )

        // Transparent background
        snackbar.view.setBackgroundColor(Color.TRANSPARENT)

        val snackBarView = snackbar.view as Snackbar.SnackbarLayout
        val parentParams = snackBarView.layoutParams as FrameLayout.LayoutParams
        parentParams.setMargins(15, 0, 15, 15)
        parentParams.height = height
        parentParams.width = FrameLayout.LayoutParams.MATCH_PARENT
        snackBarView.layoutParams = parentParams
        snackBarView.addView(snackView, 0)

        val snackBarTextTV = snackView.findViewById<TextView>(R.id.snackBarTextTV)
        snackBarTextTV.text = message
        snackbar.show()
        /*return snackbar*/
    }
}
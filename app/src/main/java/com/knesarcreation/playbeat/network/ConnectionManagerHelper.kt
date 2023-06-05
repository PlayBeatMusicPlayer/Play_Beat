package com.knesarcreation.playbeat.network

import android.content.Context
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.knesarcreation.playbeat.R
import com.knesarcreation.playbeat.receiver.ConnectionReceiver

class ConnectionManagerHelper(var context: Context) {

    fun checkConnection(): Boolean {

        // initialize intent filter
        val intentFilter = IntentFilter()

        // add action
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")

        // register receiver
        // context.registerReceiver(ConnectionReceiver(), intentFilter)

        // Initialize listener
        //ConnectionReceiver.Listener = this

        // Initialize connectivity manager
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Initialize network info
        val networkInfo = manager.activeNetworkInfo

        // get connection status
        return networkInfo != null && networkInfo.isConnectedOrConnecting

        // display snack bar
        //showSnackBar(isConnected)
    }

    fun showSnackBar(isConnected: Boolean) {

        // initialize color and message
        val message: String
        val color: Int

        // check condition
        if (isConnected) {

            // when internet is connected
            // set message
            message = "Connected to Internet."

            // set text color
            color = Color.WHITE
        } else {

            // when internet
            // is disconnected
            // set message
            message = "Not Connected to Internet."

            // set text color
            color = Color.RED
        }

        // initialize snack bar
        val snackbar = Snackbar.make(
            (context as AppCompatActivity).window.decorView,
            message,
            Snackbar.LENGTH_LONG
        )

        // initialize view
        val view: View = snackbar.view

        // Assign variable
        val textView: TextView = view.findViewById(R.id.snackbar_text)

        // set text color
        textView.setTextColor(color)

        // show snack bar
        snackbar.show()
    }


}
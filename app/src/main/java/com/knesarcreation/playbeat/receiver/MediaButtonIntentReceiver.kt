package com.knesarcreation.playbeat.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.widget.Toast

class MediaButtonIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
       /* val intentAction = intent!!.action
        Toast.makeText(context, "$intentAction", Toast.LENGTH_SHORT).show()
        if (Intent.ACTION_MEDIA_BUTTON != intentAction) {
            return
        }
        val event: KeyEvent =
            intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent? ?: return
        val action: Int = event.action
        if (action == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            Toast.makeText(context, "Button Pressed", Toast.LENGTH_LONG).show()
        }

        abortBroadcast()*/
    }
}
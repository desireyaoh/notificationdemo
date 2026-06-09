package com.test.notificationdemo.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "屏幕唤醒==================> action: ${intent.action}")
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            ScreenNotificationUtils.showNotification(context)
        }
    }

    companion object {
        private const val TAG = "ScreenOnReceiver"
    }
}

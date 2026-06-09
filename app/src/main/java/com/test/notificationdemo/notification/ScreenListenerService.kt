package com.test.notificationdemo.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.test.notificationdemo.R

class ScreenListenerService : Service() {

    private var screenOnReceiver: ScreenOnReceiver? = null
    private var screenshotObserver: ScreenshotObserver? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithMinimalNotification()
        ScreenNotificationUtils.createChannel(this)
        screenOnReceiver = ScreenOnReceiver()
        registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        screenshotObserver = ScreenshotObserver(Handler(Looper.getMainLooper()))
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        screenOnReceiver?.let { unregisterReceiver(it) }
        screenOnReceiver = null
        screenshotObserver?.let { contentResolver.unregisterContentObserver(it) }
        screenshotObserver = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class ScreenshotObserver(handler: Handler) : ContentObserver(handler) {
        private var lastTriggerMs = 0L

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            uri ?: return
            val now = System.currentTimeMillis()
            if (now - lastTriggerMs < SCREENSHOT_THROTTLE_MS) return
            lastTriggerMs = now
            checkIfScreenshot(uri)
        }

        private fun checkIfScreenshot(uri: Uri) {
            val projection = arrayOf(
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
            )
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val path = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    ) ?: return
                    if (path.contains("screenshot", ignoreCase = true)) {
                        Log.d(TAG, "检测到截屏: $path")
                        ScreenNotificationUtils.showNotification(this@ScreenListenerService)
                    }
                }
            }
        }
    }

    private fun startForegroundWithMinimalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_listener_service_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_listener_service_title))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "ScreenListenerService"
        private const val CHANNEL_ID = "screen_listener_service_channel"
        private const val NOTIFICATION_ID = 10086
        private const val SCREENSHOT_THROTTLE_MS = 1_000L
    }
}

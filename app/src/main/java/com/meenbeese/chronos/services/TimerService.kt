package com.meenbeese.chronos.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper

import androidx.core.app.NotificationCompat

import com.meenbeese.chronos.Chronos
import com.meenbeese.chronos.R
import com.meenbeese.chronos.activities.MainActivity
import com.meenbeese.chronos.data.TimerData
import com.meenbeese.chronos.receivers.TimerReceiver
import com.meenbeese.chronos.utils.FormatUtils.formatMillis


class TimerService : Service() {
    private val binder: IBinder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            if (timers?.isNotEmpty() == true) {
                val notification: Notification? = notification
                if (notification != null) {
                    handler.removeCallbacks(this)
                    handler.postDelayed(this, 10)
                }
            } else stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
    private var timers: List<TimerData>? = null
    private var notificationManager: NotificationManager? = null
    private var notificationString: String? = null

    override fun onCreate() {
        super.onCreate()
        timers = (applicationContext as Chronos).timers
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notification = notification
        if (notification != null) {
            startForeground(NOTIFICATION_ID, notification)
        }
        handler.removeCallbacks(runnable)
        runnable.run()
        return START_STICKY
    }

    private val notification: Notification?
        get() {
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    Chronos.NOTIFICATION_CHANNEL_TIMERS,
                    "Timers",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            val inboxStyle = NotificationCompat.InboxStyle()
            val string = StringBuilder()
            for (timer in timers ?: emptyList()) {
                if (!timer.isSet) continue
                var time = formatMillis(timer.remainingMillis)
                time = time.substring(0, time.length - 3)
                inboxStyle.addLine(time)
                string.append("/").append(time).append("/")
            }
            if (notificationString != null && notificationString == string.toString()) return null
            notificationString = string.toString()
            val intent = Intent(this, MainActivity::class.java)
            if (timers?.size == 1) intent.putExtra(TimerReceiver.EXTRA_TIMER_ID, 0)
            return NotificationCompat.Builder(this, Chronos.NOTIFICATION_CHANNEL_TIMERS)
                .setSmallIcon(R.drawable.ic_timer_notification)
                .setContentTitle(getString(R.string.title_set_timer))
                .setContentText("")
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setStyle(inboxStyle)
                .build()
        }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    class LocalBinder : Binder()
    companion object {
        private const val NOTIFICATION_ID = 427
    }
}

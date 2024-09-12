package com.meenbeese.chronos.activities

import android.app.AlarmManager
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.widget.TextView

import androidx.activity.ComponentActivity
import androidx.media3.common.util.UnstableApi
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

import com.meenbeese.chronos.Chronos
import com.meenbeese.chronos.R
import com.meenbeese.chronos.data.AlarmData
import com.meenbeese.chronos.data.PreferenceData
import com.meenbeese.chronos.data.SoundData
import com.meenbeese.chronos.data.TimerData
import com.meenbeese.chronos.dialogs.SnoozeDurationDialog
import com.meenbeese.chronos.dialogs.TimeChooserDialog
import com.meenbeese.chronos.dialogs.TimeChooserDialog.OnTimeChosenListener
import com.meenbeese.chronos.interfaces.SlideActionListener
import com.meenbeese.chronos.services.SleepReminderService.Companion.refreshSleepTime
import com.meenbeese.chronos.utils.FormatUtils
import com.meenbeese.chronos.utils.FormatUtils.format
import com.meenbeese.chronos.utils.FormatUtils.formatMillis
import com.meenbeese.chronos.utils.FormatUtils.formatUnit
import com.meenbeese.chronos.utils.FormatUtils.getShortFormat
import com.meenbeese.chronos.utils.ImageUtils.getBackgroundImage
import com.meenbeese.chronos.views.SlideActionView

import java.util.Date
import java.util.concurrent.TimeUnit

import kotlin.math.min


class AlarmActivity : ComponentActivity(), SlideActionListener {
    private var overlay: View? = null
    private var time: TextView? = null
    private var chronos: Chronos? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var triggerMillis: Long = 0
    private var alarm: AlarmData? = null
    private var sound: SoundData? = null
    private var isVibrate = false
    private var isSlowWake = false
    private var slowWakeMillis: Long = 0
    private var currentVolume = 0
    private var minVolume = 0
    private var originalVolume = 0
    private var volumeRange = 0
    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var isDark = false

    @UnstableApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        actionBar?.hide()

        chronos = applicationContext as Chronos
        overlay = findViewById(R.id.overlay)
        isDark = chronos!!.isDarkTheme()
        time = findViewById(R.id.time)
        val date = findViewById<TextView>(R.id.date)
        val actionView = findViewById<SlideActionView>(R.id.slideView)

        actionView.setLeftIcon(VectorDrawableCompat.create(resources, R.drawable.ic_snooze, theme)!!)
        actionView.setRightIcon(VectorDrawableCompat.create(resources, R.drawable.ic_close, theme)!!)
        actionView.setListener(this)

        isSlowWake = PreferenceData.SLOW_WAKE_UP.getValue(this)
        slowWakeMillis = PreferenceData.SLOW_WAKE_UP_TIME.getValue(this)

        when {
            intent.hasExtra(EXTRA_ALARM) -> {
                alarm = intent.getParcelableExtra(EXTRA_ALARM)
                isVibrate = alarm?.isVibrate == true
                sound = alarm?.getSound()
            }
            intent.hasExtra(EXTRA_TIMER) -> {
                val timer = intent.getParcelableExtra<TimerData>(EXTRA_TIMER)
                isVibrate = timer?.isVibrate == true
                sound = timer?.sound
            }
            else -> finish()
        }

        date.text = format(Date(), FormatUtils.FORMAT_DATE + ", " + getShortFormat(this))

        sound?.let {
            if (!it.isSetVolumeSupported) {
                audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                originalVolume = audioManager!!.getStreamVolume(AudioManager.STREAM_ALARM)
                if (isSlowWake) {
                    minVolume = 0
                    volumeRange = originalVolume - minVolume
                    currentVolume = minVolume
                    audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, minVolume, 0)
                }
            }
        }

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        triggerMillis = System.currentTimeMillis()
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                val elapsedMillis = System.currentTimeMillis() - triggerMillis
                time?.text = "-${formatMillis(elapsedMillis).dropLast(3)}"

                if (isVibrate) {
                    vibrator!!.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                }

                sound?.let {
                    if (!it.isPlaying(chronos!!)) it.play(chronos!!)
                    if (alarm != null && isSlowWake) {
                        val slowWakeProgress = elapsedMillis.toFloat() / slowWakeMillis
                        window.addFlags(WindowManager.LayoutParams.FLAGS_CHANGED)
                        if (it.isSetVolumeSupported) {
                            it.setVolume(chronos!!, min(1f, slowWakeProgress))
                        } else if (currentVolume < originalVolume) {
                            val newVolume = minVolume + min(originalVolume.toFloat(), slowWakeProgress * volumeRange).toInt()
                            if (newVolume != currentVolume) {
                                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0)
                                currentVolume = newVolume
                            }
                        }
                    }
                }

                handler?.postDelayed(this, 1000)
            }
        }
        handler?.post(runnable!!)
        sound?.play(chronos!!)
        refreshSleepTime(chronos!!)
        if (PreferenceData.RINGING_BACKGROUND_IMAGE.getValue(this)) {
            getBackgroundImage(findViewById(R.id.background)!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnnoyance()
    }

    private fun stopAnnoyance() {
        handler?.removeCallbacks(runnable!!)
        if (sound?.isPlaying(chronos!!) == true) {
            sound?.stop(chronos!!)
            if (isSlowWake && sound?.isSetVolumeSupported == false) {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        finish()
        startActivity(Intent(intent))
    }

    override fun onSlideLeft() {
        val minutes = intArrayOf(2, 5, 10, 20, 30, 60)
        val names = Array<CharSequence?>(minutes.size + 1) { i ->
            if (i < minutes.size) formatUnit(this@AlarmActivity, minutes[i]) else getString(R.string.title_snooze_custom)
        }

        stopAnnoyance()

        val snoozeDurationDialog = SnoozeDurationDialog(this, names, object : SnoozeDurationDialog.OnSnoozeDurationSelectedListener {
            override fun onSnoozeDurationSelected(which: Int) {
                if (which < minutes.size) {
                    chronos!!.newTimer().apply {
                        setDuration(TimeUnit.MINUTES.toMillis(minutes[which].toLong()), chronos)
                        setVibrate(this@AlarmActivity, isVibrate)
                        setSound(this@AlarmActivity, sound)
                        this[chronos] = getSystemService(ALARM_SERVICE) as AlarmManager
                    }
                    chronos?.onTimerStarted()
                    finish()
                } else {
                    TimeChooserDialog(this@AlarmActivity).apply {
                        setListener(object : OnTimeChosenListener {
                            override fun onTimeChosen(hours: Int, minutes: Int, seconds: Int) {
                                chronos?.newTimer()?.apply {
                                    setVibrate(this@AlarmActivity, isVibrate)
                                    setSound(this@AlarmActivity, sound)
                                    setDuration(
                                        TimeUnit.HOURS.toMillis(hours.toLong()) +
                                                TimeUnit.MINUTES.toMillis(minutes.toLong()) +
                                                TimeUnit.SECONDS.toMillis(seconds.toLong()),
                                        chronos
                                    )
                                    this[chronos] = getSystemService(ALARM_SERVICE) as AlarmManager
                                }
                                chronos?.onTimerStarted()
                                finish()
                            }
                        })
                        show()
                    }
                }
            }
        })
        snoozeDurationDialog.show()

        overlay?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    override fun onSlideRight() {
        overlay?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        finish()
    }

    companion object {
        const val EXTRA_ALARM = "meenbeese.chronos.AlarmActivity.EXTRA_ALARM"
        const val EXTRA_TIMER = "meenbeese.chronos.AlarmActivity.EXTRA_TIMER"
    }
}

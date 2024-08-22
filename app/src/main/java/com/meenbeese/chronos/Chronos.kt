package com.meenbeese.chronos

import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.media.Ringtone
import android.net.Uri
import android.os.Build
import android.widget.Toast

import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource

import com.afollestad.aesthetic.Aesthetic.Companion.get
import com.afollestad.aesthetic.AutoSwitchMode
import com.meenbeese.chronos.data.AlarmData
import com.meenbeese.chronos.data.PreferenceData
import com.meenbeese.chronos.data.SoundData.fromString
import com.meenbeese.chronos.data.TimerData
import com.meenbeese.chronos.services.SleepReminderService.Companion.refreshSleepTime
import com.meenbeese.chronos.services.TimerService
import com.meenbeese.chronos.utils.CoreHelper

import java.util.Calendar


class Chronos : Application(), Player.Listener {
    lateinit var alarms: ArrayList<AlarmData>
    lateinit var timers: ArrayList<TimerData>
    private var listeners: MutableList<ChronosListener>? = null
    private var listener: ActivityListener? = null
    private var player: ExoPlayer? = null
    private var currentRingtone: Ringtone? = null
    private var hlsMediaSourceFactory: HlsMediaSource.Factory? = null
    private var currentStream: String? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        CoreHelper.contextGetter = { this }
        listeners = ArrayList()
        alarms = ArrayList()
        timers = ArrayList()
        player = ExoPlayer.Builder(this).build()
        player?.addListener(this)
        val dataSourceFactory = DefaultDataSource.Factory(this)
        hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
        val alarmLength = PreferenceData.ALARM_LENGTH.getValue<Int>(this)
        for (id in 0 until alarmLength) {
            alarms.add(AlarmData(id, this))
        }
        val timerLength = PreferenceData.TIMER_LENGTH.getValue<Int>(this)
        for (id in 0 until timerLength) {
            val timer = TimerData(id, this)
            if (timer.isSet) timers.add(timer)
        }
        if (timerLength > 0) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, TimerService::class.java))
            } else {
                startService(Intent(this, TimerService::class.java))
            }
        }
        refreshSleepTime(this)
    }

    /**
     * Create a new alarm, assigning it an unused preference id.
     *
     * @return          The newly instantiated [AlarmData](./data/AlarmData).
     */
    fun newAlarm(): AlarmData {
        val alarm = AlarmData(alarms.size, Calendar.getInstance())
        alarm.sound = fromString(PreferenceData.DEFAULT_ALARM_RINGTONE.getValue(this, ""))
        alarms.add(alarm)
        onAlarmCountChanged()
        return alarm
    }

    /**
     * Remove an alarm and all of its its preferences.
     *
     * @param alarm     The alarm to be removed.
     */
    fun removeAlarm(alarm: AlarmData) {
        alarm.onRemoved(this)
        val index = alarms.indexOf(alarm)
        alarms.removeAt(index)
        for (i in index until alarms.size) {
            alarms[i].onIdChanged(i, this)
        }
        onAlarmCountChanged()
        onAlarmsChanged()
    }

    /**
     * Update preferences to show that the alarm count has been changed.
     */
    private fun onAlarmCountChanged() {
        PreferenceData.ALARM_LENGTH.setValue(this, alarms.size)
    }

    /**
     * Notify the application of changes to the current alarms.
     */
    fun onAlarmsChanged() {
        for (listener in listeners!!) {
            listener.onAlarmsChanged()
        }
    }

    /**
     * Create a new timer, assigning it an unused preference id.
     *
     * @return          The newly instantiated [TimerData](./data/TimerData).
     */
    fun newTimer(): TimerData {
        val timer = TimerData(timers.size)
        timers.add(timer)
        onTimerCountChanged()
        return timer
    }

    /**
     * Remove a timer and all of its preferences.
     *
     * @param timer     The timer to be removed.
     */
    fun removeTimer(timer: TimerData) {
        timer.onRemoved(this)
        val index = timers.indexOf(timer)
        timers.removeAt(index)
        for (i in index until timers.size) {
            timers[i].onIdChanged(i, this)
        }
        onTimerCountChanged()
        onTimersChanged()
    }

    /**
     * Update the preferences to show that the timer count has been changed.
     */
    private fun onTimerCountChanged() {
        PreferenceData.TIMER_LENGTH.setValue(this, timers.size)
    }

    /**
     * Notify the application of changes to the current timers.
     */
    private fun onTimersChanged() {
        for (listener in listeners!!) {
            listener.onTimersChanged()
        }
    }

    /**
     * Starts the timer service after a timer has been set.
     */
    fun onTimerStarted() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, TimerService::class.java))
        } else {
            startService(Intent(this, TimerService::class.java))
        }
    }

    /**
     * Update the application theme.
     */
    fun updateTheme() {
        if (isNight) {
            get()
                .isDark(true)
                .lightStatusBarMode(AutoSwitchMode.OFF)
                .colorPrimary(ContextCompat.getColor(this, R.color.colorNightPrimary))
                .colorStatusBar(Color.TRANSPARENT)
                .colorNavigationBar(ContextCompat.getColor(this, R.color.colorNightPrimaryDark))
                .colorAccent(ContextCompat.getColor(this, R.color.colorNightAccent))
                .colorCardViewBackground(ContextCompat.getColor(this, R.color.colorNightForeground))
                .colorWindowBackground(ContextCompat.getColor(this, R.color.colorNightPrimaryDark))
                .textColorPrimary(ContextCompat.getColor(this, R.color.textColorPrimaryNight))
                .textColorSecondary(ContextCompat.getColor(this, R.color.textColorSecondaryNight))
                .textColorPrimaryInverse(ContextCompat.getColor(this, R.color.textColorPrimary))
                .textColorSecondaryInverse(ContextCompat.getColor(this, R.color.textColorSecondary))
                .apply()
        } else {
            val theme = activityTheme
            if (theme == THEME_DAY || theme == THEME_DAY_NIGHT) {
                get()
                    .isDark(false)
                    .lightStatusBarMode(AutoSwitchMode.ON)
                    .colorPrimary(ContextCompat.getColor(this, R.color.colorPrimary))
                    .colorStatusBar(Color.TRANSPARENT)
                    .colorNavigationBar(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                    .colorAccent(ContextCompat.getColor(this, R.color.colorAccent))
                    .colorCardViewBackground(ContextCompat.getColor(this, R.color.colorForeground))
                    .colorWindowBackground(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                    .textColorPrimary(ContextCompat.getColor(this, R.color.textColorPrimary))
                    .textColorSecondary(ContextCompat.getColor(this, R.color.textColorSecondary))
                    .textColorPrimaryInverse(ContextCompat.getColor(this, R.color.textColorPrimaryNight))
                    .textColorSecondaryInverse(ContextCompat.getColor(this, R.color.textColorSecondaryNight))
                    .apply()
            } else if (theme == THEME_AMOLED) {
                get()
                    .isDark(true)
                    .lightStatusBarMode(AutoSwitchMode.OFF)
                    .colorPrimary(Color.BLACK)
                    .colorStatusBar(Color.TRANSPARENT)
                    .colorNavigationBar(Color.BLACK)
                    .colorAccent(Color.WHITE)
                    .colorCardViewBackground(Color.BLACK)
                    .colorWindowBackground(Color.BLACK)
                    .textColorPrimary(Color.WHITE)
                    .textColorSecondary(Color.WHITE)
                    .textColorPrimaryInverse(Color.BLACK)
                    .textColorSecondaryInverse(Color.BLACK)
                    .apply()
            }
        }
    }

    private val isNight: Boolean
        /**
         * Determine if the theme should be a night theme.
         *
         * @return          True if the current theme is a night theme.
         */
        get() {
            val time = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
            return (time < dayStart || time > dayEnd) && activityTheme == THEME_DAY_NIGHT || activityTheme == THEME_NIGHT
        }
    val activityTheme: Int
        /**
         * Get the theme to be used for activities and things. Despite
         * what the name implies, it does not return a theme resource,
         * but rather one of Chronos.THEME_DAY_NIGHT, Chronos.THEME_DAY,
         * Chronos.THEME_NIGHT, or Chronos.THEME_AMOLED.
         *
         * @return          The theme to be used for activities.
         */
        get() = PreferenceData.THEME.getValue(this)
    val dayStart: Int
        /**
         * @return the hour of the start of the day (24h), as specified by the user
         */
        get() = PreferenceData.DAY_START.getValue(this)
    val dayEnd: Int
        /**
         * @return the hour of the end of the day (24h), as specified by the user
         */
        get() = PreferenceData.DAY_END.getValue(this)
    private val isRingtonePlaying: Boolean
        /**
         * Determine if a ringtone is currently playing.
         *
         * @return          True if a ringtone is currently playing.
         */
        get() = currentRingtone != null && currentRingtone!!.isPlaying

    fun isDarkTheme(): Boolean {
        return activityTheme == THEME_NIGHT || activityTheme == THEME_AMOLED || (activityTheme == THEME_DAY_NIGHT && isNight)
    }

    fun playRingtone(ringtone: Ringtone) {
        if (!ringtone.isPlaying) {
            stopCurrentSound()
            ringtone.play()
        }
        currentRingtone = ringtone
    }

    /**
     * Play a stream ringtone.
     *
     * @param url       The URL of the stream to be passed to ExoPlayer.
     * @see [ExoPlayer Repo]
     */
    @UnstableApi
    private fun playStream(url: String, type: String, factory: MediaSource.Factory?) {
        stopCurrentSound()

        // Create a MediaItem from the URL
        val mediaItem: MediaItem = MediaItem.fromUri(Uri.parse(url))

        // Error handling, including when this is a progressive stream
        // rather than a HLS stream, is in onPlayerError
        player?.setMediaSource(factory!!.createMediaSource(mediaItem))
        player?.prepare()
        player?.playWhenReady = true
        currentStream = url
    }

    /**
     * Play a stream ringtone.
     *
     * @param url       The URL of the stream to be passed to ExoPlayer.
     * @see [ExoPlayer Repo]
     */
    @UnstableApi
    private fun playStream(url: String, type: String) {
        playStream(url, type, hlsMediaSourceFactory)
    }

    /**
     * Play a stream ringtone.
     *
     * @param url           The URL of the stream to be passed to ExoPlayer.
     * @param attributes    The attributes to play the stream with.
     * @see [ExoPlayer Repo]
     */
    @UnstableApi
    fun playStream(url: String, type: String, attributes: AudioAttributes?) {
        player?.stop()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(attributes!!, true)
            .build()
        playStream(url, type)
    }

    /**
     * Stop the currently playing stream.
     */
    fun stopStream() {
        player?.stop()
        currentStream = null
    }

    /**
     * Sets the player volume to the given float.
     *
     * @param volume            The volume between 0 and 1
     */
    fun setStreamVolume(volume: Float) {
        player?.volume = volume
    }

    /**
     * Determine if the passed url matches the stream that is currently playing.
     *
     * @param url           The URL to match the current stream to.
     * @return              True if the URL matches that of the currently playing
     * stream.
     */
    fun isPlayingStream(url: String): Boolean {
        return currentStream != null && currentStream == url
    }

    /**
     * Stop the currently playing sound, regardless of whether it is a ringtone
     * or a stream.
     */
    fun stopCurrentSound() {
        if (isRingtonePlaying) currentRingtone?.stop()
        stopStream()
    }

    fun addListener(listener: ChronosListener) {
        listeners?.add(listener)
    }

    fun removeListener(listener: ChronosListener) {
        listeners?.remove(listener)
    }

    fun setListener(listener: ActivityListener?) {
        this.listener = listener
        if (listener != null) updateTheme()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING, Player.STATE_READY, Player.STATE_IDLE -> {}
            else -> currentStream = null
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        currentStream = null
        val exception: Throwable? = error.cause
        exception?.printStackTrace()
        Toast.makeText(
            this,
            exception?.javaClass?.name + ": " + exception?.message,
            Toast.LENGTH_SHORT
        ).show()
    }

    val fragmentManager: FragmentManager?
        get() = if (listener != null) listener!!.fetchFragmentManager() else null

    interface ChronosListener {
        fun onAlarmsChanged()
        fun onTimersChanged()
    }

    interface ActivityListener {
        fun fetchFragmentManager(): FragmentManager?
    }

    companion object {
        const val THEME_DAY_NIGHT = 0
        const val THEME_DAY = 1
        const val THEME_NIGHT = 2
        const val THEME_AMOLED = 3
        const val NOTIFICATION_CHANNEL_STOPWATCH = "stopwatch"
        const val NOTIFICATION_CHANNEL_TIMERS = "timers"
    }
}

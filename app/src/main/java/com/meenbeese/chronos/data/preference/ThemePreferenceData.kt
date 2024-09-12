package com.meenbeese.chronos.data.preference

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate

import com.meenbeese.chronos.Chronos
import com.meenbeese.chronos.R
import com.meenbeese.chronos.data.PreferenceData
import com.meenbeese.chronos.utils.FormatUtils
import com.meenbeese.chronos.views.SunriseSunsetView

import java.util.Calendar
import java.util.Date

import kotlin.math.roundToInt


const val HOUR_LENGTH = 3600000L

/**
 * Allow the user to choose the theme of the
 * application.
 */
class ThemePreferenceData(
    context: Context?
) : BasePreferenceData<ThemePreferenceData.ViewHolder>() {
    var chronos: Chronos = context?.applicationContext as Chronos

    override fun getViewHolder(inflater: LayoutInflater, parent: ViewGroup): BasePreferenceData.ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.item_preference_theme, parent, false))
    }

    @SuppressLint("CheckResult")
    override fun bindViewHolder(holder: ViewHolder) {
        val adapter = ArrayAdapter.createFromResource(
            holder.itemView.context,
            R.array.array_themes,
            com.google.android.material.R.layout.support_simple_spinner_dropdown_item
        )

        holder.themeAutoCompleteTextView.setAdapter(adapter)

        val theme: Int = holder.chronos?.activityTheme ?: Chronos.THEME_DAY_NIGHT
        holder.themeAutoCompleteTextView.setText(adapter.getItem(theme), false)

        holder.themeAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            if (holder.themeAutoCompleteTextView.text.toString() == adapter.getItem(position)) {
                PreferenceData.THEME.setValue(holder.itemView.context, position)
                holder.sunriseLayout.visibility = if (position == Chronos.THEME_DAY_NIGHT) View.VISIBLE else View.GONE
                applyTheme(position)
            }
        }

        val listener = object : SunriseSunsetView.SunriseListener {
            override fun onSunriseChanged(view: SunriseSunsetView?, sunriseMillis: Long) {
                val hour = (sunriseMillis.toFloat() / HOUR_LENGTH).roundToInt()
                holder.sunriseTextView.text = getText(hour)
                view?.setSunrise(hour * HOUR_LENGTH, true)
                PreferenceData.DAY_START.setValue(holder.context, hour)
            }

            override fun onSunsetChanged(view: SunriseSunsetView?, sunsetMillis: Long) {
                val hour = (sunsetMillis.toFloat() / HOUR_LENGTH).roundToInt()
                holder.sunsetTextView.text = getText(hour)
                view?.setSunset(hour * HOUR_LENGTH, true)
                PreferenceData.DAY_END.setValue(holder.context, hour)
            }

            private fun getText(hour: Int): String = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
            }.run {
                FormatUtils.formatShort(holder.context, Date(timeInMillis))
            }
        }

        holder.chronos?.let { chronos ->
            listener.onSunriseChanged(holder.sunriseView, chronos.dayStart * HOUR_LENGTH)
            listener.onSunsetChanged(holder.sunriseView, chronos.dayEnd * HOUR_LENGTH)
        }

        holder.sunriseView.setListener(object : SunriseSunsetView.SunriseListener {
            override fun onSunriseChanged(view: SunriseSunsetView?, sunriseMillis: Long) {
                listener.onSunriseChanged(view, sunriseMillis)
            }

            override fun onSunsetChanged(view: SunriseSunsetView?, sunsetMillis: Long) {
                listener.onSunsetChanged(view, sunsetMillis)
            }
        })

        holder.sunriseTextView.setOnClickListener { view ->
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val style = if (chronos.isDarkTheme()) 0 else com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog

            val timePickerDialog = TimePickerDialog(view.context, style, { _, selectedHour, _ ->
                holder.chronos?.let { chronos ->
                    if (selectedHour < chronos.dayEnd) {
                        listener.onSunriseChanged(holder.sunriseView, selectedHour * HOUR_LENGTH)
                    }
                }
            }, hour, minute, true)

            timePickerDialog.show()
        }

        holder.sunsetTextView.setOnClickListener { view ->
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val style = if (chronos.isDarkTheme()) 0 else com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog

            val timePickerDialog = TimePickerDialog(view.context, style, { _, selectedHour, _ ->
                holder.chronos?.let { chronos ->
                    if (selectedHour > chronos.dayStart) {
                        listener.onSunsetChanged(holder.sunriseView, selectedHour * HOUR_LENGTH)
                    }
                }
            }, hour, minute, true)

            timePickerDialog.show()
        }
    }

    private fun applyTheme(theme: Int) {
        when (theme) {
            Chronos.THEME_DAY -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                chronos.setTheme(R.style.AppTheme)
            }
            Chronos.THEME_NIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                chronos.setTheme(R.style.AppTheme_Night)
            }
            Chronos.THEME_AMOLED -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                chronos.setTheme(R.style.AppTheme_Amoled)
            }
        }
        chronos.recreate()
    }

    /**
     * Holds child views of the current item.
     */
    class ViewHolder(v: View) : BasePreferenceData.ViewHolder(v) {
        val themeAutoCompleteTextView: AutoCompleteTextView = v.findViewById(R.id.themeSpinner)
        val sunriseLayout: View = v.findViewById(R.id.sunriseLayout)
        val sunriseView: SunriseSunsetView = v.findViewById(R.id.sunriseView)
        val sunriseTextView: TextView = v.findViewById(R.id.sunriseTextView)
        val sunsetTextView: TextView = v.findViewById(R.id.sunsetTextView)
    }
}

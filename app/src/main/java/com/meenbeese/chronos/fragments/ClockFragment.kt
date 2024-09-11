package com.meenbeese.chronos.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.meenbeese.chronos.R
import com.meenbeese.chronos.interfaces.ContextFragmentInstantiator
import com.meenbeese.chronos.views.DigitalClockView

import java.util.TimeZone


class ClockFragment : BasePagerFragment() {
    private var timezone: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_clock, container, false)
        val clockView = view.findViewById<DigitalClockView>(R.id.timeView)
        val timezoneView = view.findViewById<TextView>(R.id.timezone)
        if (arguments != null && requireArguments().containsKey(EXTRA_TIME_ZONE)) {
            timezone = arguments?.getString(EXTRA_TIME_ZONE)
            timezone?.let {
                clockView.setTimezone(it)
                clockView.setTextStyle(DigitalClockView.TextStyle.UNDERLINE)
                if (it != TimeZone.getDefault().id) {
                    timezoneView.text = String.format(
                        "%s\n%s",
                        it.replace("_".toRegex(), " "),
                        TimeZone.getTimeZone(it).displayName
                    )
                }
            }
        }
        return view
    }

    override fun getTitle(context: Context?): String? {
        return timezone
    }

    class Instantiator(context: Context?, private val timezone: String?) :
        ContextFragmentInstantiator(context!!) {
        override fun getTitle(context: Context?, position: Int): String? {
            return timezone
        }

        override fun newInstance(position: Int): BasePagerFragment {
            val args = Bundle()
            args.putString(EXTRA_TIME_ZONE, timezone)
            val fragment = ClockFragment()
            fragment.arguments = args
            return fragment
        }
    }

    companion object {
        const val EXTRA_TIME_ZONE = "com.meenbeese.chronos.fragments.ClockFragment.EXTRA_TIME_ZONE"
    }
}

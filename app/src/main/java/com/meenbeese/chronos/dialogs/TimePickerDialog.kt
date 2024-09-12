package com.meenbeese.chronos.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TimePicker

import androidx.activity.ComponentDialog

import com.google.android.material.button.MaterialButton
import com.meenbeese.chronos.R


class TimePickerDialog(context: Context?) : ComponentDialog(context!!), View.OnClickListener {

    private var timePicker: TimePicker? = null
    private var listener: OnTimeChosenListener? = null

    fun setListener(listener: OnTimeChosenListener?) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null)
        setContentView(view)

        timePicker = view.findViewById(R.id.timePicker)
        timePicker?.setIs24HourView(true)

        val cancelButton: MaterialButton = view.findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener { dismiss() }

        val okButton: MaterialButton = view.findViewById(R.id.okButton)
        okButton.setOnClickListener {
            listener?.onTimeChosen(timePicker?.hour ?: 0, timePicker?.minute ?: 0)
            dismiss()
        }
    }

    override fun onClick(view: View) {
        // Handle button clicks if needed
    }

    interface OnTimeChosenListener {
        fun onTimeChosen(hour: Int, minute: Int)
    }
}

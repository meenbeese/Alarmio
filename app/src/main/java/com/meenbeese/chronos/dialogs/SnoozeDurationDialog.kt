package com.meenbeese.chronos.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView

import androidx.activity.ComponentDialog

import com.meenbeese.chronos.R


class SnoozeDurationDialog(context: Context, private val names: Array<CharSequence?>, private val listener: OnSnoozeDurationSelectedListener) : ComponentDialog(context) {

    interface OnSnoozeDurationSelectedListener {
        fun onSnoozeDurationSelected(which: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_snooze_duration, null)
        setContentView(view)

        val listView: ListView = view.findViewById(R.id.listView)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, names)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, which, _ ->
            listener.onSnoozeDurationSelected(which)
            dismiss()
        }

        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener { dismiss() }
    }

    override fun show() {
        super.show()
        val window = window
        window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
}

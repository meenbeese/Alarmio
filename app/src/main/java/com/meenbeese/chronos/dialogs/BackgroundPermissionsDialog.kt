package com.meenbeese.chronos.dialogs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button

import androidx.activity.ComponentDialog

import com.meenbeese.chronos.R
import com.meenbeese.chronos.data.PreferenceData


class BackgroundPermissionsDialog(context: Context) : ComponentDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_background_permissions, null)
        setContentView(view)

        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val okButton: Button = view.findViewById(R.id.okButton)

        cancelButton.setOnClickListener { dismiss() }
        okButton.setOnClickListener {
            PreferenceData.INFO_BACKGROUND_PERMISSIONS.setValue(context, true)
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            dismiss()
        }
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

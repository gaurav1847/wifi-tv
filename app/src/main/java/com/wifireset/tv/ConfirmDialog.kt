package com.wifireset.tv

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView

/**
 * A TV-friendly full-screen-dimming dialog with D-pad navigable buttons.
 */
class ConfirmDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val icon: String = "⚠️",
    private val onConfirm: () -> Unit
) : Dialog(context, R.style.Theme_WifiResetTV_Dialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_confirm)

        // Make background dim nicely on TV
        window?.setDimAmount(0.85f)
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        findViewById<TextView>(R.id.tvDialogIcon).text = icon
        findViewById<TextView>(R.id.tvDialogTitle).text = title
        findViewById<TextView>(R.id.tvDialogMessage).text = message

        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        // Default focus goes to Cancel (safer option)
        btnCancel.requestFocus()

        btnCancel.setOnClickListener { dismiss() }

        btnConfirm.setOnClickListener {
            dismiss()
            onConfirm()
        }

        // Dismiss on back press
        setCancelable(true)
        setCanceledOnTouchOutside(false)
    }
}

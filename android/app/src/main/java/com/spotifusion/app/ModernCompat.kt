package com.spotifusion.app

import android.app.AlertDialog
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher

@Suppress("unused")
typealias ModernAlertDialog = AlertDialog

fun EditText.addTextChangedListener(callback: (Editable?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = callback(s)
    })
}

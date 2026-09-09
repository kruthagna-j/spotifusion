package com.spotifusion.app

import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher

typealias AlertDialog = android.app.AlertDialog

typealias ModernAlertDialog = android.app.AlertDialog

typealias AlertDialogCompat = android.app.AlertDialog

fun EditText.addTextChangedListener(callback: (Editable?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = callback(s)
    })
}

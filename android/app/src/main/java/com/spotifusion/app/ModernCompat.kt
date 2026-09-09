package com.spotifusion.app

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView

/** Compatibility helpers used by the programmatic modern Android UI. */
object AlertDialog {
    fun Builder(context: Context): android.app.AlertDialog.Builder =
        android.app.AlertDialog.Builder(context)
}

var EditText.singleLine: Boolean
    get() = isSingleLine
    set(value) = setSingleLine(value)

fun TextView.setTextColor(value: Editable?) {
    setTextColor(Color.WHITE)
}

fun EditText.addTextChangedListener(callback: (Editable?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = callback(s)
    })
}

@file:Suppress("SpellCheckingInspection")

package com.crow.modbus.ext

import android.util.Log

private val TAG = "crowforkotlin"

fun logger(message: Any?, tag: String = TAG, level: Int = Log.INFO) {
    Log.println(level, tag, message.toString())
}

fun loggerError(error: Any?, tag: String = TAG) {
    Log.e(tag, error.toString())
}

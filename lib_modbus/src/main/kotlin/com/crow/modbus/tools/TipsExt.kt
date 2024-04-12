@file:Suppress("SpellCheckingInspection", "MayBeConstant")

package com.crow.modbus.tools

import android.util.Log

private val TAG = "KModbus"

internal fun Any?.info(tag: String = TAG, level: Int = Log.INFO) {
    println(this)
//    Log.println(level, tag, this.toString())
}

internal fun Any?.error(tag: String = TAG, level: Int = Log.ERROR) {
    println(this)
//    Log.println(level, tag, this.toString())
}

internal fun Throwable.message(): String {
    return message ?: localizedMessage ?: stackTraceToString()
}
package com.crow.kmodbus

import android.util.Log

internal fun Any?.info() {
    Log.i("KModbus", this.toString())
}
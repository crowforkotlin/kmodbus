package com.crow.modbus.serialport

import com.crow.base.ext.Bytes

fun interface IDataReceive {
    fun onReceive(buffer: Bytes)
}
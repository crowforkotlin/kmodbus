package com.crow.modbus.serialport

import com.crow.modbus.ext.Bytes

fun interface IDataReceive {
    fun onReceive(buffer: Bytes)
}
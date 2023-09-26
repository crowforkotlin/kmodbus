package com.crow.modbus.serialport

import java.io.File

fun interface IOpenSerialPortSuccess {
    fun onSuccess(device: File,)
}
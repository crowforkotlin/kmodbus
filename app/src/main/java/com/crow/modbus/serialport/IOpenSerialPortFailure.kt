package com.crow.modbus.serialport

import java.io.File

fun interface IOpenSerialPortFailure {
    fun onFailure(device: File, state: SerialPortState)
}
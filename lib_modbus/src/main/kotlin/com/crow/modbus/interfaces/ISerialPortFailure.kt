package com.crow.modbus.interfaces

import com.crow.modbus.serialport.SerialPortState
import java.io.File

fun interface ISerialPortFailure {
    fun onFailure(device: File, state: SerialPortState)
}
package com.crow.modbus.comm.interfaces

import com.crow.modbus.serialport.SerialPortState
import java.io.File

fun interface IOpenSerialPortFailure {
    fun onFailure(device: File, state: SerialPortState)
}
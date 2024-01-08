package com.crow.modbus.interfaces

import java.io.File

fun interface IOpenSerialPortSuccess {
    fun onSuccess(device: File)
}
package com.crow.modbus.interfaces

import java.io.File

fun interface ISerialPortSuccess {
    fun onSuccess(device: File)
}
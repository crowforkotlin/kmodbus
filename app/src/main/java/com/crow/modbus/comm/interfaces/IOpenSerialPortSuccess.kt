package com.crow.modbus.comm.interfaces

import java.io.File

fun interface IOpenSerialPortSuccess {
    fun onSuccess(device: File,)
}
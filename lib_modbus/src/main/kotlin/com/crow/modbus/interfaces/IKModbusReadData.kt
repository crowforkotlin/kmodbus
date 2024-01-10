package com.crow.modbus.interfaces

fun interface IKModbusReadData { suspend fun onRead(array: ByteArray) }
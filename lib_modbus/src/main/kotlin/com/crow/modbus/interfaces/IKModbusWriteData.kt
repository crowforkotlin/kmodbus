package com.crow.modbus.interfaces

fun interface IKModbusWriteData { suspend fun onWrite() : ByteArray? }
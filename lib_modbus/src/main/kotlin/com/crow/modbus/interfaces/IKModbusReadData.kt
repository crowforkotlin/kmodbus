package com.crow.modbus.interfaces

fun interface IKModbusReadData { suspend fun onRead(value: Any) }
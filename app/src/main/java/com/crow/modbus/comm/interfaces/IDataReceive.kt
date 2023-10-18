package com.crow.modbus.comm.interfaces

import com.crow.modbus.ext.Bytes

fun interface IDataReceive {
    fun onReceive(buffer: Bytes)
}
package com.crow.modbus.comm.interfaces

import com.crow.modbus.ext.Bytes
import kotlinx.coroutines.CoroutineScope

fun interface IDataReceive {
    fun onReceive(scope: CoroutineScope, buffer: ByteArray)
}
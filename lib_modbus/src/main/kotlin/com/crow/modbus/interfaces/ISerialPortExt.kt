package com.crow.modbus.interfaces

import java.io.BufferedInputStream
import kotlin.io.path.fileVisitor

internal interface ISerialPortExt {

    fun reOpenSerialPort(ttySNumber: Int, baudRate: Int)
    fun reOpenSerialPort(path: String, baudRate: Int)
    fun openSerialPort(ttysNumber: Int, baudRate: Int)
    fun openSerialPort(path: String, baudRate: Int)
    fun closeSerialPort(): Boolean
}
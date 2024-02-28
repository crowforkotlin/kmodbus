package com.crow.modbus.interfaces

import com.crow.modbus.serialport.SerialPortParityFunction
import java.io.BufferedInputStream
import kotlin.io.path.fileVisitor

internal interface ISerialPortExt {

    fun reOpenSerialPort(ttySNumber: Int, baudRate: Int,  parity: SerialPortParityFunction = SerialPortParityFunction.NONE, stopBit: Int = 1, dataBit: Int = 8)
    fun reOpenSerialPort(path: String, baudRate: Int, parity: SerialPortParityFunction = SerialPortParityFunction.NONE, stopBit: Int = 1, dataBit: Int = 8)
    fun openSerialPort(ttysNumber: Int, baudRate: Int, parity: SerialPortParityFunction = SerialPortParityFunction.NONE, stopBit: Int = 1, dataBit: Int = 8)
    fun openSerialPort(path: String, baudRate: Int, parity: SerialPortParityFunction = SerialPortParityFunction.NONE, stopBit: Int = 1, dataBit: Int = 8)
    fun closeSerialPort(): Boolean
}
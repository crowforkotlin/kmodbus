package com.crow.modbus.serialport

enum class SerialPortParityFunction(val code: Int) {
    NONE(0),
    EVEN(1),
    ODD(2)
}
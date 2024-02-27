package com.crow.kmodbus

import com.crow.modbus.KModbusRtu
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.tools.asciiHexToByte
import com.crow.modbus.tools.toAsciiHexByte
import com.crow.modbus.tools.toAsciiHexBytes
import com.crow.modbus.tools.toHexList
import com.crow.modbus.tools.toIntArray
import kotlin.experimental.and

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    println(
        KModbusRtu().buildMasterOutput(
            function = KModbusFunction.WRITE_SINGLE_REGISTER,
            slaveAddress = 6,
            startAddress = 0,
            count = 2,
            value = 0
        ).toHexList()
    )
    println(toAsciiHexBytes(byteArrayOf(0x01, 0x03, 0x00, 0x00, 0x00, 0x01)).toHexList())
    val byteValue= byteArrayOf(0x01.toByte())
    println(toAsciiHexBytes(byteValue).toHexList())
    val function = asciiHexToByte(0x38, 0x33)
    println((function.toInt() and 0xFF))
    println((function.toInt() and 0xF) + 0x80)
    println((function.toInt() and 0xF) + 0x80 == function.toInt())
}
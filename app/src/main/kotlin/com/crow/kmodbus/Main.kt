package com.crow.kmodbus

import com.crow.modbus.tools.asciiHexToByte
import com.crow.modbus.tools.toAsciiHexByte
import com.crow.modbus.tools.toAsciiHexBytes
import com.crow.modbus.tools.toHexList
import kotlin.experimental.and

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    println(toAsciiHexBytes(byteArrayOf(0x01, 0x03, 0x00, 0x00, 0x00, 0x01)).toHexList())
    val byteValue= byteArrayOf(0x01.toByte())
    println(toAsciiHexBytes(byteValue).toHexList())
    val function = asciiHexToByte(0x38, 0x33)
    println((function.toInt() and 0xFF))
    println((function.toInt() and 0xF) + 0x80)
    println((function.toInt() and 0xF) + 0x80 == function.toInt())
}
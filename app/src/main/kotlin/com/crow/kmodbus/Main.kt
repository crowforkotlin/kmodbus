package com.crow.kmodbus

import android.os.Build.VERSION_CODES.P
import com.crow.modbus.model.KModbusType
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val array = ByteBuffer.wrap(ByteArray(4)).putFloat("156.7".toFloat()).array()
    println(array.map { it.toHexString() })
    println(((array[0].toInt() and 0xFF) shl 8).toShort() or (array[1].toInt() and 0xFF).toShort())
    println(((array[2].toInt() and 0xFF) shl 8).toShort() or (array[3].toInt() and 0xFF).toShort())
    println(ByteBuffer.wrap(ByteArray(2)).putShort(17180).array().map { it.toHexString() })
    println(ByteBuffer.wrap(ByteArray(2)).putShort(-19661).array().map { it.toHexString() })
}
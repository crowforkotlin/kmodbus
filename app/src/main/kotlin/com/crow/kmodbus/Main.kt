package com.crow.kmodbus

import com.crow.modbus.KModbusRtuMaster
import com.crow.modbus.model.KModbusFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import kotlin.experimental.or

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    val array = ByteBuffer.wrap(ByteArray(4)).putFloat("156.7".toFloat()).array()
    println(array.map { it.toHexString() })
    println(((array[0].toInt() and 0xFF) shl 8).toShort() or (array[1].toInt() and 0xFF).toShort())
    println(((array[2].toInt() and 0xFF) shl 8).toShort() or (array[3].toInt() and 0xFF).toShort())
    println(ByteBuffer.wrap(ByteArray(2)).putShort(17180).array().map { it.toHexString() })
    println(ByteBuffer.wrap(ByteArray(2)).putShort(-19661).array().map { it.toHexString() })
    val scope1 = CoroutineScope(Dispatchers.IO)
    val scope2 = CoroutineScope(Job())
    println(
        KModbusRtuMaster.getInstance().build(KModbusFunction.WRITE_SINGLE_REGISTER, 1, 0, value = 3).map {
            it.toHexString()
        })
    val a = byteArrayOf(0x06, 0x00, 0x00, 0x00, 0x01, 0x49, 0xdb.toByte())

    delay(100000)
}
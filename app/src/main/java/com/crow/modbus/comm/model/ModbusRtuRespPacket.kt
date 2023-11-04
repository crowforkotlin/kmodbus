package com.crow.modbus.comm.model

import com.crow.modbus.ext.logger
import java.nio.ByteBuffer


data class ModbusRtuRespPacket(
    val mSlave: Int,
    val mFunctionCode: Int,
    val mBytesCount: Int,
    val mValues: List<Int>,
) {

    fun toFloatData(): List<Float> {
        return runCatching {
            val size = mValues.size shr 2
            val values = ArrayList<Float>(size)
            val buffer= ByteBuffer.wrap(mValues.map { it.toByte() }.toByteArray())
            repeat(size) { values.add(buffer.getFloat()) }
            values
        }
            .onFailure { logger(it.stackTraceToString())  }
            .getOrElse { listOf() }
    }

    fun toIntData(intBitLength: Int = 2): List<Int> {
        return runCatching { when (intBitLength) {
                1 -> mValues
                2 -> {
                    println(mValues)
                    val chunkedValues = mValues.chunked(2)
                    val data = ArrayList<Int>(chunkedValues.size)
                    for (ints in chunkedValues) { data.add(((ints[0] and 0xFF) shl 8) or (ints[1] and 0xFF)) }
                    data
                }
                4 -> {
                    val chunkedValues = mValues.chunked(4)
                    println(chunkedValues)
                    val data = ArrayList<Int>(chunkedValues.size)
                    for (ints in chunkedValues) {
                        data.add(((ints[0] and 0xFF) shl 24) or
                                ((ints[1] and 0xFF) shl 16) or
                                ((ints[2] and 0xFF) shl 8) or
                                (ints[3] and 0xFF))
                    }
                    data
                }
                else -> error("不合法的整形长度参数！")
            } }
            .getOrElse { listOf() }
    }
}
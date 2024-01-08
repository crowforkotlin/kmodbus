package com.listen.x3player.kt.modbus.comm.model

import android.util.Log
import com.crow.modbus.ext.logger
import java.nio.ByteBuffer
import kotlin.math.abs

data class ModbusRtuRespPacket(
    val mDeviceID: Int,
    val mFunctionCode: Int,
    val mBytesCount: Int,
    val mValues: ByteArray,
) {

    companion object {
        private const val DATA_ERROR = "数据解析异常！"
    }

    fun toFloatData(index: Int, precision: Int): String {
        return runCatching {
            "%.${precision}f".format(ByteBuffer.wrap(byteArrayOf(mValues[index + 2], mValues[index + 3], mValues[index], mValues[index + 1])).float)
        }
            .onFailure { it.stackTraceToString().logger(level = Log.ERROR)  }
            .getOrElse { ModbusTcpRespPacket.DATA_ERROR }
    }

    fun toIntData(intBitLength: Int = 2, isUnsigned: Boolean = false): List<Number> {
        return runCatching { when (intBitLength) {
            1 -> mValues.map { it.toInt() }
            2 -> {
                val size = mValues.size shr 1
                val data = ArrayList<Number>(size)
                if (isUnsigned) {
                    repeat(size) { index ->
                        val pos = index shl 1
                        data.add(((mValues[pos].toInt() and 0xFF) shl 8)
                                or (mValues[pos + 1].toInt() and 0xFF))
                    }
                } else {
                    repeat(size) { index ->
                        val pos = index shl 1
                        data.add((((mValues[pos].toInt() and 0xFF) shl 8)
                                or (mValues[pos + 1].toInt() and 0xFF)).toShort())
                    }
                }
                data
            }
            4 -> {
                val size = mValues.size shr 1
                val data = ArrayList<Number>(size)
                if (isUnsigned) {
                    repeat(size) { index ->
                        val pos = index shl 2
                        data.add(((mValues[pos].toLong() and 0xFFL) shl 24)
                                or ((mValues[pos + 1].toLong() and 0xFFL) shl 16)
                                or ((mValues[pos + 2].toLong() and 0xFFL) shl 8)
                                or (mValues[pos + 3].toLong() and 0xFFL))
                    }
                } else {
                    repeat(size) { index ->
                        val pos = index shl 1
                        data.add((mValues[pos].toInt() and 0xFF shl 24)
                                or (mValues[pos + 1].toInt() and 0xFF shl 16)
                                or (mValues[pos + 2].toInt() and 0xFF shl 8)
                                or (mValues[pos + 3].toInt() and 0xFF))
                    }
                }
                data
            }
            else -> error("不合法的整形长度参数！")
        } }
            .onFailure { it.stackTraceToString().logger(level = Log.ERROR)  }
            .getOrElse { listOf() }
    }

    fun toIntData(index: Int, intBitLength: Int = 2, isUnsigned: Boolean = false): String {
        return runCatching {
            when (intBitLength) {
                1 -> {
                    if (isUnsigned) {
                        (((mValues[index].toInt() and 0xFF) shl 8) or (mValues[index + 1].toInt() and 0xFF)).toString()
                    } else {
                        ((mValues[index].toInt() shl 8) or mValues[index + 1].toInt()).toString()
                    }
                }
                2 -> {
                    if (isUnsigned) {
                        (((mValues[index].toInt() and 0xFF) shl 8)
                                or (mValues[index + 1].toInt() and 0xFF)).toString()
                    } else {
                        (((mValues[index].toInt() and 0xFF shl 8)
                                or (mValues[index + 1].toInt() and 0xFF)).toShort()).toString()
                    }
                }
                4 -> {
                    if (isUnsigned) {
                        (((mValues[index].toLong() and 0xFFL) shl 24)
                                or ((mValues[index + 1].toLong() and 0xFFL) shl 16)
                                or ((mValues[index + 2].toLong() and 0xFFL) shl 8)
                                or (mValues[index + 3].toLong() and 0xFFL)).toString()
                    } else {
                        ((mValues[index].toInt() and 0xFF shl 24)
                                or (mValues[index + 1].toInt() and 0xFF shl 16)
                                or (mValues[index + 2].toInt() and 0xFF shl 8)
                                or (mValues[index + 3].toInt() and 0xFF)).toString()
                    }
                }
                else -> error("不合法的整形长度参数！")
            }
        }
            .onFailure { println(it.stackTraceToString()) }
            .getOrElse { ModbusTcpRespPacket.DATA_ERROR }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ModbusRtuRespPacket
        if (mDeviceID != other.mDeviceID) return false
        if (mFunctionCode != other.mFunctionCode) return false
        if (mBytesCount != other.mBytesCount) return false
        if (!mValues.contentEquals(other.mValues)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = mDeviceID
        result = 31 * result + mFunctionCode
        result = 31 * result + mBytesCount
        result = 31 * result + mValues.contentHashCode()
        return result
    }
}
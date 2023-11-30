package com.crow.modbus.comm.model

import android.util.Log
import com.crow.modbus.ext.logger
import java.nio.ByteBuffer
import kotlin.math.abs

data class ModbusRtuRespPacket(
    val mDeviceID: Int,
    val mFunctionCode: Int,
    val mBytesCount: Int,
    val mValues: List<Int>,
) {

    companion object {
        private const val DATA_ERROR = "数据解析异常！"
    }

    fun toFloatData(index: Int, precision: Int): String {
        return runCatching {
            "%.${precision}f".format(ByteBuffer.wrap(mValues.subList(index , index + 4).map { it.toByte() }.toByteArray()).float)
        }
            .onFailure { it.stackTraceToString().logger(level = Log.ERROR)  }
            .getOrElse { DATA_ERROR }
    }

    fun toIntData(intBitLength: Int = 2, isUnsigned: Boolean = false): List<Int> {
        return runCatching { when (intBitLength) {
            1 -> mValues
            2 -> {
                val chunkedValues = mValues.chunked(2)
                val data = ArrayList<Int>(chunkedValues.size)
                if (isUnsigned) {
                    for (ints in chunkedValues) {
                        val value = ((ints[0] and 0xFF) shl 8) or (ints[1] and 0xFF)
                        data.add(if (value >= 0) value else 32767 + abs(value))
                    }
                } else {
                    for (ints in chunkedValues) {
                        data.add(((ints[0] and 0xFF) shl 8) or (ints[1] and 0xFF))
                    }
                }
                data
            }
            4 -> {
                val chunkedValues = mValues.chunked(4)
                println(chunkedValues)
                val data = ArrayList<Int>(chunkedValues.size)
                if (isUnsigned) {
                    for (ints in chunkedValues) {
                        val value = ((ints[0] and 0xFF) shl 24) or
                                ((ints[1] and 0xFF) shl 16) or
                                ((ints[2] and 0xFF) shl 8) or
                                (ints[3] and 0xFF)
                        data.add(if (value < 0) 32767 + value else value)
                    }
                } else {
                    for (ints in chunkedValues) {
                        data.add(((ints[0] and 0xFF) shl 24) or
                                ((ints[1] and 0xFF) shl 16) or
                                ((ints[2] and 0xFF) shl 8) or
                                (ints[3] and 0xFF))
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
                        val value = ((mValues[index] and 0xFF) shl 8) or (mValues[index+1] and 0xFF)
                        (if (value < 0) 32767 + (32769 - (abs(value))) else value).toString()
                    } else {
                        val value = ((mValues[index] and 0xFF) shl 8) or (mValues[index + 1] and 0xFF)
                        value.toString()
                    }
                }
                2 -> {
                    if (isUnsigned) {
                        val value = ((mValues[index] and 0xFF) shl 24) or
                                ((mValues[index + 1] and 0xFF) shl 16) or
                                ((mValues[index + 2] and 0xFF) shl 8) or
                                (mValues[index + 3] and 0xFF)
                        (if (value < 0) Int.MAX_VALUE.toLong() + abs(value) else value.toLong()).toString()
                    } else {
                        val value = ((mValues[index] and 0xFF) shl 24) or
                                ((mValues[index + 1] and 0xFF) shl 16) or
                                ((mValues[index + 2] and 0xFF) shl 8) or
                                (mValues[index + 3] and 0xFF)
                        value.toString()
                    }
                }
                else -> error("不合法的整形长度参数！")
            }
        }
            .onFailure { it.stackTraceToString().logger(level = Log.ERROR)  }
            .getOrElse { DATA_ERROR }
    }
}
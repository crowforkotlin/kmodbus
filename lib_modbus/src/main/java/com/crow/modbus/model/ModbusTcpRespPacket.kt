package com.crow.modbus.model

import android.util.Log
import com.crow.modbus.tools.error
import com.listen.common.base.ext.log
import com.listen.common.base.ext.logger
import java.nio.ByteBuffer
import kotlin.math.abs

data class ModbusTcpRespPacket(
    val mDeviceID: Int,
    val mFunctionCode: Int,
    val mBytesCount: Int,
    val mValues: ByteArray,
) {

    companion object {
        internal const val DATA_ERROR = "数据解析异常！"
    }

    fun toFloatData(index: Int, precision: Int): String {
        return runCatching {
            "%.${precision}f".format(ByteBuffer.wrap(byteArrayOf(mValues[index], mValues[index + 1], mValues[index + 2], mValues[index + 3])).float)
        }
            .onFailure { it.stackTraceToString().error()  }
            .getOrElse { DATA_ERROR }
    }

    fun toIntData(intBitLength: Int = 2, isUnsigned: Boolean = false): List<Int> {
        return runCatching {
            when (intBitLength) {
                1 -> mValues.map { it.toInt() }
                2 -> {
                    val size = mValues.size shr 1
                    val data = ArrayList<Int>(size)
                    if (isUnsigned) {
                        repeat(size) { index ->
                            val pos = index shl 1
                            data.add(((mValues[pos].toInt() and 0xFF) shl 8) or (mValues[pos + 1].toInt() and 0xFF))
                        }
                    } else {
                        repeat(size) { index ->
                            val pos = index shl 1
                            data.add((mValues[pos].toInt() shl 8) or (mValues[pos + 1].toInt() and 0xFF))
                        }
                    }
                    data
                }
                4 -> {
                    val size = mValues.size shr 1
                    val data = ArrayList<Int>(size)
                    if (isUnsigned) {
                        repeat(size) { index ->
                            val pos = index shl 2
                            data.add(((mValues[pos].toInt() and 0xFF) shl 24) or ((mValues[pos + 1].toInt() and 0xFF) shl 16) or ((mValues[pos + 2].toInt() and 0xFF) shl 8) or (mValues[pos + 3].toInt() and 0xFF))
                        }
                    } else {
                        repeat(size) { index ->
                            val pos = index shl 1
                            data.add((mValues[pos].toInt() shl 24) or (mValues[pos + 1].toInt()shl 16) or (mValues[pos + 2].toInt() shl 8) or (mValues[pos + 3].toInt() and 0xFF))
                        }
                    }
                    data
                }
                else -> kotlin.error("不合法的整形长度参数！")
            }
        }
            .onFailure { it.stackTraceToString().error()  }
            .getOrElse { listOf() }
    }

    fun toIntData(index: Int, intBitLength: Int = 2, isUnsigned: Boolean = false): String {
        return runCatching {
            val byte1 = mValues[index].toInt()
            val byte2 = mValues[index + 1].toInt()
            when (intBitLength) {
                1 -> {
                    if (isUnsigned) {
                        (((byte1 and 0xFF) shl 8) or (byte2 and 0xFF)).toString()
                    } else {
                        ((byte1 shl 8) or byte2).toString()
                    }
                }
                2 -> {
                    if (isUnsigned) {
                        (((byte1 and 0xFF) shl 24) or ((byte2 and 0xFF) shl 16) or ((mValues[index + 2].toInt() and 0xFF) shl 8) or (mValues[index + 3].toInt() and 0xFF)).toString()
                    } else {
                        ((byte1 shl 24) or (byte2 shl 16) or (mValues[index + 2].toInt() shl 8) or (mValues[index + 3].toInt() and 0xFF)).toString()
                    }
                }
                else -> kotlin.error("不合法的整形长度参数！")
            }
        }
            .onFailure { it.stackTraceToString().error()  }
            .getOrElse { DATA_ERROR }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModbusTcpRespPacket

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
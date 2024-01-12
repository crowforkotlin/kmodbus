@file:Suppress("FunctionName", "SpellCheckingInspection")

package com.crow.modbus.tools

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.experimental.or

internal val baseASCII_0 = Character.digit('0', 10)

internal val baseASCII_A =  Character.digit('A', 10)

internal const val baseTenF = 0xF

internal const val baseTen4 = 4

internal const val DATA_ERROR = "数据解析异常！"

fun toReverseInt8(value: Int): Int {
    return value and baseTenF shl baseTen4 or (value shr baseTen4 and baseTenF)
}


/**
 * ● 大端序Bytes 转 Int32
 *
 * ● 2023-09-13 14:53:19 周三 下午
 */
fun toInt32(bytes: ByteArray, startIndex: Int = 0, isLittleEndian: Boolean = false): Int {
    return((bytes[startIndex].toInt() and 0xFF) shl 24) or
            ((bytes[startIndex + 1].toInt() and 0xFF) shl 16) or
            ((bytes[startIndex + 2].toInt() and 0xFF) shl 8) or
            (bytes[startIndex + 3].toInt() and 0xFF)
}

/**
 * ● 无符号Int16
 *
 * ● 2024-01-10 17:46:26 周三 下午
 * @author crowforkotlin
 */
fun toUInt16(array: ByteArray, index: Int): Int {
    return ((array[index].toInt() and 0xFF) shl 8) or (array[index + 1].toInt() and 0xFF)
}

/**
 * ● 无符号Int16
 *
 * ● 2024-01-10 17:46:26 周三 下午
 * @author crowforkotlin
 */
fun toUInt16LittleEndian(array: ByteArray, index: Int = 0): Int {
    return ((array[index + 1].toInt() and 0xFF) shl 8) or (array[index].toInt() and 0xFF)
}

/**
 * ● 有符号Int16
 *
 * ● 2024-01-10 17:46:17 周三 下午
 * @author crowforkotlin
 */
fun toInt16(array: ByteArray, index: Int = 0): Int {
    return (((array[index].toInt() and 0xFF) shl 8).toShort() or (array[index + 1].toInt() and 0xFF).toShort()).toInt()
}

/**
 * ● 有符号Int16
 *
 * ● 2024-01-10 17:46:17 周三 下午
 * @author crowforkotlin
 */
fun toInt16LittleEndian(array: ByteArray, index: Int = 0): Int {
    return (((array[index + 1].toInt() and 0xFF) shl 8).toShort() or (array[index].toInt() and 0xFF).toShort()).toInt()
}

/**
 * ● 小端序Bytes 转 Int32
 *
 * ● 2023-09-23 15:07:14 周六 下午
 */
fun toInt32LittleEndian(bytes: ByteArray, startIndex: Int = 0): Int {
    return (bytes[startIndex].toInt() and 0xFF) or
            ((bytes[startIndex + 1].toInt() and 0xFF) shl 8) or
            ((bytes[startIndex + 2].toInt() and 0xFF) shl 16) or
            ((bytes[startIndex + 3].toInt() and 0xFF) shl 24)
}

/**
 * ● Int8 转 Bytes
 *
 * ● 2023-09-23 15:13:24 周六 下午
 */
fun fromInt8(int32: Int) = byteArrayOf((int32 and 0xFF).toByte())

/**
 * ● Int16转 Bytes -- 大端序
 *
 * ● 2023-09-23 15:06:59 周六 下午
 */
fun fromInt16(int32: Int) = byteArrayOf(((int32 shr 8) and 0xFF).toByte(), (int32 and 0xFF).toByte())

/**
 * ● Int16 转 Bytes -- 小端序
 *
 * ● 2023-09-23 15:06:36 周六 下午
 */
fun fromInt16LittleEndian(int32: Int) = byteArrayOf((int32 and 0xFF).toByte(), ((int32 shr 8) and 0xFF).toByte())

/**
 * ● Int32 转 Bytes -- 大端序
 *
 * ● 2023-09-23 15:06:17 周六 下午
 */
fun fromInt32(int32: Int) = byteArrayOf(
    ((int32 shr 24) and 0xFF).toByte(),
    ((int32 shr 16) and 0xFF).toByte(),
    ((int32 shr 8) and 0xFF).toByte(),
    (int32 and 0xFF).toByte()
)

/**
 * ● Int32 转 Bytes -- 小端序
 *
 * ● 2023-09-23 15:05:49 周六 下午
 */
fun fromInt32LitterEndian(int32: Int) = byteArrayOf(
    (int32 and 0xFF).toByte(),
    ((int32 shr 8) and 0xFF).toByte(),
    ((int32 shr 16) and 0xFF).toByte(),
    ((int32 shr 24) and 0xFF).toByte()
)

/**
 * ● 大端序Int 构建新的ByteArray
 *
 * ● 2023-09-13 16:52:36 周三 下午
 */
@Deprecated("It may be removed in the future.")
fun toByteArrayBigEndian(value: Any): ByteArray {
    return when (value) {
        is Int -> {
            byteArrayOf(
                ((value shr 24) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                (value and 0xFF).toByte()
            )
        }
        is Long -> {
            byteArrayOf(
                ((value shr 56) and 0xFF).toByte(),
                ((value shr 48) and 0xFF).toByte(),
                ((value shr 40) and 0xFF).toByte(),
                ((value shr 32) and 0xFF).toByte(),
                ((value shr 24) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                (value and 0xFF).toByte()
            )
        }
        is Short -> {
            val valueInt = value.toInt()
            byteArrayOf(
                ((valueInt shr 8) and 0xFF).toByte(),
                (valueInt and 0xFF).toByte()
            )
        }
        is Byte -> byteArrayOf(value)
        else -> error("type must be Int or UInt!")
    }
}

/**
 * ● 大端序Int 构建新的ByteArray
 *
 * ● 2023-09-13 16:52:36 周三 下午
 */
@Deprecated("It may be removed in the future.")
fun toByteArrayLittleEndian(value: Any): ByteArray {
    return when (value) {
        is Int -> {
            byteArrayOf(
                (value and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 24) and 0xFF).toByte()
            )
        }
        is Long -> {
            byteArrayOf(
                (value and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 24) and 0xFF).toByte(),
                ((value shr 32) and 0xFF).toByte(),
                ((value shr 40) and 0xFF).toByte(),
                ((value shr 48) and 0xFF).toByte(),
                ((value shr 56) and 0xFF).toByte(),
            )
        }
        is Short -> {
            val valueInt = value.toInt()
            byteArrayOf(
                (valueInt and 0xFF).toByte(),
                ((valueInt shr 8) and 0xFF).toByte()
            )
        }
        is Byte -> byteArrayOf(value)
        else -> error("type must be Int or UInt!")
    }
}

fun fromAsciiInt8(value: Int): Pair<Byte, Byte> {
    val hight = (value shr 0x04)
    val low = value and 0x0F
    return toAsciiInt(hight).toByte() to toAsciiInt(low).toByte()
}

fun fromAsciiInt16(value: Int): ByteArray {
    val hight = fromAsciiInt8((value shr 8) and 0xFF)
    val low= fromAsciiInt8(value and 0xFF)
    return byteArrayOf(
        hight.first,
        hight.second,
        low.first,
        low.second
    )
}

fun toAsciiInt(valueHex: Int): Int { return  if (valueHex < 10) valueHex + baseASCII_0 else valueHex - 10 + baseASCII_A }

fun toAsciiHexByte(value: Byte, stream: ByteArrayOutputStream) {
    val high = ((value.toInt() shr 4) and 0x0F) + baseASCII_0
    val low = (value.toInt() and 0x0F) + baseASCII_0
    stream.write(high)
    stream.write(low)
}

fun toAsciiHexBytes(data: ByteArray): ByteArray {
    val stream = ByteArrayOutputStream()
    for (byte in data) { toAsciiHexByte(byte, stream) }
    return stream.toByteArray()
}

fun ByteArray.copy(): ByteArray {
    val array = ByteArray(size)
    System.arraycopy(this, 0, array, 0, size)
    return array
}
fun BufferedInputStream.readBytes(size: Int, isReverse: Boolean = false): ByteArray {
    val bytes = ByteArray(size)
    var bytesReaded = 0
    while (bytesReaded < size) {
        val readed = read(bytes, bytesReaded, size - bytesReaded)
        if (readed == -1) { break }
        bytesReaded += readed
    }
    return if (isReverse) bytes.reversedArray() else bytes
}

fun ByteArray.toIntData(intBitLength: Int = 2, isUnsigned: Boolean = false): List<Int> {
    return runCatching {
        when (intBitLength) {
            1 -> map { it.toInt() }
            2 -> {
                val size = size shr 1
                val data = ArrayList<Int>(size)
                if (isUnsigned) {
                    repeat(size) { index ->
                        val pos = index shl 1
                        data.add(((this[pos].toInt() and 0xFF) shl 8) or (this[pos + 1].toInt() and 0xFF))
                    }
                } else {
                    repeat(size) { index ->
                        val pos = index shl 1
                        data.add((this[pos].toInt() shl 8) or (this[pos + 1].toInt() and 0xFF))
                    }
                }
                data
            }
            4 -> {
                val size = this.size shr 1
                val data = ArrayList<Int>(size)
                if (isUnsigned) {
                    repeat(size) { index ->
                        val pos = index shl 2
                        data.add(((this[pos].toInt() and 0xFF) shl 24) or ((this[pos + 1].toInt() and 0xFF) shl 16) or ((this[pos + 2].toInt() and 0xFF) shl 8) or (this[pos + 3].toInt() and 0xFF))
                    }
                } else {
                    repeat(size) { index ->
                        val pos = index shl 1
                        data.add((this[pos].toInt() shl 24) or (this[pos + 1].toInt()shl 16) or (this[pos + 2].toInt() shl 8) or (this[pos + 3].toInt() and 0xFF))
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

fun ByteArray.toFloatData(index: Int, precision: Int): String {
    return runCatching {
        "%.${precision}f".format(ByteBuffer.wrap(byteArrayOf(this[index], this[index + 1], this[index + 2], this[index + 3])).float)
    }
        .onFailure { it.stackTraceToString().error()  }
        .getOrElse { DATA_ERROR }
}

fun ByteArray.toIntData(index: Int, intBitLength: Int = 2, isUnsigned: Boolean = false): String {
    return runCatching {
        val byte1 = this[index].toInt()
        val byte2 = this[index + 1].toInt()
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
                    (((byte1 and 0xFF) shl 24) or ((byte2 and 0xFF) shl 16) or ((this[index + 2].toInt() and 0xFF) shl 8) or (this[index + 3].toInt() and 0xFF)).toString()
                } else {
                    ((byte1 shl 24) or (byte2 shl 16) or (this[index + 2].toInt() shl 8) or (this[index + 3].toInt() and 0xFF)).toString()
                }
            }
            else -> kotlin.error("不合法的整形长度参数！")
        }
    }
        .onFailure { it.stackTraceToString().error()  }
        .getOrElse { DATA_ERROR }
}
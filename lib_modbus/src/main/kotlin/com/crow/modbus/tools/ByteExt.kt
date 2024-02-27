@file:Suppress("FunctionName", "SpellCheckingInspection")

package com.crow.modbus.tools

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.lang.Float.floatToIntBits
import kotlin.experimental.and
import kotlin.experimental.or

internal const val ASCII_0 = '0'.code
internal const val ASCII_A = 'A'.code
internal const val ASCII_9 = '9'.code

internal const val baseTenF = 0xF

internal const val baseTen4 = 4

internal const val DATA_ERROR = "数据解析异常！"

fun toReverseInt8(value: Int): Int {
    return value and baseTenF shl baseTen4 or (value shr baseTen4 and baseTenF)
}


/**
 * ● 无符号Int32 为Long类型
 *
 * ● 2024-01-10 17:46:17 周三 下午
 * @author crowforkotlin
 */
fun toUInt32(array: ByteArray, index: Int = 0): Long {
    return (((array[index].toLong() and 0xFFL) shl 24) or
            ((array[index + 1].toLong() and 0xFFL) shl 16) or
            ((array[index + 2].toLong() and 0xFFL) shl 8) or
            (array[index + 3].toLong() and 0xFFL))
}

/**
 * ● 有符号Int32
 *
 * ● 2024-01-10 17:46:17 周三 下午
 * @author crowforkotlin
 */
fun toInt32(array: ByteArray, index: Int = 0): Int {
    return (((array[index].toInt() and 0xFF) shl 24) or
            ((array[index + 1].toInt() and 0xFF) shl 16) or
            ((array[index + 2].toInt() and 0xFF) shl 8) or
            (array[index + 3].toInt() and 0xFF))
}

/**
 * ● 无符号Int16
 *
 * ● 2024-01-10 17:46:26 周三 下午
 * @author crowforkotlin
 */
fun toUInt16(array: ByteArray, index: Int = 0): Int {
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
    val hight = (value shr 0x04) + 0x30
    val low = value and 0x0F + 0x30
    return hight.toByte() to low.toByte()
}

/**
 * ● AscillInt16 to ByteArray
 *
 * ● 2024-01-16 17:19:46 周二 下午
 * @author crowforkotlin
 */
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

fun toAsciiHexByte(valueByte: Byte, stream: ByteArrayOutputStream) {
    val value = valueByte.toInt()
    var high = ((value shr 4) and 0x0F) + ASCII_0
    var low = (value and 0x0F) + ASCII_0
    if (high > ASCII_9) high += (ASCII_A - ASCII_9 - 1)
    if (low > ASCII_9) low += (ASCII_A - ASCII_9 - 1)
    stream.write(high)
    stream.write(low)
}

fun toAsciiHexByte(valueByte: Byte): Pair<Int, Int> {
    val value = valueByte.toInt()
    var high = ((value shr 4) and 0x0F) + ASCII_0
    var low = (value and 0x0F) + ASCII_0
    if (high > ASCII_9) high += (ASCII_A - ASCII_9 - 1)
    if (low > ASCII_9) low += (ASCII_A - ASCII_9 - 1)
    return high to low
}

fun asciiHexToByte(high: Int, low: Int): Byte {
    val highNibble = if (high >= ASCII_A) high - ASCII_A + 10 else high - ASCII_0
    val lowNibble = if (low >= ASCII_A) low - ASCII_A + 10 else low - ASCII_0
    return ((highNibble shl 4) or lowNibble).toByte()
}

fun toAsciiHexBytes(data: ByteArray): ByteArray {
    val stream = ByteArrayOutputStream()
    for (byte in data) { toAsciiHexByte(byte, stream) }
    return stream.toByteArray()
}


fun fromFloat32(value: Float): ByteArray {
    val intBits = floatToIntBits(value)  // 将float转换为Int位表示
    return byteArrayOf(
        (intBits shr 24).toByte(),  // 第一个字节（最高有效字节）
        (intBits shr 16).toByte(),  // 第二个字节
        (intBits shr 8).toByte(),   // 第三个字节
        (intBits).toByte()          // 第四个字节（最低有效字节）
    )
}

fun toFloat32(value: ByteArray): Float {
    val intBits = (value[0].toInt() and 0xFF shl 24) or
            (value[1].toInt() and 0xFF shl 16) or
            (value[2].toInt() and 0xFF shl 8) or
            (value[3].toInt() and 0xFF)
    return Float.fromBits(intBits)
}


fun ByteArray.toHexList(): List<String> = map { String.format("%02x", it) }

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

fun ByteArray.toIntData(intBitLength: Int = 2, isUnsigned: Boolean = false): List<Number> {
    return runCatching {
        when (intBitLength) {
            1 -> map { it.toInt() }
            2 -> {
                val size = size shr 1
                val data = ArrayList<Int>(size)
                if (isUnsigned) repeat(size) { index -> data.add(toUInt16(this, index shl 1)) }
                else repeat(size) { index -> data.add(toInt16(this, index shl 1)) }
                data
            }
            4 -> {
                val size = this.size shr 1
                val data = ArrayList<Number>(size)
                if (isUnsigned) repeat(size) { index -> data.add(toUInt32(this, index shl 2)) }
                else repeat(size) { index -> data.add(toInt32(this, index shl 1)) }
                data
            }
            else -> kotlin.error("不合法的整形长度参数！")
        }
    }
        .onFailure { it.stackTraceToString().error()  }
        .getOrElse { listOf() }
}

fun ByteArray.toFloatData(index: Int, precision: Int): String {
    return runCatching { "%.${precision}f".format(toFloat32(byteArrayOf(this[index], this[index + 1], this[index + 2], this[index + 3]))) }
        .onFailure { it.stackTraceToString().error()  }
        .getOrElse { "0.0" }
}

fun ByteArray.toIntData(index: Int, intBitLength: Int = 2, isUnsigned: Boolean = false): String {
    return runCatching {
        when (intBitLength) {
            1 -> this[index].toInt().toString()
            2 -> (if (isUnsigned) toUInt16(this, index) else toInt16(this, index)).toString()
            4 -> (if (isUnsigned) toUInt32(this, index) else toInt32(this, index)).toString()
            else -> kotlin.error("不合法的整形长度参数！")
        }
    }
        .onFailure { it.stackTraceToString().error()  }
        .getOrElse { "0" }
}

/**
 * ● 浮点数转Int字节数组，一般不会超过两个元素
 *
 * ● 2024-01-16 17:26:17 周二 下午
 * @author crowforkotlin
 */
fun Float.toIntArray() = with(fromFloat32(this)) { intArrayOf(toInt16(this, 0), toInt16(this, 2)) }


fun ByteArray.toStringGB2312(index: Int, intBitLength: Int) : String {
    return runCatching {
        String(copyOfRange(index, index + (intBitLength shl 1)), charset = charset("GB2312")) }
        .onFailure { it.stackTraceToString().error()  }
        .getOrElse { "0" }
}
package com.crow.modbus

import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.ext.formateAsBytes
import com.listen.x3player.kt.modbus.comm.KModbusRtuMaster
import com.listen.x3player.kt.modbus.comm.model.ModbusTcpRespPacket
import java.nio.ByteBuffer

fun parseModbusFloat(bytes: ByteArray): Float {
    // 调整字节顺序（3412）
    val reorderedBytes = byteArrayOf(bytes[2], bytes[3], bytes[0], bytes[1])

    // 将字节数组转换为浮点数
    return ByteBuffer.wrap(reorderedBytes).float
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    println(
        KModbusRtuMaster.getInstance()
            .build(ModbusFunction.WRITE_HOLDING_REGISTERS, 5, 0, 2, values = intArrayOf(1, 1))
            .map { it.toHexString() })
    val data = "01 03 34 79 8D 41 2D C6 9F 43 6B 40 83 43 25 D5 E7 43 7C AE 2E 43 BA 15 9C 43 AE B0 69 40 6D 7F 3F 40 AF 6D 9B 40 F5 94 30 47 9F 51 EC 41 18 80 01 43 7A 80 0B C3 09 0A 67"
    val bytes = formateAsBytes(data) ?: return
    val bytes2 = byteArrayOf(4)
    println(
        KModbusRtuMaster.getInstance().resolve(bytes, ModbusEndian.ARRAY_BIG_BYTE_BIG)
            ?.toFloatData(0, 2)
    )
    byteArrayOf(0x7f, 0xFF.toByte())
    println(ByteBuffer.wrap(ByteArray(4)).putInt(2147483647).array().toHexString())
    println(((0xFF and 0xFF) shl 8) or (0xFF))
    val a1 = (0xFFL and 0xFF) shl 24
    val a2 = (0xFFL and 0xFF) shl 16
    val a3 = (0xFFL and 0xFF) shl 8
    val b = 0xFFL and 0xff
    println((a1 or a2 or a3 or b))
    println(bytes.toHexString())
    Int.MAX_VALUE
    println(toIntData(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), 0, 4, true))
//    val packet: ModbusRtuRespPacket = KModbusRtuMaster.getInstance().resolve(bytes, ModbusEndian.ARRAY_BIG_BYTE_BIG)
//    println(packet)
//    println(packet.toFloatData(0, 2))
//
//    println(parseModbusFloat(byteArrayOf(0x79, 0x8D.toByte(), 0x41, 0x2D)))
}

fun toIntData(mValues: ByteArray, index: Int, intBitLength: Int = 2, isUnsigned: Boolean = false): String {
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
                    (((mValues[index].toLong() and 0xFFL) shl 24) or ((mValues[index + 1].toLong() and 0xFFL) shl 16) or ((mValues[index + 2].toLong() and 0xFFL) shl 8) or (mValues[index + 3].toLong() and 0xFFL)).toString()
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
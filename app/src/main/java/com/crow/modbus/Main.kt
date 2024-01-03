package com.crow.modbus

import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusRtuRespPacket
import com.crow.modbus.ext.formateAsBytes
import com.listen.x3player.kt.modbus.comm.KModbusRtuMaster
import java.nio.ByteBuffer

fun parseModbusFloat(bytes: ByteArray): Float {
    // 调整字节顺序（3412）
    val reorderedBytes = byteArrayOf(bytes[2], bytes[3], bytes[0], bytes[1])

    // 将字节数组转换为浮点数
    return ByteBuffer.wrap(reorderedBytes).float
}

@OptIn(ExperimentalStdlibApi::class)
suspend fun main() {
    val data = "01 03 34 79 8D 41 2D C6 9F 43 6B 40 83 43 25 D5 E7 43 7C AE 2E 43 BA 15 9C 43 AE B0 69 40 6D 7F 3F 40 AF 6D 9B 40 F5 94 30 47 9F 51 EC 41 18 80 01 43 7A 80 0B C3 09 0A 67"
    val bytes = formateAsBytes(data) ?: return
    println(bytes.toHexString())
//    val packet: ModbusRtuRespPacket = KModbusRtuMaster.getInstance().resolve(bytes, ModbusEndian.ARRAY_BIG_BYTE_BIG)
//    println(packet)
//    println(packet.toFloatData(0, 2))
//
//    println(parseModbusFloat(byteArrayOf(0x79, 0x8D.toByte(), 0x41, 0x2D)))
}
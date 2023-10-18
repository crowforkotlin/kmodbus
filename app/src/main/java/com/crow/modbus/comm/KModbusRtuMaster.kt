package com.crow.modbus.comm

import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.ext.BytesOutput
import com.crow.modbus.ext.toReverseInt8

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/23 14:00
 * @Author: CrowForKotlin
 * @Description: ModbusRtuMaster
 * @formatter:on
 **************************/
class KModbusRtuMaster private constructor() : KModbus() {

    companion object {

        private var instance: KModbusRtuMaster? = null

        fun getInstance(): KModbusRtuMaster {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = KModbusRtuMaster()
                    }
                }
            }
            return instance!!
        }
    }

    /**
     * ● 构造数据
     *
     * ● 2023-10-16 16:16:40 周一 下午
     * @author crowforkotlin
     */
    fun build(
        function: ModbusFunction,
        slave: Int,
        startAddress: Int,
        count: Int,
        value: Int? = null,
        values: IntArray? = null,
        endian: ModbusEndian = ModbusEndian.ARRAY_BIG_BYTE_BIG,
    ): ByteArray {
        val output = toCalculateCRC16(
            buildOutput(
                slave,
                function,
                startAddress,
                count,
                value,
                values
            )
        ).toByteArray()
        return when (endian) {
            ModbusEndian.ARRAY_BIG_BYTE_BIG -> output
            ModbusEndian.ARRAY__LITTLE_BYTE_BIG -> output.reversedArray()
            ModbusEndian.ARRAY_LITTLE_BYTE_LITTLE -> {
                val bytes = BytesOutput()
                output.reversedArray().forEach { bytes.writeInt8(toReverseInt8(it.toInt())) }
                bytes.toByteArray()
            }
            ModbusEndian.ARRAY_BIG_BYTE_LITTLE -> {
                val bytes = BytesOutput()
                output.forEach { bytes.writeInt8(toReverseInt8(it.toInt())) }
                bytes.toByteArray()
            }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    // 给定的16位数据
    val data = 0xAB

    // 提取 "" 和 "BA"
    val a = (data shr 4) and 0xF
    val b = data and 0xF
    println(((data and 0xF) shl 4 or (data shr 4 and 0xF)).toHexString())
    println((data and 0xF shl 4 or (data shr 4 and 0xF)).toHexString())
    val result = b shl 4 or a
    val bytesOutput = BytesOutput()
    bytesOutput.writeInt8(a)
    bytesOutput.writeInt8(b)
    bytesOutput.writeInt8(result)
    println(bytesOutput.toByteArray().map { it.toHexString() })


}
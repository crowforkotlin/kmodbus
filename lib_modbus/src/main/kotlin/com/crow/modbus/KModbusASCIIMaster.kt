@file:Suppress("PrivatePropertyName")

package com.crow.modbus

import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.fromAsciiInt8
import com.crow.modbus.tools.toAsciiHexBytes
import com.crow.modbus.tools.toReverseInt8

/*************************
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/10/10 13:46
 * @Author: CrowForKotlin
 * @formatter:on
 **************************/
class KModbusASCIIMaster private constructor() : com.crow.modbus.KModbus() {

    companion object {

        private var instance: KModbusASCIIMaster? = null

        fun getInstance() : KModbusASCIIMaster {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = KModbusASCIIMaster()
                    }
                }
            }
            return instance!!
        }
    }

    private val HEAD = 0x3A
    private val END = byteArrayOf(0x0d, 0x0A)

    fun build(function: KModbusFunction, slave: Int, startAddress: Int, count: Int, value: Int? = null, values: IntArray? = null, endian: ModbusEndian = ModbusEndian.ARRAY_BIG_BYTE_BIG): ByteArray {
        val bytes = BytesOutput()
        val output = buildMasterRequestOutput(slave, function, startAddress, count, value, values).toByteArray()
        val pLRC = fromAsciiInt8(toCalculateLRC(output))
        val outputAscii = toAsciiHexBytes(output)
        bytes.writeInt8(HEAD)
        bytes.writeBytes(outputAscii, outputAscii.size)
        bytes.writeInt8(pLRC.first)
        bytes.writeInt8(pLRC.second)
        bytes.writeBytes(END, END.size)
        return when (endian) {
            ModbusEndian.ARRAY_BIG_BYTE_BIG -> output
            ModbusEndian.ARRAY_LITTLE_BYTE_BIG -> output.reversedArray()
            ModbusEndian.ARRAY_LITTLE_BYTE_LITTLE -> {
                val bo = BytesOutput()
                output.reversedArray().forEach { bo.writeInt8(toReverseInt8(it.toInt())) }
                bo.toByteArray()
            }
            ModbusEndian.ARRAY_BIG_BYTE_LITTLE -> {
                val bo = BytesOutput()
                output.forEach { bo.writeInt8(toReverseInt8(it.toInt())) }
                bo.toByteArray()
            }
        }
    }
}
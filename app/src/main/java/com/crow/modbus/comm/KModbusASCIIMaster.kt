@file:Suppress("PrivatePropertyName")

package com.crow.modbus.comm

import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.ext.BytesOutput
import com.crow.modbus.ext.fromAsciiInt8
import com.crow.modbus.ext.toAsciiHexBytes
import com.crow.modbus.ext.toReverseInt8

/*************************
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/10/10 13:46
 * @Author: CrowForKotlin
 * @formatter:on
 **************************/
class KModbusASCIIMaster private constructor() : KModbus() {

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

    fun build(function: ModbusFunction, slave: Int, startAddress: Int, count: Int, value: Int? = null, values: IntArray? = null, endian: ModbusEndian = ModbusEndian.ARRAY_BIG_BYTE_BIG): ByteArray {
        val bytes = BytesOutput()
        val output = buildOutput(slave, function, startAddress, count, value, values).toByteArray()
        val pLRC = fromAsciiInt8(toCalculateLRC(output))
        val outputAscii = toAsciiHexBytes(output)
        bytes.writeInt8(HEAD)
        bytes.writeBytes(outputAscii, outputAscii.size)
        bytes.writeInt8(pLRC.first)
        bytes.writeInt8(pLRC.second)
        bytes.writeBytes(END, END.size)
        return when (endian) {
            ModbusEndian.ARRAY_BIG_BYTE_BIG -> output
            ModbusEndian.ARRAY__LITTLE_BYTE_BIG -> output.reversedArray()
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
@file:Suppress("PrivatePropertyName")

package com.crow.modbus.serialport

import com.crow.base.ext.fromAsciiInt16
import com.crow.base.ext.fromAsciiInt8
import com.crow.base.ext.toAsciiHexBytes
import com.crow.base.ext.toAsciiInt


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

    fun build( function: ModbusFunction, slave: Int, startAddress: Int, count: Int, value: Int? = null, values: IntArray? = null): ByteArray {
        val bytes = BytesOutput()
        val output = buildOutput(slave, function, startAddress, count, value, values).toByteArray()
        val pLRC = fromAsciiInt8(toCalculateLRC(output))
        val outputAscii = toAsciiHexBytes(output)
        bytes.writeInt8(HEAD)
        bytes.writeBytes(outputAscii, outputAscii.size)
        bytes.writeInt8(pLRC.first)
        bytes.writeInt8(pLRC.second)
        bytes.writeBytes(END, END.size)
        return bytes.toByteArray()
    }
}
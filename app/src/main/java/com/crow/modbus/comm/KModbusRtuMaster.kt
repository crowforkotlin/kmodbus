package com.listen.x3player.kt.modbus.comm

import com.crow.modbus.comm.KModbus
import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.comm.model.ModbusRtuRespPacket
import com.crow.modbus.ext.BytesOutput
import com.crow.modbus.ext.baseTenF
import com.crow.modbus.ext.logger
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
            ModbusEndian.ARRAY_LITTLE_BYTE_BIG -> output.reversedArray()
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

    fun  resolve(bytes: ByteArray, endian: ModbusEndian): ModbusRtuRespPacket? {
        return runCatching {
            val inputs = when (endian) {
                ModbusEndian.ARRAY_BIG_BYTE_BIG -> bytes
                ModbusEndian.ARRAY_LITTLE_BYTE_BIG -> bytes.reversedArray()
                ModbusEndian.ARRAY_LITTLE_BYTE_LITTLE -> {
                    val bo = BytesOutput()
                    bytes.reversedArray().forEach { bo.writeInt8(toReverseInt8(it.toInt())) }
                    bo.toByteArray()
                }
                ModbusEndian.ARRAY_BIG_BYTE_LITTLE -> {
                    val bo = BytesOutput()
                    bytes.forEach { bo.writeInt8(toReverseInt8(it.toInt())) }
                    bo.toByteArray()
                }
            }
            val functionCode= inputs[1].toInt() and 0xFF
            val isFunctionCodeError = (functionCode and baseTenF) + 0x80 == functionCode
            val startIndexOfData = 3
//            "${inputs[0].toInt()} \t ${inputs.map { String.format("%02x", it) }}".log()
            if(isFunctionCodeError) {
                val dataSize = inputs.size - 4
                val values = ArrayList<Int>(dataSize)
                repeat(dataSize) { values.add(inputs[startIndexOfData + it].toInt()) }
                ModbusRtuRespPacket(
                    mDeviceID = inputs[0].toInt(),
                    mFunctionCode = functionCode,
                    mBytesCount = dataSize,
                    mValues =  values
                )
            } else {
                val byteCount = inputs[2].toInt()
                val values = ArrayList<Int>(byteCount)
                repeat(byteCount) { values.add(inputs[startIndexOfData + it].toInt()) }
                ModbusRtuRespPacket(
                    mDeviceID = inputs[0].toInt(),
                    mFunctionCode = inputs[1].toInt(),
                    mBytesCount = byteCount,
                    mValues =  values
                )
            }
        }
            .onFailure { logger(it.stackTraceToString()) }
            .getOrElse { null }
    }
}


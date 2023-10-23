package com.crow.modbus.comm

import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.comm.model.ModbusRtuRespPacket
import com.crow.modbus.ext.Bytes
import com.crow.modbus.ext.BytesOutput
import com.crow.modbus.ext.baseTenF
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

    fun resolve(bytes: Bytes, endian: ModbusEndian): ModbusRtuRespPacket {
        val inputs = when (endian) {
            ModbusEndian.ARRAY_BIG_BYTE_BIG -> bytes
            ModbusEndian.ARRAY__LITTLE_BYTE_BIG -> bytes.reversedArray()
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
        return if(isFunctionCodeError) {
            val dataSize = inputs.size - 4
            val values = ArrayList<Int>(dataSize)
            repeat(dataSize) { values.add(inputs[startIndexOfData + it].toInt()) }
            ModbusRtuRespPacket(
                mSlave = inputs[0].toInt(),
                mFunctionCode = functionCode,
                mBytesCount = dataSize,
                mValues =  values
            )
        } else {
            val byteCount = inputs[2].toInt()
            val bytes = ArrayList<Int>(byteCount)
            repeat(byteCount - 1) { bytes.add(inputs[startIndexOfData + it].toInt()) }
            ModbusRtuRespPacket(
                mSlave = inputs[0].toInt(),
                mFunctionCode = inputs[1].toInt(),
                mBytesCount = byteCount,
                mValues =  bytes
            )
        }
    }
}
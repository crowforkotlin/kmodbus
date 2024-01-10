package com.crow.modbus

import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.model.ModbusFunction
import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.baseTenF
import com.crow.modbus.tools.error
import com.crow.modbus.tools.toReverseInt8
import com.crow.modbus.model.ModbusRtuRespPacket

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
        val output = toCalculateCRC16(buildMasterRequestOutput(slave, function, startAddress, count, value, values)).toByteArray()
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

    fun resolve(bytes: ByteArray, endian: ModbusEndian): ModbusRtuRespPacket? {
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
            if(isFunctionCodeError) {
                val dataSize = inputs.size - 4
                val newBytes = ByteArray(dataSize)
                System.arraycopy(inputs, 3, newBytes, 0, dataSize)
                ModbusRtuRespPacket(
                    mDeviceID = inputs[0].toInt(),
                    mFunctionCode = functionCode,
                    mBytesCount = dataSize,
                    mValues =  newBytes
                )
            } else {
                val byteCount = inputs[2].toInt()
                val newBytes = ByteArray(byteCount)
                System.arraycopy(inputs, 3, newBytes, 0, byteCount)
                ModbusRtuRespPacket(
                    mDeviceID = inputs[0].toInt(),
                    mFunctionCode = inputs[1].toInt(),
                    mBytesCount = byteCount,
                    mValues =  newBytes
                )
            }
        }
            .onFailure { it.stackTraceToString().error() }
            .getOrElse { null }
    }
}


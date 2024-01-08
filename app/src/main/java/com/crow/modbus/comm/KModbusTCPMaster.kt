@file:Suppress("SpellCheckingInspection")

package com.listen.x3player.kt.modbus.comm

import com.crow.modbus.comm.KModbus
import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.ext.BytesOutput
import com.crow.modbus.ext.baseTenF
import com.crow.modbus.ext.log
import com.crow.modbus.ext.logger
import com.crow.modbus.ext.toReverseInt8
import com.listen.x3player.kt.modbus.comm.model.ModbusTcpRespPacket

/*************************
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/28 18:17
 * @Author: crowforkotlin
 * @Description: ModbusTCPMaster
 * @formatter:on
 **************************/
class KModbusTCPMaster private constructor() : KModbus() {


    companion object {

        var mTransaction = 0
            private set

        // transaction: Int = mTransaction, protocolIdentifier: Int = 0,


        private var instance: KModbusTCPMaster? = null

        fun getInstance(): KModbusTCPMaster {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = KModbusTCPMaster()
                    }
                }
            }
            return instance!!
        }
    }


    private var mTransactionId: Int = 0
    private val mProtocol: Int = 0

    /**
     * ● IP
     *
     * ● 2023-10-16 16:27:17 周一 下午
     * @author crowforkotlin
     */
    fun build(
        function: ModbusFunction,
        slave: Int,
        startAddress: Int,
        count: Int,
        value: Int? = null,
        values: IntArray? = null,
        transactionId: Int = mTransactionId,
        endian: ModbusEndian = ModbusEndian.ARRAY_BIG_BYTE_BIG,
    ): ByteArray {
        val pdu = buildOutput(slave, function, startAddress, count, value, values, isTcp = true)
        val size = pdu.size()
        val mbap = BytesOutput()
        mbap.writeInt16(transactionId)
        mbap.writeInt16(mProtocol)
        mbap.writeInt16(size + 1)
        mbap.writeInt8(slave)
        mbap.write(pdu.toByteArray())
        mTransactionId++
        return when (endian) {
            ModbusEndian.ARRAY_BIG_BYTE_BIG -> mbap.toByteArray()
            ModbusEndian.ARRAY_LITTLE_BYTE_BIG -> mbap.toByteArray().reversedArray()
            ModbusEndian.ARRAY_LITTLE_BYTE_LITTLE -> {
                val bytes = BytesOutput()
                mbap.toByteArray().reversedArray()
                    .forEach { bytes.writeInt8(toReverseInt8(it.toInt())) }
                bytes.toByteArray()
            }

            ModbusEndian.ARRAY_BIG_BYTE_LITTLE -> {
                val bytes = BytesOutput()
                mbap.toByteArray().forEach { bytes.writeInt8(toReverseInt8(it.toInt())) }
                bytes.toByteArray()
            }
        }
    }

    fun resolve(bytes: ByteArray, endian: ModbusEndian): ModbusTcpRespPacket? {
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
            val functionCode= inputs[7].toInt() and 0xFF
            val isFunctionCodeError = (functionCode and baseTenF) + 0x80 == functionCode
            val startIndexOfData = 9
//            "${inputs[0].toInt()} \t ${inputs.map { String.format("%02x", it) }}".log()
            if(isFunctionCodeError) {
                val dataSize = inputs.size - 11
                val newBytes = ByteArray(dataSize)
                System.arraycopy(inputs, startIndexOfData, newBytes, 0, dataSize)
                ModbusTcpRespPacket(
                    mDeviceID = inputs[6].toInt(),
                    mFunctionCode = functionCode,
                    mBytesCount = dataSize,
                    mValues =  newBytes
                )
            } else {
                val byteCount = inputs[8].toInt()
                val newBytes = ByteArray(byteCount)
                System.arraycopy(inputs, startIndexOfData, newBytes, 0, byteCount)
                newBytes.map { String.format("%02x", it) }.log()
                ModbusTcpRespPacket(
                    mDeviceID = inputs[6].toInt(),
                    mFunctionCode = functionCode,
                    mBytesCount = byteCount,
                    mValues =  newBytes
                )
            }
        }
            .onFailure { logger(it.stackTraceToString()) }
            .getOrElse { null }
    }
}
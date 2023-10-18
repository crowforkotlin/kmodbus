@file:Suppress("SpellCheckingInspection")

package com.crow.modbus.comm

import com.crow.modbus.comm.model.ModbusEndian
import com.crow.modbus.comm.model.ModbusFunction
import com.crow.modbus.ext.BytesOutput
import com.crow.modbus.ext.toReverseInt8

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
            ModbusEndian.ARRAY__LITTLE_BYTE_BIG -> mbap.toByteArray().reversedArray()
            ModbusEndian.ARRAY_LITTLE_BYTE_LITTLE -> {
                val bo = BytesOutput()
                mbap.toByteArray().reversedArray().forEach { bo.writeInt8(toReverseInt8(it.toInt())) }
                bo.toByteArray()
            }

            ModbusEndian.ARRAY_BIG_BYTE_LITTLE -> {
                val bo = BytesOutput()
                mbap.toByteArray().forEach { bo.writeInt8(toReverseInt8(it.toInt())) }
                bo.toByteArray()
            }
        }
    }
}
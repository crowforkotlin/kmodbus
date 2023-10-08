@file:Suppress("SpellCheckingInspection")

package com.crow.modbus.serialport

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

        fun getInstance() : KModbusTCPMaster {
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

    @OptIn(ExperimentalStdlibApi::class)
    fun build(function: ModbusFunction, slave: Int, startAddress: Int, count: Int,value: Int? = null, values: IntArray? = null, transactionId: Int = mTransactionId): ByteArray {
        val pdu = buildOutput(slave, function, startAddress, count, value, values, isTcp = true)
        val size = pdu.size()
        val mbap = BytesOutput()
        mbap.writeInt16(transactionId)
        mbap.writeInt16(mProtocol)
        mbap.writeInt16(size + 1)
        mbap.writeInt8(slave)
        mbap.write(pdu.toByteArray())
        mTransactionId++
        return mbap.toByteArray()
    }
}
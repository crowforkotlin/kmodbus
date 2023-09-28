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

        fun build(function: ModbusFunction, startAddress: Int, value: Int, values: IntArray) {

        }
    }
}
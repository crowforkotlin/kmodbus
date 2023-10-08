package com.crow.modbus.serialport

import com.crow.modbus.logger

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/23 14:00
 * @Author: CrowForKotlin
 * @Description: ModbusRtuMaster
 * @formatter:on
 **************************/
class KModbusRtuMaster private constructor() : KModbus(){

    companion object {

        private var instance: KModbusRtuMaster? = null

        fun getInstance() : KModbusRtuMaster {
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

    @OptIn(ExperimentalStdlibApi::class)
    fun build( function: ModbusFunction, slave: Int, startAddress: Int, count: Int, value: Int? = null, values: IntArray? = null): ByteArray {
        val output = buildOutput(slave, function, startAddress, count, value, values)
        toCalculateCRC16(output.toByteArray(), output)
        return output.toByteArray()
    }
}
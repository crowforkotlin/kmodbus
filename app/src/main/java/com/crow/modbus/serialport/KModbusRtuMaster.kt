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

    /**
     * ● 构造数据
     *
     * ● 2023-10-16 16:16:40 周一 下午
     * @author crowforkotlin
     */
    fun build( function: ModbusFunction, slave: Int, startAddress: Int, count: Int, value: Int? = null, values: IntArray? = null): ByteArray {
        return toCalculateCRC16(buildOutput(slave, function, startAddress, count, value, values)).toByteArray()
    }
}
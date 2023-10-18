package com.crow.modbus.comm.model

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/23 14:15
 * @Author: CrowForKotlin
 * @Description: ModbusException
 * @formatter:on
 **************************/
class ModbusException(errorType: ModbusErrorType, message: String) : Exception("${errorType.name} : $message")


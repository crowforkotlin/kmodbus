package com.crow.modbus.serialport

/**
 * 常见的Modbus通讯错误
 */
enum class ModbusErrorType {
    ModbusError,
    ModbusFunctionNotSupportedError,
    ModbusDuplicatedKeyError,
    ModbusMissingKeyError,
    ModbusInvalidBlockError,
    ModbusInvalidArgumentError,
    ModbusOverlapBlockError,
    ModbusOutOfBlockError,
    ModbusInvalidResponseError,
    ModbusInvalidRequestError,
    ModbusTimeoutError
}

package com.crow.modbus.model

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/23 14:12
 * @Author: CrowForKotlin
 * @Description:
 * @formatter:on
 **************************/


/**
 * 功能码（十进制显示）
 */
enum class ModbusEndian {
    // AB CD
    ARRAY_BIG_BYTE_BIG,

    // BA DC
    ARRAY_BIG_BYTE_LITTLE,

    // CD AB
    ARRAY_LITTLE_BYTE_BIG,

    // DC BA
    ARRAY_LITTLE_BYTE_LITTLE,;

}



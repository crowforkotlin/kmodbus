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
enum class KModbusFunction(val mCode: Int) {

    //读线圈寄存器
    READ_COILS(1),

    //读离散输入寄存器
    READ_DISCRETE_INPUTS(2),

    //读保持寄存器
    READ_HOLDING_REGISTERS(3),

    //读输入寄存器
    READ_INPUT_REGISTERS(4),

    //写单个线圈寄存器
    WRITE_SINGLE_COIL(5),

    //写单个保持寄存器
    WRITE_SINGLE_REGISTER(6),

    //写入多个线圈寄存器
    WRITE_MULTIPLE_COILS(15),

    //写入多个保持寄存器
    WRITE_MULTIPLE_REGISTERS(16)
}

fun getKModbusFunction(mCode: Int): KModbusFunction {
    return when(mCode) {
        KModbusFunction.READ_COILS.mCode -> KModbusFunction.READ_COILS
        KModbusFunction.READ_DISCRETE_INPUTS.mCode -> KModbusFunction.READ_DISCRETE_INPUTS
        KModbusFunction.READ_HOLDING_REGISTERS.mCode -> KModbusFunction.READ_HOLDING_REGISTERS
        KModbusFunction.READ_INPUT_REGISTERS.mCode -> KModbusFunction.READ_INPUT_REGISTERS
        KModbusFunction.WRITE_SINGLE_COIL.mCode -> KModbusFunction.WRITE_SINGLE_COIL
        KModbusFunction.WRITE_SINGLE_REGISTER.mCode -> KModbusFunction.WRITE_SINGLE_REGISTER
        KModbusFunction.WRITE_MULTIPLE_COILS.mCode -> KModbusFunction.WRITE_MULTIPLE_COILS
        KModbusFunction.WRITE_MULTIPLE_REGISTERS.mCode -> KModbusFunction.WRITE_MULTIPLE_REGISTERS
        else -> error("invalid function type $mCode")
    }
}
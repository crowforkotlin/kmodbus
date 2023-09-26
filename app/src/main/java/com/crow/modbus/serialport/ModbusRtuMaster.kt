package com.crow.modbus.serialport

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/23 14:00
 * @Author: CrowForKotlin
 * @Description: ModbusRtuMaster
 * @formatter:on
 **************************/
class ModbusRtuMaster private constructor() {

    companion object {
        fun build(slave: Int, function: Int, startAddress: Int, count: Int, value: Int, values: IntArray): ByteArray {

            //检查参数是否符合协议规定
            when {
                slave !in 0..0xFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid slave $slave")
                startAddress !in 0..0xFFFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid starting_address $startAddress")
                count !in 1..0xFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid quantity_of_x $count")
            }

            val output = BytesOutput()

            output.writeInt8(slave)

            when(function) {

                // 读线圈寄存器 、离散输入寄存器、保持寄存器、输入寄存器
                ModbusFunction.READ_COILS, ModbusFunction.READ_DISCRETE_INPUTS, ModbusFunction.READ_INPUT_REGISTERS, ModbusFunction.READ_HOLDING_REGISTERS -> {
                    output.writeInt8(function)
                    output.writeInt16(startAddress)
                    output.writeInt16(count)
                }
                ModbusFunction.WRITE_SINGLE_COIL, ModbusFunction.WRITE_SINGLE_REGISTER -> {

                    var valueCopy = value

                    //写单个寄存器指令
                    if (function == ModbusFunction.WRITE_SINGLE_COIL) if (value != 0) valueCopy = 0xff00 //如果为线圈寄存器（写1时为 FF 00,写0时为00 00）

                    output.writeInt8(function)
                    output.writeInt16(startAddress)
                    output.writeInt16(valueCopy)
                }
                ModbusFunction.WRITE_HOLDING_REGISTERS -> {

                    //写多个保持寄存器
                    output.writeInt8(function)
                    output.writeInt16(startAddress)
                    output.writeInt16(count)
                    output.writeInt8(2 * count)

                    //写入数据
                    values.forEach { output.writeInt16(it) }
                }
                ModbusFunction.WRITE_COILS -> {

                }
                else -> throw ModbusException(ModbusErrorType.ModbusError, "unknown function code!")
            }
            return  output.toByteArray()
        }
    }
}
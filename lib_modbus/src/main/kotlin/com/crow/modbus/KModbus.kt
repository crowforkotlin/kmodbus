@file:Suppress("SpellCheckingInspection")

package com.crow.modbus

import com.crow.modbus.model.ModbusErrorType
import com.crow.modbus.model.ModbusException
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusFunction.READ_COILS
import com.crow.modbus.model.KModbusFunction.READ_DISCRETE_INPUTS
import com.crow.modbus.model.KModbusFunction.READ_HOLDING_REGISTERS
import com.crow.modbus.model.KModbusFunction.READ_INPUT_REGISTERS
import com.crow.modbus.model.KModbusFunction.WRITE_COILS
import com.crow.modbus.model.KModbusFunction.WRITE_HOLDING_REGISTERS
import com.crow.modbus.model.KModbusFunction.WRITE_SINGLE_COIL
import com.crow.modbus.model.KModbusFunction.WRITE_SINGLE_REGISTER
import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.CRC16

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/28 18:38
 * @Author: CrowForKotlin
 * @Description: Modbus
 * @formatter:off
 **************************/
open class KModbus protected constructor() {

    /**
     * ● 构造输出的数据
     *
     * ● 2023-10-16 16:42:18 周一 下午
     * @author crowforkotlin
     */
    fun buildMasterRequestOutput(slave: Int, function: KModbusFunction, startAddress: Int, count: Int = 1, value: Int?, values: IntArray?, isTcp: Boolean = false): BytesOutput {

        //检查参数是否符合协议规定
        when {
            slave !in 0..0xFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid slave $slave")
            startAddress !in 0..0xFFFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid startAddress $startAddress")
        }

        val output = BytesOutput()

        if (!isTcp) {
            output.writeInt8(slave)
        }

        when(function) {

            // 读线圈寄存器 、离散输入寄存器、保持寄存器、输入寄存器
            READ_COILS, READ_DISCRETE_INPUTS, READ_INPUT_REGISTERS, READ_HOLDING_REGISTERS -> {
                if(count !in 1..0xFF) throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid count $count")
                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(count)
            }
            WRITE_SINGLE_COIL, WRITE_SINGLE_REGISTER -> {

                var valueCopy = value ?: throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Function Is $function\t , Data must be passed in!")

                //写单个寄存器指令
                if (function == WRITE_SINGLE_COIL) if (value != 0) valueCopy = 0xff00 //如果为线圈寄存器（写1时为 FF 00,写0时为00 00）

                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(valueCopy)
            }
            WRITE_HOLDING_REGISTERS -> {
                if(count !in 1..0xFF) throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid count $count")
                //写多个保持寄存器
                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(count)
                output.writeInt8(2 * count)

                //写入数据
                (values ?: throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Function Is $function, Data must be passed in!")).forEach { output.writeInt16(it) }
            }
            WRITE_COILS -> {
                if(count !in 1..0xFF) throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid count $count")
                if (values == null || values.isEmpty()) throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Function Is $function, Data must be passed in and cannot be empty!")
                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(count)
                output.writeInt8((count + 7) shr 3)
                val chunkedValues = values.toList().chunked(8)
                for (chunk in chunkedValues) {
                    output.writeInt8(toDecimal(chunk.reversed().toIntArray()))
                }
            }
        }

        return output
    }

    fun buildMasterResponseOutput() {
        // TODO
    }

    /**
     * ● CRC校验
     *
     * ● 2023-10-16 16:06:48 周一 下午
     * @author crowforkotlin
     */
    fun toCalculateCRC16(output: BytesOutput): BytesOutput {

        //计算CRC校验码
        output.writeInt16Reversal(CRC16.compute(output.toByteArray()).also { println("CRC $it") })
        return output
    }

    /**
     * ● LRC校验
     *
     * ● 2023-10-16 16:06:41 周一 下午
     * @author crowforkotlin
     */
    fun toCalculateLRC(data: ByteArray): Int {
        var iTmp = 0
        for (x in data) {
            iTmp += x.toInt()
        }
        iTmp %= 256
        iTmp = (iTmp.inv() + 1) and 0xFF // 对补码取模，确保结果在0-255范围内
        return iTmp
    }


    /**
     * ● Convert each digit component to decimal
     *
     * ● 2023-10-16 16:00:53 周一 下午
     * @author crowforkotlin
     */
    private fun toDecimal(data: IntArray): Int {
        var result = 0
        for (bit in data) {
            if (bit != 0 && bit != 1) {
                return -1  // 数据数组中包含非二进制值，返回错误
            }
            result = (result shl 1) + bit
        }
        return result
    }

    private fun toBinary(decimal: Int, size: Int = 32): IntArray {
        if (decimal < 0) {
            throw IllegalArgumentException("输入的十进制数必须是非负整数！")
        }

        val binaryArray = IntArray(size) // 假设你想要32位的二进制表示

        var number = decimal
        var index = size - 1 // 从最高位开始
        while (number > 0 && index >= 0) {
            binaryArray[index] = number and 1 // 获取最低位的二进制位
            number = number ushr 1 // 右移一位
            index--
        }

        return binaryArray
    }


}
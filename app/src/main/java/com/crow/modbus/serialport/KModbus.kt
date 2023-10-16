@file:Suppress("SpellCheckingInspection")

package com.crow.modbus.serialport

import com.crow.base.ext.Bytes
import com.crow.base.ext.fromAsciiInt16
import com.crow.base.ext.fromAsciiInt8
import kotlin.experimental.inv
import kotlin.system.measureTimeMillis

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/28 18:38
 * @Author: CrowForKotlin
 * @Description: Modbus
 * @formatter:on
 **************************/
open class KModbus protected constructor() {

    fun buildOutput(slave: Int, function: ModbusFunction, startAddress: Int, count: Int, value: Int?, values: IntArray?, isTcp: Boolean = false): BytesOutput {

        //检查参数是否符合协议规定
        when {
            slave !in 0..0xFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid slave $slave")
            startAddress !in 0..0xFFFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid startAddress $startAddress")
            count !in 1..0xFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid count $count")
        }

        val output = BytesOutput()


        if (!isTcp) {
            output.writeInt8(slave)
        }

        when(function) {

            // 读线圈寄存器 、离散输入寄存器、保持寄存器、输入寄存器
            ModbusFunction.READ_COILS, ModbusFunction.READ_DISCRETE_INPUTS, ModbusFunction.READ_INPUT_REGISTERS, ModbusFunction.READ_HOLDING_REGISTERS -> {
                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(count)
            }
            ModbusFunction.WRITE_SINGLE_COIL, ModbusFunction.WRITE_SINGLE_REGISTER -> {

                var valueCopy = value ?: throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Function Is $function\t , Data must be passed in!")

                //写单个寄存器指令
                if (function == ModbusFunction.WRITE_SINGLE_COIL) if (value != 0) valueCopy = 0xff00 //如果为线圈寄存器（写1时为 FF 00,写0时为00 00）

                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(valueCopy)
            }
            ModbusFunction.WRITE_HOLDING_REGISTERS -> {

                //写多个保持寄存器
                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(count)
                output.writeInt8(2 * count)

                //写入数据
                (values ?: throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Function Is $function, Data must be passed in!")).forEach { output.writeInt16(it) }
            }
            ModbusFunction.WRITE_COILS -> {
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

    /**
     * ● CRC校验
     *
     * ● 2023-10-16 16:06:48 周一 下午
     * @author crowforkotlin
     */
    fun toCalculateCRC16(output: BytesOutput): BytesOutput {

        //计算CRC校验码
        output.writeInt16Reversal(CRC16.compute(output.toByteArray()))
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

}
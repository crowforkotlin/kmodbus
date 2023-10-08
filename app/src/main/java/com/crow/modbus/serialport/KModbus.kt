package com.crow.modbus.serialport

import com.crow.modbus.ext.Bytes

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus.serialport
 * @Time: 2023/9/28 18:38
 * @Author: CrowForKotlin
 * @Description: Modbus
 * @formatter:on
 **************************/
open class KModbus protected constructor() {

    @OptIn(ExperimentalStdlibApi::class)
    fun buildOutput(slave: Int, function: ModbusFunction, startAddress: Int, count: Int, value: Int?, values: IntArray?, isTcp: Boolean = false): BytesOutput {

        //检查参数是否符合协议规定
        when {
            slave !in 0..0xFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid slave $slave")
            startAddress !in 0..0xFFFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid startAddress $startAddress")
            count !in 1..0xFF -> throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Invalid count $count")
        }

        val output = BytesOutput()

        if (!isTcp) output.writeInt8(slave)

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
                (values ?: throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Function Is $function\t , Data must be passed in!")).forEach { output.writeInt16(it) }
            }
            ModbusFunction.WRITE_COILS -> {

                if (values == null) throw ModbusException(ModbusErrorType.ModbusInvalidArgumentError, "Function Is $function\t , Data must be passed in!")

                //写多个线圈寄存器
                output.writeInt8(function.mCode)
                output.writeInt16(startAddress)
                output.writeInt16(count)
                //计算写入字节数
                var writeByteCount: Int = count / 8  // 8个线圈 代表 一个字节

                // 如果剩余线圈数量不是8的倍数，则需要再加一个字节
                if (count % 8 != 0) {
                    writeByteCount += 1
                }

                output.writeInt8(writeByteCount)

                var index = 0
                //如果写入数据数量 > 8 ，则需要拆分开来
                var start = 0 //数组开始位置

                var end = 7 //数组结束位置

                var splitData = IntArray(8)
                //循环写入拆分数组，直到剩下最后一组 元素个数 <= 8 的数据
                while (writeByteCount > 1) {
                    writeByteCount--
                    var sIndex = 0
                    index = start
                    while (index <= end) {
                        splitData[sIndex++] = values[index]
                        index++
                    }
                    //数据反转 对于是否要反转要看你传过来的数据，如果高低位顺序正确则不用反转
                    splitData = splitData.reversedArray()
                    //写入拆分数组
                    output.writeInt8(toDecimal(splitData))
                    start = index
                    end += 8
                }
                //写入最后剩下的数据
                val last: Int = count - index
                var tData: IntArray? = IntArray(last)
                System.arraycopy(values, index, tData, 0, last)
                //数据反转 对于是否要反转要看你传过来的数据，如果高低位顺序正确则不用反转
                tData = tData!!.reversedArray()
                output.writeInt8(toDecimal(tData))
            }
            else -> throw ModbusException(ModbusErrorType.ModbusError, "unknown function code!")
        }

        return output
    }

    fun toCalculateCRC16(bytes: Bytes, output: BytesOutput) {

        //计算CRC校验码
        val crc: Int = CRC16.compute(bytes)
        output.writeInt16Reversal(crc)
    }


    //将int[1,0,0,1,1,0]数组转换为十进制数据
    private fun toDecimal(data: IntArray?): Int {
        var result = 0
        if (data != null) {
            val sData = StringBuilder()
            for (d in data) {
                sData.append(d)
            }
            result = try {
                sData.toString().toInt(2)
            } catch (e: NumberFormatException) {
                -1
            }
        }
        return result
    }

}
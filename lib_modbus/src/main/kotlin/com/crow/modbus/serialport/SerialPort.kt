@file:Suppress("SpellCheckingInspection")

package com.crow.modbus.serialport

import java.io.FileDescriptor

/*************************
 * @Package: com.crow.modbus
 * @Time: 2023/9/23 10:32
 * @Author: CrowForKotlin
 * @Description: SerialPort
 * @formatter:on
 **************************/
open class SerialPort protected constructor() {

    var mFileDescriptor: FileDescriptor? = null

    companion object {
        init {
            // 加载共享库
            System.loadLibrary("SerialPort")
        }
    }

    /**
     * ⦁  打开串口
     * @param path 串口文件路径
     * @param baudrate 波特率
     * @param parity 校验
     * @param stopbit 停止位 1 或 2
     * @param databit 数据位 5 - 8
     * ⦁  2023-09-25 18:29:58 周一 下午
     */
    protected fun open(path: String, baudrate: Int, parity: SerialPortParityFunction, stopbit: Int, databit: Int): FileDescriptor {
        if (stopbit !in 1..2) {
            throw IllegalStateException("stopbit must in 1..2!")
        }
        if (databit !in 5..8) {
            throw IllegalStateException("databit must in 5..8!")
        }
        return open(path, baudrate, 0, parity.code, stopbit, databit)
    }

    // 定义JNI方法，用于打开串口
    private external fun open(path: String, baudrate: Int, flags: Int, parity: Int, stopbits: Int, databits: Int): FileDescriptor

    // 定义JNI方法，用于关闭串口
    protected external fun close()
}
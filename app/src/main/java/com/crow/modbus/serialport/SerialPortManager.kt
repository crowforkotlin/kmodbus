@file:Suppress("unused", "SpellCheckingInspection")

package com.crow.modbus.serialport

import com.crow.modbus.comm.interfaces.IDataReceive
import com.crow.modbus.comm.interfaces.IOpenSerialPortFailure
import com.crow.modbus.comm.interfaces.IOpenSerialPortSuccess
import com.crow.modbus.ext.Bytes
import com.crow.modbus.ext.logger
import com.crow.modbus.ext.loggerError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus
 * @Time: 2023/9/23 11:31
 * @Author: CrowForKotlin
 * @Description: SerialPortManager
 * @formatter:on
 **************************/
class SerialPortManager : SerialPort() {

    companion object {
        private val mutex = Mutex()
        private val parentJob = SupervisorJob()
        private val io = CoroutineScope(Dispatchers.IO + parentJob + CoroutineExceptionHandler { coroutineContext, throwable ->
            loggerError("SerialPort an Exception occurs! context is $coroutineContext \t exception : ${throwable.stackTraceToString()}")
        })
    }

    private var mSuccessListener: IOpenSerialPortSuccess? = null
    private var mFailureListener: IOpenSerialPortFailure? = null

    private var mFileInputStream: FileInputStream? = null
    private var mFileOutputStream: FileOutputStream? = null

    private val mReadedBuffer = Bytes(1024)

    /**
     * ● 修改文件权限为可读、可写、可执行
     *
     * ● 2023-09-23 11:41:26 周六 上午
     */
    private fun changeFilePermissions(file: File): Boolean {
        return (file.takeIf { it.exists() } ?: false).runCatching {

            // 获取ROOT权限
            val su = Runtime.getRuntime().exec("/system/bin/su")

            // 修改文件属性为 [可读 可写 可执行]
            val cmd = "chmod 777 ${file.absolutePath}\nexit\n"

            // 将命令写入 su 进程的输出流
            su.outputStream.write(cmd.toByteArray())

            // 如果 su 进程返回值为 0 并且文件可读、可写、可执行，则返回 true
            (su.waitFor() == 0 && file.canRead() && file.canWrite() && file.canExecute())
        }
            .onFailure { catch ->
                when (catch) {
                    is IOException -> logger("No root permission!")
                    else -> logger(catch.stackTraceToString())
                }
            }
            .getOrElse { false }
    }

    /**
     * ● 打开串口
     *
     * ● 2023-09-23 16:02:30 周六 下午
     */
    fun openSerialPort(path: String, baudRate: Int) {

        val device = File(path)

        // 校验串口权限
        if (!device.canRead() || !device.canWrite()) {
            if (!changeFilePermissions(device)) {
                loggerError("openSerialPort : 没有读写权限!")
                mFailureListener?.onFailure(device, SerialPortState.NO_READ_WRITE_PERMISSION)
                return
            }
        }

        mFileDescriptor = open(device.absolutePath, baudRate, SerialPortParityFunction.NONE, 1, 8)
        mFileInputStream = FileInputStream(mFileDescriptor)
        mFileOutputStream = FileOutputStream(mFileDescriptor)
        mSuccessListener?.onSuccess(device)
        logger("openSerialPort : 串口已经打开 $mFileDescriptor")
    }

    /**
     * ● 关闭串口
     *
     * ● 2023-09-23 16:02:12 周六 下午
     */
    fun closeSerialPort(): Boolean {
        return runCatching {
            parentJob.children.forEach { job -> job.cancel() }
            mFileDescriptor = null
            mFileInputStream?.close()
            mFileOutputStream?.close()
            mFileInputStream = null
            mFileOutputStream = null
            mSuccessListener = null
            mFailureListener = null
            true
        }
            .onFailure { catch -> loggerError("close serial port exception! ${catch.stackTraceToString()}") }
            .getOrElse { false }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun writeBytes(bytes: ByteArray) {
        io.launch {
            mutex.withLock {
                if (null != mFileDescriptor && null != mFileInputStream && null != mFileOutputStream) {
                    logger("writeBytes ${bytes.map { it.toHexString() }}")
                    mFileOutputStream!!.write(bytes)
                }
            }
        }
    }

    fun readBytes(iDataReceive: IDataReceive) {
        io.launch {
            while (true) {
                if (null != mFileDescriptor && null != mFileInputStream && null != mFileOutputStream) {
                    val length = mFileInputStream!!.read(mReadedBuffer)
                    logger("$length")
                    if (length <= 0) return@launch
                    val buffer = Bytes(length)
                    System.arraycopy(mReadedBuffer, 0, buffer, 0, length)
                    iDataReceive.onReceive(buffer)
                }
            }
        }
    }
}
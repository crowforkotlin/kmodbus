@file:Suppress("unused", "SpellCheckingInspection")

package com.crow.modbus.serialport

import android.util.Log
import com.crow.modbus.comm.interfaces.IDataReceive
import com.crow.modbus.comm.interfaces.IOpenSerialPortFailure
import com.crow.modbus.comm.interfaces.IOpenSerialPortSuccess
import com.crow.modbus.ext.Bytes
import com.crow.modbus.ext.logger
import com.crow.modbus.ext.loggerError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.modbus
 * @Time: 2023/9/23 11:31
 * @Author: CrowForKotlin
 * @Description: SerialPortManager
 * @formatter:on
 **************************/
class SerialPortManager : SerialPort() {

    private object CoroutineExceptionHandlerKey : CoroutineContext.Key<CoroutineExceptionHandler>
    private var mSuccessListener: IOpenSerialPortSuccess? = null
    private var mFailureListener: IOpenSerialPortFailure? = null
    private var mFileInputStream: FileInputStream? = null
    private var mFileOutputStream: FileOutputStream? = null
    private val mParentReadJob: Job = SupervisorJob()
    private var mParentWriteJob: Job = SupervisorJob()
    private val mParentIOJob = SupervisorJob()
    private val mReadedContext = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mParentReadJob + CoroutineExceptionHandler { _, cause -> cause.stackTraceToString().logger(level = Log.ERROR) })
    private val mWritedContext = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mParentWriteJob + CoroutineExceptionHandler { _, cause -> cause.stackTraceToString().logger(level = Log.ERROR) })
    private val mIO = CoroutineScope(Dispatchers.IO + mParentIOJob + object : CoroutineExceptionHandler {
        override val key: CoroutineContext.Key<*> get() = CoroutineExceptionHandlerKey
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            loggerError("SerialPort an Exception occurs! context is $context \t exception : ${exception.stackTraceToString()}")
        }
    })

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
        "◉ 正在关闭串口".logger()
        return runCatching {
            mParentIOJob.cancelChildren()
            mParentReadJob.cancelChildren()
            mParentWriteJob.cancelChildren()
            mFileDescriptor = null
            mFileInputStream?.close()
            mFileOutputStream?.close()
            mFileInputStream = null
            mFileOutputStream = null
            mSuccessListener = null
            mFailureListener = null
            true
        }
            .onSuccess { "◉ 关闭串口成功".logger() }
            .onFailure { catch -> "◉ close serial port exception! ${catch.stackTraceToString()}".logger(level = Log.ERROR) }
            .getOrElse { false }
    }

    fun writeBytesWithIOScope(bytes: ByteArray): Boolean {
        if (mFileOutputStream == null) return false
        mIO.launch {
            mFileOutputStream?.let {
                it.write(bytes)
                it.flush()
            }
        }
        return true
    }

    fun writeBytes(bytes: ByteArray): Boolean {
        (mFileOutputStream ?: return false).let {
            it.write(bytes)
            it.flush()
        }
        return true
    }

    fun readBytes(iDataReceive: IDataReceive) {
        val maxReadSize = 548
        mParentReadJob.cancelChildren()
        mReadedContext.launch {
            while (true) {
                if (null != mFileInputStream) {
                    val bytes = ByteArray(maxReadSize)
                    var mReadedBytes = 0
                    repeat(15) {
                        delay(20)
                        if (mFileInputStream!!.available() > 0) {
                            mReadedBytes += withContext(Dispatchers.IO) { mFileInputStream!!.read(bytes, mReadedBytes, bytes.size - mReadedBytes) }
                        }
                    }
                    iDataReceive.onReceive(this, bytes)
                } else {
                    delay(1000)
                }
            }
        }
    }
}
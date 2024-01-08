@file:Suppress("SpellCheckingInspection")

package com.crow.modbus

import com.crow.modbus.tools.error
import com.crow.modbus.tools.info
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.Socket
import java.util.concurrent.Executors

/**
 * ● KModbusTcp
 *
 * ● 2023/12/1 10:50
 * @author crowforkotlin
 * @formatter:on
 */
class KModbusTcp {
    private var mHost: String? = null
    private var mPort: Int? = null
    private var mTcpInitJob: Job? = null
    private var mTcpWriteJob: Job? = null
    private var mOutputJob: Job = SupervisorJob()
    private var mInputJob: Job = SupervisorJob()
    private var mServerSocket: Socket? = null
    private val mIOScope = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->  "TCP Exception ${throwable.stackTraceToString()}".error() })
    private val mOutputScope by lazy { CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mOutputJob + CoroutineExceptionHandler { _, throwable -> throwable.stackTraceToString().error() }) }
    private val mInputScope by lazy { CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mInputJob + CoroutineExceptionHandler { _, throwable -> throwable.stackTraceToString().error() }) }

    /**
     * ● 初始化TCP任务
     *
     * ● 2023-12-05 18:51:14 周二 下午
     * @author crowforkotlin
     */
    private suspend fun onStartTcpServer(ip: String, port: Int) {
        mServerSocket = runCatching { Socket(ip, port) }
            .onFailure { "TCP服务器连接异常！".error() }
            .onSuccess {
                mHost = ip
                mPort = port
            }
            .getOrElse {
                mServerSocket?.close()
                delay(2000)
                return onStartTcpServer(ip, port)
            }
    }

    /**
     * ● TCP 循环读取
     *
     * ● 2023-12-05 18:50:42 周二 下午
     * @author crowforkotlin
     */
    fun onReadRepeat(ins: InputStream, onReceive: suspend (ByteArray) -> Unit) {
        mInputScope.launch {
            val byteReadLen = 6
            while (true) {
                var byteReaded = 0
                val bytesHead = ByteArray(byteReadLen)
                while (byteReaded < byteReadLen) {
                    val bytes = ins.read(bytesHead, byteReaded, byteReadLen)
                    if (bytes == -1) { break }
                    byteReaded += bytes
                }
                val dataLen = bytesHead.last().toInt()
                val bufferLen = byteReadLen + dataLen
                val buffer = ByteArray(bufferLen)
                repeat(byteReadLen) { buffer[it] = bytesHead[it] }
                while (byteReaded < bufferLen) {
                    val bytes = ins.read(buffer, byteReaded, dataLen)
                    if (bytes == -1) { break }
                    byteReaded += bytes
                }
                if (byteReaded == 0) {
                    delay(1000)
                } else {
                    mTcpWriteJob?.cancel()
                    onReceive(buffer)
                }
            }
        }
    }

    /**
     * ● TCP 循环写入
     *
     * ● 2023-12-05 18:47:56 周二 下午
     * @author crowforkotlin
     */
    fun writeRepeat(readInterval: Long, onWrite: suspend () -> ByteArray) {
        mTcpWriteJob?.cancel()
        mTcpWriteJob = null
        mOutputJob.cancelChildren()
        val outputJob = mOutputScope.launch {
            while (true) {
                delay(readInterval)
                mServerSocket?.getOutputStream()?.write(onWrite())
            }
        }
        outputJob.invokeOnCompletion {
            "mOutputJob.invokeOnCompletion".info()
            mTcpInitJob = null
            mServerSocket?.close()
            mServerSocket = null
            if (mTcpInitJob == null) {
                mTcpInitJob = mIOScope.launch {
                    if (mServerSocket == null) {
                        onStartTcpServer(mHost ?: return@launch, mPort ?: return@launch)
                    }
                }
            }
        }
    }

    /**
     * ● 取消所有任务
     *
     * ● 2023-12-05 18:49:53 周二 下午
     * @author crowforkotlin
     */
    fun cancelAllJob() {
        mInputJob.cancel()
        mOutputJob.cancel()
        mTcpWriteJob?.cancel()
        mTcpInitJob?.cancel()
    }
}
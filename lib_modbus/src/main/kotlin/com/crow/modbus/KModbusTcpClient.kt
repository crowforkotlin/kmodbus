@file:Suppress("SpellCheckingInspection", "unused", "LocalVariableName")

package com.crow.modbus

import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusTcpMasterResp
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.baseTenF
import com.crow.modbus.tools.error
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.net.Socket
import java.net.SocketException

/**
 * ⦁  KModbusTcp
 *
 * ⦁  2023/12/1 10:50
 * @author crowforkotlin
 * @formatter:on
 */
class KModbusTcpClient(private val mDispatcher: CoroutineDispatcher = Dispatchers.IO) : KModbus() {

    /**
     * ⦁  事务ID
     *
     * ⦁  2024-01-23 17:57:09 周二 下午
     * @author crowforkotlin
     */
    private var mTransactionId: Int = 0
    private val mProtocol: Int = 0

    private var mHost: String? = null
    private var mPort: Int? = null
    private var mRetryDuration: Long = 2000L
    private var mSuccess: (suspend (ins: BufferedInputStream, ops: BufferedOutputStream) -> Unit)? = null
    private var mRetry: (() -> Unit)? = null
    private var mWriteJob: Job = Job()
    private var mServerSocket: Socket? = null
    private val mClientJob: Job = SupervisorJob()
    private val mClientScope by lazy { CoroutineScope(mDispatcher + mClientJob) }

    private fun readBytes(ins: InputStream, byteReadLen: Int = 6): Pair<Int, ByteArray> {
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
        return byteReaded to buffer
    }
    /**
     * ⦁  TCP 循环读取
     *
     * ⦁  2023-12-05 18:50:42 周二 下午
     * @author crowforkotlin
     */
    private suspend fun repeatMasterActionOnRead(ins: InputStream, onReceive: suspend (ByteArray) -> Unit) {
        onReadRepeatEnv {
            val bytes = readBytes(ins)
            if (bytes.first == 0) {
                delay(1000)
            } else {
                onReceive(bytes.second)
            }
        }
    }

    /**
     * ⦁  循环写入数据任务
     *
     * ⦁  2024-01-10 18:33:35 周三 下午
     * @author crowforkotlin
     */
    suspend fun repeatWriteData(
        ops: BufferedOutputStream,
        interval: Long,
        timeOut: Long,
        timeOutFunc: ((ByteArray) -> Unit)? = null,
        onWrite: () -> List<ByteArray>,
    ) {
        coroutineScope {
            var duration = interval
            while (isActive) {
                runCatching {
                    if (duration < 1) duration = interval else delay(duration)
                    val arrays = onWrite()
                    arrays.forEach { array ->
                        ops.write(array)
                        mWriteJob = launch {
                            delay(timeOut)
                            duration = interval - timeOut
                            timeOutFunc?.invoke(array)
                            mWriteJob.cancel()
                        }
                        mWriteJob.join()
                    }
                }
                    .onFailure { cause ->
                        if (cause is SocketException) {
                            cancel()
                            tcpClient(
                                host = mHost ?: return@onFailure,
                                port = mPort ?: return@onFailure,
                                duration = mRetryDuration,
                                onRetry = mRetry ?: return@onFailure,
                                onSuccess = mSuccess ?: return@onFailure
                            )
                        }
                        "repeatWriteData : ${cause.stackTraceToString()}".error()
                    }
            }
        }
    }


    /**
     * ⦁  启用接受数据的任务
     *
     * ⦁  2024-01-10 18:23:52 周三 下午
     * @author crowforkotlin
     */
    suspend fun repeatReceive(ins: InputStream, kModbusType: KModbusType, onReceive: (ByteArray) -> Unit) {
        when(kModbusType) {
            KModbusType.MASTER -> {
                repeatMasterActionOnRead(ins) { data ->
                    onReceive(data)
                }
            }
            KModbusType.SLAVE -> { kotlin.error("kmodbus tcp client cannot be set as a slave mode!") }
            KModbusType.CUSTOM -> {}
        }
    }

    /**
     * ⦁  启动TCP任务
     *
     * ⦁  2024-01-23 17:30:56 周二 下午
     * @author crowforkotlin
     */
    fun tcpClient(
        host: String,
        port: Int,
        isRetry: Boolean = true,
        duration: Long = mRetryDuration,
        onRetry: (() -> Unit)? = null,
        onSuccess: suspend (ins: BufferedInputStream, ops: BufferedOutputStream) -> Unit
    ) {
        mHost = host
        mPort = port
        mRetryDuration = duration
        mSuccess = onSuccess
        mRetry = onRetry
        mClientScope.launch {
            var socket: Socket? = null
            runCatching {
                val _socket = Socket(host, port)
                socket = _socket
                val ins = BufferedInputStream(_socket.getInputStream())
                val ops = BufferedOutputStream(_socket.getOutputStream())
                onSuccess.invoke(ins, ops)
                _socket.close()
            }
                .onFailure { cause ->
                    "kmodbus tcp client error : ${cause.stackTraceToString()}".error()
                    socket?.close()
                    if (isRetry) {
                        delay(duration)
                        onRetry?.invoke()
                        tcpClient(host, port, true, duration, onRetry, onSuccess)
                    }
                }
        }
    }

    /**
     * ⦁  取消所有任务
     *
     * ⦁  2023-12-05 18:49:53 周二 下午
     * @author crowforkotlin
     */
    fun cancelAll() {
        mClientJob.cancelChildren()
        mServerSocket?.close()
    }

    fun buildMasterOutput(
        function: KModbusFunction,
        slaveAddress: Int,
        startAddress: Int,
        count: Int,
        value: Int? = null,
        values: IntArray? = null,
        transactionId: Int = mTransactionId,
        endian: ModbusEndian = ModbusEndian.ARRAY_BIG_BYTE_BIG,
    ): ByteArray {
        val pdu = buildMasterRequestOutput(slaveAddress, function, startAddress, count, value, values, isTcp = true)
        val size = pdu.size()
        val mbap = BytesOutput()
        mbap.writeInt16(transactionId)
        mbap.writeInt16(mProtocol)
        mbap.writeInt16(size + 1)
        mbap.writeInt8(slaveAddress)
        mbap.write(pdu.toByteArray())
        mTransactionId++
        return getArray(mbap.toByteArray(), endian)
    }

    fun resolveMasterResp(bytes: ByteArray, endian: ModbusEndian): KModbusTcpMasterResp? {
        return runCatching {
            val inputs = getArray(bytes, endian)
            val functionCode= inputs[7].toInt() and 0xFF
            val isFunctionCodeError = (functionCode and baseTenF) + 0x80 == functionCode
            val startIndexOfData = 9
            if(isFunctionCodeError) {
                val dataSize = inputs.size - 11
                val newBytes = ByteArray(dataSize)
                System.arraycopy(inputs, startIndexOfData, newBytes, 0, dataSize)
                KModbusTcpMasterResp(
                    mSlaveID = inputs[6].toInt(),
                    mFunction = functionCode,
                    mByteCount = dataSize,
                    mValues =  newBytes
                )
            } else {
                val byteCount = inputs[8].toInt()
                val newBytes = ByteArray(byteCount)
                System.arraycopy(inputs, startIndexOfData, newBytes, 0, byteCount)
                KModbusTcpMasterResp(
                    mSlaveID = inputs[6].toInt(),
                    mFunction = functionCode,
                    mByteCount = byteCount,
                    mValues =  newBytes
                )
            }
        }
            .onFailure { it.stackTraceToString().error() }
            .getOrElse { null }
    }

    private suspend inline fun onReadRepeatEnv(crossinline onRepat: suspend () -> Unit) {
        coroutineScope {
            while (isActive) {
                runCatching { onRepat() }
                    .onFailure { cause -> cause.stackTraceToString().error() }
            }
        }
    }
}
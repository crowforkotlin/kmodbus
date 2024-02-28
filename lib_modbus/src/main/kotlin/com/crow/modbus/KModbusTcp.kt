@file:Suppress("SpellCheckingInspection", "unused")

package com.listen.x3player.kt.modbus

import com.crow.modbus.KModbus
import com.crow.modbus.interfaces.IKModbusWriteData
import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusTcpMasterResp
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.baseTenF
import com.crow.modbus.tools.error
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.Executors

/**
 * ● KModbusTcp
 *
 * ● 2023/12/1 10:50
 * @author crowforkotlin
 * @formatter:on
 */
class KModbusTcp : KModbus() {

    /**
     * ● 读取监听
     *
     * ● 2024-01-10 18:19:38 周三 下午
     * @author crowforkotlin
     */
    private var mSlaveReadListener: ArrayList<(KModbusTcpMasterResp) -> Unit>? = null
    private var mMasterReadListener: ArrayList<(ByteArray) -> Unit>? = null

    /**
     * ● 写入监听
     *
     * ● 2024-01-10 18:29:07 周三 下午
     * @author crowforkotlin
     */
    private var mWriteListener: IKModbusWriteData? = null

    /**
     * ● 事务ID
     *
     * ● 2024-01-23 17:57:09 周二 下午
     * @author crowforkotlin
     */
    private var mTransactionId: Int = 0
    private val mProtocol: Int = 0

    private var mTcpScopeJob: Job? = null
    private var mHost: String? = null
    private var mPort: Int? = null
    private var mRetryDuration: Long = 2000L
    private var mSuccess: ((ins: InputStream, ops: OutputStream) -> Unit)? = null
    private var mTcpJob: Job = SupervisorJob()
    private var mOutputJob: Job = SupervisorJob()
    private var mInputJob: Job = SupervisorJob()
    private var mWriteJob: Job = Job()
    private var mServerSocket: Socket? = null
    private val mTcpScope by lazy { CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mTcpJob + CoroutineExceptionHandler { _, cause -> cause.stackTraceToString().error() }) }
    private val mOutputScope by lazy { CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mOutputJob + CoroutineExceptionHandler { _, cause -> cause.stackTraceToString().error() }) }
    private val mInputScope by lazy { CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher() + mInputJob + CoroutineExceptionHandler { _, cause -> cause.stackTraceToString().error() }) }

    /**
     * ● TCP 循环读取
     *
     * ● 2023-12-05 18:50:42 周二 下午
     * @author crowforkotlin
     */
    private fun repeatMasterActionOnRead(ins: InputStream, onReceive: suspend (ByteArray) -> Unit) {
        mInputJob.cancelChildren()
        mInputScope.launch {
            val byteReadLen = 6
            while (isActive) {
                runCatching {
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
                        onReceive(buffer)
                    }
                }
                    .onFailure { cause ->
                        if (cause is SocketException) {
                            cancel()
                            startTcp(mHost ?: return@onFailure, mPort ?: return@onFailure, duration = mRetryDuration, onSuccess = mSuccess ?: return@onFailure)
                        }
                        "repeatMasterActionOnRead : ${cause.stackTraceToString()}".error()
                    }
            }
        }
    }

    /**
     * ● 循环写入数据任务
     *
     * ● 2024-01-10 18:33:35 周三 下午
     * @author crowforkotlin
     */
    fun startRepeatWriteDataTask(ops: OutputStream, interval: Long, timeOut: Long, timeOutFunc: ((ByteArray) -> Unit)? = null) {
        mOutputJob.cancelChildren()
        mOutputScope.launch {
            var duration = interval
            while (isActive) {
                runCatching {
                    if (duration < 1) duration = interval else delay(duration)
                    val arrays = mWriteListener?.onWrite() ?: return@runCatching
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
                            startTcp(mHost ?: return@onFailure, mPort ?: return@onFailure, duration = mRetryDuration, onSuccess = mSuccess ?: return@onFailure)
                        }
                        "startRepeatWriteDataTask : ${cause.stackTraceToString()}".error()
                    }
            }
        }
    }


    /**
     * ● 启用接受数据的任务
     *
     * ● 2024-01-10 18:23:52 周三 下午
     * @author crowforkotlin
     */
    fun startRepeatReceiveDataTask(ins: InputStream, kModbusType: KModbusType) {
        when(kModbusType) {
            KModbusType.MASTER -> {
                repeatMasterActionOnRead(ins) { data ->
                    mWriteJob.cancel()
                    mMasterReadListener?.forEach { it.invoke(data) }
                }
            }
            KModbusType.SLAVE -> {}
            KModbusType.CUSTOM -> {}
        }
    }
    fun setOnDataWriteReadyListener (listener: IKModbusWriteData) { mWriteListener = listener }
    fun addOnSlaveReceiveListener(listener: (KModbusTcpMasterResp) -> Unit) {
        if (mSlaveReadListener == null) {
            mSlaveReadListener = arrayListOf()
        }
        mSlaveReadListener?.add(listener)
    }
    fun addOnMasterReceiveListener(listener: (ByteArray) -> Unit) {
        if (mMasterReadListener == null) {
            mMasterReadListener = arrayListOf()
        }
        mMasterReadListener?.add(listener)
    }
    fun removeSlaveReceiveListener(listener: (KModbusTcpMasterResp) -> Unit) { mSlaveReadListener?.remove(listener) }
    fun removeMasterReceiveListener(listener: (ByteArray) -> Unit) { mMasterReadListener?.remove(listener) }
    fun removeAllSlaveReceiveListener() { mSlaveReadListener?.clear() }
    fun removeAllMasterReceiveListener() { mMasterReadListener?.clear() }
    fun removeOnWriteDataReadyListener() { mWriteListener = null }

    /**
     * ● 启动TCP任务
     *
     * ● 2024-01-23 17:30:56 周二 下午
     * @author crowforkotlin
     */
    fun startTcp(host: String, port: Int, retry: Boolean = true, duration: Long = mRetryDuration, onSuccess: ((ins: InputStream, ops: OutputStream) -> Unit)? = null) {
        mHost = host
        mPort = port
        mRetryDuration = duration
        mSuccess = onSuccess
        tcpScope {
            runCatching {
                mServerSocket = Socket(host, port)
                mServerSocket ?: kotlin.error("Socket connection exception!")
            }
                .onFailure { cause ->
                    "Tcp retry again --> $retry \t ${cause.message}".error()
                    if (retry) {
                        delay(duration)
                        startTcp(host, port, true, duration, onSuccess)
                    }
                }
                .onSuccess { socket ->
                    onSuccess?.invoke(socket.getInputStream(), socket.getOutputStream())
                }
        }
    }

    /**
     * ● 取消所有任务
     *
     * ● 2023-12-05 18:49:53 周二 下午
     * @author crowforkotlin
     */
    fun cancelAll() {
        mInputJob.cancelChildren()
        mOutputJob.cancelChildren()
        mTcpJob.cancelChildren()
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

    private fun getOps() = runCatching { mServerSocket?.getOutputStream() }.getOrElse { null }
    private fun getIns() = runCatching { mServerSocket?.getInputStream() }.getOrElse { null }
    fun tcpScope(scope: suspend () -> Unit) {
        mTcpJob.cancelChildren()
        mTcpScope.launch { scope() }
    }
}
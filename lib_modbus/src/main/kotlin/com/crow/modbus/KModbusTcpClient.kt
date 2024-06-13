@file:Suppress("SpellCheckingInspection", "unused", "LocalVariableName")

package com.crow.modbus

import com.crow.modbus.model.KModbusFunction
import com.crow.modbus.model.KModbusRetryCancellationException
import com.crow.modbus.model.KModbusTcpMasterResp
import com.crow.modbus.model.KModbusType
import com.crow.modbus.model.ModbusEndian
import com.crow.modbus.model.getKModbusFunction
import com.crow.modbus.tools.BytesOutput
import com.crow.modbus.tools.baseTenF
import com.crow.modbus.tools.error
import com.crow.modbus.tools.info
import com.crow.modbus.tools.message
import com.crow.modbus.tools.readBytes
import com.crow.modbus.tools.toHexList
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
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
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

    data class ClientInfo(
        val mHost: String,
        val mPort: Int,
        val mAwaitTime: Long = Long.MAX_VALUE,
        val mRetryEnable: Boolean = true,
        val mRetryDuration: Long = 2000L,
        val mRetry: (() -> Unit)? = null,
        val mSuccess: (suspend KModbusTcpClient.(ins: BufferedInputStream, ops: BufferedOutputStream, clientInfo: ClientInfo, job: Job) -> List<Job>)? = null
    )

    /**
     * ⦁  事务ID
     *
     * ⦁  2024-01-23 17:57:09 周二 下午
     * @author crowforkotlin
     */
    private var mTransactionId: Short = 32767
    private val mProtocol: Int = 0

    private var mWriteJob: Job = Job()
    private val mClientJob: Job = SupervisorJob()
    private val mClientScope by lazy { CoroutineScope(mDispatcher + mClientJob) }

    /**
     * ⦁ 读取Modbus主站响应内容
     *
     * ⦁ 2024-04-09 14:26:13 周二 下午
     * @author crowforkotlin
     */
    private fun getModbusMasterResp(ins: InputStream) : KModbusTcpMasterResp {
        val byteReadLen = 6
        var byteReaded = 0
        val mbap = ByteArray(byteReadLen)
        while (byteReaded < byteReadLen) {
            val bytes = ins.read(mbap, byteReaded, byteReadLen)
            if (bytes == -1) {
                break
            }
            byteReaded += bytes
        }
        val dataLen = mbap.last().toInt()
        val pdu = ins.readBytes(dataLen)
        if (pdu.isEmpty()) { ins.close() }
        val slaveId = pdu[0].toInt() and 0xFF
        val slaveResp = when (val function = getKModbusFunction(pdu[1].toInt())) {
            KModbusFunction.READ_COILS, KModbusFunction.READ_HOLDING_REGISTERS, KModbusFunction.READ_INPUT_REGISTERS, KModbusFunction.READ_DISCRETE_INPUTS -> {
                val byteCount = pdu[2].toInt()
                val values = ByteArray(byteCount)
                repeat(byteCount) { values[it] = pdu[3 + it] }
                KModbusTcpMasterResp(
                    mSlaveID = slaveId,
                    mFunction = function,
                    mByteCount = byteCount,
                    mValues = values
                )
            }
            else -> { kotlin.error("kmodbus tcp client get unknow function!") }
        }
        return slaveResp
    }

    /**
     * ⦁  TCP 循环读取
     *
     * ⦁  2023-12-05 18:50:42 周二 下午
     * @author crowforkotlin
     */
    private suspend fun repeatMasterActionOnRead(clientInfo: ClientInfo, clientJob: Job, ins: InputStream, onReceive: suspend (KModbusTcpMasterResp) -> Unit) {
        coroutineScope {
            while (isActive) {
                runCatching { onReceive(getModbusMasterResp(ins)) }
                    .onFailure { cause ->
                        "kmodbus tcp client repeat receive data error : ${cause.message()}".error()
                        if (clientInfo.mRetryEnable && (cause is SocketException || cause is IOException)) {
                            delay(clientInfo.mRetryDuration)
                            clientInfo.mRetry?.invoke()
                            cancel()
                            clientJob.cancel(KModbusRetryCancellationException())
                        }
                    }
            }
        }
    }

    /**
     * ⦁  循环写入数据
     *
     * ⦁  2024-01-10 18:33:35 周三 下午
     * @author crowforkotlin
     */
    fun continuouslyWriteData(
        clientInfo: ClientInfo,
        clientJob: Job,
        ops: BufferedOutputStream,
        interval: Long,
        timeOut: Long,
        timeOutFunc: ((ByteArray) -> Unit)? = null,
        onWrite: () -> List<ByteArray>,
    ): Job {
        return mClientScope.launch {
            var duration = interval
            while (isActive) {
                runCatching {
                    if (duration < 1) duration = interval else delay(duration)
                    val arrays = onWrite()
                    arrays.forEach { array ->
                        "kmodbus tcp client write data : ${array.toHexList()}".info()
                        ops.write(array)
                        ops.flush()
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
                        "kmodbus tcp client loop write data error : ${cause.message()}".error()
                        if (clientInfo.mRetryEnable && (cause is SocketException || cause is IOException)) {
                            delay(clientInfo.mRetryDuration)
                            clientInfo.mRetry?.invoke()
                            cancel()
                            clientJob.cancel(KModbusRetryCancellationException())
                        }
                    }
            }
        }
    }

    /**
     * ⦁ 只写入一次数据
     *
     * ⦁ 2024-04-09 14:36:24 周二 下午
     * @author crowforkotlin
     */
    fun writeData(
        clientInfo: ClientInfo,
        clientJob: Job,
        ops: BufferedOutputStream,
        data: ByteArray,
    ): Job {
        return mClientScope.launch {
            runCatching {
                ops.write(data)
                ops.flush()
            }
                .onFailure { cause ->
                    "kmodbus tcp client writedata error : ${cause.message()}".error()
                    if (clientInfo.mRetryEnable && (cause is SocketException || cause is IOException)) {
                        delay(clientInfo.mRetryDuration)
                        clientInfo.mRetry?.invoke()
                        cancel()
                        clientJob.cancel(KModbusRetryCancellationException())
                    }
                }
        }
    }

    /**
     * ⦁  循环读取数据
     *
     * ⦁  2024-01-10 18:23:52 周三 下午
     * @author crowforkotlin
     */
    fun continuouslyReadData(clientInfo: ClientInfo, clientJob: Job, ins: InputStream, kModbusType: KModbusType, onReceive: (KModbusTcpMasterResp) -> Unit): Job {
        return mClientScope.launch {
            when (kModbusType) {
                    KModbusType.MASTER -> {
                        repeatMasterActionOnRead(clientInfo, clientJob, ins) { data ->
                            "kmodbus tcp client receive data : $data".info()
                            mWriteJob.cancel()
                            onReceive(data)
                        }
                    }

                    KModbusType.SLAVE -> {
                        kotlin.error("kmodbus tcp client cannot be set as a slave mode!")
                    }

                    KModbusType.CUSTOM -> {}
                }
        }
    }

    /**
     * ⦁  仅读取一次
     *
     * ⦁  2024-01-10 18:23:52 周三 下午
     * @author crowforkotlin
     */
    fun receiveData(clientInfo: ClientInfo, clientJob: Job, ins: InputStream, kModbusType: KModbusType, onReceive: (KModbusTcpMasterResp) -> Unit): Job {
        return mClientScope.launch {
            runCatching {
                when (kModbusType) {
                    KModbusType.MASTER -> { onReceive(getModbusMasterResp(ins)) }
                    KModbusType.SLAVE -> { kotlin.error("kmodbus tcp client cannot be set as a slave mode!") }
                    KModbusType.CUSTOM -> {}
                }
            }
                .onFailure { cause ->
                    "kmodbus tcp client receive data error : ${cause.message()}".error()
                    if (clientInfo.mRetryEnable) {
                        delay(clientInfo.mRetryDuration)
                        clientInfo.mRetry?.invoke()
                        cancel()
                        clientJob.cancel(KModbusRetryCancellationException())
                    }
                }
        }
    }

    /**
     * ⦁  启动TCP任务
     *
     * ⦁  2024-01-23 17:30:56 周二 下午
     * @author crowforkotlin
     */
    fun tcpClient(clientInfo: ClientInfo): Job {
        var socket: Socket? = null
        var ins: BufferedInputStream? = null
        var ops: BufferedOutputStream? = null
        var jobs: List<Job>? = null
        clientInfo.apply {
            return mClientScope.launch {
                runCatching {
                    val _socket = Socket(mHost, mPort)
                    "kmodbus tcp client connect! socke connected : ${_socket.isConnected}".info()
                    socket = _socket
                    val _ins = BufferedInputStream(_socket.getInputStream())
                    ins = _ins
                    val _ops = BufferedOutputStream(_socket.getOutputStream())
                    ops = _ops
                    val _jobs = mSuccess?.invoke(this@KModbusTcpClient, _ins, _ops, clientInfo, coroutineContext.job)
                    jobs = _jobs
                    if (mAwaitTime > 0) { delay(mAwaitTime) }
                    _jobs?.forEach { it.cancel() }
                    _socket.close()
                }
                    .onFailure { cause ->
                        "kmodbus tcp client error : ${cause.message()}".error()
                        runCatching {
                            ins?.close()
                            ops?.close()
                            socket?.close()
                        }
                        jobs?.forEach { it.cancel() }
                        if (mRetryEnable) {
                            if (cause !is KModbusRetryCancellationException) { delay(mRetryDuration) }
                            mRetry?.invoke()
                            cancel()
                            tcpClient(clientInfo)
                        }
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
    }

    /**
     * ⦁ 构建主站数据
     *
     * ⦁ 2024-04-09 14:27:45 周二 下午
     * @author crowforkotlin
     */
    fun buildMasterOutput(
        function: KModbusFunction,
        slaveAddress: Int,
        startAddress: Int,
        count: Int,
        value: Int? = null,
        values: IntArray? = null,
        transactionId: Short = mTransactionId,
        endian: ModbusEndian = ModbusEndian.ABCD,
        isBase1: Boolean = false
    ): ByteArray {
        val pdu = buildMasterRequestOutput(slaveAddress, function, if(isBase1) startAddress - 1 else startAddress, count, value, values, isTcp = true)
        val size = pdu.size()
        val mbap = BytesOutput()
        mbap.writeInt16(transactionId.toInt())
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
                    mFunction = getKModbusFunction(functionCode),
                    mByteCount = dataSize,
                    mValues =  newBytes
                )
            } else {
                val byteCount = inputs[8].toInt()
                val newBytes = ByteArray(byteCount)
                System.arraycopy(inputs, startIndexOfData, newBytes, 0, byteCount)
                KModbusTcpMasterResp(
                    mSlaveID = inputs[6].toInt(),
                    mFunction = getKModbusFunction(functionCode),
                    mByteCount = byteCount,
                    mValues =  newBytes
                )
            }
        }
            .onFailure { it.stackTraceToString().error() }
            .getOrElse { null }
    }
}